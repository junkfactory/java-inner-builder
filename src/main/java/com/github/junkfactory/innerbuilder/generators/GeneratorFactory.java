package com.github.junkfactory.innerbuilder.generators;

public interface GeneratorFactory {

    static GeneratorFactory create() {
        return new InnerBuilderGeneratorFactory();
    }

    Generator createInnerBuilderGenerator(GeneratorParams generatorParams);

    Generator createBuilderClassGenerator(GeneratorParams generatorParams,
                                          BuilderClassParams builderClassParams);

    FieldsGenerator createBuilderFieldsGenerator(GeneratorParams generatorParams,
                                                 BuilderClassParams builderClassParams);

    MethodsGenerator createBuilderMethodsGenerator(GeneratorParams generatorParams,
                                                   BuilderClassParams builderClassParams,
                                                   FieldsGenerator fieldsGenerator);
}
