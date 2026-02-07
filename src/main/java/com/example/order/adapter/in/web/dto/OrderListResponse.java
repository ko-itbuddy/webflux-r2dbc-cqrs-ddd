package com.example.order.adapter.in.web.dto;

import com.example.order.application.dto.OrderListItemResult;
import java.util.List;

public record OrderListResponse(List<OrderListItemResult> orders, int count) {}
