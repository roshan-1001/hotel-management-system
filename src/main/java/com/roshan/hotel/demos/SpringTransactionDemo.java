package com.roshan.hotel.demos;

import com.roshan.hotel.HotelManagementApplication;
import com.roshan.hotel.service.BookingService;
import com.roshan.hotel.service.PaymentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


public class SpringTransactionDemo {

    public static void main(String[] args){

        try(ConfigurableApplicationContext context = SpringApplication.run(HotelManagementApplication.class,args)){
            PaymentService paymentService= context.getBean(PaymentService.class);
            try{
                paymentService.makePartialPayment(1001);
            }catch (Exception e){
                System.out.println("Payment failed " + e.getMessage());
            }
        }



    }
}
