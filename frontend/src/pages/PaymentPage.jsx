import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import LoadingSpinner from "../components/LoadingSpinner";
import { getBooking } from "../services/bookingService";
import { createOrder, getBookingPayment, verifyPayment } from "../services/paymentService";

function formatDateTime(value) {
  return new Date(value).toLocaleString();
}

function loadRazorpayScript() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }

    const existing = document.querySelector('script[data-chargeup-razorpay="true"]');
    if (existing) {
      existing.addEventListener("load", () => resolve(true), { once: true });
      existing.addEventListener("error", () => reject(new Error("Razorpay checkout script failed to load")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.dataset.chargeupRazorpay = "true";
    script.onload = () => resolve(true);
    script.onerror = () => reject(new Error("Razorpay checkout script failed to load"));
    document.body.appendChild(script);
  });
}

export default function PaymentPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        const bookingData = await getBooking(bookingId);
        setBooking(bookingData);

        try {
          const paymentData = await getBookingPayment(bookingId);
          setPayment(paymentData);
        } catch (ignored) {
          setPayment(null);
        }
      } catch (err) {
        setError(err.response?.data?.message || "Unable to load payment page");
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [bookingId]);

  const handlePayment = async () => {
    try {
      setPaying(true);
      setError("");
      await loadRazorpayScript();
      const order = await createOrder(Number(bookingId));

      const razorpay = new window.Razorpay({
        key: order.key,
        amount: Number(order.amount) * 100,
        currency: order.currency,
        name: "ChargeUp",
        description: `Charging slot at ${booking.stationName}`,
        order_id: order.razorpayOrderId,
        handler: async (response) => {
          try {
            const verification = await verifyPayment({
              bookingId: Number(bookingId),
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature
            });
            setPayment(verification);
            setSuccess("Payment completed successfully.");
            navigate("/bookings");
          } catch (err) {
            setError(err.response?.data?.message || "Payment verification failed");
          }
        },
        prefill: {},
        theme: { color: "#1d4ed8" }
      });

      razorpay.on("payment.failed", (response) => {
        setError(response.error?.description || "Payment failed");
      });

      razorpay.open();
    } catch (err) {
      setError(err.response?.data?.message || "Unable to create payment order");
    } finally {
      setPaying(false);
    }
  };

  if (loading) {
    return <LoadingSpinner label="Preparing payment..." />;
  }

  if (!booking) {
    return <div className="error-banner">Booking not found.</div>;
  }

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <span className="eyebrow">Payment</span>
          <h1>Complete your booking</h1>
          <p>Secure test-mode Razorpay checkout for the reserved charging slot.</p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {success && <div className="success-banner">{success}</div>}

      <article className="booking-summary">
        <div>
          <span>Booking ID</span>
          <strong>#{booking.id}</strong>
        </div>
        <div>
          <span>Station</span>
          <strong>{booking.stationName}</strong>
        </div>
        <div>
          <span>Session</span>
          <strong>{formatDateTime(booking.startTime)}</strong>
        </div>
        <div>
          <span>Amount</span>
          <strong>INR {booking.amount}</strong>
        </div>
        <div>
          <span>Booking Status</span>
          <strong>{booking.status}</strong>
        </div>
        <div>
          <span>Payment Status</span>
          <strong>{payment?.status || booking.paymentStatus || "NOT_CREATED"}</strong>
        </div>
      </article>

      <button className="primary-button fit-button" disabled={paying || booking.status !== "RESERVED"} onClick={handlePayment}>
        {paying ? "Launching Razorpay..." : booking.status === "RESERVED" ? "Pay with Razorpay" : "Payment Closed"}
      </button>
    </section>
  );
}
