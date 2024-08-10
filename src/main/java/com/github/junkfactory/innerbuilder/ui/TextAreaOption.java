package com.github.junkfactory.innerbuilder.ui;

public record TextAreaOption(JavaInnerBuilderOption option,
                             int numLines,
                             int numColumns,
                             String toolTip) implements SelectorOption {
    @Override
    public String caption() {
        return option.getDescription();
    }
}
