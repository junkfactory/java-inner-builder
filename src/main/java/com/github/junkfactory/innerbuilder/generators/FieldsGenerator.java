package com.github.junkfactory.innerbuilder.generators;

import com.intellij.codeInsight.generation.PsiFieldMember;

import java.util.List;

public interface FieldsGenerator extends Runnable {
    List<PsiFieldMember> getRefactoredFields();
}
