package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private User creator;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setUsername("faculty1");
        creator.setEmail("faculty1@example.com");
        creator.setClubId("CS_CLUB");
    }

    @Test
    void createEvent_savesWithCorrectFields() {
        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = start.plusHours(2);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event result = eventService.createEvent("Tech Talk", "A description", start, end,
                creator, "Room 101", "CS_CLUB", "[\"name\",\"email\"]", null);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();

        assertEquals("Tech Talk", saved.getTitle());
        assertEquals("A description", saved.getDescription());
        assertEquals(start, saved.getStartTime());
        assertEquals(end, saved.getEndTime());
        assertEquals(creator, saved.getCreatedBy());
        assertEquals("Room 101", saved.getLocation());
        assertEquals("CS_CLUB", saved.getClubId());
        assertEquals("[\"name\",\"email\"]", saved.getRegistrationSchema());
        assertTrue(saved.isPublic());
    }

    @Test
    void createEvent_defaultsLocationToTBD_whenNull() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event result = eventService.createEvent("Event", "Desc",
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1),
                creator, null, null, null, null);

        assertEquals("TBD", result.getLocation());
    }

    @Test
    void createEvent_defaultsLocationToTBD_whenBlank() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event result = eventService.createEvent("Event", "Desc",
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1),
                creator, "   ", null, null, null);

        assertEquals("TBD", result.getLocation());
    }

    @Test
    void createEvent_usesCreatorClubId_whenClubIdNull() {
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event result = eventService.createEvent("Event", "Desc",
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1),
                creator, "Room A", null, null, null);

        assertEquals("CS_CLUB", result.getClubId());
    }

    @Test
    void getPublicEvents_delegatesToRepository() {
        List<Event> expected = List.of(new Event());
        when(eventRepository.findByIsPublicTrue()).thenReturn(expected);

        List<Event> result = eventService.getPublicEvents();

        assertEquals(expected, result);
        verify(eventRepository).findByIsPublicTrue();
    }
}
