package com.github.junkfactory.innerbuilder.ui;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum JavaInnerBuilderOption {
    WITH_TO_BUILDER_METHOD("JavaInnerBuilderOption.toBuilder",
            "Generate 'toBuilder()' method",
            Type.BOOLEAN),
    WITH_VALIDATE_METHOD("JavaInnerBuilderOption.validate",
            "Generate 'validate()' method",
            Type.BOOLEAN),
    WITH_BUILDER_CLASS_ANNOTATIONS("JavaInnerBuilderOption.builderClassAnnotations",
            "Generate annotations for the builder class",
            Type.LIST);

    private final String property;
    private final String description;
    private final Type type;

    JavaInnerBuilderOption(String property, String description, Type type) {
        this.property = String.format("JavaInnerBuilder.%s", property);
        this.description = description;
        this.type = type;
    }

    public String getProperty() {
        return property;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<JavaInnerBuilderOption> findValue(String value) {
        return Arrays.stream(values())
                .filter(it -> Objects.equals(it.getProperty(), value))
                .findFirst();
    }

    public enum Type {
        BOOLEAN, LIST
    }
}
