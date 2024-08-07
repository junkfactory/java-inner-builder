package com.github.junkfactory.innerbuilder.generators;

import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiUtil;

import java.util.Objects;
import java.util.stream.Collectors;

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
            field.putCopyableUserData(UserDataKey.METHOD_REF, setterMethod.getName());
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
        var addMethod = field.hasInitializer() ? Utils.findAddMethod(field) : null;
        if (null != addMethod) {
            return generateAddToCollection(field, addMethod);
        }

        var putMethod = field.hasInitializer() ? Utils.findPutMethod(field) : null;
        if (null != putMethod) {
            return generatePutToMap(field, putMethod);
        }

        return generateBuilderSetter(field);
    }

    private PsiMethod generatePutToMap(PsiField field, PsiMethod fieldPutMethod) {
        //resolve the generic type of the map via the parameter type of the put method
        var param1 = Objects.requireNonNull(fieldPutMethod.getParameterList().getParameter(0));
        var param1Type = Utils.resolveGenericParameterType(field.getType(), param1);
        var param2 = Objects.requireNonNull(fieldPutMethod.getParameterList().getParameter(1));
        var param2Type = Utils.resolveGenericParameterType(field.getType(), param2);

        //now build the put method
        var methodName = "putTo" + StringUtil.capitalize(field.getName());
        var methodText = """
                public %s %s(%s key, %s value) {
                    this.%s.put(key, value);
                    return this;
                }""".formatted(BUILDER_CLASS_NAME, methodName, param1Type.getPresentableText(),
                param2Type.getPresentableText(), field.getName());
        var psiElementFactory = generatorParams.psi().factory();
        return psiElementFactory.createMethodFromText(methodText, field);
    }

    private PsiMethod generateAddToCollection(PsiField field, PsiMethod fieldAddMethod) {
        //resolve the generic type of the collection via the parameter type of the add method
        var param = Objects.requireNonNull(fieldAddMethod.getParameterList().getParameter(0));
        var paramType = Utils.resolveGenericParameterType(field.getType(), param);

        //now build the add method
        var methodName = "addTo" + StringUtil.capitalize(field.getName());
        var methodText = """
                public %s %s(%s %s) {
                    this.%s.add(%s);
                    return this;
                }""".formatted(BUILDER_CLASS_NAME, methodName, paramType.getPresentableText(),
                param.getName().toLowerCase(), field.getName(), param.getName());
        var psiElementFactory = generatorParams.psi().factory();
        return psiElementFactory.createMethodFromText(methodText, field);
    }

    private PsiMethod generateBuilderSetter(PsiField field) {
        var fieldType = field.getType();
        var fieldName = Utils.hasOneLetterPrefix(field.getName()) ?
                Character.toLowerCase(field.getName().charAt(1)) + field.getName().substring(2) : field.getName();

        var methodText = """
                public %s %s(%s %s) {
                    this.%s = %s;
                    return this;
                }""".formatted(BUILDER_CLASS_NAME, fieldName, fieldType.getPresentableText(),
                fieldName, field.getName(), fieldName);
        var psiElementFactory = generatorParams.psi().factory();
        return psiElementFactory.createMethodFromText(methodText, field);
    }

    private PsiMethod generateBuildMethod() {
        var targetClass = builderClassParams.targetClass();
        var targetModifierList = Objects.requireNonNull(targetClass.getModifierList());
        boolean isPublic = targetModifierList.hasModifierProperty(PsiModifier.PUBLIC);

        var buildMethod = new StringBuilder()
                .append(isPublic ? PsiModifier.PUBLIC : EMPTY)
                .append(isPublic ? SPACE : EMPTY)
                .append(targetClass.getName())
                .append(" build() {");
        if (generatorParams.options().contains(JavaInnerBuilderOption.WITH_VALIDATE_METHOD)) {
            buildMethod.append("validate();");
        }
        if (targetClass.isRecord()) {
            var recordParameters = fieldsGenerator.getFields().stream()
                    .map(PsiField::getName)
                    .collect(Collectors.joining(", "));
            buildMethod.append("return new ")
                    .append(targetClass.getName())
                    .append("(")
                    .append(recordParameters)
                    .append(");");
        } else {
            buildMethod.append("return new ")
                    .append(targetClass.getName())
                    .append("(this);");
        }
        buildMethod.append("}");

        return generatorParams.psi().factory().createMethodFromText(buildMethod.toString(), targetClass);
    }
}
