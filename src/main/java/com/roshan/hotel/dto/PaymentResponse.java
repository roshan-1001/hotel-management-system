package com.roshan.hotel.dto;

import com.roshan.hotel.enums.PaymentStatus;
import com.roshan.hotel.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long bookingId,
        BigDecimal amount,
        PaymentType type,
        PaymentStatus status,
        LocalDateTime paymentDate
) {
}
