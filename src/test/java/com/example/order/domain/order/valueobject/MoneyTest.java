package com.example.order.domain.order.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithBigDecimal() {
        Money money = Money.of(new BigDecimal("100.00"), "USD");
        
        assertThat(money.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(money.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldCreateMoneyWithDouble() {
        Money money = Money.of(50.5, "USD");
        
        assertThat(money.getAmount()).isEqualTo(new BigDecimal("50.5"));
        assertThat(money.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldCreateZeroMoney() {
        Money money = Money.zero("USD");
        
        assertThat(money.getAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(money.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNull() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null, "USD"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Amount cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenCurrencyIsNull() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("100"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Currency cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenAmountIsNegative() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-100"), "USD"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Amount cannot be negative");
    }

    @Test
    void shouldAddMoneyWithSameCurrency() {
        Money money1 = Money.of(100, "USD");
        Money money2 = Money.of(50, "USD");
        
        Money result = money1.add(money2);
        
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        Money money1 = Money.of(100, "USD");
        Money money2 = Money.of(50, "EUR");
        
        assertThatThrownBy(() -> money1.add(money2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Currency mismatch");
    }

    @Test
    void shouldMultiplyMoney() {
        Money money = Money.of(100, "USD");
        
        Money result = money.multiply(3);
        
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("300"));
    }

    @Test
    void shouldApplyDiscount() {
        Money money = Money.of(100, "USD");
        
        Money result = money.discount(new BigDecimal("0.2"));
        
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("80"));
    }

    @Test
    void shouldCompareMoney() {
        Money money1 = Money.of(100, "USD");
        Money money2 = Money.of(50, "USD");
        Money money3 = Money.of(100, "USD");
        
        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money2.isGreaterThan(money1)).isFalse();
        assertThat(money1.isGreaterThan(money3)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "100, USD, 100, USD, true",
        "100, USD, 100, EUR, false",
        "100, USD, 200, USD, false"
    })
    void shouldTestEquality(String amount1, String currency1, String amount2, String currency2, boolean expected) {
        Money money1 = Money.of(new BigDecimal(amount1), currency1);
        Money money2 = Money.of(new BigDecimal(amount2), currency2);
        
        assertThat(money1.equals(money2)).isEqualTo(expected);
    }

    @Test
    void shouldReturnCorrectStringRepresentation() {
        Money money = Money.of(100.5, "USD");
        
        assertThat(money.toString()).contains("100.5").contains("USD");
    }
}
