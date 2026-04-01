package com.campus.event.service;

import com.campus.event.domain.Booking;
import com.campus.event.domain.Room;
import com.campus.event.domain.User;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private BookingService bookingService;

    private Room room;
    private User user;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setId(1L);
        room.setRoomNumber("R101");
        room.setName("Conference Room A");

        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    @Test
    void createBooking_success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findByRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eq(1L), eq(end), eq(start))).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(1L, user, start, end, "Team meeting");

        assertNotNull(result);
        assertEquals(room, result.getRoom());
        assertEquals(user, result.getUser());
        assertEquals(start, result.getStartTime());
        assertEquals(end, result.getEndTime());
        assertEquals("Team meeting", result.getPurpose());
    }

    @Test
    void createBooking_roomNotFound_throwsException() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking(99L, user,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2),
                        "Purpose"));
    }

    @Test
    void createBooking_conflictingTime_throwsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        Booking existingBooking = new Booking();
        existingBooking.setRoom(room);
        existingBooking.setStartTime(start);
        existingBooking.setEndTime(end);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findByRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eq(1L), eq(end), eq(start))).thenReturn(List.of(existingBooking));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(1L, user, start, end, "Meeting"));

        assertTrue(ex.getMessage().contains("already booked"));
    }
}
