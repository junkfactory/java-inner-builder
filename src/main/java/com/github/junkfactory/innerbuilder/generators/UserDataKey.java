package com.github.junkfactory.innerbuilder.generators;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiMethod;

final class UserDataKey {

    private UserDataKey() {
    }

    static final Key<PsiMethod> METHOD_REF = Key.create("METHOD_REF");

}
