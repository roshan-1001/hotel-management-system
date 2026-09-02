package com.roshan.hotel.exception;

public class ReceptionistNotFoundException extends RuntimeException {
    public ReceptionistNotFoundException(String message) {
        super(message);
    }
}
