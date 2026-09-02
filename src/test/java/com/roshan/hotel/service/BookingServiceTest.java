package com.roshan.hotel.service;

import com.roshan.hotel.domain.*;
import com.roshan.hotel.exception.*;
import com.roshan.hotel.repository.*;
import com.roshan.hotel.dto.BookingResponse;
import com.roshan.hotel.enums.BookingStatus;
import com.roshan.hotel.enums.RoomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private BookingService bookingService;

    //ARRANGE
    @Test
    void shouldCreateBookingWhenRoomIsAvailable(){
        Guest guest = new Guest(1, "Roshan");

        Room room = new Room(
                101,
                RoomType.DELUXE
                // adapt this to your current Room constructor
        );

        Receptionist receptionist = new Receptionist(
                1,
                "Alice",
                LocalDate.of(2025, 1, 1)
                // adapt if your constructor differs
        );

        LocalDate startDate = LocalDate.of(2026, 9, 10);
        LocalDate endDate = LocalDate.of(2026, 9, 12);
        BigDecimal totalAmount = new BigDecimal("12000.00");


        when(guestRepository.findById(1))
                .thenReturn(Optional.of(guest));

        when(roomRepository.findByNumberForUpdate(101))
                .thenReturn(Optional.of(room));

        when(receptionistRepository.findById(1))
                .thenReturn(Optional.of(receptionist));

        when(bookingRepository.findByRoom(room))
                .thenReturn(List.of());

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BookingResponse response = bookingService.createBooking(
                1,
                101,
                1,
                startDate,
                endDate,
                totalAmount
        );

        // Assert
        assertEquals(1, response.guestId());
        assertEquals(101, response.roomNumber());
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
        assertEquals(totalAmount, response.totalAmount());
        assertEquals(BookingStatus.PENDING, response.status());

        verify(bookingRepository)
                .save(any(Booking.class));

    }

    @Test
    void shouldRejectBookingWhenRoomIsAlreadyBooked() {

        // Arrange
        Guest guest = new Guest(1, "Roshan");
        Room room = new Room(101, RoomType.DELUXE)/* your Room constructor */;
        Receptionist receptionist = new Receptionist(
                2,
                "Srag",
                LocalDate.of(2025, 1, 1));
                /* your Receptionist constructor */;

        Booking existingBooking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 15),
                new BigDecimal("10000.00")
        );

        when(guestRepository.findById(1))
                .thenReturn(Optional.of(guest));

        when(roomRepository.findByNumberForUpdate(101))
                .thenReturn(Optional.of(room));

        when(receptionistRepository.findById(1))
                .thenReturn(Optional.of(receptionist));

        when(bookingRepository.findByRoom(room))
                .thenReturn(List.of(existingBooking));

        // Act + Assert
        assertThrows(
                RoomNotAvailableException.class,
                () -> bookingService.createBooking(
                        1,
                        101,
                        1,
                        LocalDate.of(2026, 9, 12),
                        LocalDate.of(2026, 9, 17),
                        new BigDecimal("12000.00")
                )
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void shouldThrowWhenRoomDoesNotExist() {

        Guest guest = new Guest(1, "Roshan");

        when(guestRepository.findById(1))
                .thenReturn(Optional.of(guest));

        when(roomRepository.findByNumberForUpdate(999))
                .thenReturn(Optional.empty());

        assertThrows(
                RoomNotFoundException.class,
                () -> bookingService.createBooking(
                        1,
                        999,
                        1,
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12),
                        new BigDecimal("12000.00")
                )
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void shouldThrowWhenReceptionistDoesNotExist() {

        Guest guest = new Guest(1, "Roshan");
        Room room = new Room(101, RoomType.STANDARD) /* your constructor */;

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
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12),
                        new BigDecimal("12000.00")
                )
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void shouldThrowWhenGuestDoesNotExist() {

        // Arrange
        when(guestRepository.findById(999))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                GuestNotFoundException.class,
                () -> bookingService.createBooking(
                        999,
                        101,
                        1,
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 9, 12),
                        new BigDecimal("12000.00")
                )
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void shouldCancelExistingBooking() {

        Guest guest = new Guest(1, "Roshan");
        Room room = new Room(101, RoomType.STANDARD)/* your constructor */;
        Receptionist receptionist = new Receptionist(
                2,
                "Srag",
                LocalDate.of(2025, 1, 1));
        /* your Receptionist constructor */;
                /* your constructor */;

        Booking booking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        BookingResponse response =
                bookingService.cancelBooking(1L);

        assertEquals(
                BookingStatus.CANCELLED,
                response.status()
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void shouldThrowWhenCancellingMissingBooking() {

        when(bookingRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.cancelBooking(999L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void shouldRejectCancellationWhenBookingCannotBeCancelled() {

        Guest guest = new Guest(1, "Roshan");
        Room room = new Room(102, RoomType.STANDARD)/* your constructor */;
        Receptionist receptionist = new Receptionist(3, "Srag", LocalDate.of(2026, 05, 22));
                /* your constructor */;

        Booking booking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );

        // Move booking into a state where cancellation is illegal.
        // Adapt these calls to your actual domain methods.
        booking.markConfirmed();
        booking.markCheckedIn();

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                InvalidBookingStateException.class,
                () -> bookingService.cancelBooking(1L)
        );

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

}