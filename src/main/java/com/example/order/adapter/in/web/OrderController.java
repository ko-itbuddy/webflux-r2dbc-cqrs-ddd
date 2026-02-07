package com.example.order.adapter.in.web;

import com.example.common.adapter.in.web.dto.ApiResponse;
import com.example.order.adapter.in.web.dto.OrderListResponse;
import com.example.order.application.dto.*;
import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.application.port.out.OrderQueryPort;
import com.example.order.application.service.*;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ConfirmOrderHandler confirmOrderHandler;
    private final PayOrderHandler payOrderHandler;
    private final CancelOrderHandler cancelOrderHandler;
    private final ApplyDiscountHandler applyDiscountHandler;
    private final OrderQueryPort orderQueryPort;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            ConfirmOrderHandler confirmOrderHandler,
            PayOrderHandler payOrderHandler,
            CancelOrderHandler cancelOrderHandler,
            ApplyDiscountHandler applyDiscountHandler,
            OrderQueryPort orderQueryPort) {
        this.createOrderUseCase = createOrderUseCase;
        this.confirmOrderHandler = confirmOrderHandler;
        this.payOrderHandler = payOrderHandler;
        this.cancelOrderHandler = cancelOrderHandler;
        this.applyDiscountHandler = applyDiscountHandler;
        this.orderQueryPort = orderQueryPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<Order>> createOrder(@Valid @RequestBody CreateOrderCommand command) {
        return createOrderUseCase.handle(command)
                .map(res -> ApiResponse.success(HttpStatus.CREATED.value(), res));
    }

    @PostMapping("/{orderId}/confirm")
    public Mono<ApiResponse<Order>> confirmOrder(@PathVariable String orderId) {
        return confirmOrderHandler.handle(new ConfirmOrderCommand(orderId))
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @PostMapping("/{orderId}/pay")
    public Mono<ApiResponse<Order>> payOrder(@PathVariable String orderId) {
        return payOrderHandler.handle(new PayOrderCommand(orderId))
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @PostMapping("/{orderId}/cancel")
    public Mono<ApiResponse<Order>> cancelOrder(@PathVariable String orderId, @RequestParam String reason) {
        return cancelOrderHandler.handle(new CancelOrderCommand(orderId, reason))
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @PostMapping("/{orderId}/discount")
    public Mono<ApiResponse<Order>> applyDiscount(@PathVariable String orderId, @Valid @RequestBody ApplyDiscountCommand command) {
        return applyDiscountHandler.handle(command)
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @GetMapping("/{orderId}")
    public Mono<ApiResponse<Order>> getOrder(@PathVariable String orderId) {
        return orderQueryPort.findById(orderId)
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @GetMapping("/{orderId}/summary")
    public Mono<ApiResponse<OrderSummaryResult>> getOrderSummary(@PathVariable String orderId) {
        return orderQueryPort.findOrderSummary(orderId)
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @GetMapping
    public Mono<ApiResponse<OrderListResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderQueryPort.findOrderList(page, size)
                .collectList()
                .map(orders -> new OrderListResponse(orders, orders.size()))
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @GetMapping("/customer/{customerId}")
    public Mono<ApiResponse<java.util.List<Order>>> getCustomerOrders(@PathVariable String customerId) {
        return orderQueryPort.findByCustomerId(customerId)
                .collectList()
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @GetMapping("/status/{status}")
    public Mono<ApiResponse<java.util.List<Order>>> getOrdersByStatus(@PathVariable String status) {
        return orderQueryPort.findByStatus(OrderStatus.valueOf(status.toUpperCase()))
                .collectList()
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }

    @GetMapping("/customer/{customerId}/stats")
    public Mono<ApiResponse<CustomerOrderStatsResult>> getCustomerStats(@PathVariable String customerId) {
        return orderQueryPort.findCustomerStats(customerId)
                .map(res -> ApiResponse.success(HttpStatus.OK.value(), res));
    }
}