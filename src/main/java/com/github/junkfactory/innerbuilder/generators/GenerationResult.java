package com.github.junkfactory.innerbuilder.generators;

import java.util.BitSet;

public class GenerationResult {

    static final GenerationResult NO_RESULT = new GenerationResult();

    public enum Code {
        IMPORTS_ADDED,
        ANNOTATIONS_ADDED
    }

    private final BitSet result;

    GenerationResult() {
        this.result = new BitSet();
    }

    void set(Code code) {
        result.set(code.ordinal());
    }

    public boolean isEmpty() {
        return !result.isEmpty();
    }

    public boolean did(Code code) {
        return result.get(code.ordinal());
    }

    public void when(Code code, Runnable runnable) {
        if (did(code)) {
            runnable.run();
        }
    }

    public GenerationResult merge(GenerationResult other) {
        result.or(other.result);
        return this;
    }
}
