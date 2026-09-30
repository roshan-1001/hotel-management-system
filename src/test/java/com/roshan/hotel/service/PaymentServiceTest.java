package com.roshan.hotel.service;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.Guest;
import com.roshan.hotel.domain.Payment;
import com.roshan.hotel.domain.Receptionist;
import com.roshan.hotel.domain.Room;
import com.roshan.hotel.dto.PaymentResponse;
import com.roshan.hotel.enums.PaymentStatus;
import com.roshan.hotel.enums.PaymentType;
import com.roshan.hotel.enums.RoomType;
import com.roshan.hotel.exception.PaymentAmountException;
import com.roshan.hotel.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldMakePartialPayment() {

        // Arrange
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

        Booking booking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );

        when(bookingService.getBooking(1L))
                .thenReturn(booking);

        when(paymentRepository.findByBooking_Id(1L))
                .thenReturn(List.of());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentResponse response =
                paymentService.makePartialPayment(1L);

        // Assert

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(paymentCaptor.capture());

        Payment savedPayment =
                paymentCaptor.getValue();

        assertEquals(
                new BigDecimal("6000.00"),
                savedPayment.getAmount()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                savedPayment.getStatus()
        );

        verify(bookingService).confirmBooking(1L);
    }

    @Test
    void shouldRejectPartialPaymentWhenPaymentAlreadyExists() {

        // Arrange
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

        Booking booking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );

        Payment existingPayment = new Payment(
                booking,
                new BigDecimal("6000.00"),
                PaymentType.PARTIAL,
                LocalDateTime.of(2026, 9, 10, 10, 0)
        );

        existingPayment.markCompleted();

        when(bookingService.getBooking(1L))
                .thenReturn(booking);

        when(paymentRepository.findByBooking_Id(1L))
                .thenReturn(List.of(existingPayment));

        // Act + Assert
        assertThrows(
                PaymentAmountException.class,
                () -> paymentService.makePartialPayment(1L)
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(bookingService, never())
                .confirmBooking(anyLong());
    }

    @Test
    void shouldMakeFullPaymentWhenNoPreviousPaymentExists() {

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

        Booking booking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );

        when(bookingService.getBooking(1L))
                .thenReturn(booking);

        when(paymentRepository.findByBooking_Id(1L))
                .thenReturn(List.of());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentResponse response =
                paymentService.makeFullPayment(1L);

        // Assert
        assertEquals(
                new BigDecimal("12000.00"),
                response.amount()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                response.status()
        );

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(
                new BigDecimal("12000.00"),
                savedPayment.getAmount()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                savedPayment.getStatus()
        );

        verify(bookingService)
                .confirmBooking(1L);
    }

    @Test
    void shouldPayRemainingAmountWhenPartialPaymentAlreadyExists() {

        // Arrange
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

        Booking booking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );

        Payment partialPayment = new Payment(
                booking,
                new BigDecimal("6000.00"),
                PaymentType.PARTIAL,
                LocalDateTime.of(2026, 9, 10, 10, 0)
        );

        partialPayment.markCompleted();

        when(bookingService.getBooking(1L))
                .thenReturn(booking);

        when(paymentRepository.findByBooking_Id(1L))
                .thenReturn(List.of(partialPayment));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentResponse response =
                paymentService.makeFullPayment(1L);

        // Assert
        assertEquals(
                new BigDecimal("6000.00"),
                response.amount()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                response.status()
        );

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(
                new BigDecimal("6000.00"),
                savedPayment.getAmount()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                savedPayment.getStatus()
        );

        verify(bookingService, never())
                .confirmBooking(anyLong());
    }

    @Test
    void shouldRejectFullPaymentWhenBookingIsAlreadyFullyPaid() {

        // Arrange
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

        Booking booking = new Booking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                new BigDecimal("12000.00")
        );

        Payment existingPayment = new Payment(
                booking,
                new BigDecimal("12000.00"),
                PaymentType.FULL,
                LocalDateTime.of(2026, 9, 10, 10, 0)
        );

        existingPayment.markCompleted();

        when(bookingService.getBooking(1L))
                .thenReturn(booking);

        when(paymentRepository.findByBooking_Id(1L))
                .thenReturn(List.of(existingPayment));

        // Act + Assert
        assertThrows(
                PaymentAmountException.class,
                () -> paymentService.makeFullPayment(1L)
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(bookingService, never())
                .confirmBooking(anyLong());
    }
}