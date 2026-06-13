package com.ice2974.quickshulkerneoforged.common.bundling;

public interface EnderChestBundlingStackAdapter<S> extends ShulkerBundlingStackAdapter<S> {
    boolean isEnderChest(S stack);

    boolean canInsertIntoEnderChest(S stack);

    default boolean isSingleEnderChest(S stack) {
        return isEnderChest(stack) && getCount(stack) == 1;
    }

    default boolean isSingleShulkerBox(S stack) {
        return isShulkerBox(stack) && getCount(stack) == 1;
    }
}
