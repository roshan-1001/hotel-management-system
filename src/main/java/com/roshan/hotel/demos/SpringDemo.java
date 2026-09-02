package com.roshan.hotel.demos;

import com.roshan.hotel.AppConfig;
import com.roshan.hotel.service.BookingService;
import com.roshan.hotel.service.HotelService;
import com.roshan.hotel.service.PaymentService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringDemo {

    public static void main(String[] args){

        try(AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)){

            BookingService bookingService = context.getBean(BookingService.class);
            PaymentService paymentService = context.getBean(PaymentService.class);


            System.out.println(bookingService);
            System.out.println(paymentService);
        }

    }

}
