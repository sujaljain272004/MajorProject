package com.chargeup.controller;

import com.chargeup.dto.payment.CreateOrderRequest;
import com.chargeup.dto.payment.OrderResponse;
import com.chargeup.dto.payment.PaymentResponse;
import com.chargeup.dto.payment.PaymentVerifyRequest;
import com.chargeup.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/order")
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return paymentService.createOrder(request);
    }

    @PostMapping("/verify")
    public PaymentResponse verifyPayment(@Valid @RequestBody PaymentVerifyRequest request) {
        return paymentService.verifyPayment(request);
    }

    @GetMapping("/booking/{bookingId}")
    public PaymentResponse getBookingPayment(@PathVariable Long bookingId) {
        return paymentService.getBookingPayment(bookingId);
    }
}
