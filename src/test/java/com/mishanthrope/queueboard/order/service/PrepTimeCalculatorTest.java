package com.mishanthrope.queueboard.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mishanthrope.queueboard.order.config.OrdersProperties;
import com.mishanthrope.queueboard.order.model.OrderItem;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PrepTimeCalculatorTest {

    private static final int DEFAULT_CAPACITY = 5;

    private static PrepTimeCalculator calculator(int grillCapacity) {
        var prepTime = new OrdersProperties.PrepTime(
                Duration.ofMinutes(7), Duration.ofMinutes(3), Duration.ofMinutes(5), grillCapacity);
        return new PrepTimeCalculator(new OrdersProperties(Duration.ofMinutes(2), prepTime));
    }

    private static OrderItem item(int quantity) {
        var item = new OrderItem();
        item.setProductName("Шаурма");
        item.setQuantity(quantity);
        return item;
    }

    @ParameterizedTest(name = "{0} items -> {1} min")
    @CsvSource({"1,15", "2,18", "5,27", "6,32", "10,42"})
    void ownTime_forSingleItemLine_followsGrillSimulation(int quantity, long expectedMinutes) {
        Duration time = calculator(DEFAULT_CAPACITY).ownTime(List.of(item(quantity)));

        assertThat(time).isEqualTo(Duration.ofMinutes(expectedMinutes));
    }

    @Test
    void ownTime_withSeveralItemLines_sumsQuantities() {
        Duration time = calculator(DEFAULT_CAPACITY)
                .ownTime(List.of(item(2), item(3), item(1)));

        assertThat(time).isEqualTo(Duration.ofMinutes(32));
    }

    @ParameterizedTest(name = "{0} items, capacity 3 -> {1} min")
    @CsvSource({"1,15", "3,21", "4,26", "7,35"})
    void ownTime_withSmallerGrill_batchesByCapacity(int quantity, long expectedMinutes) {
        Duration time = calculator(3).ownTime(List.of(item(quantity)));

        assertThat(time).isEqualTo(Duration.ofMinutes(expectedMinutes));
    }
}
