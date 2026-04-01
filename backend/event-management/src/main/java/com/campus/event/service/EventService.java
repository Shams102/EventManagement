package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventRegistration;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository eventRegistrationRepository,
                        RegistrationRepository registrationRepository,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.registrationRepository = registrationRepository;
        this.notificationService = notificationService;
    }

    public List<Event> getPublicEvents() {
        return eventRepository.findByIsPublicTrue();
    }

    public Event createEvent(String title, String description, LocalDateTime start, LocalDateTime end,
                              User creator, String location, String clubId, String registrationSchema,
                              Integer maxAttendees) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setCreatedBy(creator);
        event.setPublic(true);
        event.setLocation(location != null && !location.isBlank() ? location : "TBD");
        if (clubId != null && !clubId.isBlank()) event.setClubId(clubId);
        else if (creator != null) event.setClubId(creator.getClubId());
        event.setRegistrationSchema(registrationSchema);
        event.setMaxAttendees(maxAttendees);
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(Long eventId, String username) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getCreatedBy() == null || !event.getCreatedBy().getUsername().equals(username)) {
            throw new SecurityException("Only the event creator can cancel this event");
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

        // Delete related records (no cascade configured)
        eventRegistrationRepository.deleteByEvent_Id(eventId);
        registrationRepository.deleteByEvent_Id(eventId);

        // Delete the event
        eventRepository.delete(event);
    }
}
