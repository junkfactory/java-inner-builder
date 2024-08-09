package com.github.junkfactory.innerbuilder.generators;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiUtil;

class BuilderClassGenerator extends AbstractGenerator implements Generator {

    private final BuilderClassParams builderClassParams;

    BuilderClassGenerator(GeneratorFactory generatorFactory,
                          GeneratorParams generatorParams,
                          BuilderClassParams builderClassParams) {
        super(generatorFactory, generatorParams);
        this.builderClassParams = builderClassParams;
    }

    @Override
    public GenerationResult generate() {
        //builder constructor
        var builderClass = builderClassParams.builderClass();
        var builderConstructor = generateBuilderConstructor();
        addMethod(builderClass, null, builderConstructor, false);

        var fieldsGenerator = generatorFactory.createBuilderFieldsGenerator(generatorParams, builderClassParams);
        fieldsGenerator.generate();

        var methodsGenerator = generatorFactory.createBuilderMethodsGenerator(generatorParams,
                builderClassParams, fieldsGenerator);
        return methodsGenerator.generate();
    }

    private PsiMethod generateBuilderConstructor() {
        var builderConstructor = generatorParams.psi().factory().createConstructor(BUILDER_CLASS_NAME);
        PsiUtil.setModifierProperty(builderConstructor, PsiModifier.PRIVATE, true);
        return builderConstructor;
    }

}
