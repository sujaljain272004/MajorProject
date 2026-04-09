package com.chargeup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chargeup.config.RazorpayProperties;
import com.chargeup.dto.payment.CreateOrderRequest;
import com.chargeup.dto.payment.OrderResponse;
import com.chargeup.dto.payment.PaymentResponse;
import com.chargeup.dto.payment.PaymentVerifyRequest;
import com.chargeup.entity.BookingStatus;
import com.chargeup.entity.Payment;
import com.chargeup.entity.PaymentStatus;
import com.chargeup.exception.BadRequestException;
import com.chargeup.exception.UnauthorizedException;
import com.chargeup.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final MappingService mappingService;
    private final RazorpayProperties razorpayProperties;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PaymentService(
        PaymentRepository paymentRepository,
        BookingService bookingService,
        MappingService mappingService,
        RazorpayProperties razorpayProperties,
        CurrentUserService currentUserService,
        ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.mappingService = mappingService;
        this.razorpayProperties = razorpayProperties;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        var booking = bookingService.getOwnedBookingEntity(request.bookingId());
        ensureBookingOwner(booking);

        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BadRequestException("Cannot pay for a canceled booking");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking is already paid");
        }

        BigDecimal amount = mappingService.slotPrice(booking);
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseGet(() -> {
            var created = new Payment();
            created.setBooking(booking);
            created.setAmount(amount);
            created.setStatus(PaymentStatus.CREATED);
            return created;
        });
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Payment is already completed for this booking");
        }

        String orderId = createRazorpayOrder(amount);
        payment.setRazorpayOrderId(orderId);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.CREATED);
        payment = paymentRepository.save(payment);

        return new OrderResponse(
            booking.getId(),
            payment.getRazorpayOrderId(),
            razorpayProperties.key().id(),
            amount,
            razorpayProperties.currency()
        );
    }

    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        var booking = bookingService.getOwnedBookingEntity(request.bookingId());
        ensureBookingOwner(booking);
        var payment = paymentRepository.findByBookingId(booking.getId())
            .orElseThrow(() -> new BadRequestException("Payment order not found"));

        if (!payment.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new BadRequestException("Razorpay order ID mismatch");
        }

        if (!isSignatureValid(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Invalid Razorpay signature");
        }

        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setStatus(PaymentStatus.PAID);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(request.razorpayPaymentId());

        return mappingService.toPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getBookingPayment(Long bookingId) {
        var booking = bookingService.getOwnedBookingEntity(bookingId);
        ensureBookingOwner(booking);
        return mappingService.toPaymentResponse(
            paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BadRequestException("Payment not found"))
        );
    }

    private void ensureBookingOwner(com.chargeup.entity.Booking booking) {
        if (!booking.getUser().getId().equals(currentUserService.getCurrentUser().getId())) {
            throw new UnauthorizedException("Only the booking owner can access payment actions");
        }
    }

    private String createRazorpayOrder(BigDecimal amount) {
        try {
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

            String requestBody = """
                {
                  "amount": %d,
                  "currency": "%s",
                  "receipt": "chargeup-%d",
                  "payment_capture": 1
                }
                """.formatted(amountInPaise, razorpayProperties.currency(), System.currentTimeMillis());

            String credentials = razorpayProperties.key().id() + ":" + razorpayProperties.key().secret();
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BadRequestException("Unable to create Razorpay order: " + response.body());
            }

            return objectMapper.readTree(response.body()).path("id").asText();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Razorpay order creation failed: " + ex.getMessage());
        }
    }

    private boolean isSignatureValid(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac sha256 = Mac.getInstance("HmacSHA256");
            sha256.init(new SecretKeySpec(razorpayProperties.key().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = sha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).equals(signature);
        } catch (Exception ex) {
            throw new BadRequestException("Payment signature verification failed");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
