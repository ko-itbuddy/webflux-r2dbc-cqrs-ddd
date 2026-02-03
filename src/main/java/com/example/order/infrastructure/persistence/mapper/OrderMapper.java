package com.example.order.infrastructure.persistence.mapper;

import com.example.order.application.service.DiscountCalculationService;
import com.example.order.application.service.OrderData;
import com.example.order.application.service.PriceValidationService;
import com.example.order.domain.order.entity.Order;
import com.example.order.domain.order.entity.OrderItem;
import com.example.order.domain.order.valueobject.Email;
import com.example.order.domain.order.valueobject.Money;
import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Mapper for converting between Order domain objects and OrderEntity persistence objects.
 * Business logic is injected via application services (Strategy pattern).
 */
@Component
public class OrderMapper {
    
    private final Function<OrderData, BigDecimal> discountCalculator;
    private final Predicate<Money> priceValidator;
    private final OrderItemMapper itemMapper;
    
    /**
     * Constructor with injected application services.
     * Services provide business logic through function interfaces.
     */
    public OrderMapper(
            DiscountCalculationService discountService,
            PriceValidationService priceService,
            OrderItemMapper itemMapper) {
        this.discountCalculator = discountService.getDiscountCalculator();
        this.priceValidator = priceService.getPriceValidator();
        this.itemMapper = itemMapper;
    }
    
    /**
     * Convert OrderEntity and OrderItemEntities to domain Order object.
     * Validates prices and calculates discount using injected business logic.
     */
    public Order toDomain(OrderEntity entity, List<OrderItemEntity> itemEntities) {
        if (entity == null) {
            return null;
        }
        
        // Convert items using injected item mapper
        List<OrderItem> items = itemMapper.toDomainList(itemEntities);
        
        // Create email value object
        Email customerEmail = Email.of(entity.getCustomerEmail());
        
        // Create the domain order (reconstructs from database state)
        Money totalAmount = Money.of(entity.getTotalAmount(), entity.getCurrency());
        
        // Validate price using injected validator
        if (!priceValidator.test(totalAmount)) {
            throw new IllegalStateException("Invalid price stored in database for order: " + entity.getId());
        }
        
        // Create OrderData for discount calculation
        int itemCount = items.stream().mapToInt(OrderItem::getQuantity).sum();
        OrderData orderData = new OrderData(
            entity.getId(),
            entity.getCustomerId(),
            entity.getTotalAmount(),
            itemCount,
            entity.getStatus(),
            entity.getCreatedAt()
        );
        
        // Calculate potential discount using injected calculator
        BigDecimal calculatedDiscount = discountCalculator.apply(orderData);
        
        // Create order - Note: We use the factory method but need to adjust state
        Order order = Order.create(entity.getCustomerId(), customerEmail, items);
        
        // Apply discount if it was stored (recreate the discount state)
        if (entity.getDiscountAmount() != null && 
            entity.getDiscountAmount().compareTo(entity.getTotalAmount()) < 0) {
            // Calculate discount percentage from stored amounts
            BigDecimal discountPercentage = BigDecimal.ONE.subtract(
                entity.getDiscountAmount().divide(entity.getTotalAmount(), 4, RoundingMode.HALF_UP)
            );
            order.applyDiscount(discountPercentage);
        }
        
        return order;
    }
    
    /**
     * Convert domain Order to OrderEntity for persistence.
     */
    public OrderEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }
        
        Money finalAmount = order.getFinalAmount();
        Money totalAmount = order.getTotalAmount();
        
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setCustomerId(order.getCustomerId());
        entity.setCustomerEmail(order.getCustomerEmail().getValue());
        entity.setStatus(order.getStatus());
        entity.setTotalAmount(totalAmount.getAmount());
        entity.setCurrency(totalAmount.getCurrency());
        
        // Store discount amount if applied
        if (!finalAmount.equals(totalAmount)) {
            entity.setDiscountAmount(finalAmount.getAmount());
        }
        
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * Extract items from a domain order as entities.
     */
    public List<OrderItemEntity> extractItemEntities(Order order) {
        if (order == null) {
            return List.of();
        }
        
        return itemMapper.toEntityList(order.getItems(), order.getId());
    }
}
