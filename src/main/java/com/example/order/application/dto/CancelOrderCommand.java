package com.example.order.application.dto;

public record CancelOrderCommand(String orderId, String reason) {
}
