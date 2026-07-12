package com.print3dmanager.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Print3dManagerErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(Print3dManagerErpApplication.class, args);
    }
}
