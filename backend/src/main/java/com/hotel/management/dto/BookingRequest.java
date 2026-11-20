package com.hotel.management.dto;

import com.hotel.management.entity.RoomType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * A new booking request: the guest's details plus what they want.
 * If roomId is given that exact room is booked; otherwise the system allocates
 * the cheapest free room of the requested type (or any type) that fits the guests.
 */
public class BookingRequest {

    @NotNull(message = "Customer details are required")
    @Valid
    private CustomerRequest customer;

    private RoomType roomType;

    private Long roomId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date cannot be in the past")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "At least 1 guest is required")
    @Max(value = 10, message = "At most 10 guests per room")
    private Integer numberOfGuests;

    @Size(max = 500, message = "Special requests must be at most 500 characters")
    private String specialRequests;

    public CustomerRequest getCustomer() { return customer; }
    public void setCustomer(CustomerRequest customer) { this.customer = customer; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public Integer getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
}
