package com.example.common.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires
@Embeddable
public final class Money {
    private BigDecimal amount;
    private String currency;

    @JsonCreator
    public static Money of(@JsonProperty("amount") Object amount, @JsonProperty("currency") String currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        
        BigDecimal decimalAmount;
        if (amount instanceof BigDecimal) {
            decimalAmount = (BigDecimal) amount;
        } else if (amount instanceof Number) {
            decimalAmount = BigDecimal.valueOf(((Number) amount).doubleValue());
        } else if (amount instanceof String) {
            decimalAmount = new BigDecimal((String) amount);
        } else {
            throw new IllegalArgumentException("Amount must be a number or string");
        }
        
        if (decimalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return new Money(decimalAmount, currency);
    }

    public static Money of(BigDecimal amount, String currency) {
        return of((Object) amount, currency);
    }

    public static Money of(double amount, String currency) {
        return of(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        validateCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    public Money discount(BigDecimal percentage) {
        BigDecimal discountFactor = BigDecimal.ONE.subtract(percentage);
        return new Money(this.amount.multiply(discountFactor), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        validateCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    private void validateCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
