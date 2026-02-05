package com.example.order.adapter.out.persistence.mapper;

import com.example.common.domain.valueobject.Email;
import com.example.common.domain.valueobject.Money;
import com.example.order.adapter.out.persistence.entity.OrderEntity;
import com.example.order.adapter.out.persistence.entity.OrderItemEntity;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderItem;
import com.example.order.domain.service.DiscountCalculationService;
import com.example.order.domain.service.PriceValidationService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public abstract class OrderMapper {

    @Autowired
    protected PriceValidationService priceService;
    
    @Autowired
    protected OrderItemMapper itemMapper;

    public Order toDomain(OrderEntity entity) {
        if (entity == null) return null;

        List<OrderItem> items = itemMapper.toDomainList(entity.getItems());
        Email customerEmail = Email.of(entity.getCustomerEmail());
        Money totalAmount = Money.of(entity.getTotalAmount(), entity.getCurrency());

        return Order.reconstitute(
            entity.getId(),
            entity.getCustomerId(),
            customerEmail,
            entity.getStatus(),
            items,
            totalAmount,
            entity.getDiscountAmount() != null ? Money.of(entity.getDiscountAmount(), entity.getCurrency()) : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    @Mapping(target = "customerEmail", source = "customerEmail.value")
    @Mapping(target = "totalAmount", source = "totalAmount.amount")
    @Mapping(target = "currency", source = "totalAmount.currency")
    @Mapping(target = "discountAmount", source = "discountAmount.amount")
    public abstract OrderEntity toEntity(Order order);

    @AfterMapping
    protected void linkItems(@MappingTarget OrderEntity entity) {
        if (entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setOrder(entity));
        }
    }
}
