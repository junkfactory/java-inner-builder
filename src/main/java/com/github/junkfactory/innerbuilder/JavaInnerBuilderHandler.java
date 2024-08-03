package com.github.junkfactory.innerbuilder;

import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

import static com.github.junkfactory.innerbuilder.FieldCollector.collectFields;
import static com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOptionSelector.selectFieldsAndOptions;

public class JavaInnerBuilderHandler implements LanguageCodeInsightActionHandler {
    @Override
    public boolean isValidFor(final Editor editor, final PsiFile file) {
        if (!(file instanceof PsiJavaFile)) {
            return false;
        }

        var project = editor.getProject();
        if (project == null) {
            return false;
        }

        return Utils.getStaticOrTopLevelClass(file, editor) != null && isApplicable(file, editor);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    private static boolean isApplicable(final PsiFile file, final Editor editor) {
        var targetElements = collectFields(file, editor);
        return !targetElements.isEmpty();
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
        var psiDocumentManager = PsiDocumentManager.getInstance(project);
        var currentDocument = psiDocumentManager.getDocument(file);
        if (currentDocument == null) {
            return;
        }

        psiDocumentManager.commitDocument(currentDocument);

        if (!EditorModificationUtil.checkModificationAllowed(editor)) {
            return;
        }

        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
            return;
        }

        var existingFields = collectFields(file, editor);
        if (!existingFields.isEmpty()) {
            var selectedFields = selectFieldsAndOptions(existingFields, project);
            if (selectedFields.isEmpty()) {
                return;
            }
            var generatorParams = new GeneratorParams(project, file, editor, selectedFields,
                    JavaPsiFacade.getElementFactory(project));
            var builderGenerator = new InnerBuilderGenerator(generatorParams);
            ApplicationManager.getApplication().runWriteAction(builderGenerator);
        }
    }

}
