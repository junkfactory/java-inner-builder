package com.github.junkfactory.innerbuilder.generators;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import java.util.List;

public record PsiParams(PsiFile file,
                        List<PsiFieldMember> selectedFields,
                        PsiElementFactory factory,
                        JavaCodeStyleManager codeStyleManager) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PsiFile file;
        private List<PsiFieldMember> selectedFields;
        private PsiElementFactory factory;
        private JavaCodeStyleManager codeStyleManager;

        private Builder() {
        }

        public Builder file(PsiFile file) {
            this.file = file;
            return this;
        }

        public Builder selectedFields(List<PsiFieldMember> selectedFields) {
            this.selectedFields = selectedFields;
            return this;
        }

        public Builder factory(PsiElementFactory factory) {
            this.factory = factory;
            return this;
        }

        public Builder codeStyleManager(JavaCodeStyleManager codeStyleManager) {
            this.codeStyleManager = codeStyleManager;
            return this;
        }

        public PsiParams build() {
            return new PsiParams(file, selectedFields, factory, codeStyleManager);
        }
    }
}
