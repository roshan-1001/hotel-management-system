package com.roshan.hotel.service;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.Payment;
import com.roshan.hotel.dto.PaymentResponse;
import com.roshan.hotel.enums.PaymentStatus;
import com.roshan.hotel.enums.PaymentType;
import com.roshan.hotel.exception.PaymentAmountException;
import com.roshan.hotel.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;

    public PaymentService(BookingService bookingService, PaymentRepository paymentRepository){
        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse makePartialPayment(long bookingId){

        Booking booking = bookingService.getBooking(bookingId);
        BigDecimal totalAmount = booking.getTotalAmount();
        BigDecimal paidAmount = getPaidAmount(bookingId);

        if(paidAmount.compareTo(BigDecimal.ZERO) != 0 ){
            throw new PaymentAmountException("Partial payment can only be made before any payment");
        }

        BigDecimal amount = totalAmount.divide(BigDecimal.valueOf(2));

        Payment payment = new Payment(
                booking,
                amount,
                PaymentType.PARTIAL,
                LocalDateTime.now()
                );
        payment.markCompleted();
        Payment savedPayment = paymentRepository.save(payment);
        bookingService.confirmBooking(bookingId);
        return toResponse(savedPayment);
    }


    @Transactional
    public PaymentResponse makeFullPayment(long bookingId){

        Booking booking = bookingService.getBooking(bookingId);

        BigDecimal totalAmount = booking.getTotalAmount();
        BigDecimal paidAmount = getPaidAmount(bookingId);

        BigDecimal amount = null;
        boolean shouldConfirmBooking = false;

        if(paidAmount.compareTo(BigDecimal.ZERO) == 0){
            amount = totalAmount;
            shouldConfirmBooking = true;
        }
        else if(paidAmount.compareTo(totalAmount)<0){
            amount = totalAmount.subtract(paidAmount);
        }
        else{
            throw new PaymentAmountException("Payment has already been completed");
        }

        Payment payment = new Payment(
                booking,
                amount,
                PaymentType.FULL,
                LocalDateTime.now()
        );

        payment.markCompleted();
        Payment savedPayment = paymentRepository.save(payment);
        if(shouldConfirmBooking){bookingService.confirmBooking(bookingId);}

        return toResponse(savedPayment);
    }

    private BigDecimal getPaidAmount(long bookingId){
        return paymentRepository
                .findByBooking_Id(bookingId)
                .stream()
                .filter(payment -> payment.getStatus()==PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PaymentResponse toResponse(Payment payment){
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getAmount(),
                payment.getType(),
                payment.getStatus(),
                payment.getPaymentDate()
        );
    }

}
