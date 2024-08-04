package com.github.junkfactory.innerbuilder;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiUtil;

class BuilderClassGenerator extends AbstractGenerator {

    private final BuilderClassParams builderClassParams;

    BuilderClassGenerator(GeneratorParams generatorParams, BuilderClassParams builderClassParams) {
        super(generatorParams);
        this.builderClassParams = builderClassParams;
    }

    @Override
    public void run() {
        //builder constructor
        var builderClass = builderClassParams.builderClass();
        var builderConstructor = generateBuilderConstructor();
        addMethod(builderClass, null, builderConstructor, false);

        new BuilderFieldsGenerator(generatorParams, builderClassParams).run();
        new BuilderMethodsGenerator(generatorParams, builderClassParams).run();
    }

    private PsiMethod generateBuilderConstructor() {
        var builderConstructor = generatorParams.psi().factory().createConstructor(BUILDER_CLASS_NAME);
        PsiUtil.setModifierProperty(builderConstructor, PsiModifier.PRIVATE, true);
        return builderConstructor;
    }

}
