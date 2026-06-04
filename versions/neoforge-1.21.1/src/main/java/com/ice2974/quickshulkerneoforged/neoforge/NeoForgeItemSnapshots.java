package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.open.HostItemSnapshot;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class NeoForgeItemSnapshots {
    private NeoForgeItemSnapshots() {
    }

    public static HostItemSnapshot snapshot(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return new HostItemSnapshot("minecraft:air", 0, "");
        }
        String itemKey = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(stack.getItem()), "item key").toString();
        String fingerprint = stack.getComponents().toString();
        return new HostItemSnapshot(itemKey, stack.getCount(), fingerprint);
    }
}
