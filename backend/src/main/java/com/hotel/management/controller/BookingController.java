package com.hotel.management.controller;

import com.hotel.management.dto.BookingRequest;
import com.hotel.management.dto.BookingResponse;
import com.hotel.management.dto.BookingUpdateRequest;
import com.hotel.management.entity.BookingStatus;
import com.hotel.management.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** GET /api/bookings or /api/bookings?status=BOOKED */
    @GetMapping
    public List<BookingResponse> getAll(@RequestParam(required = false) BookingStatus status) {
        return bookingService.getAllBookings(status);
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    /** Takes the customer's details, stores them and allocates a room. */
    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable Long id, @Valid @RequestBody BookingUpdateRequest request) {
        return bookingService.updateBooking(id, request);
    }

    @PatchMapping("/{id}/check-in")
    public BookingResponse checkIn(@PathVariable Long id) {
        return bookingService.checkIn(id);
    }

    @PatchMapping("/{id}/check-out")
    public BookingResponse checkOut(@PathVariable Long id) {
        return bookingService.checkOut(id);
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
