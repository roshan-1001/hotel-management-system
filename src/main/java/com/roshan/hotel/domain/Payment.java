package com.roshan.hotel.domain;

import com.roshan.hotel.enums.PaymentStatus;
import com.roshan.hotel.enums.PaymentType;
import com.roshan.hotel.exception.PaymentStatusException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    private  PaymentStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;


    protected Payment(){

    }

    public Payment( Booking booking, BigDecimal amount, PaymentType type, LocalDateTime paymentDate) {
        this.booking = booking;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.type = type;
        this.status = PaymentStatus.PENDING;
    }

    public long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public PaymentType getType() {
        return type;
    }

    public void markCompleted() {
        if (status != PaymentStatus.PENDING) {
            throw new PaymentStatusException(
                    "Only pending payments can be completed"
            );
        }

        status = PaymentStatus.COMPLETED;
    }

    public void markFailed() {
        if (status != PaymentStatus.PENDING) {
            throw new PaymentStatusException(
                    "Only pending payments can fail"
            );
        }

        status = PaymentStatus.FAILED;
    }
}
