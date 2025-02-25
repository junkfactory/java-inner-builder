package com.github.junkfactory.innerbuilder.generators;

import com.github.junkfactory.innerbuilder.generators.BuilderMethodsGenerator.BuilderClassName;
import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

import static com.github.junkfactory.innerbuilder.generators.GenerationResult.Code.ANNOTATIONS_ADDED;
import static com.github.junkfactory.innerbuilder.generators.GenerationResult.Code.IMPORTS_ADDED;
import static com.github.junkfactory.innerbuilder.generators.GenerationResult.NO_RESULT;

class InnerBuilderGenerator extends AbstractGenerator implements Generator {

    public record BuilderClass(PsiClass psiClass,
                               PsiType builderType,
                               BuilderClassName builderClassName,
                               boolean genericType) {
    }

    InnerBuilderGenerator(GeneratorFactory generatorFactory, GeneratorParams generatorParams) {
        super(generatorFactory, generatorParams);
    }

    @Override
    public GenerationResult generate() {
        var file = generatorParams.psi().file();
        var targetClass = Utils.getStaticOrTopLevelClass(file, generatorParams.editor());
        if (targetClass == null || BUILDER_CLASS_NAME.equals(targetClass.getName())) {
            return NO_RESULT;
        }
        var builderClass = findOrCreateBuilderClass(targetClass);

        if (!targetClass.isRecord()) {
            var constructor = generateTargetConstructor(targetClass, builderClass);
            addMethod(targetClass, null, constructor, true);
        }

        var newBuilderMethod = generateStaticBuilderMethod(targetClass, builderClass);
        addMethod(targetClass, null, newBuilderMethod, false);

        // toBuilder method
        var options = generatorParams.options();
        if (options.contains(JavaInnerBuilderOption.WITH_TO_BUILDER_METHOD)) {
            var toBuilderMethod = generateToBuilderMethod(targetClass, builderClass,
                    generatorParams.psi().selectedFields());
            addMethod(targetClass, null, toBuilderMethod, true);
        }

        var params = BuilderClassParams.builder()
                .targetClass(targetClass)
                .builderClass(builderClass)
                .build();
        var result = generatorFactory.createBuilderClassGenerator(generatorParams, params).generate();
        generationResult.merge(result);
        var codeStyleManager = generatorParams.psi().codeStyleManager();
        generationResult.when(ANNOTATIONS_ADDED, () -> codeStyleManager.shortenClassReferences(targetClass));
        generationResult.when(IMPORTS_ADDED, () -> codeStyleManager.removeRedundantImports((PsiJavaFile) file));
        CodeStyleManager.getInstance(generatorParams.project()).reformat(builderClass.psiClass());
        return generationResult;
    }

    private PsiMethod generateToBuilderMethod(PsiClass targetClass,
                                              BuilderClass builderClass,
                                              Collection<PsiFieldMember> fields) {
        var targetModifierList = Objects.requireNonNull(targetClass.getModifierList());
        boolean isPublic = targetModifierList.hasModifierProperty(PsiModifier.PUBLIC);
        var toBuilderMethod = new StringBuilder()
                .append(isPublic ? PsiModifier.PUBLIC : EMPTY)
                .append(isPublic ? SPACE : EMPTY)
                .append(builderClass.builderType().getPresentableText())
                .append(SPACE)
                .append(TO_BUILDER_NAME)
                .append("() {")
                .append("var builder = new ")
                .append(builderClass.builderType().getPresentableText())
                .append("();");
        for (var member : fields) {
            var field = member.getElement();
            toBuilderMethod
                    .append("builder.")
                    .append(field.getName())
                    .append(" = ")
                    .append(field.getName())
                    .append(';')
                    .append(System.lineSeparator());
        }
        toBuilderMethod.append(" return builder; }");
        var psiElementFactory = generatorParams.psi().factory();
        return psiElementFactory.createMethodFromText(toBuilderMethod.toString(), targetClass);
    }

    private PsiMethod generateStaticBuilderMethod(PsiClass targetClass, BuilderClass builderClass) {
        var psiElementFactory = generatorParams.psi().factory();
        var methodName = Utils.buildBuilderMethodName(builderClass.builderType());
        var newBuilderMethod = psiElementFactory.createMethodFromText(methodName, targetClass);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.PUBLIC, true);

        var existingMethod = targetClass.findMethodBySignature(newBuilderMethod, true);
        if (existingMethod == null) {
            existingMethod = newBuilderMethod;
            var newBuilderMethodBody = Objects.requireNonNull(existingMethod.getBody());
            var newStatement = psiElementFactory.createStatementFromText(String.format(
                    "return new %s();", builderClass.builderClassName().instanceClassName()), newBuilderMethod);
            newBuilderMethodBody.add(newStatement);
        }
        return existingMethod;
    }

    private PsiMethod generateTargetConstructor(final PsiClass targetClass, BuilderClass builderClass) {
        var constructor = new StringBuilder()
                .append("private ")
                .append(targetClass.getName())
                .append("(")
                .append(builderClass.builderType().getPresentableText())
                .append(" builder) {");

        for (var member : generatorParams.psi().selectedFields()) {
            var field = member.getElement();
            var setterPrototype = PropertyUtilBase.generateSetterPrototype(field);
            var setter = targetClass.findMethodBySignature(setterPrototype, true);
            var assignText = buildAssignText(field, setter);
            constructor.append(assignText).append(System.lineSeparator());
        }
        constructor.append("}");
        return generatorParams.psi().factory().createMethodFromText(constructor.toString(), targetClass);
    }

    private static @NotNull String buildAssignText(PsiField field, PsiMethod setter) {
        var fieldName = field.getName();
        boolean isFinal = false;
        var modifierList = field.getModifierList();
        if (modifierList != null) {
            isFinal = modifierList.hasModifierProperty(PsiModifier.FINAL);
        }

        final String assignText;
        if (setter == null || isFinal) {
            assignText = String.format("%1$s = builder.%1$s;", fieldName);
        } else {
            assignText = String.format("%s(builder.%s);", setter.getName(), fieldName);
        }
        return assignText;
    }

    @NotNull
    private BuilderClass findOrCreateBuilderClass(final PsiClass targetClass) {
        var builderClassName = Utils.buildClassName(BUILDER_CLASS_NAME, targetClass);
        var psiClass = targetClass.findInnerClassByName(BUILDER_CLASS_NAME, false);
        if (psiClass == null) {
            psiClass = (PsiClass) targetClass.add(createBuilderClass(targetClass, builderClassName.className()));
        }

        var psiElementFactory = generatorParams.psi().factory();
        var builderType = psiElementFactory.createTypeFromText(builderClassName.className(), targetClass);
        return new BuilderClass(psiClass, builderType, builderClassName, Utils.isGenericType(builderType));
    }

    @NotNull
    private PsiClass createBuilderClass(final PsiClass targetClass, String builderClassName) {
        var classDef = "public static final class " +
                builderClassName +
                " {}" +
                System.lineSeparator();
        return generatorParams.psi().factory().createClassFromText(classDef, targetClass)
                .getInnerClasses()[0];
    }

}
