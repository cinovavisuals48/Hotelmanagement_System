package com.hotel.management.exception;

/** The request is valid but clashes with the current state (room taken, booking already checked in, ...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
