package com.campus.event.web;

import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.Room;
import com.campus.event.repository.EventTimeSlotRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.service.RoomAvailabilityService;
import com.campus.event.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomAvailabilityService availabilityService;
    private final EventTimeSlotRepository eventTimeSlotRepository;
    private final ScheduleService scheduleService;

    public RoomController(RoomRepository roomRepository, RoomAvailabilityService availabilityService,
                          EventTimeSlotRepository eventTimeSlotRepository, ScheduleService scheduleService) {
        this.roomRepository = roomRepository;
        this.availabilityService = availabilityService;
        this.eventTimeSlotRepository = eventTimeSlotRepository;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRooms() {
        return roomRepository.findAll().stream()
            .filter(r -> r.getFloor() != null && r.getFloor().getBuilding() != null)
            .map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("roomNumber", r.getRoomNumber());
            m.put("type", r.getType());
            m.put("capacity", r.getCapacity());
            m.put("amenities", r.getAmenities());
            m.put("buildingId", r.getFloor().getBuilding().getId());
            m.put("buildingName", r.getFloor().getBuilding().getName());
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
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
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> roomAvailability(@PathVariable Long roomId,
                                                                @RequestParam LocalDateTime start,
                                                                @RequestParam LocalDateTime end) {
        boolean available = availabilityService.isRoomAvailable(roomId, start, end);
        return ResponseEntity.ok(Map.of("roomId", roomId, "available", available));
    }

    @GetMapping("/status-now")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
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
    @GetMapping("/slot-availability")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> slotAvailability(@RequestParam Long slotId) {
        EventTimeSlot slot = eventTimeSlotRepository.findById(slotId).orElse(null);
        if (slot == null) {
            return ResponseEntity.badRequest().body("Slot not found");
        }

        List<Room> rooms = roomRepository.findAll().stream()
                .filter(r -> r.getFloor() != null && r.getFloor().getBuilding() != null)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Room r : rooms) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("roomId", r.getId());
            entry.put("name", r.getName());
            entry.put("capacity", r.getCapacity());
            entry.put("type", r.getType() != null ? r.getType().name() : null);
            entry.put("buildingId", r.getFloor().getBuilding().getId());
            entry.put("buildingName", r.getFloor().getBuilding().getName());

            List<String> conflicts = scheduleService.getRoomConflicts(r.getId(), slot.getSlotStart(), slot.getSlotEnd());
            boolean available = conflicts.isEmpty();
            entry.put("available", available);
            entry.put("conflicts", conflicts);
            result.add(entry);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/event-availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> eventAvailability(@RequestParam Long eventId) {
        // Fetch all rooms (building filtering is done on the frontend)
        List<Room> rooms = roomRepository.findAll().stream()
                .filter(r -> r.getFloor() != null && r.getFloor().getBuilding() != null)
                .collect(Collectors.toList());

        List<RoomAvailabilityService.RoomEventAvailability> result =
                availabilityService.getEventAvailability(eventId, rooms);

        // Sort: fully available first, then partially, then unavailable; within each group by capacity
        result.sort((a, b) -> {
            int rankA = a.fullyAvailable ? 0 : a.partiallyAvailable ? 1 : 2;
            int rankB = b.fullyAvailable ? 0 : b.partiallyAvailable ? 1 : 2;
            if (rankA != rankB) return Integer.compare(rankA, rankB);
            return Integer.compare(a.capacity != null ? a.capacity : 0, b.capacity != null ? b.capacity : 0);
        });

        return ResponseEntity.ok(result);
    }
}
