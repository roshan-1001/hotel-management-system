package com.roshan.hotel.controller;

import com.roshan.hotel.dto.BookingResponse;
import com.roshan.hotel.dto.CreateBookingRequest;
import com.roshan.hotel.dto.PageResponse;
import com.roshan.hotel.enums.BookingStatus;
import com.roshan.hotel.enums.RoomStatus;
import com.roshan.hotel.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService){
        this.bookingService = bookingService;
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable long id){
        return bookingService.getBookingResponse(id);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request
            ){
        BookingResponse response = bookingService.createBooking(
                request.guestId(),
                request.roomNumber(),
                request.receptionistId(),
                request.startDate(),
                request.endDate(),
                request.totalAmount(),
                idempotencyKey
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancelBooking(@PathVariable long id){
        return bookingService.cancelBooking(id);

    }

    @GetMapping
    public PageResponse<BookingResponse> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Integer roomNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").descending());
        Page<BookingResponse> bookings = bookingService.getBookings(status, roomNumber, pageable);
        return PageResponse.from(bookings);
    }

}
