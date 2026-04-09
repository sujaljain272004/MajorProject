# ChargeUp API Documentation

Base URL: `http://localhost:8080/api`

Authentication:
- JWT bearer token required for all endpoints except register/login.
- Include `Authorization: Bearer <token>`.

## Auth

### POST `/auth/register`
Register as `DRIVER` or `OWNER`.

Request:
```json
{
  "name": "Sujal",
  "email": "sujal@example.com",
  "password": "secret123",
  "role": "DRIVER"
}
```

### POST `/auth/login`
```json
{
  "email": "driver@chargeup.com",
  "password": "driver123"
}
```

## Stations

### GET `/stations`
List all charging stations with slot counts.

### GET `/stations/{stationId}`
Get one station.

### GET `/stations/owner`
Owner-only list of owned stations.

### GET `/stations/owner/dashboard`
Owner dashboard summary with stations, recent bookings, and revenue.

### POST `/stations`
Owner-only create station.
```json
{
  "name": "ChargeUp Plaza",
  "location": "Indiranagar, Bengaluru",
  "latitude": 12.9784,
  "longitude": 77.6408
}
```

### PUT `/stations/{stationId}`
Owner-only update station.

### DELETE `/stations/{stationId}`
Owner-only delete station.

## Slots

### GET `/stations/{stationId}/slots`
List all slots for a station.

### GET `/slots/{slotId}`
Fetch one slot for booking confirmation.

### POST `/stations/{stationId}/slots`
Owner-only create slot.
```json
{
  "startTime": "2026-04-10T09:00:00",
  "endTime": "2026-04-10T10:00:00",
  "price": 499.00
}
```

### PUT `/slots/{slotId}`
Owner-only update slot.

### DELETE `/slots/{slotId}`
Owner-only delete slot.

## Bookings

### POST `/bookings`
Driver-only create booking.
```json
{
  "slotId": 1
}
```

Concurrency note:
- Booking uses a pessimistic row lock on the slot record.
- If two requests hit the same slot, only one transaction can mark it unavailable and create the booking.

### POST `/bookings/{bookingId}/cancel`
Cancel booking and free the slot.

### GET `/bookings`
Driver booking history.

### GET `/bookings/{bookingId}`
Get one booking owned by the logged-in driver or station owner.

### GET `/bookings/owner/all`
Owner-only bookings across owned stations.

## Payments

### POST `/payments/order`
Create Razorpay order.
```json
{
  "bookingId": 1
}
```

### POST `/payments/verify`
Verify Razorpay signature and confirm booking payment.
```json
{
  "bookingId": 1,
  "razorpayOrderId": "order_xxx",
  "razorpayPaymentId": "pay_xxx",
  "razorpaySignature": "signature_xxx"
}
```

### GET `/payments/booking/{bookingId}`
Get payment state for a booking.

## WebSocket

Endpoint: `http://localhost:8080/ws`

Topic subscription:
- `/topic/stations/{stationId}/slots`

Payload:
- Array of slot DTOs for the station.
- Frontend uses this to disable booked slots instantly.
