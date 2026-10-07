package com.roshan.hotel.repository;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.Guest;
import com.roshan.hotel.domain.Receptionist;
import com.roshan.hotel.domain.Room;
import com.roshan.hotel.enums.BookingStatus;
import com.roshan.hotel.enums.RoomType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReceptionistRepository receptionistRepository;


    // -------------------------------------------------------------------------
    // findByRoom
    // -------------------------------------------------------------------------

    @Test
    void shouldFindBookingsForRoom() {

        Room room = createRoom(99900);
        Guest guest = createGuest(19999);
        Receptionist receptionist = createReceptionist(109987);

        Booking booking = createBooking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        bookingRepository.saveAndFlush(booking);

        List<Booking> bookings =
                bookingRepository.findByRoom(room);

        assertEquals(1, bookings.size());
        assertEquals(booking.getId(), bookings.get(0).getId());
    }


    // -------------------------------------------------------------------------
    // findByIdForUpdate
    // -------------------------------------------------------------------------

    @Test
    void shouldFindBookingUsingPessimisticLockQuery() {

        Room room = createRoom(102);
        Guest guest = createGuest(2);
        Receptionist receptionist = createReceptionist(20);

        Booking booking = createBooking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        bookingRepository.saveAndFlush(booking);

        Optional<Booking> result =
                bookingRepository.findByIdForUpdate(booking.getId());

        assertTrue(result.isPresent());
        assertEquals(
                booking.getId(),
                result.get().getId()
        );
    }


    // -------------------------------------------------------------------------
    // findExpiredPendingBookings
    // -------------------------------------------------------------------------

    @Test
    void shouldFindExpiredPendingBookings() {

        Room room = createRoom(103);
        Guest guest = createGuest(3);
        Receptionist receptionist = createReceptionist(30);

        Instant expiredAt =
                Instant.parse("2026-10-07T09:00:00Z");

        Instant now =
                Instant.parse("2026-10-07T10:00:00Z");

        Booking expiredBooking = createBooking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                expiredAt
        );

        bookingRepository.saveAndFlush(expiredBooking);

        List<Booking> result =
                bookingRepository.findExpiredPendingBookings(
                        BookingStatus.PENDING_PAYMENT,
                        now
                );

        assertEquals(1, result.size());

        assertEquals(
                expiredBooking.getId(),
                result.get(0).getId()
        );
    }


    // -------------------------------------------------------------------------
    // findByStatus
    // -------------------------------------------------------------------------

    @Test
    void shouldFindBookingsByStatusWithPagination() {

        Room room = createRoom(104);
        Guest guest = createGuest(4);
        Receptionist receptionist = createReceptionist(40);

        Booking booking1 = createBooking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 3),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        Booking booking2 = createBooking(
                guest,
                room,
                receptionist,
                LocalDate.of(2026, 12, 5),
                LocalDate.of(2026, 12, 7),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        bookingRepository.save(booking1);
        bookingRepository.save(booking2);

        bookingRepository.flush();

        Page<Booking> result =
                bookingRepository.findByStatus(
                        BookingStatus.PENDING_PAYMENT,
                        PageRequest.of(0, 1)
                );

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertTrue(result.hasNext());
    }


    // -------------------------------------------------------------------------
    // EntityGraph - findAll(Pageable)
    // -------------------------------------------------------------------------

    @Test
    void shouldLoadGuestAndRoomWithEntityGraph() {

        Room room = createRoom(105);
        Guest guest = createGuest(5);
        Receptionist receptionist = createReceptionist(50);

        Booking booking = createBooking(
                guest,
                room,
                receptionist,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 1, 3),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        bookingRepository.saveAndFlush(booking);

        Page<Booking> result =
                bookingRepository.findAll(
                        PageRequest.of(
                                0,
                                10,
                                Sort.by("id").ascending()
                        )
                );

        assertFalse(result.getContent().isEmpty());

        Booking found =
                result.getContent()
                        .stream()
                        .filter(b -> b.getId() == booking.getId())
                        .findFirst()
                        .orElseThrow();

        /*
         * These associations are LAZY on Booking.
         *
         * The @EntityGraph on findAll(Pageable) tells Hibernate
         * to fetch guest and room as part of the query.
         *
         * Accessing them here should therefore work while proving
         * that the associations are available from the repository result.
         */
        assertNotNull(found.getGuest());
        assertNotNull(found.getRoom());

        assertEquals(
                guest.getId(),
                found.getGuest().getId()
        );

        assertEquals(
                room.getNumber(),
                found.getRoom().getNumber()
        );
    }


    // -------------------------------------------------------------------------
    // EntityGraph + Specification
    // -------------------------------------------------------------------------

    @Test
    void shouldFindBookingsUsingSpecificationAndEntityGraph() {

        Room room = createRoom(106);
        Guest guest = createGuest(6);
        Receptionist receptionist = createReceptionist(60);

        Booking booking = createBooking(
                guest,
                room,
                receptionist,
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 2, 3),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        bookingRepository.saveAndFlush(booking);

        Specification<Booking> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(
                                root.get("status"),
                                BookingStatus.PENDING_PAYMENT
                        );

        Page<Booking> result =
                bookingRepository.findAll(
                        specification,
                        PageRequest.of(0, 10)
                );

        assertFalse(result.getContent().isEmpty());

        Booking found =
                result.getContent()
                        .stream()
                        .filter(b -> b.getId() == booking.getId())
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                BookingStatus.PENDING_PAYMENT,
                found.getStatus()
        );

        assertNotNull(found.getGuest());
        assertNotNull(found.getRoom());

        assertEquals(
                guest.getId(),
                found.getGuest().getId()
        );

        assertEquals(
                room.getNumber(),
                found.getRoom().getNumber()
        );
    }


    // =========================================================================
    // Test data helpers
    // =========================================================================

    private Guest createGuest(int id) {

        Guest guest =
                new Guest(id, "Guest " + id);

        return guestRepository.saveAndFlush(guest);
    }


    private Room createRoom(int number) {

        Room room =
                new Room(number, RoomType.STANDARD);

        return roomRepository.saveAndFlush(room);
    }


    private Receptionist createReceptionist(int id) {

        Receptionist receptionist =
                new Receptionist(
                        id,
                        "Receptionist " + id,
                        LocalDate.of(2025, 1, 1)
                );

        return receptionistRepository.saveAndFlush(
                receptionist
        );
    }


    private Booking createBooking(
            Guest guest,
            Room room,
            Receptionist receptionist,
            LocalDate startDate,
            LocalDate endDate,
            Instant expiresAt
    ) {

        return new Booking(
                guest,
                room,
                receptionist,
                startDate,
                endDate,
                new BigDecimal("12000"),
                expiresAt
        );
    }
}