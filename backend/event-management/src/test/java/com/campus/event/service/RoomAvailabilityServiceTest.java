package com.campus.event.service;

import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.RoomBookingRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityServiceTest {

    @Mock
    private RoomBookingRequestRepository requestRepo;

    @InjectMocks
    private RoomAvailabilityService availabilityService;

    private Room room1;
    private Room room2;

    @BeforeEach
    void setUp() {
        room1 = new Room();
        room1.setId(1L);
        room1.setRoomNumber("R101");

        room2 = new Room();
        room2.setId(2L);
        room2.setRoomNumber("R102");
    }

    @Test
    void isRoomAvailable_noConflicts_returnsTrue() {
        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of());

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        assertTrue(availabilityService.isRoomAvailable(1L, start, end));
    }

    @Test
    void isRoomAvailable_overlappingBooking_returnsFalse() {
        LocalDateTime bookingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime bookingEnd = bookingStart.plusHours(2);

        RoomBookingRequest existing = new RoomBookingRequest();
        existing.setAllocatedRoom(room1);
        existing.setMeetingStart(bookingStart);
        existing.setMeetingEnd(bookingEnd);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        // Query time overlaps with the existing booking
        LocalDateTime queryStart = bookingStart.plusMinutes(30);
        LocalDateTime queryEnd = bookingEnd.plusMinutes(30);

        assertFalse(availabilityService.isRoomAvailable(1L, queryStart, queryEnd));
    }

    @Test
    void isRoomAvailable_differentRoom_returnsTrue() {
        LocalDateTime bookingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime bookingEnd = bookingStart.plusHours(2);

        RoomBookingRequest existing = new RoomBookingRequest();
        existing.setAllocatedRoom(room1); // Room 1 is booked
        existing.setMeetingStart(bookingStart);
        existing.setMeetingEnd(bookingEnd);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        // Querying Room 2 — should be available
        assertTrue(availabilityService.isRoomAvailable(2L, bookingStart, bookingEnd));
    }

    @Test
    void availabilityForRooms_mixedResults() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        RoomBookingRequest existing = new RoomBookingRequest();
        existing.setAllocatedRoom(room1);
        existing.setMeetingStart(start);
        existing.setMeetingEnd(end);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        Map<Long, Boolean> result = availabilityService.availabilityForRooms(
                List.of(1L, 2L), start, end);

        assertFalse(result.get(1L)); // Room 1 is booked
        assertTrue(result.get(2L));  // Room 2 is available
    }

    @Test
    void isRoomAvailable_nonOverlappingTime_returnsTrue() {
        LocalDateTime bookingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime bookingEnd = bookingStart.plusHours(2); // 10:00 - 12:00

        RoomBookingRequest existing = new RoomBookingRequest();
        existing.setAllocatedRoom(room1);
        existing.setMeetingStart(bookingStart);
        existing.setMeetingEnd(bookingEnd);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        // Query after the existing booking: 14:00 - 16:00
        LocalDateTime queryStart = bookingStart.plusHours(4);
        LocalDateTime queryEnd = queryStart.plusHours(2);

        assertTrue(availabilityService.isRoomAvailable(1L, queryStart, queryEnd));
    }
}
