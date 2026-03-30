package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.GetMapping;
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

    public PublicController(EventRepository eventRepository, EventRegistrationRepository eventRegistrationRepository, RoomRepository roomRepository, RoomBookingRequestRepository bookingRepository) {
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/events")
    public ResponseEntity<?> listPublicEvents() {
        try {
            java.util.List<java.util.Map<String, Object>> out = eventRepository.findByIsPublicTrue().stream()
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
                        return m;
                    })
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            java.util.Map<String, Object> body = java.util.Map.of(
                    "error", ex.getMessage(),
                    "trace", sw.toString()
            );
            return ResponseEntity.status(500).body(body);
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
            return ResponseEntity.ok(m);
        } catch (Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            java.util.Map<String, Object> body = java.util.Map.of(
                    "error", ex.getMessage(),
                    "trace", sw.toString()
            );
            return ResponseEntity.status(500).body(body);
        }
    }

    // debug endpoint to test repository access separately
    @GetMapping("/events/debug")
    public ResponseEntity<?> debugList() {
        try {
            java.util.List<Event> events = eventRepository.findByIsPublicTrue();
            return ResponseEntity.ok(java.util.Map.of("count", events.size()));
        } catch (Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body(java.util.Map.of("error", ex.getMessage(), "trace", sw.toString()));
        }
    }

    @GetMapping("/rooms")
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }

    // inspect events one-by-one to find any problematic entity (non-conflicting path)
    @GetMapping("/events/debug/list")
    public ResponseEntity<?> inspectEvents() {
        java.util.List<Event> events = eventRepository.findByIsPublicTrue();
        int idx = 0;
        for (Event e : events) {
            try {
                // attempt to access common properties
                Long id = e.getId();
                String title = e.getTitle();
                String created = e.getCreatedBy() != null ? e.getCreatedBy().getUsername() : null;
                // attempt to stringify registrationSchema
                String schema = e.getRegistrationSchema();
            } catch (Exception ex) {
                java.io.StringWriter sw = new java.io.StringWriter();
                ex.printStackTrace(new java.io.PrintWriter(sw));
                return ResponseEntity.status(500).body(java.util.Map.of("failedIndex", idx, "failedId", e.getId(), "error", ex.getMessage(), "trace", sw.toString()));
            }
            idx++;
        }
        return ResponseEntity.ok(java.util.Map.of("ok", events.size()));
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

        // Find rooms that are occupied in the given window
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
                    // overlap if start < bEnd and end > bStart
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
}


