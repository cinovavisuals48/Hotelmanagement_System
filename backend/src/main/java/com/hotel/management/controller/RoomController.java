package com.hotel.management.controller;

import com.hotel.management.dto.RoomRequest;
import com.hotel.management.entity.Room;
import com.hotel.management.entity.RoomType;
import com.hotel.management.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<Room> getAll() {
        return roomService.getAllRooms();
    }

    @GetMapping("/{id}")
    public Room getById(@PathVariable Long id) {
        return roomService.getRoom(id);
    }

    /** GET /api/rooms/available?checkIn=2026-12-01&checkOut=2026-12-03&guests=2&type=DOUBLE */
    @GetMapping("/available")
    public List<Room> getAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam(required = false) RoomType type) {
        return roomService.findAvailableRooms(checkIn, checkOut, type, guests);
    }

    @PostMapping
    public ResponseEntity<Room> create(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    @PutMapping("/{id}")
    public Room update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.updateRoom(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
