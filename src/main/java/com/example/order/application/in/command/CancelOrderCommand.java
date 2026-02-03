package com.example.order.application.in.command;

public record CancelOrderCommand(String orderId, String reason) {
}
