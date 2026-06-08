package com.yyt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.yyt.mapper")
@EnableScheduling
public class BusinessHallApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusinessHallApplication.class, args);
    }
}