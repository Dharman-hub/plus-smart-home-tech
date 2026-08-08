package ru.yandex.practicum.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.aggregator.starter.AggregationStarter;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AggregatorApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(AggregatorApp.class, args);

        AggregationStarter aggregationStarter =
                context.getBean(AggregationStarter.class);

        aggregationStarter.start();
    }
}