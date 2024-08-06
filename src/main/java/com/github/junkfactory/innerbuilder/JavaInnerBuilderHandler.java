package com.github.junkfactory.innerbuilder;

import com.github.junkfactory.innerbuilder.generators.GeneratorFactory;
import com.github.junkfactory.innerbuilder.generators.GeneratorParams;
import com.github.junkfactory.innerbuilder.generators.PsiParams;
import com.github.junkfactory.innerbuilder.generators.Utils;
import com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOption;
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
import com.intellij.util.AstLoadingFilter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

import static com.github.junkfactory.innerbuilder.generators.FieldCollector.collectFields;
import static com.github.junkfactory.innerbuilder.ui.JavaInnerBuilderOptionSelector.selectFieldsAndOptions;

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
        var targetElements = collectFields(file, editor);
        return !targetElements.isEmpty();
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {

        if (!EditorModificationUtil.checkModificationAllowed(editor)) {
            return;
        }

        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
            return;
        }

        AstLoadingFilter.disallowTreeLoading(() -> {
            var existingFields = collectFields(file, editor);
            if (existingFields.isEmpty()) {
                return;
            }

            var selectedFields = selectFieldsAndOptions(existingFields, project);
            if (selectedFields.isEmpty()) {
                return;
            }

            var psiParams = PsiParams.builder()
                    .file(file)
                    .selectedFields(selectedFields)
                    .factory(JavaPsiFacade.getElementFactory(project))
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

            if (Boolean.TRUE.equals(option.isBooleanProperty())) {
                final boolean currentSetting = propertiesComponent.getBoolean(option.getProperty(), false);
                if (currentSetting) {
                    options.add(option);
                }
            } else {
                String currentValue = String.valueOf(propertiesComponent.getValue(option.getProperty()));
                if (currentValue != null) {
                    JavaInnerBuilderOption.findValue(currentValue).ifPresent(options::add);
                }
            }
        }
        return options;
    }
}
