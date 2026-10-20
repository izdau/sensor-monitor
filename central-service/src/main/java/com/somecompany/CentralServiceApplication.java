package com.somecompany;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CentralServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CentralServiceApplication.class, args);
    }

}
