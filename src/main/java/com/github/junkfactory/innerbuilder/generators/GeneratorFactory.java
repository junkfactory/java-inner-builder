package com.github.junkfactory.innerbuilder.generators;

public interface GeneratorFactory {

    static GeneratorFactory create() {
        return new InnerBuilderGeneratorFactory();
    }

    Runnable createInnerBuilderGenerator(GeneratorParams generatorParams);

    Runnable createBuilderClassGenerator(GeneratorParams generatorParams,
                                         BuilderClassParams builderClassParams);

    FieldsGenerator createBuilderFieldsGenerator(GeneratorParams generatorParams,
                                                 BuilderClassParams builderClassParams);

    MethodsGenerator createBuilderMethodsGenerator(GeneratorParams generatorParams,
                                                   BuilderClassParams builderClassParams,
                                                   FieldsGenerator fieldsGenerator);
}
