package com.roshan.hotel.service;

import com.roshan.hotel.domain.Payment;
import com.roshan.hotel.dto.PaymentResponse;
import com.roshan.hotel.enums.IdempotencyOperation;
import com.roshan.hotel.exception.PaymentNotFoundException;
import com.roshan.hotel.payment.PaymentGatewayResult;
import com.roshan.hotel.payment.PaymentGatewayStatus;
import com.roshan.hotel.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentResultService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final IdempotencyService idempotencyService;

    public PaymentResultService(
            PaymentRepository paymentRepository,
            BookingService bookingService,
            IdempotencyService idempotencyService) {

        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PaymentResponse applyResult(
            long paymentId,
            String idempotencyKey,
            PaymentGatewayResult result) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found: " + paymentId
                        ));

        if (result.getStatus() == PaymentGatewayStatus.SUCCESS) {

            payment.markCompleted();

            bookingService.confirmBooking(
                    payment.getBooking().getId()
            );

            idempotencyService.complete(
                    IdempotencyOperation.PAYMENT,
                    idempotencyKey,
                    payment.getId()
            );

        } else if (result.getStatus() == PaymentGatewayStatus.FAILED) {

            payment.markFailed();

            idempotencyService.complete(
                    IdempotencyOperation.PAYMENT,
                    idempotencyKey,
                    payment.getId()
            );

        } else {
            // UNKNOWN
            //
            // Keep:
            // Payment = PENDING
            // Idempotency = IN_PROGRESS
            //
            // We need reconciliation/retry later.
        }

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
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