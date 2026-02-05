package com.example.order.adapter.config;

import com.example.order.domain.service.DiscountCalculationService;
import com.example.order.domain.service.PriceValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public DiscountCalculationService discountCalculationService() {
        return new DiscountCalculationService();
    }

    @Bean
    public PriceValidationService priceValidationService() {
        return new PriceValidationService();
    }
}
