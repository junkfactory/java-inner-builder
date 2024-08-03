package com.github.junkfactory.innerbuilder;

import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaInnerBuilderGenerator implements Runnable {
    @NonNls
    private static final String BUILDER_CLASS_NAME = "Builder";
    @NonNls
    private static final String BUILDER_METHOD_NAME = "builder";
    @NonNls
    private static final String TO_BUILDER_NAME = "toBuilder";

    private final Project project;
    private final PsiFile file;
    private final Editor editor;
    private final List<PsiFieldMember> selectedFields;
    private final PsiElementFactory psiElementFactory;

    public static void generate(final Project project, final Editor editor, final PsiFile file,
                                final List<PsiFieldMember> selectedFields) {
        final Runnable builderGenerator = new JavaInnerBuilderGenerator(project, file, editor, selectedFields);
        ApplicationManager.getApplication().runWriteAction(builderGenerator);
    }

    private JavaInnerBuilderGenerator(final Project project, final PsiFile file, final Editor editor,
                                      final List<PsiFieldMember> selectedFields) {
        this.project = project;
        this.file = file;
        this.editor = editor;
        this.selectedFields = selectedFields;
        psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @Override
    public void run() {
        var targetClass = JavaInnerBuilderUtils.getStaticOrTopLevelClass(file, editor);
        if (targetClass == null) {
            return;
        }
        var options = currentOptions();
        var builderClass = findOrCreateBuilderClass(targetClass);
        var builderType = psiElementFactory.createTypeFromText(BUILDER_CLASS_NAME, null);

        if (!targetClass.isRecord()) {
            var constructor = generateConstructor(targetClass, builderType);
            addMethod(targetClass, null, constructor, true);
        }

        var fieldMembers = new ArrayList<PsiFieldMember>();

        PsiElement lastAddedField = null;
        for (var fieldMember : selectedFields) {
            lastAddedField = findOrCreateField(builderClass, fieldMember, lastAddedField);
            fieldMembers.add(fieldMember);
        }

        var newBuilderMethod = generateStaticBuilderMethod(builderType);
        addMethod(targetClass, null, newBuilderMethod, false);

        // toBuilder method
        if (options.contains(JavaInnerBuilderOption.TO_BUILDER)) {
            var toBuilderMethod = generateToBuilderMethod(builderType, selectedFields);
            addMethod(targetClass, null, toBuilderMethod, true);
        }

        // builder methods
        PsiElement lastAddedElement = null;
        for (var member : fieldMembers) {
            var setterMethod = generateBuilderSetter(builderType, member);
            lastAddedElement = addMethod(builderClass, lastAddedElement, setterMethod, false);
        }

        // builder.build() method
        var buildMethod = generateBuildMethod(targetClass, selectedFields);
        addMethod(builderClass, lastAddedElement, buildMethod, targetClass.isRecord());

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(file);
        CodeStyleManager.getInstance(project).reformat(builderClass);
    }

    private PsiMethod generateToBuilderMethod(final PsiType builderType,
                                              final Collection<PsiFieldMember> fields) {

        var toBuilderMethod = psiElementFactory.createMethod(TO_BUILDER_NAME, builderType);
        PsiUtil.setModifierProperty(toBuilderMethod, PsiModifier.PUBLIC, true);
        var toBuilderBody = Objects.requireNonNull(toBuilderMethod.getBody());
        var newBuilderStatement = psiElementFactory.createStatementFromText(String.format(
                        "%s builder = new %s();", builderType.getPresentableText(),
                        builderType.getPresentableText()),
                toBuilderMethod);
        toBuilderBody.add(newBuilderStatement);

        addCopyBody(fields, toBuilderMethod);
        toBuilderBody.add(psiElementFactory.createStatementFromText("return builder;", toBuilderMethod));
        return toBuilderMethod;
    }

    private void addCopyBody(final Collection<PsiFieldMember> fields, final PsiMethod method) {
        var methodBody = Objects.requireNonNull(method.getBody());
        for (final PsiFieldMember member : fields) {
            var field = member.getElement();
            var assignStatement = psiElementFactory.createStatementFromText(String.format(
                    "%s%2$s = this.%3$s;", "builder.", field.getName(), field.getName()), method);
            methodBody.add(assignStatement);
        }
    }

    private PsiMethod generateStaticBuilderMethod(final PsiType builderType) {
        var newBuilderMethod = psiElementFactory.createMethod(BUILDER_METHOD_NAME, builderType);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.PUBLIC, true);

        var newBuilderMethodBody = Objects.requireNonNull(newBuilderMethod.getBody());
        var newStatement = psiElementFactory.createStatementFromText(String.format(
                "return new %s();", builderType.getPresentableText()), newBuilderMethod);
        newBuilderMethodBody.add(newStatement);
        return newBuilderMethod;
    }

    private PsiMethod generateBuilderSetter(final PsiType builderType, final PsiFieldMember member) {

        var field = member.getElement();
        var fieldType = field.getType();
        var fieldName = JavaInnerBuilderUtils.hasOneLetterPrefix(field.getName()) ?
                Character.toLowerCase(field.getName().charAt(1)) + field.getName().substring(2) : field.getName();

        var setterMethod = psiElementFactory.createMethod(fieldName, builderType);

        setterMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        var setterParameter = psiElementFactory.createParameter(fieldName, fieldType);

        setterMethod.getParameterList().add(setterParameter);
        var setterMethodBody = Objects.requireNonNull(setterMethod.getBody());
        var actualFieldName = "this." + fieldName;
        var assignStatement = psiElementFactory.createStatementFromText(String.format(
                "%s = %s;", actualFieldName, fieldName), setterMethod);
        setterMethodBody.add(assignStatement);
        setterMethodBody.add(JavaInnerBuilderUtils.createReturnThis(psiElementFactory, setterMethod));
        return setterMethod;
    }


    private PsiMethod generateConstructor(final PsiClass targetClass, final PsiType builderType) {
        var constructor = psiElementFactory.createConstructor(Objects.requireNonNull(targetClass.getName()));
        constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);

        var builderParameter = psiElementFactory.createParameter(BUILDER_METHOD_NAME, builderType);
        constructor.getParameterList().add(builderParameter);

        var constructorBody = Objects.requireNonNull(constructor.getBody());
        for (var member : selectedFields) {
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

    private PsiMethod generateBuildMethod(final PsiClass targetClass, final List<PsiFieldMember> selectedFields) {
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
        var builderClass = (PsiClass) targetClass.add(psiElementFactory.createClass(BUILDER_CLASS_NAME));
        PsiUtil.setModifierProperty(builderClass, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(builderClass, PsiModifier.FINAL, true);
        return builderClass;
    }

    private PsiElement findOrCreateField(final PsiClass builderClass, final PsiFieldMember member,
                                         @Nullable final PsiElement last) {
        var field = member.getElement();
        var fieldName = field.getName();
        var fieldType = field.getType();
        var existingField = builderClass.findFieldByName(fieldName, false);
        if (existingField == null ||
                !JavaInnerBuilderUtils.areTypesPresentableEqual(existingField.getType(), fieldType)) {
            if (existingField != null) {
                existingField.delete();
            }
            var newField = psiElementFactory.createField(fieldName, fieldType);
            if (last != null) {
                return builderClass.addAfter(newField, last);
            } else {
                return builderClass.add(newField);
            }
        }
        return existingField;
    }

    private PsiElement addMethod(@NotNull final PsiClass target, @Nullable final PsiElement after,
                                 @NotNull final PsiMethod newMethod, final boolean replace) {
        var existingMethod = target.findMethodBySignature(newMethod, false);
        if (existingMethod == null && newMethod.isConstructor()) {
            for (final PsiMethod constructor : target.getConstructors()) {
                if (JavaInnerBuilderUtils.areParameterListsEqual(constructor.getParameterList(),
                        newMethod.getParameterList())) {
                    existingMethod = constructor;
                    break;
                }
            }
        }
        if (existingMethod == null) {
            if (after != null) {
                return target.addAfter(newMethod, after);
            } else {
                return target.add(newMethod);
            }
        } else if (replace) {
            existingMethod.replace(newMethod);
        }
        return existingMethod;
    }

    private static EnumSet<JavaInnerBuilderOption> currentOptions() {
        final var options = EnumSet.noneOf(JavaInnerBuilderOption.class);
        final var propertiesComponent = PropertiesComponent.getInstance();
        for (var option : JavaInnerBuilderOption.values()) {

            if (Boolean.TRUE.equals(option.isBooleanProperty())) {
                final boolean currentSetting = propertiesComponent.getBoolean(option.getProperty(), false);
                if (currentSetting) {
                    options.add(option);
                }
            } else {
                String currentValue = String.valueOf(propertiesComponent.getValue(option.getProperty()));
                if (currentValue != null) {
                    JavaInnerBuilderOption.findValue(currentValue).ifPresent(options::add);
                }
            }
        }
        return options;
    }

}
