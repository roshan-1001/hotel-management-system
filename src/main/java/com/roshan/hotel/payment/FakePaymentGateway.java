package com.roshan.hotel.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FakePaymentGateway implements PaymentGateway{

    @Override
    public PaymentGatewayResult charge(String idempotencyKey, BigDecimal amount){
        return PaymentGatewayResult.success("fake-payment-" + idempotencyKey);
    }
}
