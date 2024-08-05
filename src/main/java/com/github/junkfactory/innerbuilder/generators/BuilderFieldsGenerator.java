package com.github.junkfactory.innerbuilder.generators;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

class BuilderFieldsGenerator extends AbstractGenerator implements FieldsGenerator {

    private final BuilderClassParams builderClassParams;
    private final List<PsiFieldMember> refactoredFields = new LinkedList<>();

    BuilderFieldsGenerator(GeneratorFactory generatorFactory,
                           GeneratorParams generatorParams,
                           BuilderClassParams builderClassParams) {
        super(generatorFactory, generatorParams);
        this.builderClassParams = builderClassParams;
    }

    @Override
    public List<PsiFieldMember> getRefactoredFields() {
        return refactoredFields;
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
            if (null != existingField) {
                refactoredFields.add(member);
                existingField.delete();
            }
            var newField = psiFactory.createField(fieldName, fieldType);
            newField.setInitializer(field.getInitializer());
            if (!builderClassParams.targetClass().isRecord()) {
                field.setInitializer(null);
            }
            return addField(builderClass, newField, last);
        }
        return existingField;
    }

    private PsiElement addField(PsiClass builderClass, PsiField newField, PsiElement last) {
        if (last != null) {
            return builderClass.addAfter(newField, last);
        } else {
            return builderClass.add(newField);
        }
    }

}
