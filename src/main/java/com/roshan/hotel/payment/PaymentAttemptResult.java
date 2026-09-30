package com.roshan.hotel.payment;

import com.roshan.hotel.domain.Payment;

public record PaymentAttemptResult(
        boolean newAttempt,
        Payment payment
) {
}