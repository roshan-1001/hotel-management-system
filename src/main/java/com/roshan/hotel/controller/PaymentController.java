package com.roshan.hotel.controller;

import com.roshan.hotel.dto.PaymentResponse;
import com.roshan.hotel.service.BookingService;
import com.roshan.hotel.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings/{bookingId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @RequestMapping("/partial")
    public ResponseEntity<PaymentResponse> makePartialPayment(@PathVariable long bookingId){
        PaymentResponse paymentResponse = paymentService.makePartialPayment(bookingId);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse);
    }

    @RequestMapping("/full")
    public ResponseEntity<PaymentResponse> makeFullPayment(@PathVariable long bookingId){
        PaymentResponse paymentResponse = paymentService.makeFullPayment(bookingId);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponse);
    }

}
