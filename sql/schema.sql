CREATE DATABASE IF NOT EXISTS major;
USE major;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(120),
    registration_number VARCHAR(40),
    make VARCHAR(120) NOT NULL,
    model VARCHAR(120) NOT NULL,
    connector_type VARCHAR(120) NOT NULL,
    battery_capacity_kwh DECIMAL(7, 2),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_vehicle_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS stations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    city VARCHAR(120) NOT NULL,
    pincode VARCHAR(12) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    charger_type VARCHAR(120) NOT NULL,
    connector_type VARCHAR(120) NOT NULL,
    charging_speed_kw DECIMAL(7, 2) NOT NULL,
    slot_count INT NOT NULL,
    price_per_kwh DECIMAL(10, 2) NOT NULL,
    opening_hours VARCHAR(120) NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    operating_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    owner_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_station_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS station_photos (
    station_id BIGINT NOT NULL,
    photo_url VARCHAR(1024) NOT NULL,
    CONSTRAINT fk_station_photo_station FOREIGN KEY (station_id) REFERENCES stations(id)
);

CREATE TABLE IF NOT EXISTS chargers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    station_id BIGINT NOT NULL,
    charger_type VARCHAR(120) NOT NULL,
    connector_type VARCHAR(120) NOT NULL,
    speed_kw DECIMAL(7, 2) NOT NULL,
    serial_number VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_charger_station FOREIGN KEY (station_id) REFERENCES stations(id)
);

CREATE TABLE IF NOT EXISTS slots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    station_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    available BIT NOT NULL,
    state VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT DEFAULT 0,
    reserved_by BIGINT,
    reservation_expiry DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_slot_station FOREIGN KEY (station_id) REFERENCES stations(id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    vehicle_id BIGINT,
    status VARCHAR(30) NOT NULL,
    payment_id VARCHAR(255),
    actual_arrival_time DATETIME,
    charging_start_time DATETIME,
    charging_end_time DATETIME,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    overtime_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    qr_checkin_verified BIT NOT NULL DEFAULT 0,
    extension_requested BIT,
    extension_approved BIT,
    expires_at DATETIME NOT NULL,
    arrival_grace_until DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_booking_slot FOREIGN KEY (slot_id) REFERENCES slots(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    razorpay_signature VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS charging_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL UNIQUE,
    charger_id BIGINT,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    energy_consumed DECIMAL(10, 2) NOT NULL DEFAULT 0,
    charging_duration_minutes BIGINT NOT NULL DEFAULT 0,
    charging_status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_charging_session_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS qr_checkins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    qr_code VARCHAR(255) NOT NULL,
    scanned_at DATETIME NOT NULL,
    verification_status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_qr_checkin_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    gst DECIMAL(10, 2) NOT NULL,
    energy_used DECIMAL(10, 2) NOT NULL,
    charging_duration_minutes BIGINT NOT NULL,
    generated_at DATETIME NOT NULL,
    CONSTRAINT fk_invoice_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    booking_id BIGINT,
    rating TINYINT NOT NULL,
    comment VARCHAR(2000),
    created_at DATETIME NOT NULL,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_review_station FOREIGN KEY (station_id) REFERENCES stations(id),
    CONSTRAINT fk_review_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE IF NOT EXISTS favorite_stations (
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (user_id, station_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_favorite_station FOREIGN KEY (station_id) REFERENCES stations(id)
);

CREATE UNIQUE INDEX ux_station_pin ON stations(name, latitude, longitude);
CREATE INDEX idx_station_owner ON stations(owner_id);
CREATE INDEX idx_station_public_search ON stations(verification_status, operating_status, city, pincode);
CREATE INDEX idx_station_coordinates ON stations(latitude, longitude);
CREATE INDEX idx_vehicle_user ON vehicles(user_id);
CREATE INDEX idx_charger_station_connector ON chargers(station_id, connector_type, speed_kw);
CREATE INDEX idx_slot_station ON slots(station_id);
CREATE INDEX idx_slot_station_state_time ON slots(station_id, state, start_time);
CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_booking_slot ON bookings(slot_id);
CREATE INDEX idx_booking_status_expiry ON bookings(status, expires_at);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_charging_sessions_status ON charging_sessions(charging_status);
CREATE INDEX idx_qr_checkins_booking_status ON qr_checkins(booking_id, verification_status);
CREATE INDEX idx_review_station_created ON reviews(station_id, created_at);
