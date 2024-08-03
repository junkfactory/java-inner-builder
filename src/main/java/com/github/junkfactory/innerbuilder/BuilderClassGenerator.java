package com.github.junkfactory.innerbuilder;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BuilderClassGenerator extends AbstractGenerator {

    private final PsiClass targetClass;
    private final PsiClass builderClass;
    private final PsiType builderType;

    BuilderClassGenerator(GeneratorParams generatorParams,
                          PsiClass targetClass,
                          PsiClass builderClass,
                          PsiType builderType) {
        super(generatorParams);
        this.targetClass = targetClass;
        this.builderClass = builderClass;
        this.builderType = builderType;
    }

    @Override
    public void run() {
        var selectedFields = generatorParams.psi().selectedFields();
        var fieldMembers = new ArrayList<PsiFieldMember>();
        PsiElement lastAddedField = null;
        for (var fieldMember : selectedFields) {
            lastAddedField = findOrCreateField(builderClass, fieldMember, lastAddedField);
            fieldMembers.add(fieldMember);
        }
        //builder constructor
        var builderConstructor = generateBuilderConstructor();
        addMethod(builderClass, null, builderConstructor, false);

        // builder methods
        PsiElement lastAddedElement = null;
        for (var member : fieldMembers) {
            var setterMethod = generateBuilderSetter(builderType, member);
            lastAddedElement = addMethod(builderClass, lastAddedElement, setterMethod, false);
        }

        // builder.build() method
        var buildMethod = generateBuildMethod(targetClass, selectedFields);
        addMethod(builderClass, lastAddedElement, buildMethod, targetClass.isRecord());
    }

    private PsiMethod generateBuilderConstructor() {
        var builderConstructor = generatorParams.psi().factory().createConstructor(BUILDER_CLASS_NAME);
        PsiUtil.setModifierProperty(builderConstructor, PsiModifier.PRIVATE, true);
        return builderConstructor;
    }

    private PsiMethod generateBuilderSetter(final PsiType builderType, final PsiFieldMember member) {

        var field = member.getElement();
        var fieldType = field.getType();
        var fieldName = Utils.hasOneLetterPrefix(field.getName()) ?
                Character.toLowerCase(field.getName().charAt(1)) + field.getName().substring(2) : field.getName();

        var psiElementFactory = generatorParams.psi().factory();
        var setterMethod = psiElementFactory.createMethod(fieldName, builderType);

        setterMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        var setterParameter = psiElementFactory.createParameter(fieldName, fieldType);

        setterMethod.getParameterList().add(setterParameter);
        var setterMethodBody = Objects.requireNonNull(setterMethod.getBody());
        var actualFieldName = "this." + fieldName;
        var assignStatement = psiElementFactory.createStatementFromText(String.format(
                "%s = %s;", actualFieldName, fieldName), setterMethod);
        setterMethodBody.add(assignStatement);
        setterMethodBody.add(Utils.createReturnThis(psiElementFactory, setterMethod));
        return setterMethod;
    }

    private PsiMethod generateBuildMethod(final PsiClass targetClass, final List<PsiFieldMember> selectedFields) {
        var psiElementFactory = generatorParams.psi().factory();
        var targetClassType = psiElementFactory.createType(targetClass);
        var buildMethod = psiElementFactory.createMethod("build", targetClassType);

        var targetModifierList = Objects.requireNonNull(targetClass.getModifierList());
        Stream.of(PsiModifier.PUBLIC, PsiModifier.PACKAGE_LOCAL)
                .filter(targetModifierList::hasModifierProperty)
                .findFirst()
                .ifPresent(modifier -> PsiUtil.setModifierProperty(buildMethod, modifier, true));

        var buildMethodBody = Objects.requireNonNull(buildMethod.getBody());
        final PsiStatement returnStatement;
        if (targetClass.isRecord()) {
            var recordParameters = selectedFields.stream()
                    .map(m -> m.getElement().getName())
                    .collect(Collectors.joining(", "));
            returnStatement = psiElementFactory.createStatementFromText(String.format(
                    "return new %s(%s);", targetClass.getName(), recordParameters), buildMethod);
        } else {
            returnStatement = psiElementFactory.createStatementFromText(String.format(
                    "return new %s(this);", targetClass.getName()), buildMethod);
        }
        buildMethodBody.add(returnStatement);
        return buildMethod;
    }

    private PsiElement findOrCreateField(final PsiClass builderClass, final PsiFieldMember member,
                                         @Nullable final PsiElement last) {
        var field = member.getElement();
        var fieldName = field.getName();
        var fieldType = field.getType();
        var existingField = builderClass.findFieldByName(fieldName, false);
        if (existingField == null ||
                Utils.areTypesPresentableNotEqual(existingField.getType(), fieldType)) {
            if (existingField != null) {
                existingField.delete();
            }
            var newField = generatorParams.psi().factory().createField(fieldName, fieldType);
            if (last != null) {
                return builderClass.addAfter(newField, last);
            } else {
                return builderClass.add(newField);
            }
        }
        return existingField;
    }

}
