package com.riogandarilla.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GandarillaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GandarillaApiApplication.class, args);
    }
}
