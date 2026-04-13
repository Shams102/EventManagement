package com.campus.event.service;

import com.campus.event.domain.Booking;
import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.Room;
import com.campus.event.domain.User;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Booking createBooking(Long roomId, User user, LocalDateTime start, LocalDateTime end, String purpose) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));

        List<Booking> conflicts = bookingRepository
                .findByRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(roomId, end, start);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Room is already booked for the selected time range");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setPurpose(purpose);
        return bookingRepository.save(booking);
    }

    /**
     * Create a booking directly from an EventTimeSlot — uses slot.start and slot.end
     * as the booking boundaries (NOT the parent event's overall start/end).
     */
    @Transactional
    public Booking createBookingFromSlot(EventTimeSlot slot, Room room, User user, String purpose) {
        if (slot == null || room == null) {
            throw new IllegalArgumentException("Slot and room must not be null");
        }
        return createBooking(room.getId(), user, slot.getSlotStart(), slot.getSlotEnd(), purpose);
    }

    /**
     * Safe booking creation that runs in its own transaction and returns null on failure
     * instead of throwing. This is used during approval to avoid rolling back the
     * parent approval transaction when a booking conflict exists.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking createBookingSafe(Long roomId, User user, LocalDateTime start, LocalDateTime end, String purpose) {
        try {
            return createBooking(roomId, user, start, end, purpose);
        } catch (Exception e) {
            log.warn("Safe booking creation skipped (room={}, start={}, end={}): {}", roomId, start, end, e.getMessage());
            return null;
        }
    }
}
