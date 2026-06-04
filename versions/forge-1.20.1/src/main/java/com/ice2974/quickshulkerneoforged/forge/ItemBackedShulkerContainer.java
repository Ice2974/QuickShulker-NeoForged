package com.ice2974.quickshulkerneoforged.forge;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class ItemBackedShulkerContainer extends SimpleContainer {
    private boolean dirty;

    public ItemBackedShulkerContainer(ForgeShulkerContentAccess access, ItemStack hostStack) {
        super(ForgeShulkerContentAccess.SHULKER_SIZE);
        NonNullList<ItemStack> items = access.readItemStacks(hostStack);
        for (int i = 0; i < items.size(); i++) {
            setItem(i, items.get(i).copy());
        }
        dirty = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public NonNullList<ItemStack> copyContents() {
        NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) {
            items.set(i, getItem(i).copy());
        }
        return items;
    }
}
