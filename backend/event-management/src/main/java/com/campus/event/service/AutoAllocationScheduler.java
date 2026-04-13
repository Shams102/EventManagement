package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AutoAllocationScheduler {
    private static final Logger log = LoggerFactory.getLogger(AutoAllocationScheduler.class);

    private final RoomBookingRequestRepository requestRepo;
    private final RoomRepository roomRepo;
    private final RoomAvailabilityService availabilityService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.allocations.auto.enabled:false}")
    private boolean enabled;

    @Value("${app.allocations.auto.timeoutMinutes:120}")
    private int timeoutMinutes;

    public AutoAllocationScheduler(RoomBookingRequestRepository requestRepo, RoomRepository roomRepo, RoomAvailabilityService availabilityService,
                                   UserRepository userRepository, NotificationService notificationService) {
        this.requestRepo = requestRepo;
        this.roomRepo = roomRepo;
        this.availabilityService = availabilityService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Run every 10 minutes
    @Scheduled(cron = "0 */10 * * * *")
    public void autoAllocatePending() {
        if (!enabled) return;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<RoomBookingRequest> pending = requestRepo.findPendingOlderThan(cutoff);
        for (RoomBookingRequest r : pending) {
            try {
                LocalDateTime start = windowStart(r);
                LocalDateTime end = windowEnd(r);
                if (start == null || end == null || !end.isAfter(start)) continue;
                Room allocated = tryPreferences(r, start, end);
                if (allocated == null) allocated = tryAny(start, end);
                if (allocated != null) {
                    r.setAllocatedRoom(allocated);
                    r.setStatus(RoomBookingStatus.APPROVED);
                    r.setApprovedAt(LocalDateTime.now());
                    r.setApprovedByUsername("AUTO");
                    requestRepo.save(r);
                    log.info("Auto-allocated request {} to room {}", r.getId(), allocated.getName());
                    if (r.getRequestedByUsername() != null) {
                        final String subj = "Room request auto-approved";
                        final String msg = "Your room request (ID " + r.getId() + ") has been auto-approved for room '" + allocated.getName() + "'.";
                        userRepository.findByUsername(r.getRequestedByUsername())
                                .ifPresent(u -> notificationService.notifyAllChannels(u, subj, msg));
                    }
                }
            } catch (Exception e) {
                log.warn("Auto-allocation failed for request {}: {}", r.getId(), e.getMessage());
            }
        }
    }

    private Room tryPreferences(RoomBookingRequest r, LocalDateTime start, LocalDateTime end) {
        Room[] prefs = new Room[]{r.getPref1(), r.getPref2(), r.getPref3()};
        for (Room pref : prefs) {
            if (pref == null) continue;
            if (availabilityService.isRoomAvailable(pref.getId(), start, end)) return pref;
        }
        return null;
    }

    private Room tryAny(LocalDateTime start, LocalDateTime end) {
        for (Room room : roomRepo.findAll()) {
            if (availabilityService.isRoomAvailable(room.getId(), start, end)) return room;
        }
        return null;
    }

    private static LocalDateTime windowStart(RoomBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getStartTime() != null) return e.getStartTime();
        return r.getMeetingStart();
    }

    private static LocalDateTime windowEnd(RoomBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getEndTime() != null) return e.getEndTime();
        return r.getMeetingEnd();
    }
}