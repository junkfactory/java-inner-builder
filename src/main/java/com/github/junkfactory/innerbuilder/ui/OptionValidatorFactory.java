package com.github.junkfactory.innerbuilder.ui;

import com.intellij.openapi.Disposable;
import com.intellij.psi.PsiManager;

import javax.swing.JComponent;

@FunctionalInterface
interface OptionValidatorFactory {
    void create(PsiManager psiManager, Disposable disposable, JComponent optionComponent);
}
