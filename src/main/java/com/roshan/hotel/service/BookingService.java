package com.roshan.hotel.service;

import com.roshan.hotel.domain.*;
import com.roshan.hotel.dto.BookingResponse;
import com.roshan.hotel.enums.BookingStatus;
import com.roshan.hotel.enums.IdempotencyStatus;
import com.roshan.hotel.exception.*;
import com.roshan.hotel.repository.BookingRepository;
import com.roshan.hotel.repository.GuestRepository;
import com.roshan.hotel.repository.ReceptionistRepository;
import com.roshan.hotel.repository.RoomRepository;
import com.roshan.hotel.specification.BookingSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final ReceptionistRepository receptionistRepository;
    private final RoomRepository roomRepository;
    private final Clock clock;
    private final IdempotencyService idempotencyService;

    public BookingService(BookingRepository bookingRepository, GuestRepository guestRepository, RoomRepository roomRepository, ReceptionistRepository receptionistRepository, Clock clock, IdempotencyService idempotencyService){
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.receptionistRepository = receptionistRepository;
        this.guestRepository = guestRepository;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
    }

    public Booking getBooking(long id){
        return bookingRepository.findById(id).orElseThrow(() ->
                new BookingNotFoundException(
                        "Booking not found: " + id
                )
        );
    }

    @Transactional
    public void confirmBooking (long bookingId){

        Instant confirmedAt = clock.instant();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking no found: " + bookingId));
        booking.confirmPayment(confirmedAt);

    }

    @Transactional
    public void expirePendingBookings() {

        Instant now = clock.instant();

        List<Booking> expiredBookings =
                bookingRepository.findExpiredPendingBookings(
                        BookingStatus.PENDING_PAYMENT,
                        now
                );

        for (Booking booking : expiredBookings) {
            booking.expire(now);
        }
    }

    @Transactional
    public BookingResponse checkIn (long bookingId){

        Booking booking = getBooking(bookingId);
        booking.markCheckedIn();
        return toResponse(booking);


    }

    @Transactional
    public BookingResponse checkOut (long bookingId){

        Booking booking = getBooking(bookingId);
        booking.markCheckedOut();
        return toResponse(booking);

    }
    @Transactional
    public BookingResponse cancelBooking (long bookingId){

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(()-> new BookingNotFoundException("Booking not found with id: " + bookingId));
        booking.markCancelled();
        return toResponse(booking);

    }

    public BookingResponse getBookingResponse(long id){

        Booking booking = bookingRepository.findById(id).orElseThrow(()-> new BookingNotFoundException("Booking not found: " + id));

        return  toResponse(booking);
    }

    @Transactional
    public BookingResponse createBooking(int guestId, int roomNumber, int receptionistId, LocalDate startDate, LocalDate endDate, BigDecimal totalAmount, String idempotencyKey){

        String requestHash = generateRequestHash(
                guestId,
                roomNumber,
                receptionistId,
                startDate,
                endDate,
                totalAmount
        );

        boolean claimed = idempotencyService.claim(
                idempotencyKey,
                requestHash
        );

        if (!claimed) {
            return handleExistingIdempotencyRequest(
                    idempotencyKey,
                    requestHash
            );
        }
        Guest guest = guestRepository.findById(guestId).orElseThrow(()-> new GuestNotFoundException("Guest not available for id: " + guestId));
        Room room = roomRepository.findByNumberForUpdate(roomNumber).orElseThrow(() -> new RoomNotFoundException("Room not found with number " + roomNumber));
        Receptionist receptionist = receptionistRepository.findById(receptionistId).orElseThrow(()->new ReceptionistNotFoundException("Receptionist does not exist with id: " + receptionistId));

        if(!isRoomAvailable(room,startDate,endDate)){
            throw new RoomNotAvailableException("Room" + roomNumber + " is not available from " + startDate + " to " + endDate);
        }

        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(5));

        Booking booking = new Booking(guest,room,receptionist,startDate,endDate,totalAmount, expiresAt);
        Booking savedBooking = bookingRepository.save(booking);

        idempotencyService.complete(
                idempotencyKey,
                savedBooking.getId()
        );

        return toResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookings(BookingStatus status,Integer roomNumber, Pageable pageable){

        Specification<Booking> spec = Specification.unrestricted();

        if (status == null && roomNumber == null) {
            return bookingRepository
                    .findAll(pageable)
                    .map(this::toResponse);
        }

        if(status != null){
            spec = spec.and(BookingSpecification.hasStatus(status));
        }
        if(roomNumber != null){
            spec = spec.and(BookingSpecification.hasRoomNumber(roomNumber));
        }

        return bookingRepository
                .findAll(spec,pageable)
                .map(this::toResponse);
    }

    private boolean isRoomAvailable(Room room, LocalDate startDate, LocalDate endDate ){

        List<Booking> bookings = this.bookingRepository.findByRoom(room);
        Instant now = clock.instant();
        for (Booking booking : bookings){
            if (startDate.isBefore(booking.getEndDate()) && endDate.isAfter(booking.getStartDate()) ){
                if (booking.blocksInventory(now)){
                    return false;
                }
            }
        }
        return true;
    }

    private BookingResponse toResponse(Booking booking){
        return new BookingResponse(
                booking.getId(),
                booking.getGuest().getId(),
                booking.getRoom().getNumber(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getTotalAmount(),
                booking.getStatus()
        );
    }

    private String generateRequestHash(
            long guestId,
            int roomNumber,
            long receptionistId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalAmount) {

        String canonicalRequest =
                guestId + "|" +
                        roomNumber + "|" +
                        receptionistId + "|" +
                        startDate + "|" +
                        endDate + "|" +
                        totalAmount.stripTrailingZeros().toPlainString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }


    }

    private BookingResponse handleExistingIdempotencyRequest(
            String idempotencyKey,
            String requestHash) {

        IdempotencyRecord record =
                idempotencyService.get(idempotencyKey);

        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency key has already been used with a different request"
            );
        }

        if (record.getStatus() != IdempotencyStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Request is still being processed"
            );
        }

        Booking booking = bookingRepository
                .findById(record.getResourceId())
                .orElseThrow(() ->
                        new BookingNotFoundException("Booking not found for id:  " +
                                record.getResourceId()
                        )
                );

        return toResponse(booking);
    }
}
