package com.github.junkfactory.innerbuilder.ui;

record CheckboxSelectorOption(JavaInnerBuilderOption option, char mnemonic, String toolTip) implements SelectorOption {
    CheckboxSelectorOption(JavaInnerBuilderOption option, char mnemonic) {
        this(option, mnemonic, "");
    }

    @Override
    public String caption() {
        return option.getDescription();
    }
}
