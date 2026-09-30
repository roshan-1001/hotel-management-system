package com.roshan.hotel.exception;

public class PaymentInProgressException extends RuntimeException {
    public PaymentInProgressException(String message) {
        super(message);
    }
}
