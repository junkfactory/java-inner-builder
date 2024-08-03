package com.github.junkfactory.innerbuilder;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class JavaInnerBuilderUtils {
    @NonNls
    static final String JAVA_DOT_LANG = "java.lang.";

    private JavaInnerBuilderUtils() {
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

            if (!areTypesPresentableEqual(param1Param.getType(), param2Param.getType())) {
                return false;
            }
        }

        return true;
    }

    static boolean areTypesPresentableEqual(PsiType type1, PsiType type2) {
        if (type1 != null && type2 != null) {
            var type1Canonical = stripJavaLang(type1.getPresentableText());
            var type2Canonical = stripJavaLang(type2.getPresentableText());
            return type1Canonical.equals(type2Canonical);
        }
        return false;
    }

    /**
     * @param file   psi file
     * @param editor editor
     * @return psiClass if class is static or top level. Otherwise returns {@code null}
     */
    @Nullable
    static PsiClass getStaticOrTopLevelClass(PsiFile file, Editor editor) {
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

    static PsiStatement createReturnThis(@NotNull PsiElementFactory psiElementFactory, @Nullable PsiElement context) {
        return psiElementFactory.createStatementFromText("return this;", context);
    }
}
