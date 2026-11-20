package com.hotel.management.dto;

import java.math.BigDecimal;

public record DashboardStats(
        long totalRooms,
        long availableRooms,
        long occupiedRooms,
        long maintenanceRooms,
        long totalCustomers,
        long upcomingBookings,
        long checkedInGuests,
        long todayCheckIns,
        long todayCheckOuts,
        BigDecimal totalRevenue
) {
}
