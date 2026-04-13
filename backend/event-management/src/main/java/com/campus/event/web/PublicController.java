package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.BuildingRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final RoomRepository roomRepository;
    private final RoomBookingRequestRepository bookingRepository;
    private final BuildingRepository buildingRepository;
    private final EventService eventService;

    public PublicController(EventRepository eventRepository,
                            EventRegistrationRepository eventRegistrationRepository,
                            RoomRepository roomRepository,
                            RoomBookingRequestRepository bookingRepository,
                            BuildingRepository buildingRepository,
                            EventService eventService) {
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.buildingRepository = buildingRepository;
        this.eventService = eventService;
    }

    @GetMapping("/buildings")
    public ResponseEntity<?> listActiveBuildings() {
        var buildings = buildingRepository.findByIsActiveTrue().stream()
                .map(b -> {
                    java.util.HashMap<String, Object> m = new java.util.HashMap<>();
                    m.put("id", b.getId());
                    m.put("name", b.getName());
                    m.put("code", b.getCode());
                    m.put("description", b.getDescription());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/events")
    public ResponseEntity<?> listPublicEvents() {
        try {
            List<Map<String, Object>> out = eventRepository.findByIsPublicTrue().stream()
                    .map(e -> {
                        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
                        m.put("id", e.getId());
                        m.put("title", e.getTitle());
                        m.put("description", e.getDescription());
                        m.put("startTime", e.getStartTime());
                        m.put("endTime", e.getEndTime());
                        m.put("location", e.getLocation());
                        m.put("clubId", e.getClubId());
                        m.put("registrationSchema", e.getRegistrationSchema());
                        m.put("isPublic", e.isPublic());
                        m.put("createdBy", e.getCreatedBy() != null ? e.getCreatedBy().getUsername() : null);
                        m.put("maxAttendees", e.getMaxAttendees());
                        m.put("currentRegistrations", eventRegistrationRepository.findByEvent_Id(e.getId()).size());
                        m.put("buildingId", e.getBuilding() != null ? e.getBuilding().getId() : null);
                        m.put("buildingName", e.getBuilding() != null ? e.getBuilding().getName() : null);
                        return m;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load events"));
        }
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<?> getEvent(@org.springframework.web.bind.annotation.PathVariable Long id) {
        try {
            Event e = eventRepository.findById(id).orElse(null);
            if (e == null) return ResponseEntity.notFound().build();
            java.util.HashMap<String, Object> m = new java.util.HashMap<>();
            m.put("id", e.getId());
            m.put("title", e.getTitle());
            m.put("description", e.getDescription());
            m.put("startTime", e.getStartTime());
            m.put("endTime", e.getEndTime());
            m.put("location", e.getLocation());
            m.put("clubId", e.getClubId());
            m.put("registrationSchema", e.getRegistrationSchema());
            m.put("isPublic", e.isPublic());
            m.put("createdBy", e.getCreatedBy() != null ? e.getCreatedBy().getUsername() : null);
            m.put("maxAttendees", e.getMaxAttendees());
            m.put("currentRegistrations", eventRegistrationRepository.findByEvent_Id(e.getId()).size());
            m.put("buildingId", e.getBuilding() != null ? e.getBuilding().getId() : null);
            m.put("buildingName", e.getBuilding() != null ? e.getBuilding().getName() : null);
            return ResponseEntity.ok(m);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load event"));
        }
    }

    @GetMapping("/events/debug")
    public ResponseEntity<?> debugList() {
        try {
            List<Event> events = eventRepository.findByIsPublicTrue();
            return ResponseEntity.ok(Map.of("count", events.size()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/rooms")
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }

    @GetMapping("/events/debug/list")
    public ResponseEntity<?> inspectEvents() {
        List<Event> events = eventRepository.findByIsPublicTrue();
        int idx = 0;
        for (Event e : events) {
            try {
                Long eid = e.getId();
                String title = e.getTitle();
                String created = e.getCreatedBy() != null ? e.getCreatedBy().getUsername() : null;
                String schema = e.getRegistrationSchema();
            } catch (Exception ex) {
                return ResponseEntity.status(500).body(Map.of(
                        "failedIndex", idx, "failedId", e.getId(), "error", ex.getMessage()));
            }
            idx++;
        }
        return ResponseEntity.ok(Map.of("ok", events.size()));
    }

    @GetMapping("/rooms/available")
    public List<Room> listAvailableRooms(@RequestParam("start") String startIso,
                                         @RequestParam("end") String endIso) {
        LocalDateTime start = LocalDateTime.parse(startIso);
        LocalDateTime end = LocalDateTime.parse(endIso);
        if (!end.isAfter(start)) {
            return List.of();
        }

        List<Room> allRooms = roomRepository.findAll();
        List<RoomBookingRequest> bookings = bookingRepository.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));

        Set<Long> occupiedRoomIds = bookings.stream()
                .filter(b -> b.getAllocatedRoom() != null)
                .filter(b -> {
                    LocalDateTime bStart;
                    LocalDateTime bEnd;
                    if (b.getEvent() != null && b.getEvent().getStartTime() != null && b.getEvent().getEndTime() != null) {
                        bStart = b.getEvent().getStartTime();
                        bEnd = b.getEvent().getEndTime();
                    } else if (b.getMeetingStart() != null && b.getMeetingEnd() != null) {
                        bStart = b.getMeetingStart();
                        bEnd = b.getMeetingEnd();
                    } else {
                        return false;
                    }
                    return start.isBefore(bEnd) && end.isAfter(bStart);
                })
                .map(b -> b.getAllocatedRoom().getId())
                .collect(Collectors.toSet());

        return allRooms.stream()
                .filter(r -> r.getId() != null && !occupiedRoomIds.contains(r.getId()))
                .collect(Collectors.toList());
    }

    @GetMapping("/rooms/{roomId}/bookings")
    public List<Map<String, Object>> roomBookings(@org.springframework.web.bind.annotation.PathVariable Long roomId,
                                                  @RequestParam("start") String startIso,
                                                  @RequestParam("end") String endIso) {
        LocalDateTime start = LocalDateTime.parse(startIso);
        LocalDateTime end = LocalDateTime.parse(endIso);
        if (!end.isAfter(start)) {
            return List.of();
        }

        List<RoomBookingRequest> bookings = bookingRepository.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return bookings.stream()
                .filter(b -> b.getAllocatedRoom() != null && roomId.equals(b.getAllocatedRoom().getId()))
                .filter(b -> {
                    LocalDateTime bStart;
                    LocalDateTime bEnd;
                    if (b.getEvent() != null && b.getEvent().getStartTime() != null && b.getEvent().getEndTime() != null) {
                        bStart = b.getEvent().getStartTime();
                        bEnd = b.getEvent().getEndTime();
                    } else if (b.getMeetingStart() != null && b.getMeetingEnd() != null) {
                        bStart = b.getMeetingStart();
                        bEnd = b.getMeetingEnd();
                    } else {
                        return false;
                    }
                    return start.isBefore(bEnd) && end.isAfter(bStart);
                })
                .map(b -> {
                    java.util.HashMap<String, Object> m = new java.util.HashMap<>();
                    m.put("id", b.getId());
                    m.put("status", b.getStatus().name());
                    if (b.getEvent() != null) {
                        m.put("type", "EVENT");
                        m.put("title", b.getEvent().getTitle());
                        m.put("start", b.getEvent().getStartTime());
                        m.put("end", b.getEvent().getEndTime());
                    } else {
                        m.put("type", "MEETING");
                        m.put("title", b.getMeetingPurpose());
                        m.put("start", b.getMeetingStart());
                        m.put("end", b.getMeetingEnd());
                    }
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Public endpoint: room allocations for a public event (no auth required).
     * Reuses the same slot-based logic as the secured endpoint.
     */
    @GetMapping("/events/{eventId}/room-allocations")
    public ResponseEntity<?> publicEventRoomAllocations(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();
        // Only expose allocations for public events
        if (!event.isPublic()) {
            return ResponseEntity.status(403).body(Map.of("error", "Event is not public"));
        }
        return ResponseEntity.ok(eventService.getEventRoomAllocations(eventId));
    }
}
