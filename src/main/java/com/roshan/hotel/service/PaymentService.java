package com.roshan.hotel.service;

import com.roshan.hotel.domain.Booking;
import com.roshan.hotel.domain.IdempotencyRecord;
import com.roshan.hotel.domain.Payment;
import com.roshan.hotel.dto.PaymentResponse;
import com.roshan.hotel.enums.IdempotencyOperation;
import com.roshan.hotel.enums.IdempotencyStatus;
import com.roshan.hotel.enums.PaymentStatus;
import com.roshan.hotel.enums.PaymentType;
import com.roshan.hotel.exception.IdempotencyConflictException;
import com.roshan.hotel.exception.PaymentAmountException;
import com.roshan.hotel.exception.PaymentInProgressException;
import com.roshan.hotel.exception.PaymentNotFoundException;
import com.roshan.hotel.payment.PaymentAttemptResult;
import com.roshan.hotel.payment.PaymentGateway;
import com.roshan.hotel.payment.PaymentGatewayResult;
import com.roshan.hotel.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final IdempotencyService idempotencyService;
    private final PaymentAttemptService paymentAttemptService;
    private final PaymentResultService paymentResultService;

    public PaymentService(BookingService bookingService, PaymentRepository paymentRepository, PaymentGateway paymentGateway, IdempotencyService idempotencyService, PaymentAttemptService paymentAttemptService, PaymentResultService paymentResultService){
        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.paymentResultService = paymentResultService;
        this.idempotencyService = idempotencyService;
        this.paymentAttemptService = paymentAttemptService;
    }

    @Transactional
    public PaymentResponse makePartialPayment(long bookingId, String idempotencyKey){

        String requestHash = sha256(bookingId + ":PARTIAL");

        boolean claimed = idempotencyService.claim(IdempotencyOperation.PAYMENT, idempotencyKey, requestHash);

        if(!claimed){

            IdempotencyRecord existing = idempotencyService.get(IdempotencyOperation.PAYMENT, idempotencyKey);

            if(!existing.getRequestHash().equals(requestHash)){
                throw new IdempotencyConflictException("Idempotency key was already used for a different request");
            }
            if(existing.getStatus()== IdempotencyStatus.IN_PROGRESS){
                throw new PaymentInProgressException("Payment request is already being processed");
            }
            if(existing.getStatus() == IdempotencyStatus.COMPLETED){
                Payment existingPayment = paymentRepository.findById(existing.getResourceId()).orElseThrow(()-> new PaymentNotFoundException("Payment not found"));
                return toResponse((existingPayment));
            }
        }


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
        //payment.markCompleted();
        Payment savedPayment = paymentRepository.save(payment);
        //bookingService.confirmBooking(bookingId);
        return toResponse(savedPayment);
    }



    public PaymentResponse makeFullPayment(
            long bookingId,
            String idempotencyKey) {

        PaymentAttemptResult attempt =
                paymentAttemptService.createFullPaymentAttempt(
                        bookingId,
                        idempotencyKey
                );

        // Idempotent retry: payment already exists.
        if (!attempt.newAttempt()) {
            return toResponse(attempt.payment());
        }

        Payment payment = attempt.payment();

        PaymentGatewayResult result =
                paymentGateway.charge(
                        idempotencyKey,
                        payment.getAmount()
                );

        return paymentResultService.applyResult(
                payment.getId(),
                idempotencyKey,
                result
        );
    }

    private BigDecimal getPaidAmount(long bookingId){
        return paymentRepository
                .findByBooking_Id(bookingId)
                .stream()
                .filter(payment -> payment.getStatus()==PaymentStatus.SUCCESS)
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

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

}
