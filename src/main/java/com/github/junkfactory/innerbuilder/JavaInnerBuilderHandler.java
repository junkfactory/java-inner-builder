package com.github.junkfactory.innerbuilder;

import com.github.junkfactory.innerbuilder.generators.FieldCollector;
import com.github.junkfactory.innerbuilder.generators.GeneratorFactory;
import com.github.junkfactory.innerbuilder.generators.GeneratorParams;
import com.github.junkfactory.innerbuilder.generators.PsiParams;
import com.github.junkfactory.innerbuilder.generators.Utils;
import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOptionSelector;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.AstLoadingFilter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

class JavaInnerBuilderHandler implements LanguageCodeInsightActionHandler {

    private static final GeneratorFactory generatorFactory = GeneratorFactory.create();

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
        return file instanceof PsiJavaFile && FieldCollector.builder()
                .file(file)
                .editor(editor)
                .build()
                .hasFields();
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {

        if (!EditorModificationUtil.checkModificationAllowed(editor)) {
            return;
        }

        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
            return;
        }

        var fieldCollector = FieldCollector.builder()
                .file(file)
                .editor(editor)
                .build();

        AstLoadingFilter.disallowTreeLoading(() -> {
            var existingFields = fieldCollector.collectFields();
            if (existingFields.isEmpty()) {
                return;
            }

            var optionsDialog = JavaInnerBuilderOptionSelector.builder()
                    .project(project)
                    .members(existingFields)
                    .build();
            var selectedFields = optionsDialog.selectFieldsAndOptions();
            if (selectedFields.isEmpty()) {
                return;
            }

            var psiParams = PsiParams.builder()
                    .file(file)
                    .selectedFields(selectedFields)
                    .factory(JavaPsiFacade.getElementFactory(project))
                    .codeStyleManager(JavaCodeStyleManager.getInstance(project))
                    .psiManager(PsiManager.getInstance(project))
                    .build();
            var generatorParams = GeneratorParams.builder()
                    .project(project)
                    .editor(editor)
                    .psi(psiParams)
                    .options(currentOptions())
                    .build();
            var builderGenerator = generatorFactory.createInnerBuilderGenerator(generatorParams);
            ApplicationManager.getApplication().runWriteAction(builderGenerator);
        });

    }

    private Set<JavaInnerBuilderOption> currentOptions() {
        final var options = EnumSet.noneOf(JavaInnerBuilderOption.class);
        final var propertiesComponent = PropertiesComponent.getInstance();
        for (var option : JavaInnerBuilderOption.values()) {
            switch (option.getType()) {
                case BOOLEAN:
                    if (propertiesComponent.getBoolean(option.getProperty(), false)) {
                        options.add(option);
                    }
                    break;
                case LIST:
                    var list = propertiesComponent.getList(option.getProperty());
                    if (null != list && !list.isEmpty()) {
                        options.add(option);
                    }
                    break;
                default:
                    String currentValue = String.valueOf(propertiesComponent.getValue(option.getProperty()));
                    JavaInnerBuilderOption.findValue(currentValue).ifPresent(options::add);
            }
        }
        return options;
    }
}
