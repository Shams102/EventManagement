package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.Registration;
import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RegistrationRepository;
import com.campus.event.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setTitle("Tech Talk");
        event.setStartTime(LocalDateTime.now().plusDays(10));
        event.setEndTime(LocalDateTime.now().plusDays(10).plusHours(2));
    }

    @Test
    void registerForEvent_success() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndEmail(1L, "student@example.com")).thenReturn(false);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        Registration result = registrationService.registerForEvent(1L, "student@example.com", "John Doe");

        assertNotNull(result);
        assertEquals(event, result.getEvent());
        assertEquals("student@example.com", result.getEmail());
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void registerForEvent_eventNotFound_throwsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                registrationService.registerForEvent(99L, "test@example.com", "Test"));
    }

    @Test
    void registerForEvent_alreadyRegistered_throwsException() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndEmail(1L, "student@example.com")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                registrationService.registerForEvent(1L, "student@example.com", "John"));

        assertTrue(ex.getMessage().contains("Already registered"));
    }

    @Test
    void registerForEvent_closedTwoDaysBefore_throwsException() {
        event.setStartTime(LocalDateTime.now().plusDays(1)); // Less than 2 days away

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                registrationService.registerForEvent(1L, "student@example.com", "John"));

        assertTrue(ex.getMessage().contains("Registration closed"));
    }

    @Test
    void registerForEvent_adminRole_prevented() {
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(Role.ADMIN));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndEmail(1L, "admin@example.com")).thenReturn(false);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                registrationService.registerForEvent(1L, "admin@example.com", "Admin User"));

        assertTrue(ex.getMessage().contains("Admins and Faculty"));
    }

    @Test
    void registerForEvent_creatorCannotRegister() {
        User creator = new User();
        creator.setUsername("creator1");
        creator.setEmail("creator@example.com");
        creator.setRoles(Set.of(Role.GENERAL_USER));
        event.setCreatedBy(creator);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndEmail(1L, "creator@example.com")).thenReturn(false);
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(creator));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                registrationService.registerForEvent(1L, "creator@example.com", "Creator"));

        assertTrue(ex.getMessage().contains("creators cannot register"));
    }

    @Test
    void registerForEvent_clubAssociateOwnClub_prevented() {
        event.setClubId("TECH_CLUB");

        User clubMember = new User();
        clubMember.setUsername("clubmember");
        clubMember.setEmail("club@example.com");
        clubMember.setRoles(Set.of(Role.CLUB_ASSOCIATE));
        clubMember.setClubId("TECH_CLUB");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndEmail(1L, "club@example.com")).thenReturn(false);
        when(userRepository.findByEmail("club@example.com")).thenReturn(Optional.of(clubMember));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                registrationService.registerForEvent(1L, "club@example.com", "Club Member"));

        assertTrue(ex.getMessage().contains("Club Associates cannot register"));
    }
}
