package com.hotel.management.service;

import com.hotel.management.dto.RoomRequest;
import com.hotel.management.entity.BookingStatus;
import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.entity.RoomType;
import com.hotel.management.exception.BadRequestException;
import com.hotel.management.exception.ConflictException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.repository.BookingRepository;
import com.hotel.management.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RoomService {

    /** Bookings in these states hold a room. */
    public static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES =
            EnumSet.of(BookingStatus.BOOKED, BookingStatus.CHECKED_IN);

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByRoomNumberAsc();
    }

    @Transactional(readOnly = true)
    public Room getRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id " + id));
    }

    @Transactional(readOnly = true)
    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, RoomType type, int guests) {
        validateStay(checkIn, checkOut);
        return roomRepository.findAvailableRooms(checkIn, checkOut, type, Math.max(guests, 1), ACTIVE_BOOKING_STATUSES);
    }

    public Room createRoom(RoomRequest request) {
        String number = request.getRoomNumber().trim();
        if (roomRepository.existsByRoomNumber(number)) {
            throw new ConflictException("Room number " + number + " already exists");
        }
        if (request.getStatus() == RoomStatus.OCCUPIED) {
            throw new BadRequestException("A new room cannot start as OCCUPIED; check a guest in instead");
        }
        Room room = new Room();
        apply(room, request);
        room.setStatus(request.getStatus() == null ? RoomStatus.AVAILABLE : request.getStatus());
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, RoomRequest request) {
        Room room = getRoom(id);
        String number = request.getRoomNumber().trim();
        roomRepository.findByRoomNumber(number)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("Room number " + number + " already exists");
                });

        RoomStatus requested = request.getStatus();
        if (requested != null && requested != room.getStatus()) {
            if (room.getStatus() == RoomStatus.OCCUPIED) {
                throw new ConflictException("Room " + room.getRoomNumber()
                        + " is occupied; check the guest out before changing its status");
            }
            if (requested == RoomStatus.OCCUPIED) {
                throw new BadRequestException("Rooms become OCCUPIED only through guest check-in");
            }
            room.setStatus(requested);
        }
        apply(room, request);
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        Room room = getRoom(id);
        if (bookingRepository.existsByRoomIdAndStatusIn(id, ACTIVE_BOOKING_STATUSES)) {
            throw new ConflictException("Room " + room.getRoomNumber()
                    + " has upcoming or current bookings and cannot be deleted");
        }
        if (bookingRepository.existsByRoomId(id)) {
            throw new ConflictException("Room " + room.getRoomNumber()
                    + " has booking history; set it to MAINTENANCE instead of deleting it");
        }
        roomRepository.delete(room);
    }

    static void validateStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BadRequestException("Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out date must be after the check-in date");
        }
        if (checkIn.plusDays(90).isBefore(checkOut)) {
            throw new BadRequestException("A single booking cannot be longer than 90 nights");
        }
    }

    private void apply(Room room, RoomRequest request) {
        room.setRoomNumber(request.getRoomNumber().trim());
        room.setRoomType(request.getRoomType());
        room.setPricePerNight(request.getPricePerNight());
        room.setCapacity(request.getCapacity());
        room.setFloor(request.getFloor());
        room.setDescription(request.getDescription());
    }
}
