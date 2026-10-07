package com.roshan.hotel.service;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.Guest;
import com.roshan.hotel.domain.Receptionist;
import com.roshan.hotel.domain.Room;
import com.roshan.hotel.dto.BookingResponse;
import com.roshan.hotel.domain.IdempotencyRecord;
import com.roshan.hotel.enums.BookingStatus;
import com.roshan.hotel.enums.IdempotencyOperation;
import com.roshan.hotel.enums.IdempotencyStatus;
import com.roshan.hotel.exception.BookingNotFoundException;
import com.roshan.hotel.exception.GuestNotFoundException;
import com.roshan.hotel.exception.IdempotencyConflictException;
import com.roshan.hotel.exception.InvalidBookingStateException;
import com.roshan.hotel.exception.ReceptionistNotFoundException;
import com.roshan.hotel.exception.RoomNotAvailableException;
import com.roshan.hotel.exception.RoomNotFoundException;
import com.roshan.hotel.repository.BookingRepository;
import com.roshan.hotel.repository.GuestRepository;
import com.roshan.hotel.repository.ReceptionistRepository;
import com.roshan.hotel.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ReceptionistRepository receptionistRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private Clock clock;

    @InjectMocks
    private BookingService bookingService;


    // -------------------------------------------------------------------------
    // getBooking
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnBookingWhenBookingExists() {

        Booking booking = mock(Booking.class);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.getBooking(1L);

        assertEquals(booking, result);

        verify(bookingRepository).findById(1L);
    }


    @Test
    void shouldThrowWhenBookingDoesNotExist() {

        when(bookingRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.getBooking(999L)
        );

        verify(bookingRepository).findById(999L);
    }


    // -------------------------------------------------------------------------
    // confirmBooking
    // -------------------------------------------------------------------------

    @Test
    void shouldConfirmBooking() {

        Booking booking = mock(Booking.class);

        Instant confirmedAt =
                Instant.parse("2026-10-07T10:00:00Z");

        when(bookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        when(clock.instant())
                .thenReturn(confirmedAt);

        bookingService.confirmBooking(1L);

        verify(booking)
                .confirmPayment(confirmedAt);

        // Booking is managed by JPA, so explicit save is unnecessary.
        verify(bookingRepository, never())
                .save(any());
    }


    // -------------------------------------------------------------------------
    // checkIn
    // -------------------------------------------------------------------------

    @Test
    void shouldCheckInBooking() {

        Booking booking = mock(Booking.class);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        stubBookingResponse(booking, BookingStatus.CHECKED_IN);

        BookingResponse response =
                bookingService.checkIn(1L);

        verify(booking)
                .markCheckedIn();

        assertEquals(1L, response.id());
        assertEquals(10, response.guestId());
        assertEquals(101, response.roomNumber());
        assertEquals(
                BookingStatus.CHECKED_IN,
                response.status()
        );
    }


    // -------------------------------------------------------------------------
    // checkOut
    // -------------------------------------------------------------------------

    @Test
    void shouldCheckOutBooking() {

        Booking booking = mock(Booking.class);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        stubBookingResponse(booking, BookingStatus.CHECKED_OUT);

        BookingResponse response =
                bookingService.checkOut(1L);

        verify(booking)
                .markCheckedOut();

        assertEquals(1L, response.id());
        assertEquals(10, response.guestId());
        assertEquals(101, response.roomNumber());
        assertEquals(
                BookingStatus.CHECKED_OUT,
                response.status()
        );
    }


    // -------------------------------------------------------------------------
    // cancelBooking
    // -------------------------------------------------------------------------

    @Test
    void shouldCancelBooking() {

        Booking booking = mock(Booking.class);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        stubBookingResponse(booking, BookingStatus.CANCELLED);

        BookingResponse response =
                bookingService.cancelBooking(1L);

        verify(booking)
                .markCancelled();

        assertEquals(1L, response.id());
        assertEquals(10, response.guestId());
        assertEquals(101, response.roomNumber());
        assertEquals(
                BookingStatus.CANCELLED,
                response.status()
        );

        // Dirty checking handles the update.
        verify(bookingRepository, never())
                .save(any());
    }


    @Test
    void shouldRejectCancellationWhenBookingDoesNotExist() {

        when(bookingRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.cancelBooking(999L)
        );

        verify(bookingRepository)
                .findById(999L);

        verify(bookingRepository, never())
                .save(any());
    }


    @Test
    void shouldRejectInvalidCancellation() {

        Booking booking = mock(Booking.class);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        doThrow(
                new InvalidBookingStateException(
                        "Booking cannot be cancelled"
                )
        ).when(booking).markCancelled();

        assertThrows(
                InvalidBookingStateException.class,
                () -> bookingService.cancelBooking(1L)
        );

        verify(booking)
                .markCancelled();

        verify(bookingRepository, never())
                .save(any());
    }


    // =========================================================================
    // createBooking
    // =========================================================================

    @Test
    void shouldCreateBookingSuccessfully() {

        int guestId = 1;
        int roomNumber = 101;
        int receptionistId = 10;

        LocalDate startDate =
                LocalDate.of(2026, 10, 10);

        LocalDate endDate =
                LocalDate.of(2026, 10, 12);

        BigDecimal totalAmount =
                new BigDecimal("12000");

        String idempotencyKey =
                "booking-success-123";

        Instant now =
                Instant.parse("2026-10-07T10:00:00Z");

        Guest guest = mock(Guest.class);
        Room room = mock(Room.class);
        Receptionist receptionist = mock(Receptionist.class);
        Booking savedBooking = mock(Booking.class);

        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(idempotencyKey),
                anyString()
        )).thenReturn(true);

        when(guestRepository.findById(guestId))
                .thenReturn(Optional.of(guest));

        when(roomRepository.findByNumberForUpdate(roomNumber))
                .thenReturn(Optional.of(room));

        when(receptionistRepository.findById(receptionistId))
                .thenReturn(Optional.of(receptionist));

        // No existing bookings for this room.
        when(bookingRepository.findByRoom(room))
                .thenReturn(List.of());

        when(clock.instant())
                .thenReturn(now);

        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(savedBooking);

        when(savedBooking.getId())
                .thenReturn(100L);

        when(savedBooking.getGuest())
                .thenReturn(guest);

        when(savedBooking.getRoom())
                .thenReturn(room);

        when(savedBooking.getStartDate())
                .thenReturn(startDate);

        when(savedBooking.getEndDate())
                .thenReturn(endDate);

        when(savedBooking.getTotalAmount())
                .thenReturn(totalAmount);

        when(savedBooking.getStatus())
                .thenReturn(BookingStatus.PENDING_PAYMENT);

        when(guest.getId())
                .thenReturn(guestId);

        when(room.getNumber())
                .thenReturn(roomNumber);

        BookingResponse response =
                bookingService.createBooking(
                        guestId,
                        roomNumber,
                        receptionistId,
                        startDate,
                        endDate,
                        totalAmount,
                        idempotencyKey
                );

        assertEquals(100L, response.id());
        assertEquals(guestId, response.guestId());
        assertEquals(roomNumber, response.roomNumber());
        assertEquals(
                BookingStatus.PENDING_PAYMENT,
                response.status()
        );

        verify(idempotencyService)
                .claim(
                        eq(IdempotencyOperation.BOOKING),
                        eq(idempotencyKey),
                        anyString()
                );

        verify(roomRepository)
                .findByNumberForUpdate(roomNumber);

        verify(bookingRepository)
                .save(any(Booking.class));

        verify(idempotencyService)
                .complete(
                        IdempotencyOperation.BOOKING,
                        idempotencyKey,
                        100L
                );
    }


    // -------------------------------------------------------------------------
    // createBooking - Guest
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectBookingWhenGuestDoesNotExist() {

        String key = "booking-guest-missing";

        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(key),
                anyString()
        )).thenReturn(true);

        when(guestRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                GuestNotFoundException.class,
                () -> bookingService.createBooking(
                        999,
                        101,
                        10,
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        new BigDecimal("12000"),
                        key
                )
        );

        verify(roomRepository, never())
                .findByNumberForUpdate(anyInt());

        verify(bookingRepository, never())
                .save(any());

        verify(idempotencyService, never())
                .complete(
                        any(),
                        anyString(),
                        anyLong()
                );
    }


    // -------------------------------------------------------------------------
    // createBooking - Room
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectBookingWhenRoomDoesNotExist() {

        String key = "booking-room-missing";

        Guest guest = mock(Guest.class);

        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(key),
                anyString()
        )).thenReturn(true);

        when(guestRepository.findById(1))
                .thenReturn(Optional.of(guest));

        when(roomRepository.findByNumberForUpdate(999))
                .thenReturn(Optional.empty());

        assertThrows(
                RoomNotFoundException.class,
                () -> bookingService.createBooking(
                        1,
                        999,
                        10,
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        new BigDecimal("12000"),
                        key
                )
        );

        verify(receptionistRepository, never())
                .findById(anyInt());

        verify(bookingRepository, never())
                .save(any());

        verify(idempotencyService, never())
                .complete(
                        any(),
                        anyString(),
                        anyLong()
                );
    }


    // -------------------------------------------------------------------------
    // createBooking - Receptionist
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectBookingWhenReceptionistDoesNotExist() {

        String key = "booking-receptionist-missing";

        Guest guest = mock(Guest.class);
        Room room = mock(Room.class);

        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(key),
                anyString()
        )).thenReturn(true);

        when(guestRepository.findById(1))
                .thenReturn(Optional.of(guest));

        when(roomRepository.findByNumberForUpdate(101))
                .thenReturn(Optional.of(room));

        when(receptionistRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                ReceptionistNotFoundException.class,
                () -> bookingService.createBooking(
                        1,
                        101,
                        999,
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        new BigDecimal("12000"),
                        key
                )
        );

        verify(bookingRepository, never())
                .save(any());

        verify(idempotencyService, never())
                .complete(
                        any(),
                        anyString(),
                        anyLong()
                );
    }


    // -------------------------------------------------------------------------
    // createBooking - Room unavailable
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectBookingWhenRoomIsUnavailable() {

        String key = "booking-room-unavailable";

        LocalDate requestedStart =
                LocalDate.of(2026, 10, 10);

        LocalDate requestedEnd =
                LocalDate.of(2026, 10, 12);

        Guest guest = mock(Guest.class);
        Room room = mock(Room.class);
        Receptionist receptionist = mock(Receptionist.class);
        Booking existingBooking = mock(Booking.class);

        Instant now =
                Instant.parse("2026-10-07T10:00:00Z");

        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(key),
                anyString()
        )).thenReturn(true);

        when(guestRepository.findById(1))
                .thenReturn(Optional.of(guest));

        when(roomRepository.findByNumberForUpdate(101))
                .thenReturn(Optional.of(room));

        when(receptionistRepository.findById(10))
                .thenReturn(Optional.of(receptionist));

        when(clock.instant())
                .thenReturn(now);

        // Existing booking overlaps requested dates.
        when(existingBooking.getStartDate())
                .thenReturn(LocalDate.of(2026, 10, 9));

        when(existingBooking.getEndDate())
                .thenReturn(LocalDate.of(2026, 10, 11));

        when(existingBooking.blocksInventory(now))
                .thenReturn(true);

        when(bookingRepository.findByRoom(room))
                .thenReturn(List.of(existingBooking));

        assertThrows(
                RoomNotAvailableException.class,
                () -> bookingService.createBooking(
                        1,
                        101,
                        10,
                        requestedStart,
                        requestedEnd,
                        new BigDecimal("12000"),
                        key
                )
        );

        verify(bookingRepository, never())
                .save(any());

        verify(idempotencyService, never())
                .complete(
                        any(),
                        anyString(),
                        anyLong()
                );
    }


    // -------------------------------------------------------------------------
    // createBooking - Idempotency
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnExistingBookingForCompletedIdempotencyRequest() {

        String key = "booking-retry";

        Guest guest = mock(Guest.class);
        Room room = mock(Room.class);
        Booking existingBooking = mock(Booking.class);
        IdempotencyRecord record = mock(IdempotencyRecord.class);

        String[] requestHash =
                new String[1];

        /*
         * Capture the request hash generated by BookingService.
         *
         * This avoids duplicating the private hash-generation algorithm
         * inside the test.
         */
        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(key),
                anyString()
        )).thenAnswer(invocation -> {
            requestHash[0] =
                    invocation.getArgument(2);
            return false;
        });

        when(idempotencyService.get(
                IdempotencyOperation.BOOKING,
                key
        )).thenReturn(record);

        when(record.getRequestHash())
                .thenAnswer(invocation -> requestHash[0]);

        when(record.getStatus())
                .thenReturn(IdempotencyStatus.COMPLETED);

        when(record.getResourceId())
                .thenReturn(100L);

        when(bookingRepository.findById(100L))
                .thenReturn(Optional.of(existingBooking));

        stubBookingResponse(
                existingBooking,
                BookingStatus.PENDING_PAYMENT
        );

        when(existingBooking.getId())
                .thenReturn(100L);


        BookingResponse response =
                bookingService.createBooking(
                        1,
                        101,
                        10,
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        new BigDecimal("12000"),
                        key
                );

        assertEquals(100L, response.id());

        verify(bookingRepository)
                .findById(100L);

        // No new booking should be created.
        verify(bookingRepository, never())
                .save(any());

        verify(roomRepository, never())
                .findByNumberForUpdate(anyInt());

        verify(idempotencyService, never())
                .complete(
                        any(),
                        anyString(),
                        anyLong()
                );
    }


    @Test
    void shouldRejectIdempotencyKeyWhenRequestIsDifferent() {

        String key = "booking-conflict";

        IdempotencyRecord record =
                mock(IdempotencyRecord.class);

        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(key),
                anyString()
        )).thenReturn(false);

        when(idempotencyService.get(
                IdempotencyOperation.BOOKING,
                key
        )).thenReturn(record);

        when(record.getRequestHash())
                .thenReturn("different-request-hash");

        assertThrows(
                IdempotencyConflictException.class,
                () -> bookingService.createBooking(
                        1,
                        101,
                        10,
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        new BigDecimal("12000"),
                        key
                )
        );

        verify(bookingRepository, never())
                .save(any());

        verify(roomRepository, never())
                .findByNumberForUpdate(anyInt());
    }


    @Test
    void shouldRejectIdempotencyRequestWhenPreviousRequestIsStillProcessing() {

        String key = "booking-in-progress";

        IdempotencyRecord record =
                mock(IdempotencyRecord.class);

        String[] requestHash =
                new String[1];

        when(idempotencyService.claim(
                eq(IdempotencyOperation.BOOKING),
                eq(key),
                anyString()
        )).thenAnswer(invocation -> {
            requestHash[0] =
                    invocation.getArgument(2);
            return false;
        });

        when(idempotencyService.get(
                IdempotencyOperation.BOOKING,
                key
        )).thenReturn(record);

        when(record.getRequestHash())
                .thenAnswer(invocation -> requestHash[0]);

        when(record.getStatus())
                .thenReturn(IdempotencyStatus.IN_PROGRESS);

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(
                        1,
                        101,
                        10,
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        new BigDecimal("12000"),
                        key
                )
        );

        verify(bookingRepository, never())
                .save(any());

        verify(roomRepository, never())
                .findByNumberForUpdate(anyInt());
    }


    // =========================================================================
    // Test helpers
    // =========================================================================

    private void stubBookingResponse(
            Booking booking,
            BookingStatus status
    ) {

        Guest guest = mock(Guest.class);
        Room room = mock(Room.class);

        when(booking.getId())
                .thenReturn(1L);

        when(booking.getGuest())
                .thenReturn(guest);

        when(booking.getRoom())
                .thenReturn(room);

        when(booking.getStartDate())
                .thenReturn(LocalDate.of(2026, 10, 10));

        when(booking.getEndDate())
                .thenReturn(LocalDate.of(2026, 10, 12));

        when(booking.getTotalAmount())
                .thenReturn(new BigDecimal("12000"));

        when(booking.getStatus())
                .thenReturn(status);

        when(guest.getId())
                .thenReturn(10);

        when(room.getNumber())
                .thenReturn(101);
    }
}