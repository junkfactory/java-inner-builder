package com.github.junkfactory.innerbuilder;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;

import java.util.List;

record GeneratorParams(Project project,
                       PsiFile file,
                       Editor editor,
                       List<PsiFieldMember> selectedFields,
                       PsiElementFactory psiElementFactory) {
}
