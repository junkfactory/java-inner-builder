package com.github.junkfactory.innerbuilder.generators;

class InnerBuilderGeneratorFactory implements GeneratorFactory {
    @Override
    public Generator createInnerBuilderGenerator(GeneratorParams generatorParams) {
        return new InnerBuilderGenerator(this, generatorParams);
    }

    @Override
    public Generator createBuilderClassGenerator(GeneratorParams generatorParams,
                                                 BuilderClassParams builderClassParams) {
        return new BuilderClassGenerator(this, generatorParams, builderClassParams);
    }

    @Override
    public FieldsGenerator createBuilderFieldsGenerator(GeneratorParams generatorParams,
                                                        BuilderClassParams builderClassParams) {
        return new BuilderFieldsGenerator(this, generatorParams, builderClassParams);
    }

    @Override
    public MethodsGenerator createBuilderMethodsGenerator(GeneratorParams generatorParams,
                                                          BuilderClassParams builderClassParams,
                                                          FieldsGenerator fieldsGenerator) {
        return new BuilderMethodsGenerator(this, generatorParams, builderClassParams, fieldsGenerator);
    }
}
