package com.roshan.hotel;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
@ComponentScan("com.roshan.hotel")
public class AppConfig {

    @Bean
    public Clock clock(){
        return Clock.systemUTC();
    }
}
