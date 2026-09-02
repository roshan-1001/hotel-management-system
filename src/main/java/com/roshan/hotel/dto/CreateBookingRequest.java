package com.roshan.hotel.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBookingRequest(

        @Positive(message = "guestId must be positive")
        int guestId,

        @Positive(message = "roomNumber must be positive")
        int roomNumber,

        @Positive(message = "receptionistId must be positive")
        int receptionistId,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotNull(message = "totalAmount is required")
        @Positive(message = "totalAmount should be greater than 0")
        BigDecimal totalAmount
        ) {
}
