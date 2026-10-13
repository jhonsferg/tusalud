package com.codesoftlabs.tusalud.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TusaludApplication {

    public static void main(String[] args) {
        SpringApplication.run(TusaludApplication.class, args);
    }
}
