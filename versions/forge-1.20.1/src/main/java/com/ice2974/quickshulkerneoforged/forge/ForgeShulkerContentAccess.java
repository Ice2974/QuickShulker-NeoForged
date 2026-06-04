package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.content.ContainerContentAccess;
import com.ice2974.quickshulkerneoforged.common.content.ContainerContentSnapshot;
import com.ice2974.quickshulkerneoforged.common.content.ContainerSlotSnapshot;
import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ForgeShulkerContentAccess implements ContainerContentAccess<ItemStack> {
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
                    ForgeItemSnapshots.snapshot(stack).itemKey(),
                    stack.getCount(),
                    ForgeItemSnapshots.snapshot(stack).contentFingerprint()
                ));
            }
        }
        return new ContainerContentSnapshot(items.size(), snapshots);
    }

    @Override
    public ContentWriteResult writeContents(ItemStack hostHandle, ContainerContentSnapshot snapshot) {
        return ContentWriteResult.rejected("Stage 4 Forge writes use live ItemStack menus instead of lossy common snapshots.");
    }

    public NonNullList<ItemStack> readItemStacks(ItemStack hostHandle) {
        NonNullList<ItemStack> items = NonNullList.withSize(SHULKER_SIZE, ItemStack.EMPTY);
        CompoundTag blockEntityTag = BlockItem.getBlockEntityData(hostHandle);
        if (blockEntityTag != null) {
            ContainerHelper.loadAllItems(blockEntityTag, items);
        }
        return items;
    }

    public ContentWriteResult writeItemStacks(ItemStack hostHandle, NonNullList<ItemStack> items) {
        CompoundTag blockEntityTag = BlockItem.getBlockEntityData(hostHandle);
        CompoundTag target = blockEntityTag == null ? new CompoundTag() : blockEntityTag.copy();
        ContainerHelper.saveAllItems(target, items, false);

        boolean hasItems = false;
        ListTag list = target.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!ItemStack.of(entry).isEmpty()) {
                hasItems = true;
                break;
            }
        }

        if (hasItems) {
            BlockItem.setBlockEntityData(hostHandle, BlockEntityType.SHULKER_BOX, target);
        } else {
            target.remove("Items");
            if (target.isEmpty()) {
                CompoundTag stackTag = hostHandle.getTag();
                if (stackTag != null) {
                    stackTag.remove("BlockEntityTag");
                    if (stackTag.isEmpty()) {
                        hostHandle.setTag(null);
                    }
                }
            } else {
                BlockItem.setBlockEntityData(hostHandle, BlockEntityType.SHULKER_BOX, target);
            }
        }
        return ContentWriteResult.applied("Wrote shulker box contents back to BlockEntityTag.");
    }
}
