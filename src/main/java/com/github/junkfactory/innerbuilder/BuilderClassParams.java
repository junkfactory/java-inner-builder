package com.github.junkfactory.innerbuilder;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;

public record BuilderClassParams(PsiClass targetClass, PsiClass builderClass, PsiType builderType) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PsiClass targetClass;
        private PsiClass builderClass;
        private PsiType builderType;

        private Builder() {
        }

        public Builder targetClass(PsiClass targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder builderClass(PsiClass builderClass) {
            this.builderClass = builderClass;
            return this;
        }

        public Builder builderType(PsiType builderType) {
            this.builderType = builderType;
            return this;
        }

        public BuilderClassParams build() {
            return new BuilderClassParams(targetClass, builderClass, builderType);
        }
    }
}
