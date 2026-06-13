package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.bundling.PlayerEnderChestContentAccess;
import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ForgePlayerEnderChestContentAccess implements PlayerEnderChestContentAccess<Player, ItemStack> {
    @Override
    public List<ItemStack> readPlayerEnderChestContents(Player playerHandle) {
        Container enderChest = playerHandle.getEnderChestInventory();
        List<ItemStack> contents = new ArrayList<>(enderChest.getContainerSize());
        for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
            contents.add(enderChest.getItem(slot).copy());
        }
        return contents;
    }

    @Override
    public ContentWriteResult writePlayerEnderChestContents(Player playerHandle, List<ItemStack> contents) {
        Container enderChest = playerHandle.getEnderChestInventory();
        int limit = Math.min(enderChest.getContainerSize(), contents.size());
        for (int slot = 0; slot < limit; slot++) {
            enderChest.setItem(slot, contents.get(slot).copy());
        }
        for (int slot = limit; slot < enderChest.getContainerSize(); slot++) {
            enderChest.setItem(slot, ItemStack.EMPTY);
        }
        enderChest.setChanged();
        return ContentWriteResult.applied("Wrote player-scoped Forge ender chest contents back to the live inventory.");
    }
}
