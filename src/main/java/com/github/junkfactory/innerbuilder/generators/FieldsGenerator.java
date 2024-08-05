package com.github.junkfactory.innerbuilder.generators;

import com.intellij.psi.PsiField;

import java.util.List;

public interface FieldsGenerator extends Runnable {
    List<PsiField> getFields();
}
