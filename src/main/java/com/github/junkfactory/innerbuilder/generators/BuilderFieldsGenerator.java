package com.github.junkfactory.innerbuilder.generators;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

class BuilderFieldsGenerator extends AbstractGenerator implements FieldsGenerator {

    private final BuilderClassParams builderClassParams;
    private final List<PsiField> fields = new LinkedList<>();

    BuilderFieldsGenerator(GeneratorFactory generatorFactory,
                           GeneratorParams generatorParams,
                           BuilderClassParams builderClassParams) {
        super(generatorFactory, generatorParams);
        this.builderClassParams = builderClassParams;
    }

    @Override
    public List<PsiField> getFields() {
        return fields;
    }

    @Override
    public void run() {
        PsiField lastAddedField = null;
        for (var fieldMember : generatorParams.psi().selectedFields()) {
            lastAddedField = createOrUpdateField(builderClassParams.builderClass(), fieldMember, lastAddedField);
            fields.add(lastAddedField);
        }
        cleanupFields(builderClassParams.builderClass());
    }

    private void cleanupFields(PsiClass builderClass) {
        for (var field : builderClass.getFields()) {
            if (!fields.contains(field)) {
                deleteFieldAndMethodIfExists(builderClass, field);
            }
        }
    }

    private PsiField createOrUpdateField(final PsiClass builderClass, final PsiFieldMember member,
                                         @Nullable final PsiElement last) {
        var psiFactory = generatorParams.psi().factory();
        var field = member.getElement();
        var fieldName = field.getName();
        var fieldType = field.getType();
        var existingField = builderClass.findFieldByName(fieldName, false);
        if (existingField == null || Utils.areTypesPresentableNotEqual(existingField.getType(), fieldType)) {
            deleteFieldAndMethodIfExists(builderClass, existingField);
            var newField = psiFactory.createField(fieldName, fieldType);
            newField.setInitializer(field.getInitializer());
            if (!builderClassParams.targetClass().isRecord()) {
                field.setInitializer(null);
            }
            existingField = (PsiField) addElement(builderClass, newField, last);
        }
        return existingField;
    }

    private void deleteFieldAndMethodIfExists(PsiClass builderClass, PsiField field) {
        if (null == field) {
            return;
        }
        Optional.ofNullable(field.getCopyableUserData(UserDataKey.METHOD_REF))
                .map(m -> builderClass.findMethodBySignature(m, false))
                .ifPresent(PsiElement::delete);
        field.delete();
    }

}
