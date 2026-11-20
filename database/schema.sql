-- =====================================================================
-- Hotel Management System - MySQL schema
-- Run this whole script in MySQL Workbench (File > Open SQL Script,
-- then the lightning-bolt "Execute" button).
--
-- Running it is optional: the Spring Boot app creates the database
-- and tables on first start (createDatabaseIfNotExist + ddl-auto=update)
-- and seeds the same sample rooms. Use this script when you want to
-- create or reset everything from Workbench yourself.
--
-- WARNING: the DROP TABLE lines delete all existing hotel data.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS hotel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hotel_db;

DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS rooms;

-- ---------------------------------------------------------------------
-- Rooms
-- ---------------------------------------------------------------------
CREATE TABLE rooms (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    room_number     VARCHAR(10)   NOT NULL,
    room_type       VARCHAR(20)   NOT NULL,             -- SINGLE, DOUBLE, DELUXE, SUITE
    price_per_night DECIMAL(10,2) NOT NULL,
    capacity        INT           NOT NULL,
    floor           INT           NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, OCCUPIED, MAINTENANCE
    description     VARCHAR(255)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rooms_room_number UNIQUE (room_number),
    CONSTRAINT chk_rooms_type   CHECK (room_type IN ('SINGLE', 'DOUBLE', 'DELUXE', 'SUITE')),
    CONSTRAINT chk_rooms_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE')),
    CONSTRAINT chk_rooms_capacity CHECK (capacity BETWEEN 1 AND 10),
    CONSTRAINT chk_rooms_price  CHECK (price_per_night > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Customers (guests)
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    address         VARCHAR(255) NULL,
    id_proof_type   VARCHAR(30)  NULL,                  -- e.g. Aadhaar, Passport, Driving Licence
    id_proof_number VARCHAR(50)  NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_customers_email UNIQUE (email)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Bookings (one room per booking)
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    customer_id      BIGINT        NOT NULL,
    room_id          BIGINT        NOT NULL,
    check_in_date    DATE          NOT NULL,
    check_out_date   DATE          NOT NULL,
    number_of_guests INT           NOT NULL,
    total_amount     DECIMAL(10,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'BOOKED', -- BOOKED, CHECKED_IN, CHECKED_OUT, CANCELLED
    special_requests VARCHAR(500)  NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_bookings_room     FOREIGN KEY (room_id)     REFERENCES rooms (id),
    CONSTRAINT chk_bookings_dates   CHECK (check_out_date > check_in_date),
    CONSTRAINT chk_bookings_status  CHECK (status IN ('BOOKED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    INDEX idx_bookings_room_dates (room_id, check_in_date, check_out_date),
    INDEX idx_bookings_status (status)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Sample rooms
-- ---------------------------------------------------------------------
INSERT INTO rooms (room_number, room_type, price_per_night, capacity, floor, status, description) VALUES
('101', 'SINGLE', 1500.00, 1, 1, 'AVAILABLE', 'Cozy single room with city view'),
('102', 'SINGLE', 1500.00, 1, 1, 'AVAILABLE', 'Single room near the lobby'),
('103', 'DOUBLE', 2500.00, 2, 1, 'AVAILABLE', 'Double room with queen bed'),
('104', 'DOUBLE', 2500.00, 2, 1, 'AVAILABLE', 'Double room with twin beds'),
('201', 'DOUBLE', 2800.00, 3, 2, 'AVAILABLE', 'Large double room with extra bed'),
('202', 'DELUXE', 4000.00, 3, 2, 'AVAILABLE', 'Deluxe room with balcony'),
('203', 'DELUXE', 4000.00, 3, 2, 'AVAILABLE', 'Deluxe room with pool view'),
('301', 'SUITE',  7500.00, 4, 3, 'AVAILABLE', 'Executive suite with living area'),
('302', 'SUITE',  9000.00, 6, 3, 'AVAILABLE', 'Family suite with two bedrooms');
