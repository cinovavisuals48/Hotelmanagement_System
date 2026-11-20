package com.hotel.management.dto;

import com.hotel.management.entity.Booking;
import com.hotel.management.entity.BookingStatus;
import com.hotel.management.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Flattened view of a booking returned by the REST API. */
public record BookingResponse(
        Long id,
        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhone,
        Long roomId,
        String roomNumber,
        RoomType roomType,
        BigDecimal pricePerNight,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        long nights,
        Integer numberOfGuests,
        BigDecimal totalAmount,
        BookingStatus status,
        String specialRequests,
        LocalDateTime createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getCustomer().getId(),
                b.getCustomer().getFirstName() + " " + b.getCustomer().getLastName(),
                b.getCustomer().getEmail(),
                b.getCustomer().getPhone(),
                b.getRoom().getId(),
                b.getRoom().getRoomNumber(),
                b.getRoom().getRoomType(),
                b.getRoom().getPricePerNight(),
                b.getCheckInDate(),
                b.getCheckOutDate(),
                ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate()),
                b.getNumberOfGuests(),
                b.getTotalAmount(),
                b.getStatus(),
                b.getSpecialRequests(),
                b.getCreatedAt()
        );
    }
}
