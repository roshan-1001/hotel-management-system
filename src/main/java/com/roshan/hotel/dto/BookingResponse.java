package com.roshan.hotel.dto;

import com.roshan.hotel.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        int guestId,
        int roomNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalAmount,
        BookingStatus status
) {

}
