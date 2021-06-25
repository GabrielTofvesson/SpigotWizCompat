package dev.w1zzrd.spigot.wizcompat.enchantment;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Objects;

public final class EnchantmentRegistryEntry<T extends Enchantment> {
    private final T enchantment;
    private final NamespacedKey nsKey;

    EnchantmentRegistryEntry(final T enchantment, final NamespacedKey nsKey) {
        this.enchantment = enchantment;
        this.nsKey = nsKey;
    }

    public T getEnchantment() {
        return enchantment;
    }

    public NamespacedKey getNsKey() {
        return nsKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnchantmentRegistryEntry<?> that = (EnchantmentRegistryEntry<?>) o;
        return enchantment.equals(that.enchantment) && nsKey.equals(that.nsKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enchantment, nsKey);
    }
}
