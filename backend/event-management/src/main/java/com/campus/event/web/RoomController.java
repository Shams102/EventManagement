package com.campus.event.web;

import com.campus.event.domain.Room;
import com.campus.event.repository.RoomRepository;
import com.campus.event.service.RoomAvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomAvailabilityService availabilityService;

    public RoomController(RoomRepository roomRepository, RoomAvailabilityService availabilityService) {
        this.roomRepository = roomRepository;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public List<Map<String, Object>> listRooms() {
        return roomRepository.findAll().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("roomNumber", r.getRoomNumber());
            m.put("type", r.getType());
            m.put("capacity", r.getCapacity());
            m.put("amenities", r.getAmenities());
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> availability(@RequestParam LocalDateTime start,
                                                                  @RequestParam LocalDateTime end) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> body = rooms.stream().map(r -> {
            boolean available = availabilityService.isRoomAvailable(r.getId(), start, end);
            Map<String, Object> m = new HashMap<>();
            m.put("roomId", r.getId());
            m.put("name", r.getName());
            m.put("available", available);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{roomId}/availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<Map<String, Object>> roomAvailability(@PathVariable Long roomId,
                                                                @RequestParam LocalDateTime start,
                                                                @RequestParam LocalDateTime end) {
        boolean available = availabilityService.isRoomAvailable(roomId, start, end);
        return ResponseEntity.ok(Map.of("roomId", roomId, "available", available));
    }

    @GetMapping("/status-now")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> statusNow() {
        LocalDateTime now = LocalDateTime.now();
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> body = rooms.stream().map(r -> {
            boolean available = availabilityService.isRoomAvailable(r.getId(), now.minusMinutes(1), now.plusMinutes(1));
            Map<String, Object> m = new HashMap<>();
            m.put("roomId", r.getId());
            m.put("name", r.getName());
            m.put("status", available ? "EMPTY" : "OCCUPIED");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
