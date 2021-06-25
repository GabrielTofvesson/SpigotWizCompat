package dev.w1zzrd.spigot.wizcompat.block;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

public final class Chests {
    private Chests() { throw new UnsupportedOperationException("Functional class"); }

    public static InventoryHolder getChestInventoryHolder(final Chest chest) {
        return Objects.requireNonNull(chest.getBlockInventory().getHolder()).getInventory().getHolder();
    }

    public static boolean isDoubleChest(final Block block) {
        return block.getState() instanceof Chest && getChestInventoryHolder((Chest) block.getState()) instanceof DoubleChest;
    }

    public static Block getLeftChest(final Chest chest) {
        if (isDoubleChest(chest.getBlock()))
            return ((Chest) Objects.requireNonNull(((DoubleChest) getChestInventoryHolder(chest)).getLeftSide())).getBlock();
        else
            return chest.getBlock();
    }

    public static Block getRightChest(final Chest chest) {
        if (isDoubleChest(chest.getBlock()))
            return ((Chest) Objects.requireNonNull(((DoubleChest) getChestInventoryHolder(chest)).getRightSide())).getBlock();
        else
            return chest.getBlock();
    }
}
