package com.mishanthrope.queueboard.domain.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void constructor_stripsAndLowerCases() {
        assertThat(new Email("  Cook@Example.COM ").value()).isEqualTo("cook@example.com");
    }

    @Test
    void equals_ignoresCaseAndSurroundingSpaces() {
        assertThat(new Email(" COOK@example.com")).isEqualTo(new Email("cook@example.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void constructor_withBlankValue_throwsInvalidEmployee(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(InvalidEmployeeError.class)
                .hasMessage("Email must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain", "no-at.example.com", "@example.com", "cook@", "a@b@c.com",
            "co ok@example.com"})
    void constructor_withMalformedValue_throwsInvalidEmployee(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(InvalidEmployeeError.class)
                .hasMessage("Email is malformed");
    }

    @Test
    void constructor_withTooLongValue_throwsInvalidEmployee() {
        String tooLong = "x".repeat(250) + "@a.com";

        assertThatThrownBy(() -> new Email(tooLong)).isInstanceOf(InvalidEmployeeError.class);
    }
}
