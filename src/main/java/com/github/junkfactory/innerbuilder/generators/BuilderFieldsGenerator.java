package com.github.junkfactory.innerbuilder.generators;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

class BuilderFieldsGenerator extends AbstractGenerator {

    private final BuilderClassParams builderClassParams;

    BuilderFieldsGenerator(GeneratorFactory generatorFactory,
                           GeneratorParams generatorParams,
                           BuilderClassParams builderClassParams) {
        super(generatorFactory, generatorParams);
        this.builderClassParams = builderClassParams;
    }

    @Override
    public void run() {
        PsiElement lastAddedField = null;
        for (var fieldMember : generatorParams.psi().selectedFields()) {
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
