package com.example.order.adapter.out.persistence.mapper;

import com.example.order.domain.model.OrderItem;
import com.example.common.domain.valueobject.Money;
import com.example.order.adapter.out.persistence.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    default OrderItem toDomain(OrderItemEntity entity) {
        if (entity == null) return null;
        return OrderItem.of(
            entity.getProductId(),
            entity.getProductName(),
            entity.getQuantity(),
            Money.of(entity.getUnitPrice(), entity.getCurrency())
        );
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "unitPrice", source = "domain.unitPrice.amount")
    @Mapping(target = "currency", source = "domain.unitPrice.currency")
    OrderItemEntity toEntity(OrderItem domain);

    List<OrderItem> toDomainList(List<OrderItemEntity> entities);

    default List<OrderItemEntity> toEntityList(List<OrderItem> domains) {
        if (domains == null) return null;
        return domains.stream().map(this::toEntity).toList();
    }
}