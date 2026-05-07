package com.taxi.analytics;

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class
})
@EnableAsync
@Import(DynamicDataSourceAutoConfiguration.class)
public class TaxiAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaxiAnalyticsApplication.class, args);
    }
}