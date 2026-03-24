-- ============================================================
-- NagarSeva E-Governance Portal – MySQL Database Schema
-- Run this file: mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS nagarseva CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nagarseva;

-- ── USERS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    phone       VARCHAR(15)  NOT NULL,
    aadhaar     VARCHAR(12)  UNIQUE,
    dob         DATE,
    ward        VARCHAR(80),
    address     TEXT,
    password    VARCHAR(255) NOT NULL COMMENT 'SHA-256 hashed',
    role        ENUM('citizen','admin','staff') DEFAULT 'citizen',
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone)
) ENGINE=InnoDB;

-- ── SERVICE REQUESTS ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS service_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id      VARCHAR(20) UNIQUE NOT NULL COMMENT 'e.g. NS1234567',
    user_id         BIGINT NULL,
    applicant_name  VARCHAR(100) NOT NULL,
    mobile          VARCHAR(15)  NOT NULL,
    email           VARCHAR(150),
    ward            VARCHAR(80)  NOT NULL,
    location        VARCHAR(255) NOT NULL,
    category        VARCHAR(50)  NOT NULL COMMENT 'road|water|waste|light|cert|tax|health|other',
    sub_category    VARCHAR(80),
    priority        ENUM('low','medium','high','urgent') DEFAULT 'medium',
    description     TEXT NOT NULL,
    status          ENUM('PENDING','IN_REVIEW','ASSIGNED','IN_PROGRESS','RESOLVED','REJECTED') DEFAULT 'PENDING',
    assigned_to     VARCHAR(100) COMMENT 'Department or officer name',
    remarks         TEXT COMMENT 'Internal notes',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_request_id (request_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_ward (ward)
) ENGINE=InnoDB;

-- ── REQUEST HISTORY ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS request_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id  VARCHAR(20) NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20) NOT NULL,
    changed_by  VARCHAR(100),
    remarks     TEXT,
    changed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_req_id (request_id)
) ENGINE=InnoDB;

-- ── SAMPLE DATA ────────────────────────────────────────────
INSERT INTO users (full_name, email, phone, aadhaar, ward, address, password) VALUES
('Demo Citizen', 'demo@nagarseva.gov.in', '9876543210', '123456789012',
 'Ward 3 – Fairlands', '12/A, Fairlands Road, Salem',
 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3') -- Demo@123
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO service_requests (request_id, applicant_name, mobile, email, ward, location, category, sub_category, priority, description, status) VALUES
('NS2024001', 'Demo Citizen', '9876543210', 'demo@nagarseva.gov.in', 'Ward 3 – Fairlands', 'Fairlands Main Road', 'road', 'Pothole', 'high', 'Large pothole near bus stop causing accidents', 'RESOLVED'),
('NS2024002', 'Demo Citizen', '9876543210', 'demo@nagarseva.gov.in', 'Ward 3 – Fairlands', 'Gandhi Nagar 2nd Street', 'water', 'Water Leakage', 'medium', 'Water pipe leaking for 3 days', 'IN_PROGRESS'),
('NS2024003', 'Demo Citizen', '9876543210', 'demo@nagarseva.gov.in', 'Ward 5 – Ammapet', 'Ammapet Market', 'waste', 'Garbage Not Collected', 'low', 'Garbage not collected for 5 days', 'PENDING')
ON DUPLICATE KEY UPDATE id=id;