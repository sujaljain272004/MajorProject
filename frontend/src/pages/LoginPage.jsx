import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { login, register } from "../services/authService";

const initialRegister = {
  name: "",
  email: "",
  password: "",
  role: "DRIVER"
};

const initialLogin = {
  email: "",
  password: ""
};

export default function LoginPage() {
  const [mode, setMode] = useState("login");
  const [loginForm, setLoginForm] = useState(initialLogin);
  const [registerForm, setRegisterForm] = useState(initialRegister);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { loginSession } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      const payload = mode === "login"
        ? {
            email: loginForm.email.trim(),
            password: loginForm.password
          }
        : {
            ...registerForm,
            name: registerForm.name.trim(),
            email: registerForm.email.trim()
          };

      const authResponse = mode === "login" ? await login(payload) : await register(payload);
      loginSession(authResponse);
      navigate(authResponse.user.role === "OWNER" ? "/admin" : "/");
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="auth-layout">
      <div className="hero-panel">
        <span className="eyebrow">ChargeUp EV Network</span>
        <h1>Book charging slots in real time and manage stations without overbooking.</h1>
        <p>
          Drivers can find stations, lock an available slot, and pay securely.
          Owners get station management, live occupancy updates, and revenue tracking in one portal.
        </p>
        <div className="credential-card">
          <strong>Seeded demo accounts</strong>
          <p>Owner: owner@chargeup.com / owner123</p>
          <p>Driver: driver@chargeup.com / driver123</p>
        </div>
      </div>

      <div className="auth-card">
        <div className="segmented-control">
          <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>
            Login
          </button>
          <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>
            Register
          </button>
        </div>

        <form className="form-grid" onSubmit={handleSubmit}>
          {mode === "register" && (
            <>
              <label>
                Full Name
                <input
                  required
                  value={registerForm.name}
                  onChange={(event) => setRegisterForm((current) => ({ ...current, name: event.target.value }))}
                />
              </label>
              <label>
                Role
                <select
                  value={registerForm.role}
                  onChange={(event) => setRegisterForm((current) => ({ ...current, role: event.target.value }))}
                >
                  <option value="DRIVER">Driver</option>
                  <option value="OWNER">Station Owner</option>
                </select>
              </label>
            </>
          )}

          <label>
            Email
            <input
              required
              type="email"
              value={mode === "login" ? loginForm.email : registerForm.email}
              onChange={(event) => (
                mode === "login"
                  ? setLoginForm((current) => ({ ...current, email: event.target.value }))
                  : setRegisterForm((current) => ({ ...current, email: event.target.value }))
              )}
            />
          </label>

          <label>
            Password
            <input
              required
              type="password"
              value={mode === "login" ? loginForm.password : registerForm.password}
              onChange={(event) => (
                mode === "login"
                  ? setLoginForm((current) => ({ ...current, password: event.target.value }))
                  : setRegisterForm((current) => ({ ...current, password: event.target.value }))
              )}
            />
          </label>

          {error && <div className="error-banner">{error}</div>}

          <button className="primary-button" disabled={loading} type="submit">
            {loading ? "Please wait..." : mode === "login" ? "Login" : "Create Account"}
          </button>
        </form>
      </div>
    </section>
  );
}
