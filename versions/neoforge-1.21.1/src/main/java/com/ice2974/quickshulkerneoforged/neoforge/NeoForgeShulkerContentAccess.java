package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.content.ContainerContentAccess;
import com.ice2974.quickshulkerneoforged.common.content.ContainerContentSnapshot;
import com.ice2974.quickshulkerneoforged.common.content.ContainerSlotSnapshot;
import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class NeoForgeShulkerContentAccess implements ContainerContentAccess<ItemStack> {
    public static final int SHULKER_SIZE = 27;

    @Override
    public ContainerContentSnapshot readContents(ItemStack hostHandle) {
        NonNullList<ItemStack> items = readItemStacks(hostHandle);
        List<ContainerSlotSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                snapshots.add(new ContainerSlotSnapshot(
                    i,
                    NeoForgeItemSnapshots.snapshot(stack).itemKey(),
                    stack.getCount(),
                    NeoForgeItemSnapshots.snapshot(stack).contentFingerprint()
                ));
            }
        }
        return new ContainerContentSnapshot(items.size(), snapshots);
    }

    @Override
    public ContentWriteResult writeContents(ItemStack hostHandle, ContainerContentSnapshot snapshot) {
        return ContentWriteResult.rejected("Stage 5 NeoForge writes use live ItemStack menus instead of lossy common snapshots.");
    }

    public NonNullList<ItemStack> readItemStacks(ItemStack hostHandle) {
        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SIZE, ItemStack.EMPTY);
        ItemContainerContents contents = hostHandle.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(items);
        }
        return items;
    }

    public ContentWriteResult writeItemStacks(ItemStack hostHandle, NonNullList<ItemStack> items) {
        hostHandle.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return ContentWriteResult.applied("Wrote shulker box contents back to DataComponents.CONTAINER.");
    }
}
