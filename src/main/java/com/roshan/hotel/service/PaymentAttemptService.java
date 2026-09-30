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
import com.roshan.hotel.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class PaymentAttemptService {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;

    public PaymentAttemptService(
            BookingService bookingService,
            PaymentRepository paymentRepository,
            IdempotencyService idempotencyService) {

        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PaymentAttemptResult createFullPaymentAttempt(
            long bookingId,
            String idempotencyKey) {

        String requestHash = sha256(bookingId + ":FULL");

        boolean claimed = idempotencyService.claim(
                IdempotencyOperation.PAYMENT,
                idempotencyKey,
                requestHash
        );

        if (!claimed) {

            IdempotencyRecord existing =
                    idempotencyService.get(
                            IdempotencyOperation.PAYMENT,
                            idempotencyKey
                    );

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key was already used for a different request"
                );
            }

            if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new PaymentInProgressException(
                        "Payment request is already being processed"
                );
            }

            if (existing.getStatus() == IdempotencyStatus.COMPLETED) {

                Payment existingPayment =
                        paymentRepository.findById(existing.getResourceId())
                                .orElseThrow(() ->
                                        new PaymentNotFoundException(
                                                "Payment not found"
                                        ));

                return new PaymentAttemptResult(
                        false,
                        existingPayment
                );
            }
        }

        Booking booking = bookingService.getBooking(bookingId);

        BigDecimal totalAmount = booking.getTotalAmount();
        BigDecimal paidAmount = getPaidAmount(bookingId);

        BigDecimal amount;

        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {

            amount = totalAmount;

        } else if (paidAmount.compareTo(totalAmount) < 0) {

            amount = totalAmount.subtract(paidAmount);

        } else {

            throw new PaymentAmountException(
                    "Payment has already been completed"
            );
        }

        Payment payment = new Payment(
                booking,
                amount,
                PaymentType.FULL,
                LocalDateTime.now()
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        return new PaymentAttemptResult(
                true,
                savedPayment
        );
    }

    private BigDecimal getPaidAmount(long bookingId) {

        return paymentRepository
                .findByBooking_Id(bookingId)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
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
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            input.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : hash) {

                String hex =
                        Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }
}