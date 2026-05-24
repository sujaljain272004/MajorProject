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
Owner/admin network list of active verified public stations with slot counts. Driver clients use nearby search.

### GET `/stations/nearby`
Driver discovery endpoint. Provide live GPS coordinates or a city/pincode fallback.

Query examples:
```text
/stations/nearby?latitude=12.9716&longitude=77.5946&radiusKm=10&availableOnly=true
/stations/nearby?city=Bengaluru&pincode=560001&connectorType=CCS2&fastCharging=true&maxPrice=25
```

Notes:
- Radius is restricted to 5, 10, or 25 km.
- GPS searches calculate distance with Haversine and sort nearest first.
- Results exclude paused, pending, and rejected stations.

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
  "city": "Bengaluru",
  "pincode": "560038",
  "latitude": 12.9784,
  "longitude": 77.6408,
  "chargerType": "DC Fast",
  "connectorType": "CCS2",
  "chargingSpeedKw": 120,
  "slotCount": 6,
  "pricePerKwh": 21.50,
  "openingHours": "24 hours"
}
```

New and materially edited stations become `PENDING` until admin verification.

### POST `/stations/{stationId}/photos`
Owner-only multipart station photo upload. Use `file` with JPEG, PNG, or WebP up to 5MB.

### POST `/stations/{stationId}/status?status=PAUSED`
Owner-only pause or resume with `PAUSED` or `ACTIVE`.

### POST `/stations/{stationId}/verification?verified=true`
Admin-only station verification decision.

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
- If two requests hit the same slot, only one transaction can reserve it.
- Booking creation moves a slot from `AVAILABLE` to `RESERVED` for a 10-minute payment hold.
- Verified payment moves the booking and slot to `BOOKED`.
- A scheduled lifecycle job releases expired reservations and paid bookings that miss the arrival grace period.

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
- Slot state is one of `AVAILABLE`, `RESERVED`, `BOOKED`, `CHARGING`, `COMPLETED`, `CANCELLED`.
- Frontend uses this to disable unavailable slots instantly.
