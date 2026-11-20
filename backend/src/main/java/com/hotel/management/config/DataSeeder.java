package com.hotel.management.config;

import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomType;
import com.hotel.management.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Adds a set of sample rooms the first time the application starts against an empty database. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoomRepository roomRepository;
    private final boolean enabled;

    public DataSeeder(RoomRepository roomRepository,
                      @Value("${hotel.seed-sample-rooms:true}") boolean enabled) {
        this.roomRepository = roomRepository;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled || roomRepository.count() > 0) {
            return;
        }
        List<Room> rooms = List.of(
                new Room("101", RoomType.SINGLE, new BigDecimal("1500.00"), 1, 1, "Cozy single room with city view"),
                new Room("102", RoomType.SINGLE, new BigDecimal("1500.00"), 1, 1, "Single room near the lobby"),
                new Room("103", RoomType.DOUBLE, new BigDecimal("2500.00"), 2, 1, "Double room with queen bed"),
                new Room("104", RoomType.DOUBLE, new BigDecimal("2500.00"), 2, 1, "Double room with twin beds"),
                new Room("201", RoomType.DOUBLE, new BigDecimal("2800.00"), 3, 2, "Large double room with extra bed"),
                new Room("202", RoomType.DELUXE, new BigDecimal("4000.00"), 3, 2, "Deluxe room with balcony"),
                new Room("203", RoomType.DELUXE, new BigDecimal("4000.00"), 3, 2, "Deluxe room with pool view"),
                new Room("301", RoomType.SUITE, new BigDecimal("7500.00"), 4, 3, "Executive suite with living area"),
                new Room("302", RoomType.SUITE, new BigDecimal("9000.00"), 6, 3, "Family suite with two bedrooms")
        );
        roomRepository.saveAll(rooms);
        log.info("Seeded {} sample rooms", rooms.size());
    }
}
