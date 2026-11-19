package com.mishanthrope.queueboard.domain.employee;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final int MAX_LENGTH = 255;
    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidEmployeeError("Email must not be blank");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidEmployeeError("Email is malformed");
        }
    }
}
