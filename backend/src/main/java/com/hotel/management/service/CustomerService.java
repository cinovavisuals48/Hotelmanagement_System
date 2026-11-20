package com.hotel.management.service;

import com.hotel.management.dto.CustomerRequest;
import com.hotel.management.entity.Customer;
import com.hotel.management.exception.ConflictException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.repository.BookingRepository;
import com.hotel.management.repository.CustomerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    public CustomerService(CustomerRepository customerRepository, BookingRepository bookingRepository) {
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers(String search) {
        if (search == null || search.isBlank()) {
            return customerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String q = search.trim();
        return customerRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                        q, q, q, q);
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }

    public Customer createCustomer(CustomerRequest request) {
        String email = request.getEmail().trim();
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A customer with email " + email + " already exists");
        }
        Customer customer = new Customer();
        apply(customer, request);
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        String email = request.getEmail().trim();
        customerRepository.findByEmailIgnoreCase(email)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("A customer with email " + email + " already exists");
                });
        apply(customer, request);
        return customerRepository.save(customer);
    }

    /**
     * Used by the booking flow: returning guests are matched by email and their
     * details refreshed; new guests are registered.
     */
    public Customer findOrCreate(CustomerRequest request) {
        return customerRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .map(existing -> {
                    apply(existing, request);
                    return customerRepository.save(existing);
                })
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    apply(customer, request);
                    return customerRepository.save(customer);
                });
    }

    public void deleteCustomer(Long id) {
        Customer customer = getCustomer(id);
        if (bookingRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer " + customer.getFirstName() + " " + customer.getLastName()
                    + " has bookings and cannot be deleted; delete or keep their bookings first");
        }
        customerRepository.delete(customer);
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setFirstName(request.getFirstName().trim());
        customer.setLastName(request.getLastName().trim());
        customer.setEmail(request.getEmail().trim().toLowerCase());
        customer.setPhone(request.getPhone().trim());
        customer.setAddress(blankToNull(request.getAddress()));
        customer.setIdProofType(blankToNull(request.getIdProofType()));
        customer.setIdProofNumber(blankToNull(request.getIdProofNumber()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
