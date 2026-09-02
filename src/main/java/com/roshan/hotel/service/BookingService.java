package com.roshan.hotel.service;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.Guest;
import com.roshan.hotel.domain.Receptionist;
import com.roshan.hotel.domain.Room;
import com.roshan.hotel.dto.BookingResponse;
import com.roshan.hotel.enums.BookingStatus;
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
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final ReceptionistRepository receptionistRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, GuestRepository guestRepository, RoomRepository roomRepository, ReceptionistRepository receptionistRepository){
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.receptionistRepository = receptionistRepository;
        this.guestRepository = guestRepository;
    }

    public Booking getBooking(long id){
        return bookingRepository.findById(id).orElseThrow(() ->
                new BookingNotFoundException(
                        "Booking not found: " + id
                )
        );
    }

    public void confirmBooking (long bookingId){

        Booking booking = getBooking(bookingId);
        booking.markConfirmed();
        bookingRepository.save(booking);

    }
    public void checkIn (long bookingId){

        Booking booking = getBooking(bookingId);
        booking.markCheckedIn();
        bookingRepository.save(booking);

    }
    public void checkOut (long bookingId){

        Booking booking = getBooking(bookingId);
        booking.markCheckedOut();
        bookingRepository.save(booking);

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
    public BookingResponse createBooking(int guestId, int roomNumber, int receptionistId, LocalDate startDate, LocalDate endDate, BigDecimal totalAmount){
        Guest guest = guestRepository.findById(guestId).orElseThrow(()-> new GuestNotFoundException("Guest not available for id: " + guestId));
        Room room = roomRepository.findByNumberForUpdate(roomNumber)
                .orElseThrow(
                        () -> new RoomNotFoundException("Room not found with number " + roomNumber)
                );        Receptionist receptionist = receptionistRepository.findById(receptionistId).orElseThrow(()->new ReceptionistNotFoundException("Rceptionist does not exist with id: " + receptionistId));

        if(!isRoomAvailable(room,startDate,endDate)){
            throw new RoomNotAvailableException("Room" + roomNumber + " is not available from " + startDate + " to " + endDate);
        }

        Booking booking = new Booking(guest,room,receptionist,startDate,endDate,totalAmount);
        Booking savedBooking = bookingRepository.save(booking);

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

        for (Booking booking : bookings){
            if (startDate.isBefore(booking.getEndDate()) && endDate.isAfter(booking.getStartDate()) ){
                return false;
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
}
