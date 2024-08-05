package com.github.junkfactory.innerbuilder.generators;

class InnerBuilderGeneratorFactory implements GeneratorFactory {
    @Override
    public Runnable createInnerBuilderGenerator(GeneratorParams generatorParams) {
        return new InnerBuilderGenerator(this, generatorParams);
    }

    @Override
    public Runnable createBuilderClassGenerator(GeneratorParams generatorParams,
                                                BuilderClassParams builderClassParams) {
        return new BuilderClassGenerator(this, generatorParams, builderClassParams);
    }

    @Override
    public Runnable createBuilderFieldsGenerator(GeneratorParams generatorParams,
                                                 BuilderClassParams builderClassParams) {
        return new BuilderFieldsGenerator(this, generatorParams, builderClassParams);
    }

    @Override
    public Runnable createBuilderMethodsGenerator(GeneratorParams generatorParams,
                                                  BuilderClassParams builderClassParams) {
        return new BuilderMethodsGenerator(this, generatorParams, builderClassParams);
    }
}
