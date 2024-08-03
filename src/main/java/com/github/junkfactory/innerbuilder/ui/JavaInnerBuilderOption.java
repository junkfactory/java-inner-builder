package com.github.junkfactory.innerbuilder.ui;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum JavaInnerBuilderOption {
    TO_BUILDER("toBuilder", "Generate 'toBuilder' method");

    private final String property;
    private final String description;
    private final Boolean booleanProperty;

    JavaInnerBuilderOption(final String property, String description) {
        this(property, description, true);
    }

    JavaInnerBuilderOption(final String property, String description, final Boolean booleanProperty) {
        this.property = String.format("JavaInnerBuilder.%s", property);
        this.description = description;
        this.booleanProperty = booleanProperty;
    }

    public String getProperty() {
        return property;
    }

    public Boolean isBooleanProperty() {
        return booleanProperty;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<JavaInnerBuilderOption> findValue(String value) {
        return Arrays.stream(values())
                .filter(it -> Objects.equals(it.getProperty(), value))
                .findFirst();
    }

}
