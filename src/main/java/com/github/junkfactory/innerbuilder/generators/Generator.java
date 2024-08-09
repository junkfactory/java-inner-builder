package com.github.junkfactory.innerbuilder.generators;

public interface Generator extends Runnable {

    GenerationResult generate();

    default void run() {
        generate();
    }
}
