package com.hotel.management.service;

import com.hotel.management.dto.DashboardStats;
import com.hotel.management.entity.BookingStatus;
import com.hotel.management.entity.RoomStatus;
import com.hotel.management.repository.BookingRepository;
import com.hotel.management.repository.CustomerRepository;
import com.hotel.management.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    public DashboardService(RoomRepository roomRepository,
                            CustomerRepository customerRepository,
                            BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
    }

    public DashboardStats getStats() {
        LocalDate today = LocalDate.now();
        return new DashboardStats(
                roomRepository.count(),
                roomRepository.countByStatus(RoomStatus.AVAILABLE),
                roomRepository.countByStatus(RoomStatus.OCCUPIED),
                roomRepository.countByStatus(RoomStatus.MAINTENANCE),
                customerRepository.count(),
                bookingRepository.countByStatus(BookingStatus.BOOKED),
                bookingRepository.countByStatus(BookingStatus.CHECKED_IN),
                bookingRepository.countByCheckInDateAndStatus(today, BookingStatus.BOOKED),
                bookingRepository.countByCheckOutDateAndStatus(today, BookingStatus.CHECKED_IN),
                bookingRepository.sumTotalAmountByStatusIn(EnumSet.of(BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT))
        );
    }
}
