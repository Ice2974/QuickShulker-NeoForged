package com.ice2974.quickshulkerneoforged.common.bundling;

public interface ShulkerBundlingStackAdapter<S> {
    S empty();

    S copy(S stack);

    S copyWithCount(S stack, int count);

    boolean isEmpty(S stack);

    boolean isShulkerBox(S stack);

    boolean canInsertIntoShulker(S stack);

    boolean canStacksMerge(S existingStack, S incomingStack);

    int getCount(S stack);

    int getMaxStackSize(S stack);
}
