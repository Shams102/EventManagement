package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room-requests")
public class RoomBookingRequestController {

    private final RoomBookingRequestRepository requestRepo;
    private final EventRepository eventRepo;
    private final RoomRepository roomRepo;
    private final com.campus.event.service.ScheduleService scheduleService;

    public RoomBookingRequestController(RoomBookingRequestRepository requestRepo, EventRepository eventRepo, RoomRepository roomRepo, com.campus.event.service.ScheduleService scheduleService) {
        this.requestRepo = requestRepo;
        this.eventRepo = eventRepo;
        this.roomRepo = roomRepo;
        this.scheduleService = scheduleService;
    }

    public static class CreateRequest {
        public Long eventId;
        public Long pref1RoomId;
        public Long pref2RoomId;
        public Long pref3RoomId;
        public LocalDateTime meetingStart;
        public LocalDateTime meetingEnd;
        public String meetingPurpose;
    }

    public static class SimpleBookingRequest {
        public Long roomId;
        public String date;
        public String purpose;
        public LocalDateTime meetingStart;
        public LocalDateTime meetingEnd;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_ASSOCIATE')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req, @AuthenticationPrincipal UserDetails principal) {
        if (req.pref1RoomId == null || req.pref2RoomId == null || req.pref3RoomId == null) {
            return ResponseEntity.badRequest().body("Three room preferences are required");
        }

        boolean eventMode = req.eventId != null;
        boolean meetingMode = req.meetingStart != null && req.meetingEnd != null && req.meetingPurpose != null && !req.meetingPurpose.isBlank();

        if (eventMode == meetingMode) {
            return ResponseEntity.badRequest().body("Specify either eventId or meetingStart/meetingEnd/meetingPurpose, but not both");
        }

        Event event = null;
        if (eventMode) {
            event = eventRepo.findById(req.eventId).orElse(null);
            if (event == null) return ResponseEntity.badRequest().body("Event not found");

            long days = Duration.between(LocalDateTime.now(), event.getStartTime()).toDays();
            if (days < 5) return ResponseEntity.badRequest().body("Minimum 5 days advance notice required for events");
        } else {
            // meeting mode
            if (!req.meetingEnd.isAfter(req.meetingStart)) {
                return ResponseEntity.badRequest().body("meetingEnd must be after meetingStart");
            }
            // server-side cutoff: meetingStart must be strictly in the future (reject past/start-now bookings)
            if (!req.meetingStart.isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body("meetingStart must be in the future");
            }
        }

        Room r1 = roomRepo.findById(req.pref1RoomId).orElse(null);
        Room r2 = roomRepo.findById(req.pref2RoomId).orElse(null);
        Room r3 = roomRepo.findById(req.pref3RoomId).orElse(null);
        if (r1 == null || r2 == null || r3 == null) return ResponseEntity.badRequest().body("Invalid room preference(s)");

        RoomBookingRequest rr = new RoomBookingRequest();
        rr.setEvent(event);
        if (meetingMode) {
            rr.setMeetingStart(req.meetingStart);
            rr.setMeetingEnd(req.meetingEnd);
            rr.setMeetingPurpose(req.meetingPurpose);
        }
        rr.setPref1(r1); rr.setPref2(r2); rr.setPref3(r3);
        rr.setStatus(RoomBookingStatus.PENDING);
        rr.setRequestedByUsername(principal.getUsername());
        RoomBookingRequest saved = requestRepo.save(rr);
        
        Map<String, List<String>> conflicts = null;
        if (eventMode && event != null) {
            conflicts = scheduleService.validateEventRoomPreferences(req.pref1RoomId, req.pref2RoomId, req.pref3RoomId, event.getStartTime(), event.getEndTime());
        } else {
            conflicts = scheduleService.validateEventRoomPreferences(req.pref1RoomId, req.pref2RoomId, req.pref3RoomId, req.meetingStart, req.meetingEnd);
        }
        
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", saved.getStatus().name(), "conflicts", conflicts));
    }

    @PostMapping("/meeting")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
    public ResponseEntity<?> createSimpleMeeting(@Valid @RequestBody SimpleBookingRequest req, @AuthenticationPrincipal UserDetails principal) {
        if (req.roomId == null || req.meetingStart == null || req.meetingEnd == null || req.purpose == null) {
            return ResponseEntity.badRequest().body("roomId, meetingStart, meetingEnd, and purpose are required");
        }

        if (!req.meetingEnd.isAfter(req.meetingStart)) {
            return ResponseEntity.badRequest().body("meetingEnd must be after meetingStart");
        }

        // Server-side cutoff: meetingStart must be strictly in the future (client allows same-day fixed-slot bookings)
        if (!req.meetingStart.isAfter(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("meetingStart must be in the future");
        }

        Room room = roomRepo.findById(req.roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        RoomBookingRequest bookingRequest = new RoomBookingRequest();
        bookingRequest.setMeetingStart(req.meetingStart);
        bookingRequest.setMeetingEnd(req.meetingEnd);
        bookingRequest.setMeetingPurpose(req.purpose);
        bookingRequest.setPref1(room);
        bookingRequest.setPref2(room);
        bookingRequest.setPref3(room);
        bookingRequest.setStatus(RoomBookingStatus.PENDING);
        bookingRequest.setRequestedByUsername(principal.getUsername());
        
        RoomBookingRequest saved = requestRepo.save(bookingRequest);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", saved.getStatus().name()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine(@AuthenticationPrincipal UserDetails principal) {
        return requestRepo.findByRequestedByUsernameOrderByRequestedAtDesc(principal.getUsername())
                .stream()
                .map(r -> {
                    java.util.HashMap<String, Object> m = new java.util.HashMap<>();
                    m.put("id", r.getId());
                    boolean hasEvent = r.getEvent() != null;
                    m.put("eventId", hasEvent ? r.getEvent().getId() : null);
                    m.put("eventTitle", hasEvent ? r.getEvent().getTitle() : r.getMeetingPurpose());
                    m.put("start", hasEvent ? r.getEvent().getStartTime() : r.getMeetingStart());
                    m.put("end", hasEvent ? r.getEvent().getEndTime() : r.getMeetingEnd());
                    m.put("status", r.getStatus().name());
                    m.put("allocatedRoom", r.getAllocatedRoom() != null ? r.getAllocatedRoom().getName() : null);
                    m.put("pref1", r.getPref1() != null ? r.getPref1().getName() : null);
                    m.put("pref2", r.getPref2() != null ? r.getPref2().getName() : null);
                    m.put("pref3", r.getPref3() != null ? r.getPref3().getName() : null);
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
    public ResponseEntity<?> cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        RoomBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
        if (!principal.getUsername().equals(req.getRequestedByUsername())) {
            return ResponseEntity.status(403).body("Not allowed to cancel this request");
        }
        if (req.getStatus() != RoomBookingStatus.PENDING) {
            return ResponseEntity.badRequest().body("Only pending requests can be cancelled");
        }

        LocalDateTime start;
        if (req.getEvent() != null && req.getEvent().getStartTime() != null) {
            start = req.getEvent().getStartTime();
        } else {
            start = req.getMeetingStart();
        }
        if (start != null && LocalDateTime.now().isAfter(start.minusDays(2))) {
            return ResponseEntity.badRequest().body("Cannot cancel within 2 days of the booking start");
        }

        req.setStatus(RoomBookingStatus.REJECTED);
        requestRepo.save(req);
        return ResponseEntity.ok("Cancelled");
    }
}
