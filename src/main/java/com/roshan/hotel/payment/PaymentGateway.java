package com.roshan.hotel.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;


public interface PaymentGateway {

    PaymentGatewayResult charge(
            String idempotencyKey,
            BigDecimal amount
    );
}
