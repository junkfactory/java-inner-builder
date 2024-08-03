package com.github.junkfactory.innerbuilder.ui;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.NonFocusableCheckBox;

import javax.swing.JComponent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaInnerBuilderOptionSelector {

    private static final DropdownListCellRenderer RENDERER = new DropdownListCellRenderer();
    private static final List<SelectorOption> OPTIONS = createGeneratorOptions();

    private static List<SelectorOption> createGeneratorOptions() {
        var options = new ArrayList<SelectorOption>();
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

    private JavaInnerBuilderOptionSelector() {
    }

    public static List<PsiFieldMember> selectFieldsAndOptions(final List<PsiFieldMember> members,
                                                              final Project project) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return members;
        }

        final JComponent[] optionCheckBoxes = buildOptions();

        final PsiFieldMember[] memberArray = members.toArray(new PsiFieldMember[0]);

        final MemberChooser<PsiFieldMember> chooser = new MemberChooser<>(memberArray,
                false, // allowEmptySelection
                true,  // allowMultiSelection
                project, null, optionCheckBoxes);

        chooser.setTitle("Select Fields and Options for the Builder");
        chooser.selectElements(memberArray);
        if (chooser.showAndGet()) {
            return chooser.getSelectedElements();
        }

        return List.of();
    }

    private static JComponent[] buildOptions() {
        var propertiesComponent = PropertiesComponent.getInstance();
        var optionCount = OPTIONS.size();
        var checkBoxesArray = new JComponent[optionCount];
        for (int i = 0; i < optionCount; i++) {
            checkBoxesArray[i] = buildOptions(propertiesComponent, OPTIONS.get(i));
        }
        return checkBoxesArray;
    }

    private static JComponent buildOptions(PropertiesComponent propertiesComponent, SelectorOption selectorOption) {
        if (selectorOption instanceof CheckboxSelectorOption checkboxSelectorOption) {
            return buildCheckbox(propertiesComponent, checkboxSelectorOption);
        }
        return buildDropdown(propertiesComponent, (DropdownSelectorOption) selectorOption);
    }

    private static JComponent buildCheckbox(PropertiesComponent propertiesComponent,
                                            CheckboxSelectorOption selectorOption) {
        var optionCheckBox = new NonFocusableCheckBox(selectorOption.caption());
        optionCheckBox.setMnemonic(selectorOption.mnemonic());
        optionCheckBox.setToolTipText(selectorOption.toolTip());

        var optionProperty = selectorOption.option().getProperty();
        optionCheckBox.setSelected(propertiesComponent.isTrueValue(optionProperty));
        optionCheckBox.addItemListener(
                event -> propertiesComponent.setValue(optionProperty, Boolean.toString(optionCheckBox.isSelected())));
        return optionCheckBox;
    }

    private static JComponent buildDropdown(PropertiesComponent propertiesComponent,
                                            DropdownSelectorOption selectorOption) {
        final var comboBox = new ComboBox<DropdownSelectorOptionValue>();
        comboBox.setEditable(false);
        comboBox.setRenderer(RENDERER);
        selectorOption.values().forEach(comboBox::addItem);

        comboBox.setSelectedItem(setSelectedComboBoxItem(propertiesComponent, selectorOption));
        comboBox.addItemListener(event -> setPropertiesComponentValue(propertiesComponent, selectorOption, event));

        var labeledComponent = LabeledComponent.create(comboBox, selectorOption.caption());
        labeledComponent.setToolTipText(selectorOption.toolTip());

        return labeledComponent;
    }

    private static void setPropertiesComponentValue(PropertiesComponent propertiesComponent,
                                                    DropdownSelectorOption selectorOption, ItemEvent itemEvent) {
        var value = (DropdownSelectorOptionValue) itemEvent.getItem();
        propertiesComponent.setValue(selectorOption.option().getProperty(), value.option().getProperty());
    }

    private static DropdownSelectorOptionValue setSelectedComboBoxItem(PropertiesComponent propertiesComponent,
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
}
