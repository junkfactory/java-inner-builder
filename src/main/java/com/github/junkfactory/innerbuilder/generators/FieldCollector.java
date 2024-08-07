package com.github.junkfactory.innerbuilder.generators;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.intellij.openapi.util.text.StringUtil.hasLowerCaseChar;

public class FieldCollector {

    private static final String OBJECT_CLASS_NAME = "Object";

    private final PsiFile file;
    private final Editor editor;

    private FieldCollector(Builder builder) {
        file = builder.file;
        editor = builder.editor;
    }

    public boolean hasFields() {
        return !collectFields(true).isEmpty();
    }

    @NotNull
    public List<PsiFieldMember> collectFields() {
        return collectFields(false);
    }

    @NotNull
    private List<PsiFieldMember> collectFields(boolean checkOnly) {
        var offset = editor.getCaretModel().getOffset();
        var element = file.findElementAt(offset);
        if (element == null) {
            return List.of();
        }

        var clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (clazz == null || clazz.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return List.of();
        }

        var allFields = new ArrayList<PsiFieldMember>();

        var project = Objects.requireNonNull(editor.getProject());
        PsiClass classToExtractFieldsFrom = clazz;
        while (classToExtractFieldsFrom != null) {
            var classFieldMembers = collectFieldsInClass(project, clazz, classToExtractFieldsFrom);
            if (checkOnly) {
                return classFieldMembers.findAny().stream().toList();
            }
            allFields.addAll(0, classFieldMembers.toList());
            classToExtractFieldsFrom = classToExtractFieldsFrom.getSuperClass();
        }

        return allFields;
    }

    private Stream<PsiFieldMember> collectFieldsInClass(Project project, PsiClass accessObjectClass,
                                                        PsiClass classToExtractFieldsFrom) {
        if (AbstractGenerator.BUILDER_CLASS_NAME.equals(classToExtractFieldsFrom.getName()) ||
                OBJECT_CLASS_NAME.equals(classToExtractFieldsFrom.getName())) {
            return Stream.empty();
        }
        var helper = JavaPsiFacade.getInstance(project).getResolveHelper();
        return Arrays.stream(classToExtractFieldsFrom.getFields())
                .filter(field -> helper.isAccessible(field, classToExtractFieldsFrom, accessObjectClass) ||
                        hasSetter(classToExtractFieldsFrom, field.getName()))
                .filter(field -> !field.hasModifierProperty(PsiModifier.STATIC))
                .filter(field -> hasLowerCaseChar(field.getName()))
                .filter(field -> Objects.nonNull(field.getContainingClass()))
                .map(field -> buildFieldMember(field, field.getContainingClass(), classToExtractFieldsFrom));
    }

    private boolean hasSetter(PsiClass clazz, String name) {
        for (int i = 0; i < clazz.getAllMethods().length; i++) {
            if (clazz.getAllMethods()[i].getName().equals(String.format("set%s", StringUtil.capitalize(name)))) {
                return true;
            }
        }

        return false;
    }

    private PsiFieldMember buildFieldMember(final PsiField field, final PsiClass containingClass,
                                            final PsiClass clazz) {
        return new PsiFieldMember(field,
                TypeConversionUtil.getSuperClassSubstitutor(containingClass, clazz, PsiSubstitutor.EMPTY));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PsiFile file;
        private Editor editor;

        private Builder() {
        }

        public Builder file(PsiFile file) {
            this.file = file;
            return this;
        }

        public Builder editor(Editor editor) {
            this.editor = editor;
            return this;
        }

        public FieldCollector build() {
            return new FieldCollector(this);
        }
    }
}
