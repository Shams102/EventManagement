package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRepository;
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
import java.util.List;

@RestController
@RequestMapping("/api/events")
@org.springframework.web.bind.annotation.CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final NotificationService notificationService;

    public EventController(EventService eventService, UserRepository userRepository, EventRepository eventRepository,
                           EventRegistrationRepository registrationRepository, NotificationService notificationService) {
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
    public ResponseEntity<?> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User creator = userRepository.findByUsername(principal.getUsername()).orElseGet(() -> {
            User u = new User();
            u.setUsername(principal.getUsername());
            return u;
        });
        Event event = eventService.createEvent(
                request.getTitle(),
                request.getDescription(),
                request.getStart(),
                request.getEnd(),
                creator,
                request.getLocation(),
                request.getClubId(),
                request.getRegistrationSchema(),
                request.getMaxAttendees()
        );
        return ResponseEntity.ok(event.getId());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
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
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
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

            String oldTitle = existing.getTitle();
            LocalDateTime oldStart = existing.getStartTime();
            LocalDateTime oldEnd = existing.getEndTime();
            String oldLocation = existing.getLocation();

            existing.setTitle(request.getTitle());
            existing.setDescription(request.getDescription());
            existing.setStartTime(request.getStart());
            existing.setEndTime(request.getEnd());
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
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
}
