package com.github.junkfactory.innerbuilder;

import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

class InnerBuilderGenerator extends AbstractGenerator {

    InnerBuilderGenerator(GeneratorParams generatorParams) {
        super(generatorParams);
    }

    @Override
    public void run() {
        var file = generatorParams.psi().file();
        var targetClass = Utils.getStaticOrTopLevelClass(file, generatorParams.editor());
        if (targetClass == null || BUILDER_CLASS_NAME.equals(targetClass.getName())) {
            return;
        }
        var psiElementFactory = generatorParams.psi().factory();
        var builderClass = findOrCreateBuilderClass(targetClass);
        var builderType = psiElementFactory.createTypeFromText(BUILDER_CLASS_NAME, null);

        if (!targetClass.isRecord()) {
            var constructor = generateConstructor(targetClass, builderType);
            addMethod(targetClass, null, constructor, true);
        }

        var newBuilderMethod = generateStaticBuilderMethod(builderType);
        addMethod(targetClass, null, newBuilderMethod, false);

        // toBuilder method
        var options = generatorParams.options();
        if (options.contains(JavaInnerBuilderOption.WITH_TO_BUILDER_METHOD)) {
            var toBuilderMethod = generateToBuilderMethod(builderType, generatorParams.psi().selectedFields());
            addMethod(targetClass, null, toBuilderMethod, true);
        }

        new BuilderClassGenerator(generatorParams, targetClass, builderClass, builderType).run();

        var project = generatorParams.project();
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(file);
        CodeStyleManager.getInstance(project).reformat(builderClass);
    }

    private PsiMethod generateToBuilderMethod(final PsiType builderType,
                                              final Collection<PsiFieldMember> fields) {
        var psiElementFactory = generatorParams.psi().factory();
        var toBuilderMethod = psiElementFactory.createMethod(TO_BUILDER_NAME, builderType);
        PsiUtil.setModifierProperty(toBuilderMethod, PsiModifier.PUBLIC, true);
        var toBuilderBody = Objects.requireNonNull(toBuilderMethod.getBody());
        var newBuilderStatement = psiElementFactory.createStatementFromText(String.format(
                "%s builder = new %s();", builderType.getPresentableText(),
                builderType.getPresentableText()), toBuilderMethod);
        toBuilderBody.add(newBuilderStatement);
        addCopyBody(fields, toBuilderMethod);
        toBuilderBody.add(psiElementFactory.createStatementFromText("return builder;", toBuilderMethod));
        return toBuilderMethod;
    }

    private void addCopyBody(final Collection<PsiFieldMember> fields, final PsiMethod method) {
        var methodBody = Objects.requireNonNull(method.getBody());
        for (final PsiFieldMember member : fields) {
            var field = member.getElement();
            var assignStatement = generatorParams.psi().factory().createStatementFromText(String.format(
                    "%s%2$s = this.%3$s;", "builder.", field.getName(), field.getName()), method);
            methodBody.add(assignStatement);
        }
    }

    private PsiMethod generateStaticBuilderMethod(final PsiType builderType) {
        var psiElementFactory = generatorParams.psi().factory();
        var newBuilderMethod = psiElementFactory.createMethod(BUILDER_METHOD_NAME, builderType);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.PUBLIC, true);

        var newBuilderMethodBody = Objects.requireNonNull(newBuilderMethod.getBody());
        var newStatement = psiElementFactory.createStatementFromText(String.format(
                "return new %s();", builderType.getPresentableText()), newBuilderMethod);
        newBuilderMethodBody.add(newStatement);
        return newBuilderMethod;
    }

    private PsiMethod generateConstructor(final PsiClass targetClass, final PsiType builderType) {
        var psiElementFactory = generatorParams.psi().factory();
        var constructor = psiElementFactory.createConstructor(Objects.requireNonNull(targetClass.getName()));
        constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);

        var builderParameter = psiElementFactory.createParameter(BUILDER_METHOD_NAME, builderType);
        constructor.getParameterList().add(builderParameter);

        var constructorBody = Objects.requireNonNull(constructor.getBody());
        for (var member : generatorParams.psi().selectedFields()) {
            var field = member.getElement();
            var setterPrototype = PropertyUtilBase.generateSetterPrototype(field);
            var setter = targetClass.findMethodBySignature(setterPrototype, true);
            var assignText = buildAssignText(field, setter);
            var assignStatement = psiElementFactory.createStatementFromText(assignText, null);
            constructorBody.add(assignStatement);
        }

        return constructor;
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
