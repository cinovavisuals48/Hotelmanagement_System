package com.hotel.management.entity;

/**
 * AVAILABLE   - room can be allocated to new bookings
 * OCCUPIED    - a guest is currently checked in
 * MAINTENANCE - room is out of service and is never allocated
 */
public enum RoomStatus {
    AVAILABLE,
    OCCUPIED,
    MAINTENANCE
}
