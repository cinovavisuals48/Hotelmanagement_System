package com.hotel.management.dto;

import com.hotel.management.entity.RoomStatus;
import com.hotel.management.entity.RoomType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 10, message = "Room number must be at most 10 characters")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "1.00", message = "Price per night must be at least 1.00")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal pricePerNight;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 10, message = "Capacity must be at most 10")
    private Integer capacity;

    @NotNull(message = "Floor is required")
    @Min(value = 0, message = "Floor cannot be negative")
    private Integer floor;

    /** Optional. Only AVAILABLE or MAINTENANCE can be set by hand; OCCUPIED is set by check-in. */
    private RoomStatus status;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
