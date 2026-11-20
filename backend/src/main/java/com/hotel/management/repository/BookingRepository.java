package com.hotel.management.repository;

import com.hotel.management.entity.Booking;
import com.hotel.management.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByOrderByCreatedAtDesc();

    List<Booking> findByCustomerIdOrderByCheckInDateDesc(Long customerId);

    List<Booking> findByStatusOrderByCheckInDateAsc(BookingStatus status);

    boolean existsByRoomIdAndStatusIn(Long roomId, Collection<BookingStatus> statuses);

    boolean existsByCustomerIdAndStatusIn(Long customerId, Collection<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

    boolean existsByRoomId(Long roomId);

    boolean existsByCustomerId(Long customerId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    java.math.BigDecimal sumTotalAmountByStatusIn(@Param("statuses") Collection<BookingStatus> statuses);

    long countByCheckInDateAndStatus(LocalDate date, BookingStatus status);

    long countByCheckOutDateAndStatus(LocalDate date, BookingStatus status);

    /** Counts active bookings on a room that overlap a stay, ignoring one booking (used when editing it). */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.room.id = :roomId
              AND b.status IN :activeStatuses
              AND b.checkInDate < :checkOut
              AND b.checkOutDate > :checkIn
              AND (:excludeId IS NULL OR b.id <> :excludeId)
            """)
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut,
                          @Param("activeStatuses") Collection<BookingStatus> activeStatuses,
                          @Param("excludeId") Long excludeId);
}
