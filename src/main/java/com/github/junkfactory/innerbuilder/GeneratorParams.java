package com.github.junkfactory.innerbuilder;

import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import java.util.Set;

record GeneratorParams(Project project,
                       Editor editor,
                       PsiParams psi,
                       Set<JavaInnerBuilderOption> options) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Project project;
        private Editor editor;
        private PsiParams psi;
        private Set<JavaInnerBuilderOption> options;

        private Builder() {
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public Builder editor(Editor editor) {
            this.editor = editor;
            return this;
        }

        public Builder psi(PsiParams psi) {
            this.psi = psi;
            return this;
        }

        public Builder options(Set<JavaInnerBuilderOption> options) {
            this.options = options;
            return this;
        }

        GeneratorParams build() {
            return new GeneratorParams(project, editor, psi, options);
        }
    }
}
