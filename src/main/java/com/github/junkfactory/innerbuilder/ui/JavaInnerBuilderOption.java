package com.github.junkfactory.innerbuilder.ui;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum JavaInnerBuilderOption {
    WITH_TO_BUILDER_METHOD("JavaInnerBuilderOption.toBuilder", "Generate 'toBuilder()' method"),
    WITH_VALIDATE_METHOD("JavaInnerBuilderOption.validate", "Generate 'validate()' method"),
    WITH_BUILDER_CLASS_ANNOTATIONS("JavaInnerBuilderOption.builderClassAnnotations",
            "Generate annotations for the builder class");

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
