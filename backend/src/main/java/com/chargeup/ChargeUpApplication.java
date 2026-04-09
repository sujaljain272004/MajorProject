package com.chargeup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChargeUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargeUpApplication.class, args);
    }
}
