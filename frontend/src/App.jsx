import { Navigate, Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import { useAuth } from "./hooks/useAuth";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import BookingPage from "./pages/BookingPage";
import DashboardPage from "./pages/DashboardPage";
import LoginPage from "./pages/LoginPage";
import PaymentPage from "./pages/PaymentPage";
import StationDetailPage from "./pages/StationDetailPage";
import UserBookingsPage from "./pages/UserBookingsPage";

export default function App() {
  const { isAuthenticated, user } = useAuth();

  return (
    <div className="app-shell">
      <Navbar />
      <main className="page-shell">
        <Routes>
          <Route
            path="/login"
            element={isAuthenticated ? <Navigate to={user?.role === "OWNER" ? "/admin" : "/"} replace /> : <LoginPage />}
          />
          <Route
            path="/"
            element={(
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/stations/:stationId"
            element={(
              <ProtectedRoute>
                <StationDetailPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/booking/:slotId"
            element={(
              <ProtectedRoute role="DRIVER">
                <BookingPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/payment/:bookingId"
            element={(
              <ProtectedRoute role="DRIVER">
                <PaymentPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/bookings"
            element={(
              <ProtectedRoute role="DRIVER">
                <UserBookingsPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin"
            element={(
              <ProtectedRoute role="OWNER">
                <AdminDashboardPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="*"
            element={<Navigate to={isAuthenticated ? (user?.role === "OWNER" ? "/admin" : "/") : "/login"} replace />}
          />
        </Routes>
      </main>
    </div>
  );
}
