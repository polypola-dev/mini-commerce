package com.minicommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulith
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class MiniCommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniCommerceApplication.class, args);
    }
}
