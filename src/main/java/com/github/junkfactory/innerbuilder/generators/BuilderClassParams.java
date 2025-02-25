package com.github.junkfactory.innerbuilder.generators;

import com.github.junkfactory.innerbuilder.generators.InnerBuilderGenerator.BuilderClass;
import com.intellij.psi.PsiClass;

public record BuilderClassParams(PsiClass targetClass,
                                 BuilderClass builderClass) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PsiClass targetClass;
        private BuilderClass builderClass;

        private Builder() {
        }

        public Builder targetClass(PsiClass targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder builderClass(BuilderClass builderClass) {
            this.builderClass = builderClass;
            return this;
        }

        public BuilderClassParams build() {
            return new BuilderClassParams(targetClass, builderClass);
        }
    }
}
