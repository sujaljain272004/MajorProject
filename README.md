# ChargeUp

ChargeUp is a production-style EV Charging Booking System with:
- Spring Boot backend
- MySQL database
- React frontend
- Razorpay test-mode payments
- WebSocket slot availability updates

## Project Layout

```text
backend/   Spring Boot REST API, JWT auth, JPA, WebSocket, Razorpay integration
frontend/  React + Axios + Vite single-page app
sql/       Database schema
docs/      API documentation
```

## Core Features

- Driver registration/login with JWT authentication
- Nearby station dashboard with dummy map coordinates
- Real-time slot availability updates
- Concurrent-safe slot booking
- Booking cancellation
- Razorpay test payment flow
- Driver booking history
- Owner station CRUD
- Owner slot CRUD
- Owner bookings and revenue dashboard

## Concurrency Handling

The booking flow is protected in `BookingService` using a database transaction and `PESSIMISTIC_WRITE` row locking on the slot lookup.

Result:
- two users can request the same slot at the same time
- only the first transaction can lock and mark the slot unavailable
- the second request receives a "Slot is already booked" error

## Backend Setup

1. Make sure MySQL is running.
2. Create the database with `sql/schema.sql`, or let Spring Boot auto-create/update tables.
3. Confirm the credentials in [backend/src/main/resources/application.properties](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/backend/src/main/resources/application.properties).
4. Install Maven locally if it is not already installed.
5. Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`.

Seeded accounts:
- Owner: `owner@chargeup.com` / `owner123`
- Driver: `driver@chargeup.com` / `driver123`

## Frontend Setup

1. Install frontend dependencies:

```bash
cd frontend
npm install
```

2. Copy `.env.example` to `.env` if you want to override defaults.
3. Start the frontend:

```bash
npm run dev
```

Frontend runs on `http://localhost:5173`.

## Razorpay Test Configuration

The backend is already configured with the provided test keys in [backend/src/main/resources/application.properties](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/backend/src/main/resources/application.properties).

## Key Backend Areas

- Auth and JWT: [backend/src/main/java/com/chargeup/controller/AuthController.java](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/backend/src/main/java/com/chargeup/controller/AuthController.java)
- Station CRUD and owner dashboard: [backend/src/main/java/com/chargeup/service/StationService.java](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/backend/src/main/java/com/chargeup/service/StationService.java)
- Concurrent booking logic: [backend/src/main/java/com/chargeup/service/BookingService.java](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/backend/src/main/java/com/chargeup/service/BookingService.java)
- Razorpay integration: [backend/src/main/java/com/chargeup/service/PaymentService.java](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/backend/src/main/java/com/chargeup/service/PaymentService.java)
- WebSocket updates: [backend/src/main/java/com/chargeup/service/SlotBroadcastService.java](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/backend/src/main/java/com/chargeup/service/SlotBroadcastService.java)

## Frontend Pages

- Login/Register: [frontend/src/pages/LoginPage.jsx](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/frontend/src/pages/LoginPage.jsx)
- Dashboard: [frontend/src/pages/DashboardPage.jsx](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/frontend/src/pages/DashboardPage.jsx)
- Station detail with live slots: [frontend/src/pages/StationDetailPage.jsx](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/frontend/src/pages/StationDetailPage.jsx)
- Booking: [frontend/src/pages/BookingPage.jsx](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/frontend/src/pages/BookingPage.jsx)
- Payment: [frontend/src/pages/PaymentPage.jsx](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/frontend/src/pages/PaymentPage.jsx)
- User bookings: [frontend/src/pages/UserBookingsPage.jsx](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/frontend/src/pages/UserBookingsPage.jsx)
- Owner admin dashboard: [frontend/src/pages/AdminDashboardPage.jsx](/mnt/c/Users/sujal/OneDrive/Documents/Desktop/MajorProject/frontend/src/pages/AdminDashboardPage.jsx)
