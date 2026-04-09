package com.campus.event.service;

import com.campus.event.domain.Building;
import com.campus.event.domain.Event;
import com.campus.event.domain.EventRegistration;
import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.EventTimingModel;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.EventTimeSlotRepository;
import com.campus.event.repository.NotificationDeliveryRepository;
import com.campus.event.repository.NotificationMessageRepository;
import com.campus.event.repository.NotificationThreadRepository;
import com.campus.event.repository.RegistrationRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.ThreadMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final RegistrationRepository registrationRepository;
    private final EventTimeSlotRepository eventTimeSlotRepository;
    private final NotificationService notificationService;
    private final RoomBookingRequestRepository roomBookingRequestRepository;
    private final ThreadMessageRepository threadMessageRepository;
    private final NotificationThreadRepository notificationThreadRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationMessageRepository notificationMessageRepository;

    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository eventRegistrationRepository,
                        RegistrationRepository registrationRepository,
                        EventTimeSlotRepository eventTimeSlotRepository,
                        NotificationService notificationService,
                        RoomBookingRequestRepository roomBookingRequestRepository,
                        ThreadMessageRepository threadMessageRepository,
                        NotificationThreadRepository notificationThreadRepository,
                        NotificationDeliveryRepository notificationDeliveryRepository,
                        NotificationMessageRepository notificationMessageRepository) {
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.registrationRepository = registrationRepository;
        this.eventTimeSlotRepository = eventTimeSlotRepository;
        this.notificationService = notificationService;
        this.roomBookingRequestRepository = roomBookingRequestRepository;
        this.threadMessageRepository = threadMessageRepository;
        this.notificationThreadRepository = notificationThreadRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationMessageRepository = notificationMessageRepository;
    }

    public List<Event> getPublicEvents() {
        return eventRepository.findByIsPublicTrue();
    }

    /**
     * Original single-day create (backward compatible).
     */
    public Event createEvent(String title, String description, LocalDateTime start, LocalDateTime end,
                              User creator, Building building, String location, String clubId,
                              String registrationSchema, Integer maxAttendees) {
        return createEvent(title, description, start, end, creator, building, location, clubId,
                registrationSchema, maxAttendees, null, null);
    }

    /**
     * Full create supporting multi-day timing models.
     */
    @Transactional
    public Event createEvent(String title, String description, LocalDateTime start, LocalDateTime end,
                              User creator, Building building, String location, String clubId,
                              String registrationSchema, Integer maxAttendees,
                              EventTimingModel timingModel,
                              List<EventTimeSlot> explicitSlots) {

        if (creator != null) {
            // Anti-duplicate protection (idempotency against rapid multi-clicking)
            if (eventRepository.existsByTitleAndStartTimeAndCreatedBy_Id(title, start, creator.getId())) {
                throw new IllegalArgumentException("Duplicate event detected. You already have an event with this title starting at this time.");
            }
            // Basic user-level overlap boundary protection
            if (eventRepository.hasOverlappingEvents(creator.getId(), start, end)) {
                throw new IllegalArgumentException("Overlap detected! You are already hosting another event that conflicts with this timeframe.");
            }
        }

        if (timingModel == null) {
            timingModel = EventTimingModel.SINGLE_DAY;
        }

        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setCreatedBy(creator);
        event.setBuilding(building);
        event.setPublic(true);
        event.setLocation(location != null && !location.isBlank() ? location : "TBD");
        if (clubId != null && !clubId.isBlank()) event.setClubId(clubId);
        else if (creator != null) event.setClubId(creator.getClubId());
        event.setRegistrationSchema(registrationSchema);
        event.setMaxAttendees(maxAttendees);
        event.setTimingModel(timingModel);

        Event saved = eventRepository.save(event);

        // Generate time slots based on timing model
        List<EventTimeSlot> slots = generateTimeSlots(saved, timingModel, explicitSlots);
        eventTimeSlotRepository.saveAll(slots);

        return saved;
    }

    /**
     * Generates time slots based on the timing model:
     * - SINGLE_DAY / MULTI_DAY_CONTINUOUS: single slot from event start to end
     * - MULTI_DAY_FIXED: one slot per day with the same daily start/end time
     * - FLEXIBLE: uses the explicit slots provided by the user
     */
    private List<EventTimeSlot> generateTimeSlots(Event event, EventTimingModel model,
                                                   List<EventTimeSlot> explicitSlots) {
        List<EventTimeSlot> slots = new ArrayList<>();

        switch (model) {
            case SINGLE_DAY:
            case MULTI_DAY_CONTINUOUS:
                // One continuous block
                slots.add(new EventTimeSlot(event, event.getStartTime(), event.getEndTime(), 0));
                break;

            case MULTI_DAY_FIXED:
                // Repeat the same daily time window across each day
                LocalDate startDate = event.getStartTime().toLocalDate();
                LocalDate endDate = event.getEndTime().toLocalDate();
                LocalTime dailyStart = event.getStartTime().toLocalTime();
                LocalTime dailyEnd = event.getEndTime().toLocalTime();

                int dayIdx = 0;
                LocalDate current = startDate;
                while (!current.isAfter(endDate)) {
                    LocalDateTime slotStart = LocalDateTime.of(current, dailyStart);
                    LocalDateTime slotEnd;
                    if (!dailyEnd.isAfter(dailyStart)) { // e.g., 6 PM to 2 AM next day
                        slotEnd = LocalDateTime.of(current.plusDays(1), dailyEnd);
                    } else {
                        slotEnd = LocalDateTime.of(current, dailyEnd);
                    }
                    slots.add(new EventTimeSlot(event, slotStart, slotEnd, dayIdx));
                    dayIdx++;
                    current = current.plusDays(1);
                }
                break;

            case FLEXIBLE:
                // User-provided slots
                if (explicitSlots != null && !explicitSlots.isEmpty()) {
                    int idx = 0;
                    for (EventTimeSlot s : explicitSlots) {
                        s.setEvent(event);
                        s.setDayIndex(idx++);
                        slots.add(s);
                    }
                } else {
                    // Fallback: single slot
                    slots.add(new EventTimeSlot(event, event.getStartTime(), event.getEndTime(), 0));
                }
                break;
        }

        return slots;
    }

    @Transactional
    public void deleteEvent(Long eventId, String username) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getCreatedBy() == null || !event.getCreatedBy().getUsername().equals(username)) {
            throw new SecurityException("Only the event creator can cancel this event");
        }

        boolean hasAllocatedRoom = roomBookingRequestRepository.existsByEvent_IdAndStatusIn(
                eventId, Set.of(com.campus.event.domain.RoomBookingStatus.APPROVED, com.campus.event.domain.RoomBookingStatus.CONFIRMED));
        if (hasAllocatedRoom) {
            throw new IllegalStateException("Cannot cancel event: Room already allocated.");
        }

        if (event.getStartTime() != null && event.getStartTime().isBefore(LocalDateTime.now().plusDays(2))) {
            throw new IllegalStateException("Cannot cancel event: Less than 2 days remaining.");
        }

        // Notify registered users about cancellation before deleting
        List<EventRegistration> regs = eventRegistrationRepository.findByEvent_Id(eventId);
        if (!regs.isEmpty()) {
            String subject = "Event cancelled: " + event.getTitle();
            String body = "The event \"" + event.getTitle() + "\" scheduled for " + event.getStartTime()
                    + " has been cancelled by the organizer.";
            for (EventRegistration r : regs) {
                if (r.getUser() != null) {
                    notificationService.notifyAllChannels(r.getUser(), subject, body);
                }
            }
        }

        // Delete related records
        roomBookingRequestRepository.deleteByEvent_Id(eventId);
        eventTimeSlotRepository.deleteByEvent_Id(eventId);
        eventRegistrationRepository.deleteByEvent_Id(eventId);
        registrationRepository.deleteByEvent_Id(eventId);
        threadMessageRepository.deleteByThread_Event_Id(eventId);
        threadMessageRepository.deleteByThread_Notification_Event_Id(eventId);
        notificationThreadRepository.deleteByEvent_Id(eventId);
        notificationThreadRepository.deleteByNotification_Event_Id(eventId);
        notificationDeliveryRepository.deleteByNotification_Event_Id(eventId);
        notificationMessageRepository.deleteByEvent_Id(eventId);

        // Delete the event
        eventRepository.delete(event);
    }
}

