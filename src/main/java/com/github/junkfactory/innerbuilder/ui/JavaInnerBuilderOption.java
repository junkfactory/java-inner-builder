package com.github.junkfactory.innerbuilder.ui;

import com.intellij.ide.util.PropertiesComponent;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    public static Set<JavaInnerBuilderOption> currentOptions() {
        final var options = EnumSet.noneOf(JavaInnerBuilderOption.class);
        final var propertiesComponent = PropertiesComponent.getInstance();
        for (var option : JavaInnerBuilderOption.values()) {

            if (Boolean.TRUE.equals(option.isBooleanProperty())) {
                final boolean currentSetting = propertiesComponent.getBoolean(option.getProperty(), false);
                if (currentSetting) {
                    options.add(option);
                }
            } else {
                String currentValue = String.valueOf(propertiesComponent.getValue(option.getProperty()));
                if (currentValue != null) {
                    JavaInnerBuilderOption.findValue(currentValue).ifPresent(options::add);
                }
            }
        }
        return options;
    }
}
