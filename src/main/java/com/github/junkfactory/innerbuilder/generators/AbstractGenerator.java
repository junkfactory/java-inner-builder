package com.github.junkfactory.innerbuilder.generators;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class AbstractGenerator implements Runnable {

    @NonNls
    static final String BUILDER_CLASS_NAME = "Builder";
    @NonNls
    static final String BUILDER_METHOD_NAME = "builder";
    @NonNls
    static final String TO_BUILDER_NAME = "toBuilder";

    protected final GeneratorFactory generatorFactory;
    protected final GeneratorParams generatorParams;

    protected AbstractGenerator(GeneratorFactory generatorFactory, GeneratorParams generatorParams) {
        this.generatorFactory = generatorFactory;
        this.generatorParams = generatorParams;
    }

    protected PsiElement addElement(PsiElement target, PsiElement element, PsiElement after) {
        if (after != null) {
            return target.addAfter(element, after);
        }
        return target.add(element);
    }

    protected PsiElement addMethod(@NotNull final PsiClass target, @Nullable final PsiElement after,
                                   @NotNull final PsiMethod newMethod, final boolean replace) {
        var existingMethod = target.findMethodBySignature(newMethod, false);
        if (existingMethod == null && newMethod.isConstructor()) {
            existingMethod = findConstructor(target, newMethod);
        }
        if (existingMethod == null) {
            return addElement(target, newMethod, after);
        } else if (replace) {
            existingMethod.replace(newMethod);
        }
        return existingMethod;
    }

    private PsiMethod findConstructor(PsiClass target, PsiMethod newMethod) {
        for (var constructor : target.getConstructors()) {
            if (Utils.areParameterListsEqual(constructor.getParameterList(), newMethod.getParameterList())) {
                return constructor;
            }
        }
        return null;
    }

}
