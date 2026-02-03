package com.example.order.infrastructure.web.handler;

import com.example.order.application.in.command.ApplyDiscountCommand;
import com.example.order.application.in.command.CancelOrderCommand;
import com.example.order.application.in.command.ConfirmOrderCommand;
import com.example.order.application.in.command.CreateOrderCommand;
import com.example.order.application.in.command.PayOrderCommand;
import com.example.order.application.command.handler.ApplyDiscountHandler;
import com.example.order.application.command.handler.CancelOrderHandler;
import com.example.order.application.command.handler.ConfirmOrderHandler;
import com.example.order.application.command.handler.CreateOrderHandler;
import com.example.order.application.command.handler.PayOrderHandler;
import com.example.order.domain.order.entity.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Web handler for order command operations.
 * Handles POST/PUT requests for creating and modifying orders.
 */
@Component
public class OrderCommandWebHandler {
    
    private final CreateOrderHandler createOrderHandler;
    private final ConfirmOrderHandler confirmOrderHandler;
    private final PayOrderHandler payOrderHandler;
    private final CancelOrderHandler cancelOrderHandler;
    private final ApplyDiscountHandler applyDiscountHandler;
    
    public OrderCommandWebHandler(
            CreateOrderHandler createOrderHandler,
            ConfirmOrderHandler confirmOrderHandler,
            PayOrderHandler payOrderHandler,
            CancelOrderHandler cancelOrderHandler,
            ApplyDiscountHandler applyDiscountHandler) {
        this.createOrderHandler = createOrderHandler;
        this.confirmOrderHandler = confirmOrderHandler;
        this.payOrderHandler = payOrderHandler;
        this.cancelOrderHandler = cancelOrderHandler;
        this.applyDiscountHandler = applyDiscountHandler;
    }
    
    /**
     * POST /api/orders - Create a new order.
     */
    public Mono<ServerResponse> createOrder(ServerRequest request) {
        return request.bodyToMono(CreateOrderRequest.class)
            .flatMap(req -> {
                List<CreateOrderCommand.OrderItemCommand> items = req.items().stream()
                    .map(item -> new CreateOrderCommand.OrderItemCommand(
                        item.productId(), item.productName(), item.quantity(), 
                        item.unitPrice(), item.currency()
                    ))
                    .toList();
                
                CreateOrderCommand command = new CreateOrderCommand(
                    req.customerId(), req.customerEmail(), items
                );
                
                return createOrderHandler.handle(command);
            })
            .flatMap(order -> ServerResponse.status(201).bodyValue(mapToOrderResponse(order)))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(new ErrorResponse(e.getMessage())));
    }
    
    /**
     * POST /api/orders/{orderId}/confirm - Confirm an order.
     */
    public Mono<ServerResponse> confirmOrder(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        ConfirmOrderCommand command = new ConfirmOrderCommand(orderId);
        
        return confirmOrderHandler.handle(command)
            .flatMap(order -> ServerResponse.ok().bodyValue(mapToOrderResponse(order)))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(new ErrorResponse(e.getMessage())));
    }
    
    /**
     * POST /api/orders/{orderId}/pay - Pay for an order.
     */
    public Mono<ServerResponse> payOrder(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        PayOrderCommand command = new PayOrderCommand(orderId);
        
        return payOrderHandler.handle(command)
            .flatMap(order -> ServerResponse.ok().bodyValue(mapToOrderResponse(order)))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(new ErrorResponse(e.getMessage())));
    }
    
    /**
     * POST /api/orders/{orderId}/cancel - Cancel an order.
     */
    public Mono<ServerResponse> cancelOrder(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        String reason = request.queryParam("reason").orElse("Customer request");
        CancelOrderCommand command = new CancelOrderCommand(orderId, reason);
        
        return cancelOrderHandler.handle(command)
            .flatMap(order -> ServerResponse.ok().bodyValue(mapToOrderResponse(order)))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(new ErrorResponse(e.getMessage())));
    }
    
    /**
     * POST /api/orders/{orderId}/discount - Apply discount to an order.
     */
    public Mono<ServerResponse> applyDiscount(ServerRequest request) {
        String orderId = request.pathVariable("orderId");
        
        return request.bodyToMono(ApplyDiscountRequest.class)
            .flatMap(req -> {
                ApplyDiscountCommand command = new ApplyDiscountCommand(orderId, req.discountPercentage());
                return applyDiscountHandler.handle(command);
            })
            .flatMap(order -> ServerResponse.ok().bodyValue(mapToOrderResponse(order)))
            .onErrorResume(e -> ServerResponse.badRequest().bodyValue(new ErrorResponse(e.getMessage())));
    }
    
    // Helper methods
    private OrderResponse mapToOrderResponse(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getCustomerEmail().getValue(),
            order.getStatus().name(),
            order.getTotalAmount().getAmount(),
            order.getTotalAmount().getCurrency(),
            order.getDiscountAmount() != null ? order.getDiscountAmount().getAmount() : null,
            order.getFinalAmount().getAmount(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
    
    // DTO Records
    public record CreateOrderRequest(
        String customerId,
        String customerEmail,
        List<OrderItemRequest> items
    ) {
        public record OrderItemRequest(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            String currency
        ) {
        }
    }
    
    public record ApplyDiscountRequest(BigDecimal discountPercentage) {
    }
    
    public record OrderResponse(
        String orderId,
        String customerId,
        String customerEmail,
        String status,
        BigDecimal totalAmount,
        String currency,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }
    
    public record ErrorResponse(String error) {
    }
}
