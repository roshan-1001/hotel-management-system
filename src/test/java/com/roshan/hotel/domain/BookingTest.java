package com.roshan.hotel.domain;

import com.roshan.hotel.enums.BookingStatus;
import com.roshan.hotel.enums.RoomType;
import com.roshan.hotel.exception.InvalidBookingStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingTest {

    private Booking createBooking() {

        Guest guest = new Guest(1, "Roshan");

        Room room = new Room(
                101,
                RoomType.DELUXE
        );

        Receptionist receptionist = new Receptionist(
                1,
                "Receptionist 1",
                LocalDate.of(2025, 1, 1)
        );

        return new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );
    }


    @Test
    void shouldCreateBookingInPendingState() {

        Booking booking = createBooking();

        assertEquals(
                BookingStatus.PENDING,
                booking.getStatus()
        );
    }


    @Test
    void shouldConfirmPendingBooking() {

        Booking booking = createBooking();

        booking.markConfirmed();

        assertEquals(
                BookingStatus.CONFIRMED,
                booking.getStatus()
        );
    }


    @Test
    void shouldCancelPendingBooking() {

        Booking booking = createBooking();

        booking.markCancelled();

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );
    }


    @Test
    void shouldRejectCancellationAfterCheckIn() {

        Booking booking = createBooking();

        booking.markConfirmed();
        booking.markCheckedIn();

        assertThrows(
                InvalidBookingStateException.class,
                booking::markCancelled
        );
    }
}