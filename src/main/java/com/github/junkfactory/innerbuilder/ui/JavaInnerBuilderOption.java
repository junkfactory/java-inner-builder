package com.github.junkfactory.innerbuilder.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiManager;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum JavaInnerBuilderOption {
    WITH_TO_BUILDER_METHOD("JavaInnerBuilderOption.toBuilder",
            "Generate 'toBuilder()' method",
            Type.BOOLEAN),
    WITH_VALIDATE_METHOD("JavaInnerBuilderOption.validate",
            "Generate 'validate()' method",
            Type.BOOLEAN);

    private final String property;
    private final String description;
    private final Type type;
    private final OptionValidatorFactory validatorFactory;

    JavaInnerBuilderOption(String property, String description, Type type) {
        this(property, description, type, (p, d, j) -> {
        });
    }

    JavaInnerBuilderOption(String property, String description, Type type,
                           OptionValidatorFactory validatorFactory) {
        this.property = String.format("JavaInnerBuilder.%s", property);
        this.description = description;
        this.type = type;
        this.validatorFactory = validatorFactory;
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

    public void createValidator(PsiManager psiManager, Disposable disposable, JComponent component) {
        if (component instanceof LabeledComponent<?> labeledComponent) {
            component = labeledComponent.getComponent();
        }
        validatorFactory.create(psiManager, disposable, component);
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
