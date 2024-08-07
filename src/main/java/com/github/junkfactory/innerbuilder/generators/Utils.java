package com.github.junkfactory.innerbuilder.generators;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Utils {
    @NonNls
    static final String JAVA_DOT_LANG = "java.lang.";

    private Utils() {
    }

    static boolean hasOneLetterPrefix(String str) {
        if (str.length() == 1) {
            return false;
        }
        return Character.isLowerCase(str.charAt(0)) && Character.isUpperCase(str.charAt(1));
    }

    static String stripJavaLang(String typeString) {
        return typeString.startsWith(JAVA_DOT_LANG) ? typeString.substring(JAVA_DOT_LANG.length()) : typeString;
    }

    static boolean areParameterListsEqual(PsiParameterList paramList1, PsiParameterList paramList2) {
        if (paramList1.getParametersCount() != paramList2.getParametersCount()) {
            return false;
        }

        var param1Params = paramList1.getParameters();
        var param2Params = paramList2.getParameters();
        for (int i = 0; i < param1Params.length; i++) {
            var param1Param = param1Params[i];
            var param2Param = param2Params[i];

            if (areTypesPresentableNotEqual(param1Param.getType(), param2Param.getType())) {
                return false;
            }
        }

        return true;
    }

    static boolean areTypesPresentableNotEqual(PsiType type1, PsiType type2) {
        if (type1 != null && type2 != null) {
            var type1Canonical = stripJavaLang(type1.getPresentableText());
            var type2Canonical = stripJavaLang(type2.getPresentableText());
            return !type1Canonical.equals(type2Canonical);
        }
        return true;
    }

    /**
     * @param file   psi file
     * @param editor editor
     * @return psiClass if class is static or top level. Otherwise returns {@code null}
     */
    @Nullable
    public static PsiClass getStaticOrTopLevelClass(PsiFile file, Editor editor) {
        var offset = editor.getCaretModel().getOffset();
        var element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }

        var topLevelClass = PsiUtil.getTopLevelClass(element);
        var psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass != null && (psiClass.hasModifierProperty(PsiModifier.STATIC) ||
                psiClass.getManager().areElementsEquivalent(psiClass, topLevelClass))) {
            return psiClass;
        } else {
            return null;
        }
    }


    @Nullable
    public static PsiMethod findAddMethod(PsiField field) {
        return findMethod(field, "add", 1);
    }

    @Nullable
    public static PsiMethod findPutMethod(PsiField field) {
        return findMethod(field, "put", 2);
    }

    @Nullable
    public static PsiMethod findMethod(PsiField field, String methodName, int parameterCount) {
        if (isFieldInitializedWithImmutableInstance(field)) {
            return null;
        }
        var fieldClass = PsiUtil.resolveClassInClassTypeOnly(field.getType());
        var methods = Optional.ofNullable(fieldClass)
                .map(PsiClass::getAllMethods)
                .orElseGet(() -> new PsiMethod[0]);
        for (var method : methods) {
            if (method.getName().equals(methodName) &&
                    method.getParameterList().getParametersCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    public static PsiType resolveGenericParameterType(PsiType fieldType, PsiParameter param) {
        //resolve the generic type of the collection via the parameter type of the add method
        return PsiUtil.resolveGenericsClassInType(fieldType)
                .getSubstitutor()
                .substitute(param.getType());
    }

    public static boolean isFieldInitializedWithImmutableInstance(PsiField field) {
        var initializer = field.getInitializer();
        if (null == initializer) {
            return false;
        }
        var initializerType = initializer.getType();
        if (null == initializerType) {
            return false;
        }
        var initializerClass = PsiUtil.resolveClassInClassTypeOnly(initializerType);
        return null != initializerClass && initializerClass.hasModifierProperty(PsiModifier.ABSTRACT);
    }


}
