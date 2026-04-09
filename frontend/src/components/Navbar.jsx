import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <header className="navbar">
      <Link to="/" className="brand">
        ChargeUp
      </Link>

      <nav className="nav-links">
        <NavLink to="/">Dashboard</NavLink>
        {user?.role === "DRIVER" && <NavLink to="/bookings">My Bookings</NavLink>}
        {user?.role === "OWNER" && <NavLink to="/admin">Admin Portal</NavLink>}
      </nav>

      <div className="nav-actions">
        <span className="user-chip">
          {user?.name} · {user?.role}
        </span>
        <button className="secondary-button" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </header>
  );
}
