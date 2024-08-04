package com.github.junkfactory.innerbuilder;

import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BuilderMethodsGenerator extends AbstractGenerator {

    private final BuilderClassParams builderClassParams;

    public BuilderMethodsGenerator(GeneratorParams generatorParams, BuilderClassParams builderClassParams) {
        super(generatorParams);
        this.builderClassParams = builderClassParams;
    }

    @Override
    public void run() {
        var builderClass = builderClassParams.builderClass();
        PsiElement lastAddedElement = null;
        for (var member : generatorParams.psi().selectedFields()) {
            var setterMethod = generateBuilderSetter(builderClassParams.builderType(), member);
            lastAddedElement = addMethod(builderClass, lastAddedElement, setterMethod, false);
        }

        //build validate method
        var options = generatorParams.options();
        if (options.contains(JavaInnerBuilderOption.WITH_VALIDATE_METHOD)) {
            var validateMethod = generateValidateMethod();
            addMethod(builderClass, lastAddedElement, validateMethod, false);
        }

        // builder.build() method
        var buildMethod = generateBuildMethod();
        addMethod(builderClass, null, buildMethod, builderClassParams.builderClass().isRecord());
    }

    private PsiMethod generateValidateMethod() {
        var psiElementFactory = generatorParams.psi().factory();
        var voidType = psiElementFactory.createPrimitiveType("void");
        var validateMethod = psiElementFactory.createMethod("validate", voidType);
        PsiUtil.setModifierProperty(validateMethod, PsiModifier.PRIVATE, true);
        return validateMethod;
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

    private PsiMethod generateBuildMethod() {
        var targetClass = builderClassParams.targetClass();
        var psiElementFactory = generatorParams.psi().factory();
        var targetClassType = psiElementFactory.createType(targetClass);
        var buildMethod = psiElementFactory.createMethod("build", targetClassType);

        var targetModifierList = Objects.requireNonNull(targetClass.getModifierList());
        Stream.of(PsiModifier.PUBLIC, PsiModifier.PACKAGE_LOCAL)
                .filter(targetModifierList::hasModifierProperty)
                .findFirst()
                .ifPresent(modifier -> PsiUtil.setModifierProperty(buildMethod, modifier, true));

        var buildMethodBody = Objects.requireNonNull(buildMethod.getBody());
        if (generatorParams.options().contains(JavaInnerBuilderOption.WITH_VALIDATE_METHOD)) {
            var validateCall = psiElementFactory.createStatementFromText("validate();", buildMethod);
            buildMethodBody.add(validateCall);
        }

        final PsiStatement returnStatement;
        if (targetClass.isRecord()) {
            var recordParameters = generatorParams.psi().selectedFields().stream()
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
}
