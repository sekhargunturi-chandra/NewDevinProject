package com.capstone.fraudcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FraudCaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudCaseApplication.class, args);
    }
}
