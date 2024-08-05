package com.github.junkfactory.innerbuilder.generators;

import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BuilderMethodsGenerator extends AbstractGenerator implements MethodsGenerator {

    private final BuilderClassParams builderClassParams;
    private final FieldsGenerator fieldsGenerator;

    BuilderMethodsGenerator(GeneratorFactory generatorFactory,
                            GeneratorParams generatorParams,
                            BuilderClassParams builderClassParams,
                            FieldsGenerator fieldsGenerator) {
        super(generatorFactory, generatorParams);
        this.builderClassParams = builderClassParams;
        this.fieldsGenerator = fieldsGenerator;
    }

    @Override
    public void run() {
        var builderClass = builderClassParams.builderClass();
        PsiElement lastAddedElement = null;
        for (var field : fieldsGenerator.getFields()) {
            var setterMethod = generateFieldMethod(field);
            field.putCopyableUserData(UserDataKey.METHOD_REF, setterMethod);
            lastAddedElement = addMethod(builderClass, lastAddedElement, setterMethod, false);
        }

        var options = generatorParams.options();
        if (options.contains(JavaInnerBuilderOption.WITH_VALIDATE_METHOD)) {
            var validateMethod = generateValidateMethod();
            addMethod(builderClass, lastAddedElement, validateMethod, false);
        }

        var buildMethod = generateBuildMethod();
        addMethod(builderClass, null, buildMethod, builderClassParams.targetClass().isRecord());
    }

    private PsiMethod generateValidateMethod() {
        var psiElementFactory = generatorParams.psi().factory();
        var voidType = psiElementFactory.createPrimitiveType("void");
        var validateMethod = psiElementFactory.createMethod("validate", voidType);
        PsiUtil.setModifierProperty(validateMethod, PsiModifier.PRIVATE, true);
        return validateMethod;
    }

    private PsiMethod generateFieldMethod(PsiField field) {
        var addMethod = field.hasInitializer() ? findAddMethod(field) : null;
        if (null != addMethod) {
            return generateAddToCollection(field, addMethod);
        }
        return generateBuilderSetter(field);
    }

    private PsiMethod findAddMethod(PsiField field) {
        var fieldClass = PsiUtil.resolveClassInClassTypeOnly(field.getType());
        var methods = Optional.ofNullable(fieldClass)
                .map(PsiClass::getAllMethods)
                .orElseGet(() -> new PsiMethod[0]);
        for (var method : methods) {
            if (method.getName().equals("add") && method.getParameterList().getParametersCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private PsiMethod generateAddToCollection(PsiField field, PsiMethod fieldAddMethod) {
        //resolve the generic type of the collection via the parameter type of the add method
        var param = Objects.requireNonNull(fieldAddMethod.getParameterList().getParameter(0));
        var paramType = PsiUtil.resolveGenericsClassInType(field.getType())
                .getSubstitutor()
                .substitute(param.getType());

        //now build the add method
        var fieldName = "addTo" + StringUtil.capitalize(field.getName());
        var psiElementFactory = generatorParams.psi().factory();
        var addMethod = psiElementFactory.createMethod(fieldName, builderClassParams.builderType());
        PsiUtil.setModifierProperty(addMethod, PsiModifier.PUBLIC, true);

        var addParameter = psiElementFactory.createParameter(param.getName().toLowerCase(), paramType);
        addMethod.getParameterList().add(addParameter);

        var addMethodBody = Objects.requireNonNull(addMethod.getBody());
        var addBody = psiElementFactory.createStatementFromText(String.format(
                "this.%s.add(%s);", field.getName(), param.getName()), addMethod);
        addMethodBody.add(addBody);
        addMethodBody.add(Utils.createReturnThis(psiElementFactory, addMethod));
        return addMethod;
    }

    private PsiMethod generateBuilderSetter(PsiField field) {
        var fieldType = field.getType();
        var fieldName = Utils.hasOneLetterPrefix(field.getName()) ?
                Character.toLowerCase(field.getName().charAt(1)) + field.getName().substring(2) : field.getName();

        var psiElementFactory = generatorParams.psi().factory();
        var setterMethod = psiElementFactory.createMethod(fieldName, builderClassParams.builderType());
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
