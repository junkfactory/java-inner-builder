package com.github.junkfactory.innerbuilder.ui;

import com.github.junkfactory.innerbuilder.generators.Utils;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.ClassUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JTextArea;
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
            Type.LIST,
            (p, d, j) -> new ComponentValidator(d).withValidator(() -> {
                if (j instanceof JTextArea textArea) {
                    var errors = new StringBuilder();
                    var annotations = Utils.stringToList(textArea.getText());
                    for (var annotation : annotations) {
                        if (ClassUtil.findPsiClass(p, annotation) == null) {
                            errors.append(" - ").append(annotation).append("\n");
                        }
                    }
                    if (!errors.isEmpty()) {
                        textArea.setBorder(new CustomLineBorder(JBColor.RED, JBUI.insets(1)));
                        return new ValidationInfo(errors.insert(0, "Annotations not found")
                                .append(System.lineSeparator()).toString(), textArea);
                    }
                    textArea.setBorder(new CustomLineBorder(JBColor.border(), JBUI.insets(1)));
                }
                return null;
            }).installOn(j));

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
