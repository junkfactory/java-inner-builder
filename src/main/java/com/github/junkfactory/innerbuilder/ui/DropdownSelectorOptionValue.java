package com.github.junkfactory.innerbuilder.ui;

record DropdownSelectorOptionValue(JavaInnerBuilderOption option) {
    static DropdownSelectorOptionValue of(JavaInnerBuilderOption option) {
        return new DropdownSelectorOptionValue(option);
    }
}
