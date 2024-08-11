package com.github.junkfactory.innerbuilder.ui;

import com.github.junkfactory.innerbuilder.generators.Utils;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaInnerBuilderOptionSelector {

    private final DropdownListCellRenderer renderer = new DropdownListCellRenderer();

    private final List<PsiFieldMember> members;
    private final Project project;

    private JavaInnerBuilderOptionSelector(Builder builder) {
        members = builder.members;
        project = builder.project;
    }

    public static Builder builder() {
        return new Builder();
    }

    private List<SelectorOption> createGeneratorOptions() {
        var options = new ArrayList<SelectorOption>();
        options.add(new CheckboxSelectorOption(
                JavaInnerBuilderOption.WITH_TO_BUILDER_METHOD,
                'o'
        ));
        options.add(new CheckboxSelectorOption(
                JavaInnerBuilderOption.WITH_VALIDATE_METHOD,
                'v'
        ));
        options.add(new TextAreaOption(
                JavaInnerBuilderOption.WITH_BUILDER_CLASS_ANNOTATIONS,
                5,
                40,
                "Use fully qualified class names separated by new lines."
        ));
        return options;
    }

    public List<PsiFieldMember> selectFieldsAndOptions() {
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return members;
        }

        var optionsArray = buildOptions();
        var memberArray = members.toArray(new PsiFieldMember[0]);
        var chooser = new ValidatingFieldMemberChooser(memberArray,
                false, // allowEmptySelection
                true,  // allowMultiSelection
                project, null, optionsArray) {
        };
        for (var optionComponent : optionsArray) {
            var builderOption = (JavaInnerBuilderOption) optionComponent.getClientProperty(
                    JavaInnerBuilderOption.class);
            builderOption.createValidator(PsiManager.getInstance(project), chooser.getDisposable(), optionComponent);
        }

        chooser.setTitle("Select Fields and Options for the Builder");
        chooser.selectElements(memberArray);
        if (chooser.showAndGet()) {
            var optionControls = chooser.getOptionControls();
            setPropertyValuesFromOptions(optionControls);
            return chooser.getSelectedElements();
        }
        return List.of();
    }

    private void setPropertyValuesFromOptions(JComponent[] optionComponents) {
        var propertiesComponent = PropertiesComponent.getInstance();
        for (var component : optionComponents) {
            var option = (JavaInnerBuilderOption) component.getClientProperty(JavaInnerBuilderOption.class);
            if (component instanceof LabeledComponent<?> labeledComponent &&
                    labeledComponent.getComponent() instanceof JTextArea textArea) {
                var annotations = Utils.stringToList(textArea.getText());
                propertiesComponent.setList(option.getProperty(), annotations);
            } else if (component instanceof NonFocusableCheckBox checkBox) {
                propertiesComponent.setValue(option.getProperty(),
                        Boolean.toString(checkBox.isSelected()));
            } else if (component instanceof LabeledComponent<?> labeledComponent &&
                    labeledComponent.getComponent() instanceof ComboBox<?> comboBox) {
                var selectedValue = (DropdownSelectorOptionValue) comboBox.getSelectedItem();
                if (null != selectedValue) {
                    propertiesComponent.setValue(option.getProperty(), selectedValue.option().getProperty());
                }
            }
        }
    }

    private JComponent[] buildOptions() {
        var propertiesComponent = PropertiesComponent.getInstance();
        var options = createGeneratorOptions();
        var optionCount = options.size();
        var checkBoxesArray = new JComponent[optionCount];
        for (int i = 0; i < optionCount; i++) {
            var option = options.get(i);
            var optionComponent = buildOptions(propertiesComponent, option);
            optionComponent.putClientProperty(JavaInnerBuilderOption.class, option.option());
            checkBoxesArray[i] = optionComponent;
        }
        return checkBoxesArray;
    }

    private JComponent buildOptions(PropertiesComponent propertiesComponent, SelectorOption selectorOption) {
        if (selectorOption instanceof CheckboxSelectorOption checkboxSelectorOption) {
            return buildCheckbox(propertiesComponent, checkboxSelectorOption);
        } else if (selectorOption instanceof TextAreaOption textAreaOption) {
            return buildTextArea(propertiesComponent, textAreaOption);
        }
        return buildDropdown(propertiesComponent, (DropdownSelectorOption) selectorOption);
    }

    @NotNull
    private LabeledComponent<JTextArea> buildTextArea(PropertiesComponent propertiesComponent,
                                                      TextAreaOption textAreaOption) {
        var textArea = new JTextArea(textAreaOption.numLines(), textAreaOption.numColumns());
        if (textAreaOption.option().getType() == JavaInnerBuilderOption.Type.LIST) {
            var annotations = propertiesComponent.getList(textAreaOption.option().getProperty());
            if (null != annotations) {
                textArea.setText(String.join("\n", annotations));
            }
        } else {
            textArea.setText(propertiesComponent.getValue(textAreaOption.option().getProperty()));
        }
        textArea.setBorder(new CustomLineBorder(JBColor.border(), JBUI.insets(1)));
        var labeledComponent = LabeledComponent.create(textArea, textAreaOption.caption());
        labeledComponent.setToolTipText(textAreaOption.toolTip());
        return labeledComponent;
    }

    private JComponent buildCheckbox(PropertiesComponent propertiesComponent,
                                     CheckboxSelectorOption selectorOption) {
        var optionCheckBox = new NonFocusableCheckBox(selectorOption.caption());
        optionCheckBox.setMnemonic(selectorOption.mnemonic());
        optionCheckBox.setToolTipText(selectorOption.toolTip());

        var optionProperty = selectorOption.option().getProperty();
        optionCheckBox.setSelected(propertiesComponent.isTrueValue(optionProperty));
        return optionCheckBox;
    }

    private JComponent buildDropdown(PropertiesComponent propertiesComponent,
                                     DropdownSelectorOption selectorOption) {
        final var comboBox = new ComboBox<DropdownSelectorOptionValue>();
        comboBox.setEditable(false);
        comboBox.setRenderer(renderer);
        selectorOption.values().forEach(comboBox::addItem);

        comboBox.setSelectedItem(setSelectedComboBoxItem(propertiesComponent, selectorOption));

        var labeledComponent = LabeledComponent.create(comboBox, selectorOption.caption());
        labeledComponent.setToolTipText(selectorOption.toolTip());
        return labeledComponent;
    }

    private DropdownSelectorOptionValue setSelectedComboBoxItem(PropertiesComponent propertiesComponent,
                                                                DropdownSelectorOption selectorOption) {
        var selectedValue = propertiesComponent.getValue(selectorOption.option().getProperty());
        var selectorOptionValue = selectorOption.values()
                .stream()
                .filter(it -> Objects.equals(it.option().getProperty(), selectedValue))
                .findFirst()
                .orElse(selectorOption.values().get(0));
        if (null == selectedValue) {
            propertiesComponent.setValue(selectorOption.option().getProperty(),
                    selectorOptionValue.option().getProperty());
        }
        return selectorOptionValue;
    }

    public static final class Builder {
        private List<PsiFieldMember> members;
        private Project project;

        private Builder() {
        }

        public Builder members(List<PsiFieldMember> members) {
            this.members = members;
            return this;
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public JavaInnerBuilderOptionSelector build() {
            return new JavaInnerBuilderOptionSelector(this);
        }
    }
}
