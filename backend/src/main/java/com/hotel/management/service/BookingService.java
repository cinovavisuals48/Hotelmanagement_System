package com.hotel.management.service;

import com.hotel.management.dto.BookingRequest;
import com.hotel.management.dto.BookingResponse;
import com.hotel.management.dto.BookingUpdateRequest;
import com.hotel.management.entity.*;
import com.hotel.management.exception.BadRequestException;
import com.hotel.management.exception.ConflictException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.repository.BookingRepository;
import com.hotel.management.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.hotel.management.service.RoomService.ACTIVE_BOOKING_STATUSES;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final CustomerService customerService;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          CustomerService customerService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.customerService = customerService;
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings(BookingStatus status) {
        List<Booking> bookings = status == null
                ? bookingRepository.findAllByOrderByCreatedAtDesc()
                : bookingRepository.findByStatusOrderByCheckInDateAsc(status);
        return bookings.stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForCustomer(Long customerId) {
        customerService.getCustomer(customerId); // 404 if the customer does not exist
        return bookingRepository.findByCustomerIdOrderByCheckInDateDesc(customerId)
                .stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {
        return BookingResponse.from(findBooking(id));
    }

    // ---------------------------------------------------------------- create

    /**
     * Books a room for a customer:
     * 1. validates the stay,
     * 2. stores (or refreshes) the customer's details,
     * 3. allocates a room - the requested one, or the cheapest free room that matches the type and
     *    fits the guests - locking it so a concurrent request cannot take it,
     * 4. saves the booking with the total price.
     * READ_COMMITTED makes the overlap re-check after the row lock see bookings committed by a
     * concurrent request (MySQL's default REPEATABLE_READ would keep reading an older snapshot).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(BookingRequest request) {
        RoomService.validateStay(request.getCheckInDate(), request.getCheckOutDate());
        int guests = request.getNumberOfGuests();

        Room room = request.getRoomId() != null
                ? lockRequestedRoom(request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate(), guests, null)
                : allocateRoom(request.getRoomType(), request.getCheckInDate(), request.getCheckOutDate(), guests);

        Customer customer = customerService.findOrCreate(request.getCustomer());

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(guests);
        booking.setSpecialRequests(blankToNull(request.getSpecialRequests()));
        booking.setTotalAmount(calculateTotal(room, request.getCheckInDate(), request.getCheckOutDate()));
        booking.setStatus(BookingStatus.BOOKED);

        return BookingResponse.from(bookingRepository.save(booking));
    }

    // ---------------------------------------------------------------- update

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse updateBooking(Long id, BookingUpdateRequest request) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ConflictException("Only bookings with status BOOKED can be changed (this one is "
                    + booking.getStatus() + ")");
        }
        RoomService.validateStay(request.getCheckInDate(), request.getCheckOutDate());
        if (request.getCheckInDate().isBefore(LocalDate.now())
                && !request.getCheckInDate().equals(booking.getCheckInDate())) {
            throw new BadRequestException("Check-in date cannot be moved into the past");
        }

        Long roomId = request.getRoomId() != null ? request.getRoomId() : booking.getRoom().getId();
        Room room = lockRequestedRoom(roomId, request.getCheckInDate(), request.getCheckOutDate(),
                request.getNumberOfGuests(), booking.getId());

        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setSpecialRequests(blankToNull(request.getSpecialRequests()));
        booking.setTotalAmount(calculateTotal(room, request.getCheckInDate(), request.getCheckOutDate()));

        return BookingResponse.from(bookingRepository.save(booking));
    }

    // ------------------------------------------------------- status changes

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse checkIn(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ConflictException("Only BOOKED bookings can be checked in (this one is " + booking.getStatus() + ")");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(booking.getCheckInDate())) {
            throw new ConflictException("Check-in opens on " + booking.getCheckInDate());
        }
        if (!today.isBefore(booking.getCheckOutDate())) {
            throw new ConflictException("This booking's stay has already ended; cancel it instead");
        }
        Room room = roomRepository.findByIdForUpdate(booking.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new ConflictException("Room " + room.getRoomNumber() + " is still occupied by another guest");
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ConflictException("Room " + room.getRoomNumber()
                    + " is under maintenance; edit the booking to move the guest to another room");
        }
        room.setStatus(RoomStatus.OCCUPIED);
        booking.setStatus(BookingStatus.CHECKED_IN);
        return BookingResponse.from(booking);
    }

    public BookingResponse checkOut(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ConflictException("Only CHECKED_IN bookings can be checked out (this one is " + booking.getStatus() + ")");
        }
        booking.getRoom().setStatus(RoomStatus.AVAILABLE);
        booking.setStatus(BookingStatus.CHECKED_OUT);
        return BookingResponse.from(booking);
    }

    public BookingResponse cancel(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ConflictException("Only BOOKED bookings can be cancelled (this one is " + booking.getStatus() + ")");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
    }

    // ---------------------------------------------------------------- delete

    public void deleteBooking(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new ConflictException("The guest is checked in; check them out before deleting this booking");
        }
        bookingRepository.delete(booking);
    }

    // --------------------------------------------------------------- helpers

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
    }

    /** Picks the cheapest free room that matches, re-checking each candidate under a row lock. */
    private Room allocateRoom(RoomType type, LocalDate checkIn, LocalDate checkOut, int guests) {
        List<Room> candidates = roomRepository.findAvailableRooms(checkIn, checkOut, type, guests, ACTIVE_BOOKING_STATUSES);
        for (Room candidate : candidates) {
            Room locked = roomRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (locked != null && locked.getStatus() != RoomStatus.MAINTENANCE
                    && bookingRepository.countOverlapping(locked.getId(), checkIn, checkOut,
                    ACTIVE_BOOKING_STATUSES, null) == 0) {
                return locked;
            }
        }
        String what = type == null ? "room" : type.name() + " room";
        throw new ConflictException("Sorry, no " + what + " for " + guests + " guest(s) is free from "
                + checkIn + " to " + checkOut + ". Try other dates or another room type.");
    }

    /** Locks a specific room and verifies it can take this stay. */
    private Room lockRequestedRoom(Long roomId, LocalDate checkIn, LocalDate checkOut, int guests, Long excludeBookingId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id " + roomId));
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ConflictException("Room " + room.getRoomNumber() + " is under maintenance");
        }
        if (room.getCapacity() < guests) {
            throw new BadRequestException("Room " + room.getRoomNumber() + " holds at most "
                    + room.getCapacity() + " guest(s)");
        }
        if (bookingRepository.countOverlapping(roomId, checkIn, checkOut, ACTIVE_BOOKING_STATUSES, excludeBookingId) > 0) {
            throw new ConflictException("Room " + room.getRoomNumber() + " is already booked between "
                    + checkIn + " and " + checkOut);
        }
        return room;
    }

    private static BigDecimal calculateTotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
