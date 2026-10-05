package com.roshan.hotel.service;

import com.roshan.hotel.domain.Guest;
import com.roshan.hotel.domain.Receptionist;
import com.roshan.hotel.domain.Room;
import com.roshan.hotel.dto.BookingResponse;
import com.roshan.hotel.enums.RoomType;
import com.roshan.hotel.exception.RoomNotAvailableException;
import com.roshan.hotel.repository.BookingRepository;
import com.roshan.hotel.repository.GuestRepository;
import com.roshan.hotel.repository.ReceptionistRepository;
import com.roshan.hotel.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReceptionistRepository receptionistRepository;


    @Test
    void concurrentBookingsForSameRoom_onlyOneSucceeds() throws Exception {

        // Unique IDs so repeated test runs don't collide.
        int id = (int) (System.nanoTime() & 0x7fffffff);

        int roomNumber = id;
        int guestId = id;
        int receptionistId = id;

        Guest guest = new Guest(
                guestId,
                "Concurrency Test Guest"
        );

        Room room = new Room(
                roomNumber,
                RoomType.DELUXE
        );

        Receptionist receptionist = new Receptionist(
                receptionistId,
                "Concurrency Test Receptionist",
                LocalDate.of(2026, 1, 1)
        );

        // Seed data BEFORE starting concurrent requests.
        guestRepository.save(guest);
        roomRepository.save(room);
        receptionistRepository.save(receptionist);


        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        Callable<BookingResponse> requestA = () -> {

            start.await();

            return bookingService.createBooking(
                    guestId,
                    roomNumber,
                    receptionistId,
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 10, 15),
                    new BigDecimal("10000"),
                    "concurrency-test-A-" + id
            );
        };


        Callable<BookingResponse> requestB = () -> {

            start.await();

            return bookingService.createBooking(
                    guestId,
                    roomNumber,
                    receptionistId,
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 10, 15),
                    new BigDecimal("10000"),
                    "concurrency-test-B-" + id
            );
        };


        Future<BookingResponse> futureA = executor.submit(requestA);
        Future<BookingResponse> futureB = executor.submit(requestB);

        // Release both threads at approximately the same time.
        start.countDown();


        int successfulRequests = 0;
        int rejectedRequests = 0;


        for (Future<BookingResponse> future : new Future[]{
                futureA,
                futureB
        }) {

            try {

                future.get(10, TimeUnit.SECONDS);

                successfulRequests++;

            } catch (ExecutionException e) {

                Throwable cause = e.getCause();

                if (cause instanceof RoomNotAvailableException) {
                    rejectedRequests++;
                } else {
                    throw e;
                }
            }
        }


        executor.shutdown();


        // Exactly one booking should have succeeded.
        assertEquals(1, successfulRequests);

        // Exactly one should have been rejected.
        assertEquals(1, rejectedRequests);


        // Database should contain exactly one booking for this room.
        Room persistedRoom = roomRepository
                .findById(roomNumber)
                .orElseThrow();

        assertEquals(
                1,
                bookingRepository.findByRoom(persistedRoom).size()
        );
    }
}