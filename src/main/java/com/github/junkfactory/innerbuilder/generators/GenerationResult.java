package com.github.junkfactory.innerbuilder.generators;

import java.util.BitSet;

public class GenerationResult {

    static final GenerationResult NO_RESULT = new GenerationResult();

    public enum Code {
        ADD_IMPORT
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
}
