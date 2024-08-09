package com.github.junkfactory.innerbuilder.generators;

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
        var psiElementFactory = generatorParams.psi().factory();
        var builderClass = findOrCreateBuilderClass(targetClass);
        var builderType = psiElementFactory.createTypeFromText(BUILDER_CLASS_NAME, targetClass);

        if (!targetClass.isRecord()) {
            var constructor = generateTargetConstructor(targetClass, builderType);
            addMethod(targetClass, null, constructor, true);
        }

        var newBuilderMethod = generateStaticBuilderMethod(targetClass, builderType);
        addMethod(targetClass, null, newBuilderMethod, false);

        // toBuilder method
        var options = generatorParams.options();
        if (options.contains(JavaInnerBuilderOption.WITH_TO_BUILDER_METHOD)) {
            var toBuilderMethod = generateToBuilderMethod(targetClass, builderType,
                    generatorParams.psi().selectedFields());
            addMethod(targetClass, null, toBuilderMethod, true);
        }

        var params = BuilderClassParams.builder()
                .targetClass(targetClass)
                .builderClass(builderClass)
                .builderType(builderType)
                .build();
        var result = generatorFactory.createBuilderClassGenerator(generatorParams, params).generate();

        var codeStyleManager = generatorParams.psi().codeStyleManager();
        result.when(ANNOTATIONS_ADDED, () -> codeStyleManager.shortenClassReferences(file));
        result.when(IMPORTS_ADDED, () -> codeStyleManager.removeRedundantImports((PsiJavaFile) file));
        CodeStyleManager.getInstance(generatorParams.project()).reformat(builderClass);
        return result;
    }

    private PsiMethod generateToBuilderMethod(PsiClass targetClass,
                                              PsiType builderType,
                                              Collection<PsiFieldMember> fields) {
        var targetModifierList = Objects.requireNonNull(targetClass.getModifierList());
        boolean isPublic = targetModifierList.hasModifierProperty(PsiModifier.PUBLIC);
        var toBuilderMethod = new StringBuilder()
                .append(isPublic ? PsiModifier.PUBLIC : EMPTY)
                .append(isPublic ? SPACE : EMPTY)
                .append(builderType.getPresentableText())
                .append(SPACE)
                .append(TO_BUILDER_NAME)
                .append("() {")
                .append("var builder = new ")
                .append(builderType.getPresentableText())
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

    private PsiMethod generateStaticBuilderMethod(PsiClass targetClass, PsiType builderType) {
        var psiElementFactory = generatorParams.psi().factory();
        var newBuilderMethod = psiElementFactory.createMethod(BUILDER_METHOD_NAME, builderType);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.PUBLIC, true);

        var existingMethod = targetClass.findMethodBySignature(newBuilderMethod, true);
        if (existingMethod == null) {
            existingMethod = newBuilderMethod;
            var newBuilderMethodBody = Objects.requireNonNull(existingMethod.getBody());
            var newStatement = psiElementFactory.createStatementFromText(String.format(
                    "return new %s();", builderType.getPresentableText()), newBuilderMethod);
            newBuilderMethodBody.add(newStatement);
        }
        return existingMethod;
    }

    private PsiMethod generateTargetConstructor(final PsiClass targetClass, final PsiType builderType) {
        var constructor = new StringBuilder()
                .append("private ")
                .append(targetClass.getName())
                .append("(")
                .append(builderType.getPresentableText())
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
    private PsiClass findOrCreateBuilderClass(final PsiClass targetClass) {
        var builderClass = targetClass.findInnerClassByName(BUILDER_CLASS_NAME, false);
        if (builderClass == null) {
            return createBuilderClass(targetClass);
        }

        return builderClass;
    }

    @NotNull
    private PsiClass createBuilderClass(final PsiClass targetClass) {
        var builderClass = (PsiClass) targetClass.add(generatorParams.psi().factory()
                .createClass(BUILDER_CLASS_NAME));
        PsiUtil.setModifierProperty(builderClass, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(builderClass, PsiModifier.FINAL, true);
        return builderClass;
    }

}
