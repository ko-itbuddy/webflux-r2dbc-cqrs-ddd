package com.example.order.infrastructure.persistence.mapper;

import com.example.order.domain.order.entity.OrderItem;
import com.example.order.domain.order.valueobject.Money;
import com.example.order.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between OrderItem domain objects and OrderItemEntity persistence objects.
 */
@Component
public class OrderItemMapper {
    
    /**
     * Convert an OrderItemEntity to a domain OrderItem.
     */
    public OrderItem toDomain(OrderItemEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Money unitPrice = Money.of(entity.getUnitPrice(), entity.getCurrency());
        
        return OrderItem.of(
            entity.getProductId(),
            entity.getProductName(),
            entity.getQuantity(),
            unitPrice
        );
    }
    
    /**
     * Convert an OrderItem domain object to an OrderItemEntity.
     */
    public OrderItemEntity toEntity(OrderItem domain, String orderId) {
        if (domain == null) {
            return null;
        }
        
        OrderItemEntity entity = new OrderItemEntity();
        entity.setOrderId(orderId);
        entity.setProductId(domain.getProductId());
        entity.setProductName(domain.getProductName());
        entity.setQuantity(domain.getQuantity());
        entity.setUnitPrice(domain.getUnitPrice().getAmount());
        entity.setCurrency(domain.getUnitPrice().getCurrency());
        
        return entity;
    }
    
    /**
     * Convert a list of OrderItemEntity objects to domain OrderItem objects.
     */
    public List<OrderItem> toDomainList(List<OrderItemEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        
        return entities.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert a list of domain OrderItem objects to OrderItemEntity objects.
     */
    public List<OrderItemEntity> toEntityList(List<OrderItem> domains, String orderId) {
        if (domains == null) {
            return List.of();
        }
        
        return domains.stream()
            .map(item -> toEntity(item, orderId))
            .collect(Collectors.toList());
    }
}
