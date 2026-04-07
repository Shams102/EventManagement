package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventRegistration;
import com.campus.event.domain.NotificationDelivery;
import com.campus.event.domain.NotificationMessage;
import com.campus.event.domain.NotificationStatus;
import com.campus.event.domain.Registration;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.NotificationDeliveryRepository;
import com.campus.event.repository.NotificationMessageRepository;
import com.campus.event.repository.RegistrationRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors; 

@RestController
@RequestMapping("/api")
public class EventRegistrationController {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final RegistrationRepository legacyRegistrationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationMessageRepository notificationMessageRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    public EventRegistrationController(EventRepository eventRepository,
                                       EventRegistrationRepository registrationRepository,
                                       RegistrationRepository legacyRegistrationRepository,
                                       UserRepository userRepository,
                                       NotificationService notificationService,
                                       NotificationMessageRepository notificationMessageRepository,
                                       NotificationDeliveryRepository notificationDeliveryRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.legacyRegistrationRepository = legacyRegistrationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationMessageRepository = notificationMessageRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
    }

    @PostMapping("/events/{eventId}/register")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    @Transactional
    public ResponseEntity<?> register(@PathVariable Long eventId,
                                      @AuthenticationPrincipal UserDetails principal,
                                      @Valid @RequestBody(required = false) Map<String, Object> payload) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();

        // prevent duplicate
        if (registrationRepository.existsByEvent_IdAndUser_Username(eventId, principal.getUsername())) {
            return ResponseEntity.ok(Map.of("status", "already_registered"));
        }

        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        // role checks: Admins cannot register
        Set<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (roles.contains("ROLE_ADMIN")
                || roles.contains("ROLE_BUILDING_ADMIN") || roles.contains("ROLE_CENTRAL_ADMIN")) {
            return ResponseEntity.status(403).body("Admins cannot register for events");
        }

        // club associates cannot register for their own club events
        if (roles.contains("ROLE_CLUB_ASSOCIATE") && user.getClubId() != null && event.getClubId() != null
                && user.getClubId().equals(event.getClubId())) {
            return ResponseEntity.status(403).body("Club Associates cannot register for their own club events");
        }

        // creators cannot register for their own events (covers missing clubId cases)
        if (event.getCreatedBy() != null && user.getUsername() != null && event.getCreatedBy().getUsername().equals(user.getUsername())) {
            return ResponseEntity.status(403).body("Event creators cannot register for their own events");
        }

        // registration deadline: closed 2 days before start and disallow past events
        if (event.getStartTime() != null) {
            if (event.getStartTime().isBefore(LocalDateTime.now().plusSeconds(1))) {
                return ResponseEntity.status(403).body("Cannot register for past events");
            }
            if (LocalDateTime.now().isAfter(event.getStartTime().minusDays(2))) {
                return ResponseEntity.status(403).body("Registration closed 2 days before event start");
            }
        }

        // capacity check
        if (event.getMaxAttendees() != null && event.getMaxAttendees() > 0) {
            long currentCount = registrationRepository.findByEvent_Id(eventId).size();
            if (currentCount >= event.getMaxAttendees()) {
                return ResponseEntity.status(403).body("Event is full");
            }
        }

        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        registrationRepository.save(reg);

        // Backfill existing event notifications to late registrants so they can see prior announcements.
        try {
            if (user.getId() != null) {
                List<NotificationMessage> existingMessages = notificationMessageRepository.findByEvent_IdOrderByCreatedAtDesc(eventId);
                List<Long> alreadyDelivered = notificationDeliveryRepository.findDeliveredNotificationIdsForUserEvent(user.getId(), eventId);
                java.util.HashSet<Long> alreadySet = new java.util.HashSet<>(alreadyDelivered);
                java.util.List<NotificationDelivery> toCreate = new java.util.ArrayList<>();
                for (NotificationMessage nm : existingMessages) {
                    if (nm == null || nm.getId() == null) continue;
                    if (alreadySet.contains(nm.getId())) continue;
                    NotificationDelivery d = new NotificationDelivery();
                    d.setNotification(nm);
                    d.setUser(user);
                    d.setDeliveryStatus(NotificationStatus.PENDING);
                    toCreate.add(d);
                }
                if (!toCreate.isEmpty()) {
                    notificationDeliveryRepository.saveAll(toCreate);
                }
            }
        } catch (Exception ignored) {
            // Best-effort backfill; do not fail registration if backfill fails.
        }

        // Keep legacy email-based registrations in sync so older UI endpoints (e.g. /api/registrations/mine)
        // also reflect this registration.
        try {
            String email = user.getEmail();
            if (email != null && !legacyRegistrationRepository.existsByEventIdAndEmail(eventId, email)) {
                Registration legacy = new Registration();
                legacy.setEvent(event);
                legacy.setEmail(email);
                Object fullName = payload != null ? payload.get("fullName") : null;
                legacy.setFullName(fullName != null ? String.valueOf(fullName) : user.getUsername());
                legacyRegistrationRepository.save(legacy);
            }
        } catch (Exception ignored) {
            // Best-effort sync; do not fail registration if legacy table insert fails.
        }

        String subject = "Registration confirmed: " + event.getTitle();
        String msg = "You are registered for '" + event.getTitle() + "' starting at " + event.getStartTime() + ".";
        notificationService.notifyAllChannels(user, subject, msg);
        return ResponseEntity.ok(Map.of("registrationId", reg.getId()));
    }

    @GetMapping("/event-registrations/mine")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> myRegistrations(@AuthenticationPrincipal UserDetails principal) {
        List<EventRegistration> regs = registrationRepository.findByUserUsernameWithEvent(principal.getUsername());
        List<Map<String, Object>> body = regs.stream().map(r -> {
            HashMap<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("eventId", r.getEvent() != null ? r.getEvent().getId() : null);
            m.put("title", r.getEvent() != null ? r.getEvent().getTitle() : null);
            m.put("startTime", r.getEvent() != null ? r.getEvent().getStartTime() : null);
            m.put("endTime", r.getEvent() != null ? r.getEvent().getEndTime() : null);
            m.put("location", r.getEvent() != null ? r.getEvent().getLocation() : null);
            m.put("registeredAt", r.getRegisteredAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
