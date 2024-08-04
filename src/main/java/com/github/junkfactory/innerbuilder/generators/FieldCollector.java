package com.github.junkfactory.innerbuilder.generators;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
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

import static com.intellij.openapi.util.text.StringUtil.hasLowerCaseChar;

public class FieldCollector {

    private static final String OBJECT_CLASS_NAME = "Object";

    private FieldCollector() {
    }

    @NotNull
    public static List<PsiFieldMember> collectFields(final PsiFile file, final Editor editor) {
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

        PsiClass classToExtractFieldsFrom = clazz;
        while (classToExtractFieldsFrom != null) {
            var classFieldMembers = collectFieldsInClass(element, clazz, classToExtractFieldsFrom);
            allFields.addAll(0, classFieldMembers);
            classToExtractFieldsFrom = classToExtractFieldsFrom.getSuperClass();
        }

        return allFields;
    }

    private static List<PsiFieldMember> collectFieldsInClass(final PsiElement element,
                                                             final PsiClass accessObjectClass,
                                                             final PsiClass classToExtractFieldsFrom) {
        if (AbstractGenerator.BUILDER_CLASS_NAME.equals(classToExtractFieldsFrom.getName()) ||
                OBJECT_CLASS_NAME.equals(classToExtractFieldsFrom.getName())) {
            return List.of();
        }
        var helper = JavaPsiFacade.getInstance(classToExtractFieldsFrom.getProject()).getResolveHelper();
        return Arrays.stream(classToExtractFieldsFrom.getFields())
                .filter(field -> helper.isAccessible(field, classToExtractFieldsFrom, accessObjectClass) ||
                        hasSetter(classToExtractFieldsFrom, field.getName()))
                .filter(field -> !PsiTreeUtil.isAncestor(field, element, false))
                .filter(field -> !field.hasModifierProperty(PsiModifier.STATIC))
                .filter(field -> hasLowerCaseChar(field.getName()))
                .filter(field -> {
                    if (field.hasModifierProperty(PsiModifier.FINAL)) {
                        if (field.hasInitializer()) {
                            return false;
                        }
                        return accessObjectClass.isEquivalentTo(classToExtractFieldsFrom);
                    }
                    return true;
                })
                .filter(field -> Objects.nonNull(field.getContainingClass()))
                .map(field -> buildFieldMember(field, field.getContainingClass(), classToExtractFieldsFrom))
                .toList();
    }

    private static boolean hasSetter(PsiClass clazz, String name) {
        for (int i = 0; i < clazz.getAllMethods().length; i++) {
            if (clazz.getAllMethods()[i].getName().equals(String.format("set%s", StringUtil.capitalize(name)))) {
                return true;
            }
        }

        return false;
    }

    private static PsiFieldMember buildFieldMember(final PsiField field, final PsiClass containingClass,
                                                   final PsiClass clazz) {
        return new PsiFieldMember(field,
                TypeConversionUtil.getSuperClassSubstitutor(containingClass, clazz, PsiSubstitutor.EMPTY));
    }
}
