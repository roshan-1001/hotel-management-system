package com.roshan.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class HotelManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelManagementApplication.class, args);
    }
}
