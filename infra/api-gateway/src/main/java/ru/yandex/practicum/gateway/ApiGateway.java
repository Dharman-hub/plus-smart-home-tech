package ru.yandex.practicum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ApiGateway {

    public static void main(String[] args) {
        SpringApplication.run(ApiGateway.class, args);
    }
}