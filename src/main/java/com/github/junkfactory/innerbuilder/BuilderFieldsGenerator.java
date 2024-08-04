package com.github.junkfactory.innerbuilder;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

class BuilderFieldsGenerator extends AbstractGenerator {

    private final BuilderClassParams builderClassParams;
    private PsiElement lastAddedField;

    BuilderFieldsGenerator(GeneratorParams generatorParams, BuilderClassParams builderClassParams) {
        super(generatorParams);
        this.builderClassParams = builderClassParams;
    }

    public PsiElement getLastAddedField() {
        return lastAddedField;
    }

    @Override
    public void run() {
        var selectedFields = generatorParams.psi().selectedFields();
        for (var fieldMember : selectedFields) {
            lastAddedField = findOrCreateField(builderClassParams.builderClass(), fieldMember, lastAddedField);
        }
    }

    private PsiElement findOrCreateField(final PsiClass builderClass, final PsiFieldMember member,
                                         @Nullable final PsiElement last) {
        var psiFactory = generatorParams.psi().factory();
        var field = member.getElement();
        var fieldName = field.getName();
        var fieldType = field.getType();
        var existingField = builderClass.findFieldByName(fieldName, false);
        if (existingField == null || Utils.areTypesPresentableNotEqual(existingField.getType(), fieldType)) {
            if (existingField != null) {
                existingField.delete();
            }
            var newField = psiFactory.createField(fieldName, fieldType);
            if (last != null) {
                return builderClass.addAfter(newField, last);
            } else {
                return builderClass.add(newField);
            }
        }
        return existingField;
    }

}
