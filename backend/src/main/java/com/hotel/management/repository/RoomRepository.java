package com.hotel.management.repository;

import com.hotel.management.entity.BookingStatus;
import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.entity.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    List<Room> findAllByOrderByRoomNumberAsc();

    long countByStatus(RoomStatus status);

    /** Loads a room with a row lock so two concurrent bookings cannot grab the same room. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    /**
     * Rooms that are not under maintenance, can hold the guests, optionally match a type,
     * and have no active booking overlapping the requested stay.
     * Two stays overlap when existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn,
     * so a guest may check in on the same day the previous guest checks out.
     */
    @Query("""
            SELECT r FROM Room r
            WHERE r.status <> com.hotel.management.entity.RoomStatus.MAINTENANCE
              AND r.capacity >= :guests
              AND (:roomType IS NULL OR r.roomType = :roomType)
              AND NOT EXISTS (
                  SELECT b FROM Booking b
                  WHERE b.room = r
                    AND b.status IN :activeStatuses
                    AND b.checkInDate < :checkOut
                    AND b.checkOutDate > :checkIn
              )
            ORDER BY r.pricePerNight ASC, r.capacity ASC, r.roomNumber ASC
            """)
    List<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut,
                                  @Param("roomType") RoomType roomType,
                                  @Param("guests") int guests,
                                  @Param("activeStatuses") Collection<BookingStatus> activeStatuses);
}
