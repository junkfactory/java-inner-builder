package com.github.junkfactory.innerbuilder.ui;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
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
        options.add(new TextAreaOption(
                JavaInnerBuilderOption.WITH_BUILDER_CLASS_ANNOTATIONS,
                5,
                40,
                "Generate annotations for the builder class"
        ));
        options.add(new CheckboxSelectorOption(
                JavaInnerBuilderOption.WITH_TO_BUILDER_METHOD,
                'o'
        ));
        options.add(new CheckboxSelectorOption(
                JavaInnerBuilderOption.WITH_VALIDATE_METHOD,
                'v'
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

        final JComponent[] options = buildOptions();

        final PsiFieldMember[] memberArray = members.toArray(new PsiFieldMember[0]);

        final MemberChooser<PsiFieldMember> chooser = new MemberChooser<>(memberArray,
                false, // allowEmptySelection
                true,  // allowMultiSelection
                project, null, options);

        chooser.setTitle("Select Fields and Options for the Builder");
        chooser.selectElements(memberArray);
        if (chooser.showAndGet()) {
            setPropertyValuesFromOptions(chooser.getOptionControls());
            return chooser.getSelectedElements();
        }
        return List.of();
    }

    private void setPropertyValuesFromOptions(JComponent[] options) {
        var propertiesComponent = PropertiesComponent.getInstance();
        for (var option : options) {
            if (option instanceof LabeledComponent<?> labeledComponent &&
                    labeledComponent.getComponent() instanceof JTextArea textArea) {
                var textAreaOption = (JavaInnerBuilderOption) textArea.getClientProperty(JavaInnerBuilderOption.class);
                propertiesComponent.setValue(textAreaOption.getProperty(), textArea.getText());
            } else if (option instanceof NonFocusableCheckBox checkBox) {
                var checkboxSelectorOption =
                        (JavaInnerBuilderOption) option.getClientProperty(JavaInnerBuilderOption.class);
                propertiesComponent.setValue(checkboxSelectorOption.getProperty(),
                        Boolean.toString(checkBox.isSelected()));
            } else if (option instanceof LabeledComponent<?> labeledComponent &&
                    labeledComponent.getComponent() instanceof ComboBox<?> comboBox) {
                var selectorOption = (JavaInnerBuilderOption) option.getClientProperty(JavaInnerBuilderOption.class);
                var selectedValue = (DropdownSelectorOptionValue) comboBox.getSelectedItem();
                if (null != selectedValue) {
                    propertiesComponent.setValue(selectorOption.getProperty(), selectedValue.option().getProperty());
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
            checkBoxesArray[i] = buildOptions(propertiesComponent, options.get(i));
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
        textArea.putClientProperty(JavaInnerBuilderOption.class, textAreaOption.option());
        textArea.setText(propertiesComponent.getValue(textAreaOption.option().getProperty()));
        textArea.setBorder(new CustomLineBorder(JBColor.border(), JBUI.insets(1)));
        var labeledComponent = LabeledComponent.create(textArea, textAreaOption.caption());
        labeledComponent.setToolTipText(textAreaOption.toolTip());
        return labeledComponent;
    }

    private JComponent buildCheckbox(PropertiesComponent propertiesComponent,
                                     CheckboxSelectorOption selectorOption) {
        var optionCheckBox = new NonFocusableCheckBox(selectorOption.caption());
        optionCheckBox.putClientProperty(JavaInnerBuilderOption.class, selectorOption.option());
        optionCheckBox.setMnemonic(selectorOption.mnemonic());
        optionCheckBox.setToolTipText(selectorOption.toolTip());

        var optionProperty = selectorOption.option().getProperty();
        optionCheckBox.setSelected(propertiesComponent.isTrueValue(optionProperty));
        return optionCheckBox;
    }

    private JComponent buildDropdown(PropertiesComponent propertiesComponent,
                                     DropdownSelectorOption selectorOption) {
        final var comboBox = new ComboBox<DropdownSelectorOptionValue>();
        comboBox.putClientProperty(JavaInnerBuilderOption.class, selectorOption.option());
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
