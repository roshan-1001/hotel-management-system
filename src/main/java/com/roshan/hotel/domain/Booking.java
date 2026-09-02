package com.roshan.hotel.domain;

import com.roshan.hotel.enums.BookingStatus;
import com.roshan.hotel.exception.BookingNotFoundException;
import com.roshan.hotel.exception.InvalidBookingStateException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptionist_id", nullable = false)
    private Receptionist receptionist;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    protected Booking(){
    }

    public Booking( Guest guest, Room room, Receptionist receptionist, LocalDate startDate, LocalDate endDate, BigDecimal totalAmount) {
        this.guest = guest;
        this.room = room;
        this.receptionist = receptionist;

        if(totalAmount==null || totalAmount.signum() <= 0 ){
            throw new IllegalArgumentException("Booking amount must be greater than 0");
        }
        this.totalAmount = totalAmount;

        updateDates(startDate,endDate);

        this.status = BookingStatus.PENDING;
    }

    public long getId() {
        return id;
    }

    public Guest getGuest() {
        return guest;
    }

    public Room getRoom() {
        return room;
    }

    public Receptionist getReceptionist() {
        return receptionist;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void updateDates(LocalDate startDate, LocalDate endDate){
        if(startDate == null || endDate == null){
            throw new IllegalArgumentException("Booking dates cannot be null");
        }
        if (!endDate.isAfter(startDate)){
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void markConfirmed(){
        if(status != BookingStatus.PENDING){
            throw new InvalidBookingStateException("Only pending bookings can be confirmed");
        }
        this.status = BookingStatus.CONFIRMED;
    }
    public void markCheckedIn(){
        if(status != BookingStatus.CONFIRMED){
            throw new InvalidBookingStateException(("Only confirmed guests can check in"));
        }
        this.status = BookingStatus.CHECKED_IN;
    }
    public void markCheckedOut(){
        if(status != BookingStatus.CHECKED_IN){
            throw new InvalidBookingStateException("Only checked-in guests can check-out");
        }
        this.status = BookingStatus.CHECKED_OUT;
    }
    public void markCancelled(){
        if(status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED){
            throw new InvalidBookingStateException("Only pending or confirmed bookings can be cancelled");
        }
        this.status = BookingStatus.CANCELLED;
    }
}
