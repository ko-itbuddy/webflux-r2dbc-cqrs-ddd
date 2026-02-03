package com.example.order.application.in.command;

import java.math.BigDecimal;

public record ApplyDiscountCommand(String orderId, BigDecimal discountPercentage) {
}
