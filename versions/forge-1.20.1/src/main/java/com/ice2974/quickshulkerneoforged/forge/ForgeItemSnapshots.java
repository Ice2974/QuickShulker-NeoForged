package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.HostItemSnapshot;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeItemSnapshots {
    private ForgeItemSnapshots() {
    }

    public static HostItemSnapshot snapshot(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return new HostItemSnapshot("minecraft:air", 0, "");
        }
        String itemKey = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(stack.getItem()), "item key").toString();
        CompoundTag tag = stack.getTag();
        String fingerprint = tag == null ? "" : tag.copy().toString();
        return new HostItemSnapshot(itemKey, stack.getCount(), fingerprint);
    }
}
