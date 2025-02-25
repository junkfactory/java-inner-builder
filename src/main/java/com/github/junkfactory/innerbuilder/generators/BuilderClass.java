package com.github.junkfactory.innerbuilder.generators;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;

public record BuilderClass(PsiClass psiClass,
                           PsiType builderType,
                           BuilderClassName builderClassName,
                           boolean genericType) {
}

