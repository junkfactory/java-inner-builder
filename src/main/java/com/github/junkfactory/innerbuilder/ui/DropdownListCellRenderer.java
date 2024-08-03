package com.github.junkfactory.innerbuilder.ui;

import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;

class DropdownListCellRenderer extends SimpleListCellRenderer<DropdownSelectorOptionValue> {

    @Override
    public void customize(@NotNull JList<? extends DropdownSelectorOptionValue> list,
                          DropdownSelectorOptionValue value,
                          int index,
                          boolean selected,
                          boolean hasFocus) {
        setText(value.option().getDescription());
    }
}
