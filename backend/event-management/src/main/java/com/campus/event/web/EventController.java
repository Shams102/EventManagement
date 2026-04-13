package com.campus.event.web;

import com.campus.event.domain.Building;
import com.campus.event.domain.Event;
import com.campus.event.domain.User;
import com.campus.event.repository.BuildingRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.EventService;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.service.NotificationService;
import com.campus.event.web.dto.CreateEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/events")
@org.springframework.web.bind.annotation.CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BuildingRepository buildingRepository;
    private final com.campus.event.service.BuildingTimetableService buildingTimetableService;
    private final EventRegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final RoomBookingRequestRepository roomBookingRequestRepository;

    public EventController(EventService eventService, UserRepository userRepository,
                           EventRepository eventRepository, BuildingRepository buildingRepository,
                           com.campus.event.service.BuildingTimetableService buildingTimetableService,
                           EventRegistrationRepository registrationRepository,
                           NotificationService notificationService,
                           RoomBookingRequestRepository roomBookingRequestRepository) {
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.buildingRepository = buildingRepository;
        this.buildingTimetableService = buildingTimetableService;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
        this.roomBookingRequestRepository = roomBookingRequestRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE','CENTRAL_ADMIN','BUILDING_ADMIN')")
    public ResponseEntity<?> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        // Core Protection: Intercept Zero or Negative duration inputs globally before DB validation triggers a 500 Constraint Error
        if (!request.getEnd().isAfter(request.getStart())) {
            return ResponseEntity.badRequest().body("Event end time must be strictly after the start time.");
        }

        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Building not found with ID: " + request.getBuildingId()));

        User creator = userRepository.findByUsername(principal.getUsername()).orElseGet(() -> {
            User u = new User();
            u.setUsername(principal.getUsername());
            return u;
        });

        // Parse timing model (defaults to SINGLE_DAY)
        com.campus.event.domain.EventTimingModel timingModel = com.campus.event.domain.EventTimingModel.SINGLE_DAY;
        if (request.getTimingModel() != null && !request.getTimingModel().isBlank()) {
            try {
                String model = request.getTimingModel().trim().toUpperCase();
                if ("OVERNIGHT".equals(model) || "CONTINUOUS_MULTI_DAY".equals(model)) {
                    model = "MULTI_DAY_CONTINUOUS";
                }
                timingModel = com.campus.event.domain.EventTimingModel.valueOf(model);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid timingModel: " + request.getTimingModel());
            }
        }

        // Build explicit time slots for FLEXIBLE mode
        java.util.List<com.campus.event.domain.EventTimeSlot> explicitSlots = null;
        if (request.getTimeSlots() != null && !request.getTimeSlots().isEmpty()) {
            explicitSlots = new java.util.ArrayList<>();
            for (CreateEventRequest.TimeSlotInput ts : request.getTimeSlots()) {
                if (ts.getSlotStart() == null || ts.getSlotEnd() == null) {
                    return ResponseEntity.badRequest().body("Each time slot must have slotStart and slotEnd");
                }
                if (!ts.getSlotEnd().isAfter(ts.getSlotStart())) {
                    return ResponseEntity.badRequest().body("slotEnd must be strictly after slotStart (Positive duration)");
                }
                explicitSlots.add(new com.campus.event.domain.EventTimeSlot(null, ts.getSlotStart(), ts.getSlotEnd(), null));
            }

            // Enforce internal chronology for algorithmic checks
            explicitSlots.sort(java.util.Comparator.comparing(com.campus.event.domain.EventTimeSlot::getSlotStart));

            // Validate parent bounds containment
            if (explicitSlots.get(0).getSlotStart().isBefore(request.getStart()) || 
                explicitSlots.get(explicitSlots.size() - 1).getSlotEnd().isAfter(request.getEnd())) {
                return ResponseEntity.badRequest().body("Flexible slots must logically reside entirely within the global start and end bounds.");
            }

            // O(N) overlap iteration trap
            for (int i = 0; i < explicitSlots.size() - 1; i++) {
                if (explicitSlots.get(i).getSlotEnd().isAfter(explicitSlots.get(i + 1).getSlotStart())) {
                    return ResponseEntity.badRequest().body("Overlapping flexible slots detected. Please resolve internal conflicts before submission.");
                }
            }
        }

        // Global limits
        long maximumDaysSpan = java.time.temporal.ChronoUnit.DAYS.between(request.getStart(), request.getEnd());
        if (maximumDaysSpan > 30) {
            return ResponseEntity.badRequest().body("Events cannot span more than 30 consecutive days.");
        }

        // Building hours validation must be slot-based (EventTimeSlot is source of truth)
        if (timingModel == com.campus.event.domain.EventTimingModel.MULTI_DAY_CONTINUOUS
                || timingModel == com.campus.event.domain.EventTimingModel.SINGLE_DAY) {
            java.util.List<com.campus.event.domain.EventTimeSlot> generatedSlots =
                    buildContinuousDayBoundedSlots(request.getStart(), request.getEnd());
            if (!validateSlotsAgainstBuildingHours(building.getId(), timingModel, generatedSlots)) {
                return ResponseEntity.badRequest().body("One or more event days are outside building operating hours.");
            }
        }

        // Validate complex multi-day boundaries
        if (timingModel == com.campus.event.domain.EventTimingModel.FLEXIBLE && (explicitSlots == null || explicitSlots.isEmpty())) {
            return ResponseEntity.badRequest().body("FLEXIBLE timing model requires specific time slots.");
        }

        // FLEXIBLE: validate each explicit slot individually against building hours
        if (timingModel == com.campus.event.domain.EventTimingModel.FLEXIBLE && explicitSlots != null) {
            if (!validateSlotsAgainstBuildingHours(building.getId(), timingModel, explicitSlots)) {
                return ResponseEntity.badRequest().body("One or more event days are outside building operating hours.");
            }
        }

        if (timingModel == com.campus.event.domain.EventTimingModel.MULTI_DAY_FIXED) {
            java.time.LocalDate startDate = request.getStart().toLocalDate();
            java.time.LocalDate endDate = request.getEnd().toLocalDate();
            if (!endDate.isAfter(startDate)) {
                return ResponseEntity.badRequest().body("MULTI_DAY_FIXED requires an end date strictly after the start date.");
            }
            // Validate each day's time window individually against building hours
            java.time.LocalTime dailyStart = request.getStart().toLocalTime();
            java.time.LocalTime dailyEnd = request.getEnd().toLocalTime();
            java.time.LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                LocalDateTime slotStart = LocalDateTime.of(cur, dailyStart);
                LocalDateTime slotEnd;
                if (!dailyEnd.isAfter(dailyStart)) {
                    // Overnight: e.g. 18:00–02:00 → rolls to next day
                    slotEnd = LocalDateTime.of(cur.plusDays(1), dailyEnd);
                } else {
                    slotEnd = LocalDateTime.of(cur, dailyEnd);
                }
                if (!buildingTimetableService.isBookingWithinBuildingHours(building.getId(), slotStart, slotEnd)) {
                    return ResponseEntity.badRequest().body("One or more event days are outside building operating hours.");
                }
                cur = cur.plusDays(1);
            }
        }

        Event event;
        try {
            event = eventService.createEvent(
                    request.getTitle(),
                    request.getDescription(),
                    request.getStart(),
                    request.getEnd(),
                    creator,
                    building,
                    request.getLocation(),
                    request.getClubId(),
                    request.getRegistrationSchema(),
                    request.getMaxAttendees(),
                    timingModel,
                    explicitSlots
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        return ResponseEntity.ok(event.getId());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE','CENTRAL_ADMIN','BUILDING_ADMIN')")
    public ResponseEntity<List<java.util.Map<String, Object>>> myEvents(@AuthenticationPrincipal UserDetails principal) {
        List<Event> events = eventRepository.findByCreatedBy_Username(principal.getUsername());
        List<java.util.Map<String, Object>> body = events.stream().map(e -> {
            java.util.HashMap<String, Object> m = new java.util.HashMap<>();
            m.put("id", e.getId());
            m.put("title", e.getTitle());
            m.put("description", e.getDescription());
            m.put("startTime", e.getStartTime());
            m.put("endTime", e.getEndTime());
            m.put("location", e.getLocation());
            m.put("clubId", e.getClubId());
            m.put("registrationSchema", e.getRegistrationSchema());
            m.put("maxAttendees", e.getMaxAttendees());
            m.put("buildingId", e.getBuilding() != null ? e.getBuilding().getId() : null);
            m.put("buildingName", e.getBuilding() != null ? e.getBuilding().getName() : null);
            boolean hasApprovedBooking = roomBookingRequestRepository.existsByEvent_IdAndStatusIn(
                    e.getId(),
                    Set.of(com.campus.event.domain.RoomBookingStatus.APPROVED, com.campus.event.domain.RoomBookingStatus.CONFIRMED));
            m.put("hasApprovedBooking", hasApprovedBooking);
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{eventId}/room-allocations")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE','CENTRAL_ADMIN','BUILDING_ADMIN')")
    public ResponseEntity<?> eventRoomAllocations(@PathVariable Long eventId,
                                                  @AuthenticationPrincipal UserDetails principal) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();

        boolean isOwner = event.getCreatedBy() != null
                && principal != null
                && principal.getUsername() != null
                && principal.getUsername().equals(event.getCreatedBy().getUsername());
        boolean canViewAny = principal != null && (
                principal.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                        || principal.getAuthorities().stream().anyMatch(a -> "ROLE_BUILDING_ADMIN".equals(a.getAuthority()))
                        || principal.getAuthorities().stream().anyMatch(a -> "ROLE_CENTRAL_ADMIN".equals(a.getAuthority()))
        );
        if (!isOwner && !canViewAny) {
            return ResponseEntity.status(403).body(Map.of("error", "Not allowed to view allocations for this event"));
        }

        return ResponseEntity.ok(eventService.getEventRoomAllocations(eventId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE','CENTRAL_ADMIN','BUILDING_ADMIN')")
    public ResponseEntity<?> updateEvent(@PathVariable Long id,
                                         @Valid @RequestBody CreateEventRequest request,
                                         @AuthenticationPrincipal UserDetails principal) {
        try {
            Event existing = eventRepository.findById(id).orElse(null);
            if (existing == null) return ResponseEntity.notFound().build();

            LocalDateTime now = LocalDateTime.now();
            if (existing.getStartTime() != null && now.isAfter(existing.getStartTime().minusDays(2))) {
                return ResponseEntity.badRequest().body("Event cannot be edited within 2 days of start");
            }

            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Building not found with ID: " + request.getBuildingId()));

            String oldTitle = existing.getTitle();
            LocalDateTime oldStart = existing.getStartTime();
            LocalDateTime oldEnd = existing.getEndTime();
            String oldLocation = existing.getLocation();

            existing.setTitle(request.getTitle());
            existing.setDescription(request.getDescription());
            existing.setStartTime(request.getStart());
            existing.setEndTime(request.getEnd());
            existing.setBuilding(building);
            existing.setLocation(request.getLocation());
            if (request.getClubId() != null && !request.getClubId().isBlank()) {
                existing.setClubId(request.getClubId());
            }
            existing.setRegistrationSchema(request.getRegistrationSchema());
            existing.setMaxAttendees(request.getMaxAttendees());
            eventRepository.save(existing);

            // Notify registrants about updates
            List<com.campus.event.domain.EventRegistration> regs = registrationRepository.findByEvent_Id(existing.getId());
            if (!regs.isEmpty()) {
                boolean changed = (oldTitle != null && !oldTitle.equals(existing.getTitle()))
                        || (oldStart != null && !oldStart.equals(existing.getStartTime()))
                        || (oldEnd != null && !oldEnd.equals(existing.getEndTime()))
                        || (oldLocation != null && !oldLocation.equals(existing.getLocation()));
                if (changed) {
                    String subject = "Event updated: " + existing.getTitle();
                    String body = "Event details updated. Title: " + existing.getTitle() + ", Start: " + existing.getStartTime() +
                            ", End: " + existing.getEndTime() + ", Location: " + existing.getLocation();
                    for (com.campus.event.domain.EventRegistration r : regs) {
                        if (r.getUser() != null) {
                            notificationService.notifyAllChannels(r.getUser(), subject, body);
                        }
                    }
                }
            }
            return ResponseEntity.ok("Updated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE','CENTRAL_ADMIN','BUILDING_ADMIN')")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails principal) {
        try {
            eventService.deleteEvent(id, principal.getUsername());
            return ResponseEntity.ok("Event cancelled successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static List<com.campus.event.domain.EventTimeSlot> buildContinuousDayBoundedSlots(
            LocalDateTime start, LocalDateTime end) {
        java.util.List<com.campus.event.domain.EventTimeSlot> slots = new java.util.ArrayList<>();
        if (start == null || end == null || !end.isAfter(start)) {
            return slots;
        }
        java.time.LocalDate startDate = start.toLocalDate();
        java.time.LocalDate endDate = end.toLocalDate();
        if (startDate.equals(endDate)) {
            slots.add(new com.campus.event.domain.EventTimeSlot(null, start, end, 0));
            return slots;
        }

        int dayIndex = 0;
        java.time.LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            LocalDateTime slotStart;
            LocalDateTime slotEnd;
            if (cur.equals(startDate)) {
                slotStart = start;
                slotEnd = LocalDateTime.of(cur, LocalTime.of(23, 59, 59));
            } else if (cur.equals(endDate)) {
                slotStart = LocalDateTime.of(cur, LocalTime.MIDNIGHT);
                slotEnd = end;
            } else {
                slotStart = LocalDateTime.of(cur, LocalTime.MIDNIGHT);
                slotEnd = LocalDateTime.of(cur, LocalTime.of(23, 59, 59));
            }
            slots.add(new com.campus.event.domain.EventTimeSlot(null, slotStart, slotEnd, dayIndex++));
            cur = cur.plusDays(1);
        }
        return slots;
    }

    private boolean validateSlotsAgainstBuildingHours(Long buildingId,
                                                      com.campus.event.domain.EventTimingModel timingModel,
                                                      List<com.campus.event.domain.EventTimeSlot> slots) {
        if (slots == null || slots.isEmpty()) return true;
        // Overnight / continuous mode: allow midnight boundary crossing without rejecting the whole event.
        // Enforce only that the first slot starts within hours and the last slot ends within hours.
        if (timingModel == com.campus.event.domain.EventTimingModel.MULTI_DAY_CONTINUOUS) {
            com.campus.event.domain.EventTimeSlot first = slots.get(0);
            com.campus.event.domain.EventTimeSlot last = slots.get(slots.size() - 1);
            return buildingTimetableService.isTimeWithinBuildingHours(buildingId, first.getSlotStart())
                    && buildingTimetableService.isTimeWithinBuildingHours(buildingId, last.getSlotEnd());
        }
        for (com.campus.event.domain.EventTimeSlot slot : slots) {
            if (!buildingTimetableService.isBookingWithinBuildingHours(buildingId, slot.getSlotStart(), slot.getSlotEnd())) {
                return false;
            }
        }
        return true;
    }
}
