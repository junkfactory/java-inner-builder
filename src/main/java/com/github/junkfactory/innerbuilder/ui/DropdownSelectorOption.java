package com.github.junkfactory.innerbuilder.ui;

import java.util.List;

record DropdownSelectorOption(JavaInnerBuilderOption option, String toolTip,
                              List<DropdownSelectorOptionValue> values) implements SelectorOption {
    @Override
    public String caption() {
        return option.getDescription();
    }
}
