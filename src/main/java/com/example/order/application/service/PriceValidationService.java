package com.example.order.application.service;

import com.example.order.domain.order.valueobject.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.function.Predicate;

@Service
public class PriceValidationService {
    
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000.00");
    
    public boolean validatePrice(Money price) {
        if (price == null) {
            return false;
        }
        
        BigDecimal amount = price.getAmount();
        
        return amount.compareTo(MIN_PRICE) >= 0 && 
               amount.compareTo(MAX_PRICE) <= 0;
    }
    
    public Predicate<Money> getPriceValidator() {
        return this::validatePrice;
    }
}
