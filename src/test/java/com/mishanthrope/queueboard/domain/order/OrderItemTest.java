package com.mishanthrope.queueboard.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderItemTest {

    static Stream<Arguments> invalidItems() {
        return Stream.of(
                Arguments.of("empty product name", "", 1, List.of(),
                        "Product name must be 1 to 100 characters"),
                Arguments.of("blank product name", "   ", 1, List.of(),
                        "Product name must be 1 to 100 characters"),
                Arguments.of("null product name", null, 1, List.of(),
                        "Product name must be 1 to 100 characters"),
                Arguments.of("product name over 100", "x".repeat(101), 1, List.of(),
                        "Product name must be 1 to 100 characters"),
                Arguments.of("quantity 0", "Шаурма", 0, List.of(),
                        "Quantity of 'Шаурма' must be between 1 and 99"),
                Arguments.of("negative quantity", "Шаурма", -1, List.of(),
                        "Quantity of 'Шаурма' must be between 1 and 99"),
                Arguments.of("quantity 100", "Шаурма", 100, List.of(),
                        "Quantity of 'Шаурма' must be between 1 and 99"),
                Arguments.of("modifier over 100", "Шаурма", 1, List.of("x".repeat(101)),
                        "Modifiers of 'Шаурма' must be at most 100 characters"));
    }

    static Stream<Arguments> validItems() {
        return Stream.of(
                Arguments.of("name of 1 character", "x", 1, List.of()),
                Arguments.of("name of 100 characters", "x".repeat(100), 1, List.of()),
                Arguments.of("quantity 99", "Шаурма", 99, List.of()),
                Arguments.of("modifier of 100 characters", "Шаурма", 1,
                        List.of("x".repeat(100))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidItems")
    void constructor_withInvalidData_throwsInvalidOrder(
            String description, String name, int quantity, List<String> modifiers,
            String message) {
        assertThatThrownBy(() -> new OrderItem(null, name, quantity, modifiers))
                .isInstanceOf(InvalidOrderError.class)
                .hasMessage(message);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validItems")
    void constructor_withBoundaryData_keepsValues(
            String description, String name, int quantity, List<String> modifiers) {
        var item = new OrderItem(null, name, quantity, modifiers);

        assertThat(item.id()).isNull();
        assertThat(item.productName()).isEqualTo(name);
        assertThat(item.quantity()).isEqualTo(quantity);
        assertThat(item.modifiers()).isEqualTo(modifiers);
    }

    @Test
    void constructor_withNullModifiers_usesEmptyList() {
        assertThat(new OrderItem(null, "Шаурма", 1, null).modifiers()).isEmpty();
    }

    @Test
    void constructor_copiesModifiersImmutably() {
        var source = new ArrayList<>(List.of("без цибулі"));
        var item = new OrderItem(1L, "Шаурма", 1, source);

        source.add("гострий соус");

        assertThat(item.modifiers()).containsExactly("без цибулі");
        assertThatThrownBy(() -> item.modifiers().add("сир"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
