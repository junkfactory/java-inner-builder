package com.github.junkfactory.innerbuilder.ui;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.LabeledComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

class ValidatingFieldMemberChooser extends MemberChooser<PsiFieldMember> {
    ValidatingFieldMemberChooser(PsiFieldMember[] elements, boolean allowEmptySelection, boolean allowMultiSelection,
                                 @NotNull Project project, @Nullable JComponent headerPanel,
                                 JComponent[] optionControls) {
        super(elements, allowEmptySelection, allowMultiSelection, project, headerPanel, optionControls);
    }

    @Override
    protected void doOKAction() {
        boolean hasErrors = false;
        for (var optionComponent : getOptionControls()) {
            optionComponent = optionComponent instanceof LabeledComponent<?> labeledComponent ?
                    labeledComponent.getComponent() : optionComponent;
            var optionalValidator = ComponentValidator.getInstance(optionComponent);
            if (optionalValidator.isPresent()) {
                var validator = optionalValidator.get();
                validator.revalidate();
                hasErrors |= null != validator.getValidationInfo();
            }
        }
        if (!hasErrors) {
            super.doOKAction();
        }
    }
}
