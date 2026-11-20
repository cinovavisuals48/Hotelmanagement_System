package com.hotel.management.controller;

import com.hotel.management.dto.BookingResponse;
import com.hotel.management.dto.CustomerRequest;
import com.hotel.management.entity.Customer;
import com.hotel.management.service.BookingService;
import com.hotel.management.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final BookingService bookingService;

    public CustomerController(CustomerService customerService, BookingService bookingService) {
        this.customerService = customerService;
        this.bookingService = bookingService;
    }

    /** GET /api/customers?search=john */
    @GetMapping
    public List<Customer> getAll(@RequestParam(required = false) String search) {
        return customerService.getAllCustomers(search);
    }

    @GetMapping("/{id}")
    public Customer getById(@PathVariable Long id) {
        return customerService.getCustomer(id);
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> getBookings(@PathVariable Long id) {
        return bookingService.getBookingsForCustomer(id);
    }

    @PostMapping
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
