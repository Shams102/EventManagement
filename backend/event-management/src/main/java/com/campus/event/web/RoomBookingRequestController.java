package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.EventTimeSlotRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.campus.event.service.BuildingTimetableService;
import com.campus.event.service.EventRoomBookingSplitService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room-requests")
public class RoomBookingRequestController {

    private final RoomBookingRequestRepository requestRepo;
    private final EventRepository eventRepo;
    private final RoomRepository roomRepo;
    private final com.campus.event.service.ScheduleService scheduleService;
    private final BuildingTimetableService buildingTimetableService;
    private final EventRoomBookingSplitService eventRoomBookingSplitService;
    private final EventTimeSlotRepository eventTimeSlotRepository;
    private final com.campus.event.service.RoomAvailabilityService availabilityService;

    public RoomBookingRequestController(RoomBookingRequestRepository requestRepo, EventRepository eventRepo, RoomRepository roomRepo,
                                        com.campus.event.service.ScheduleService scheduleService,
                                        BuildingTimetableService buildingTimetableService,
                                        EventRoomBookingSplitService eventRoomBookingSplitService,
                                        EventTimeSlotRepository eventTimeSlotRepository,
                                        com.campus.event.service.RoomAvailabilityService availabilityService) {
        this.requestRepo = requestRepo;
        this.eventRepo = eventRepo;
        this.roomRepo = roomRepo;
        this.scheduleService = scheduleService;
        this.buildingTimetableService = buildingTimetableService;
        this.eventRoomBookingSplitService = eventRoomBookingSplitService;
        this.eventTimeSlotRepository = eventTimeSlotRepository;
        this.availabilityService = availabilityService;
    }

    public static class CreateRequest {
        public Long eventId;
        /** Required: all room preferences must belong to this building. */
        public Long buildingId;
        public Long pref1RoomId;
        public Long pref2RoomId;
        public Long pref3RoomId;
        public LocalDateTime meetingStart;
        public LocalDateTime meetingEnd;
        public String meetingPurpose;
    }

    public static class SimpleBookingRequest {
        public Long roomId;
        /** Required: must match the building that contains {@code roomId}. */
        public Long buildingId;
        public String date;
        public String purpose;
        public LocalDateTime meetingStart;
        public LocalDateTime meetingEnd;
    }

    private static boolean roomBelongsToBuilding(Room room, Long buildingId) {
        if (room == null || buildingId == null) return false;
        return room.getFloor() != null
                && room.getFloor().getBuilding() != null
                && buildingId.equals(room.getFloor().getBuilding().getId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','CLUB_ASSOCIATE','FACULTY')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req, @AuthenticationPrincipal UserDetails principal) {
        if (req.pref1RoomId == null || req.pref2RoomId == null || req.pref3RoomId == null) {
            return ResponseEntity.badRequest().body("Three room preferences are required");
        }
        if (req.buildingId == null) {
            return ResponseEntity.badRequest().body("buildingId is required");
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
            if (event.getBuilding() == null || !req.buildingId.equals(event.getBuilding().getId())) {
                return ResponseEntity.badRequest().body("buildingId must match the event's building");
            }

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
        if (!roomBelongsToBuilding(r1, req.buildingId)
                || !roomBelongsToBuilding(r2, req.buildingId)
                || !roomBelongsToBuilding(r3, req.buildingId)) {
            return ResponseEntity.badRequest().body("All selected rooms must belong to the chosen building");
        }

        if (eventMode && event != null) {
            if (requestRepo.existsByEvent_IdAndStatus(event.getId(), RoomBookingStatus.PENDING)) {
                return ResponseEntity.badRequest().body("A room booking request is already pending for this event.");
            }
            // For multi-day events, validate each time slot individually against building hours
            List<EventTimeSlot> slots = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(event.getId());
            if (slots.isEmpty()) {
                // Legacy fallback: validate full range
                if (!buildingTimetableService.isBookingWithinBuildingHours(req.buildingId, event.getStartTime(), event.getEndTime())) {
                    return ResponseEntity.badRequest().body("Event time is outside building operating hours");
                }
            } else {
                for (EventTimeSlot slot : slots) {
                    if (!buildingTimetableService.isBookingWithinBuildingHours(req.buildingId, slot.getSlotStart(), slot.getSlotEnd())) {
                        return ResponseEntity.badRequest().body(
                            "Event time slot " + slot.getSlotStart().toLocalDate() + " (" +
                            slot.getSlotStart().toLocalTime() + "–" + slot.getSlotEnd().toLocalTime() +
                            ") is outside building operating hours");
                    }
                }
            }
        } else {
            if (!buildingTimetableService.isBookingWithinBuildingHours(req.buildingId, req.meetingStart, req.meetingEnd)) {
                return ResponseEntity.badRequest().body("Meeting time is outside building operating hours");
            }
        }

        Map<String, List<String>> conflicts;
        if (eventMode && event != null) {
            // Use per-slot conflict detection for multi-day events
            List<EventTimeSlot> slots = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(event.getId());
            if (slots.isEmpty()) {
                // Fallback for legacy events with no slots recorded
                conflicts = scheduleService.validateEventRoomPreferences(req.pref1RoomId, req.pref2RoomId, req.pref3RoomId, event.getStartTime(), event.getEndTime());
            } else {
                conflicts = scheduleService.validateEventRoomPreferencesMultiSlot(req.pref1RoomId, req.pref2RoomId, req.pref3RoomId, slots);
            }
        } else {
            conflicts = scheduleService.validateEventRoomPreferences(req.pref1RoomId, req.pref2RoomId, req.pref3RoomId, req.meetingStart, req.meetingEnd);
        }

        if (eventMode) {
            List<RoomBookingRequest> toSave = eventRoomBookingSplitService.buildEventRequests(event, r1, r2, r3, principal.getUsername());
            List<Long> ids = new ArrayList<>();
            RoomBookingRequest first = null;
            for (RoomBookingRequest rr : toSave) {
                RoomBookingRequest saved = requestRepo.save(rr);
                ids.add(saved.getId());
                if (first == null) {
                    first = saved;
                }
            }
            Map<String, Object> body = new HashMap<>();
            body.put("id", first.getId());
            body.put("ids", ids);
            body.put("status", first.getStatus().name());
            body.put("conflicts", conflicts);
            if (first.getSplitGroupId() != null) {
                body.put("splitGroupId", first.getSplitGroupId().toString());
                body.put("splitApprovalNote",
                        "Mixed large-hall vs normal-room preferences: multiple pending requests were created. Approving one allocation automatically rejects the others in the same group.");
            }
            return ResponseEntity.ok(body);
        }

        RoomBookingRequest rr = new RoomBookingRequest();
        rr.setEvent(null);
        rr.setMeetingStart(req.meetingStart);
        rr.setMeetingEnd(req.meetingEnd);
        rr.setMeetingPurpose(req.meetingPurpose);
        rr.setPref1(r1);
        rr.setPref2(r2);
        rr.setPref3(r3);
        rr.setStatus(RoomBookingStatus.PENDING);
        rr.setRequestedByUsername(principal.getUsername());
        RoomBookingRequest saved = requestRepo.save(rr);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", saved.getStatus().name(), "conflicts", conflicts));
    }

    @PostMapping("/meeting")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','FACULTY','CLUB_ASSOCIATE')")
    @Transactional
    public ResponseEntity<?> createSimpleMeeting(@Valid @RequestBody SimpleBookingRequest req, @AuthenticationPrincipal UserDetails principal) {
        if (req.roomId == null || req.meetingStart == null || req.meetingEnd == null || req.purpose == null) {
            return ResponseEntity.badRequest().body("roomId, meetingStart, meetingEnd, and purpose are required");
        }
        if (req.buildingId == null) {
            return ResponseEntity.badRequest().body("buildingId is required");
        }

        if (!req.meetingEnd.isAfter(req.meetingStart)) {
            return ResponseEntity.badRequest().body("meetingEnd must be after meetingStart");
        }

        // Server-side cutoff: meetingStart must be strictly in the future (client allows same-day fixed-slot bookings)
        if (!req.meetingStart.isAfter(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("meetingStart must be in the future");
        }

        // Secure race conditions via pessimistic row-locking on the Room
        Room room = roomRepo.findByIdWithPessimisticLock(req.roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }
        if (!roomBelongsToBuilding(room, req.buildingId)) {
            return ResponseEntity.badRequest().body("Room does not belong to the selected building");
        }

        boolean available = availabilityService.isRoomAvailable(room.getId(), req.meetingStart, req.meetingEnd);
        if (!available) {
            return ResponseEntity.status(400).body("Room is not available in the requested window");
        }

        RoomBookingRequest bookingRequest = new RoomBookingRequest();
        bookingRequest.setMeetingStart(req.meetingStart);
        bookingRequest.setMeetingEnd(req.meetingEnd);
        bookingRequest.setMeetingPurpose(req.purpose);
        bookingRequest.setPref1(room);
        bookingRequest.setPref2(room);
        bookingRequest.setPref3(room);
        bookingRequest.setAllocatedRoom(room);
        bookingRequest.setStatus(RoomBookingStatus.APPROVED);
        bookingRequest.setApprovedAt(java.time.LocalDateTime.now());
        bookingRequest.setApprovedByUsername(principal.getUsername());
        bookingRequest.setRequestedByUsername(principal.getUsername());
        
        RoomBookingRequest saved = requestRepo.save(bookingRequest);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", saved.getStatus().name(), "allocatedRoom", room.getName()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','FACULTY','CLUB_ASSOCIATE')")
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
                    if (r.getSplitGroupId() != null) {
                        m.put("splitGroupId", r.getSplitGroupId().toString());
                    }
                    if (r.getEvent() != null && r.getEvent().getBuilding() != null) {
                        m.put("buildingName", r.getEvent().getBuilding().getName());
                    }
                    if (r.getPref1() != null) {
                        m.put("approvalScope", com.campus.event.service.RoomApprovalRules.scopeForRoom(r.getPref1()).name());
                    }
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','FACULTY','CLUB_ASSOCIATE')")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        RoomBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
        if (!principal.getUsername().equals(req.getRequestedByUsername())) {
            return ResponseEntity.status(403).body("Not allowed to cancel this request");
        }

        List<RoomBookingRequest> batch = req.getSplitGroupId() == null
                ? List.of(req)
                : requestRepo.findBySplitGroupId(req.getSplitGroupId()).stream()
                .filter(x -> principal.getUsername().equals(x.getRequestedByUsername()))
                .toList();

        for (RoomBookingRequest r : batch) {
            if (r.getStatus() != RoomBookingStatus.PENDING) {
                return ResponseEntity.badRequest().body("Only pending requests can be cancelled");
            }
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

        for (RoomBookingRequest r : batch) {
            r.setStatus(RoomBookingStatus.REJECTED);
            requestRepo.save(r);
        }
        return ResponseEntity.ok("Cancelled");
    }
}
