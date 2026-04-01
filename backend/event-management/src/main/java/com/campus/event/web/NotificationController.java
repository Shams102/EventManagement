package com.campus.event.web;

import com.campus.event.domain.Notification;
import com.campus.event.domain.User;
import com.campus.event.domain.Event;
import com.campus.event.domain.NotificationThread;
import com.campus.event.domain.NotificationDelivery;
import com.campus.event.domain.NotificationMessage;
import com.campus.event.domain.NotificationStatus;
import com.campus.event.repository.NotificationRepository;
import com.campus.event.service.NotificationCenterService;
import com.campus.event.repository.UserRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.NotificationMessageRepository;
import com.campus.event.repository.NotificationDeliveryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationCenterService centerService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final NotificationMessageRepository messageRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                  NotificationCenterService centerService,
                                  UserRepository userRepository,
                                  EventRepository eventRepository,
                                  NotificationMessageRepository messageRepository,
                                  NotificationDeliveryRepository deliveryRepository) {
        this.notificationRepository = notificationRepository;
        this.centerService = centerService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.messageRepository = messageRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> mine(@AuthenticationPrincipal UserDetails principal) {
        List<Notification> items = notificationRepository.findByUser_UsernameOrderByCreatedAtDesc(principal.getUsername());
        List<Map<String, Object>> body = items.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType().name());
            m.put("status", n.getStatus().name());
            m.put("subject", n.getSubject());
            m.put("message", n.getMessage());
            m.put("createdAt", n.getCreatedAt());
            m.put("sentAt", n.getSentAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> globalInbox(@AuthenticationPrincipal UserDetails principal) {
        List<com.campus.event.domain.NotificationDelivery> deliveries = centerService.getDeliveriesForUser(principal.getUsername());
        List<Map<String, Object>> body = deliveries.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("deliveryId", d.getId());
            m.put("id", d.getNotification().getId());
            m.put("title", d.getNotification().getTitle());
            m.put("message", d.getNotification().getMessage());
            m.put("origin", d.getNotification().getOrigin().name());
            m.put("urgency", d.getNotification().getUrgency().name());
            m.put("threadEnabled", d.getNotification().isThreadEnabled());
            m.put("read", d.getReadAt() != null);
            m.put("muted", d.isMuted());
            m.put("createdAt", d.getNotification().getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> broadcast(@AuthenticationPrincipal UserDetails principal, @RequestBody Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "");
        String message = (String) body.getOrDefault("message", "");
        String urg = (String) body.getOrDefault("urgency", "NORMAL");
        boolean threadEnabled = Boolean.TRUE.equals(body.get("threadEnabled"));
        com.campus.event.domain.Urgency urgency = com.campus.event.domain.Urgency.valueOf(urg);
        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        centerService.createBroadcast(title, message, urgency, threadEnabled, user);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/events/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY','CLUB_ASSOCIATE')")
    public ResponseEntity<?> createEventNotification(@PathVariable Long eventId, @AuthenticationPrincipal UserDetails principal, @RequestBody Map<String, Object> body) {
        Event ev = eventRepository.findById(eventId).orElse(null);
        if (ev == null) return ResponseEntity.notFound().build();
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        if (!isAdmin && (ev.getCreatedBy() == null || !principal.getUsername().equals(ev.getCreatedBy().getUsername()))) {
            return ResponseEntity.status(403).body("Only event owner or admin can post event notifications");
        }
        String title = (String) body.getOrDefault("title", "");
        String message = (String) body.getOrDefault("message", "");
        String urg = (String) body.getOrDefault("urgency", "NORMAL");
        boolean threadEnabled = Boolean.TRUE.equals(body.get("threadEnabled"));
        com.campus.event.domain.Urgency urgency = com.campus.event.domain.Urgency.valueOf(urg);
        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        centerService.createEventNotification(eventId, title, message, urgency, threadEnabled, user);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> eventInbox(@PathVariable Long eventId, @AuthenticationPrincipal UserDetails principal) {
        // only registered users (or admins) can view event-tailored messages
        boolean isOwner = false;
        try {
            isOwner = eventRepository.findById(eventId).map(e -> e.getCreatedBy() != null && e.getCreatedBy().getUsername().equals(principal.getUsername())).orElse(false);
        } catch (Exception ignored) { }
        // if not owner, check registration
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        // Backfill deliveries for owner/admin so they can see and discuss older event notifications.
        if (isOwner || isAdmin) {
            try {
                User u = userRepository.findByUsername(principal.getUsername()).orElse(null);
                if (u != null && u.getId() != null) {
                    List<NotificationMessage> msgs = messageRepository.findByEvent_IdOrderByCreatedAtDesc(eventId);
                    List<Long> delivered = deliveryRepository.findDeliveredNotificationIdsForUserEvent(u.getId(), eventId);
                    java.util.HashSet<Long> deliveredSet = new java.util.HashSet<>(delivered);
                    java.util.ArrayList<NotificationDelivery> toCreate = new java.util.ArrayList<>();
                    for (NotificationMessage nm : msgs) {
                        if (nm == null || nm.getId() == null) continue;
                        if (deliveredSet.contains(nm.getId())) continue;
                        NotificationDelivery d = new NotificationDelivery();
                        d.setNotification(nm);
                        d.setUser(u);
                        d.setDeliveryStatus(NotificationStatus.PENDING);
                        toCreate.add(d);
                    }
                    if (!toCreate.isEmpty()) {
                        deliveryRepository.saveAll(toCreate);
                    }
                }
            } catch (Exception ignored) {
                // best-effort backfill
            }
        }

        if (!isAdmin) {
            // check event registration repository through service deliveries
            List<com.campus.event.domain.NotificationDelivery> deliveries = centerService.getDeliveriesForUser(principal.getUsername());
            boolean hasForEvent = deliveries.stream().anyMatch(d -> d.getNotification().getEvent() != null && d.getNotification().getEvent().getId().equals(eventId));
            if (!hasForEvent && !isOwner) {
                return ResponseEntity.status(403).body(null);
            }
        }
        List<com.campus.event.domain.NotificationDelivery> deliveries = centerService.getDeliveriesForUser(principal.getUsername());
        List<Map<String, Object>> body = deliveries.stream()
                .filter(d -> d.getNotification().getEvent() != null && d.getNotification().getEvent().getId().equals(eventId))
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("deliveryId", d.getId());
                    m.put("id", d.getNotification().getId());
                    m.put("title", d.getNotification().getTitle());
                    m.put("message", d.getNotification().getMessage());
                    m.put("read", d.getReadAt() != null);
                    m.put("createdAt", d.getNotification().getCreatedAt());
                    m.put("threadEnabled", d.getNotification().isThreadEnabled());
                    return m;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/deliveries/{deliveryId}/mark-read")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<?> markRead(@PathVariable Long deliveryId, @AuthenticationPrincipal UserDetails principal) {
        try {
            centerService.markDeliveryRead(deliveryId, principal.getUsername());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deliveries/{deliveryId}/mute")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<?> mute(@PathVariable Long deliveryId, @AuthenticationPrincipal UserDetails principal, @RequestBody Map<String, Object> body) {
        boolean mute = Boolean.TRUE.equals(body.get("mute"));
        try {
            centerService.muteDelivery(deliveryId, principal.getUsername(), mute);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", se.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Threads
    @PostMapping("/threads")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<?> createThread(@AuthenticationPrincipal UserDetails principal, @RequestBody Map<String, Object> body) {
        Long notificationId = body.get("notificationId") == null ? null : Long.valueOf(String.valueOf(body.get("notificationId")));
        Long eventId = body.get("eventId") == null ? null : Long.valueOf(String.valueOf(body.get("eventId")));
        String title = (String) body.getOrDefault("title", null);
        String message = (String) body.getOrDefault("message", null);
        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (notificationId != null) {
            NotificationThread t = centerService.createThreadForNotification(notificationId, title, message, user);
            return ResponseEntity.ok(Map.of("threadId", t.getId()));
        } else if (eventId != null) {
            NotificationThread t = centerService.createThreadForEvent(eventId, title, message, user);
            return ResponseEntity.ok(Map.of("threadId", t.getId()));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "notificationId or eventId required"));
        }
    }

    @GetMapping("/threads/{threadId}/messages")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getThreadMessages(@PathVariable Long threadId) {
        List<com.campus.event.domain.ThreadMessage> msgs = centerService.getMessages(threadId);
        List<Map<String, Object>> body = msgs.stream().map(m -> {
            Map<String, Object> mm = new HashMap<>();
            mm.put("id", m.getId());
            mm.put("author", m.getAuthor() != null ? m.getAuthor().getUsername() : null);
            mm.put("content", m.getContent());
            mm.put("createdAt", m.getCreatedAt());
            return mm;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/threads/{threadId}/messages")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<?> postThreadMessage(@PathVariable Long threadId, @AuthenticationPrincipal UserDetails principal, @RequestBody Map<String, Object> body) {
        String content = (String) body.getOrDefault("content", "");
        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        com.campus.event.domain.ThreadMessage m = centerService.postThreadMessage(threadId, user, content);
        return ResponseEntity.ok(Map.of("id", m.getId()));
    }

    @PostMapping("/threads/{threadId}/close")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<?> closeThread(@PathVariable Long threadId, @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        centerService.closeThread(threadId, user);
        return ResponseEntity.ok(Map.of("status", "closed"));
    }
}
