package com.example.order.adapter.out.persistence.mapper;

import com.example.common.domain.valueobject.Money;
import com.example.order.adapter.out.persistence.entity.OrderItemEntity;
import com.example.order.domain.model.OrderItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-06T00:19:23+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.5.jar, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class OrderItemMapperImpl implements OrderItemMapper {

    @Override
    public OrderItemEntity toEntity(OrderItem domain) {
        if ( domain == null ) {
            return null;
        }

        OrderItemEntity.OrderItemEntityBuilder orderItemEntity = OrderItemEntity.builder();

        orderItemEntity.unitPrice( domainUnitPriceAmount( domain ) );
        orderItemEntity.currency( domainUnitPriceCurrency( domain ) );
        orderItemEntity.productId( domain.getProductId() );
        orderItemEntity.productName( domain.getProductName() );
        orderItemEntity.quantity( domain.getQuantity() );

        return orderItemEntity.build();
    }

    @Override
    public List<OrderItem> toDomainList(List<OrderItemEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<OrderItem> list = new ArrayList<OrderItem>( entities.size() );
        for ( OrderItemEntity orderItemEntity : entities ) {
            list.add( toDomain( orderItemEntity ) );
        }

        return list;
    }

    private BigDecimal domainUnitPriceAmount(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Money unitPrice = orderItem.getUnitPrice();
        if ( unitPrice == null ) {
            return null;
        }
        BigDecimal amount = unitPrice.getAmount();
        if ( amount == null ) {
            return null;
        }
        return amount;
    }

    private String domainUnitPriceCurrency(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Money unitPrice = orderItem.getUnitPrice();
        if ( unitPrice == null ) {
            return null;
        }
        String currency = unitPrice.getCurrency();
        if ( currency == null ) {
            return null;
        }
        return currency;
    }
}
