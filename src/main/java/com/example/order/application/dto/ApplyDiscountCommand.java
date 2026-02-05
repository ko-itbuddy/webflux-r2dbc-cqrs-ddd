package com.example.order.application.dto;

import java.math.BigDecimal;

public record ApplyDiscountCommand(String orderId, BigDecimal discountPercentage) {
}
