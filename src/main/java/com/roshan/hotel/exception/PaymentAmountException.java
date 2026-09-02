package com.roshan.hotel.exception;

public class PaymentAmountException extends RuntimeException {
    public PaymentAmountException(String message) {
        super(message);
    }
}
