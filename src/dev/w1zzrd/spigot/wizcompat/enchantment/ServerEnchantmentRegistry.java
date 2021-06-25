package dev.w1zzrd.spigot.wizcompat.enchantment;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static dev.w1zzrd.spigot.wizcompat.WizCompatPlugin.logError;

public final class ServerEnchantmentRegistry {
    private static final Field field_acceptingNew;
    private static final Field field_byKey;
    private static final Field field_ByName;
    
    private static boolean loggedFailReason = false;

    
    static {
        Field acceptingNew;
        Field byKey;
        Field byName;
        try {
            acceptingNew = Enchantment.class.getDeclaredField("acceptingNew");
            acceptingNew.setAccessible(true);
            acceptingNew.set(null, true);
            
            byKey = Enchantment.class.getDeclaredField("byKey");
            byKey.setAccessible(true);
            
            byName = Enchantment.class.getDeclaredField("byName");
            byName.setAccessible(true);
        } catch (final Throwable reason) {
            acceptingNew = null;
            byKey = null;
            byName = null;

            logError("Could not access necessary fields for enchantments: custom enchantments will not be registered! Reason:");
            reason.printStackTrace();
        }

        field_acceptingNew = acceptingNew;
        field_byKey = byKey;
        field_ByName = byName;
    }


    private ServerEnchantmentRegistry() { throw new UnsupportedOperationException("Functional class"); }

    private static final Map<Plugin, List<EnchantmentRegistryEntry>> enchantmentRegistry = new HashMap<>();

    /**
     * Register a custom enchantment in the server registry.<br>
     * Note: when a plugin is disabled, all registry entries for said plugin should be un-registered
     * @param plugin Plugin for which the enchantment will be registered
     * @param enchantment Enchantment to register
     * @param nsKey Namespaced key to associate with the given enchantment
     * @return An instance of {@link EnchantmentRegistryEntry} if registration was successful, otherwise returns null
     * @see #unRegisterEnchantment(Plugin, EnchantmentRegistryEntry)
     */
    public static <T extends Enchantment> EnchantmentRegistryEntry<T> registerEnchantment(
            final Plugin plugin,
            final T enchantment,
            final NamespacedKey nsKey
    ) {
        if (!ensureReflection()) {
            logError(String.format("Skipping registration of enchantment: %s", nsKey.toString()));
            return null;
        }

        final EnchantmentRegistryEntry<T> entry = new EnchantmentRegistryEntry<>(enchantment, nsKey);

        if (!ensureNotExists(plugin, entry))
            return null;

        getPluginRegistry(plugin).add(entry);

        Enchantment.registerEnchantment(enchantment);

        return entry;
    }

    /**
     * Register a custom enchantment in the server registry.<br>
     * Note: when a plugin is disabled, all registry entries for said plugin should be un-registered
     * @param plugin Plugin for which the enchantment will be registered
     * @param enchantment Enchantment to register
     * @param enchantmentName Enchantment name to associate with the given enchantment
     * @return An instance of {@link EnchantmentRegistryEntry} if registration was successful, otherwise returns null
     * @see #unRegisterEnchantment(Plugin, EnchantmentRegistryEntry)
     */
    public static <T extends Enchantment> EnchantmentRegistryEntry<T> registerEnchantment(
            final Plugin plugin,
            final T enchantment,
            final String enchantmentName
    ) {
        return registerEnchantment(plugin, enchantment, new NamespacedKey(plugin, enchantmentName));
    }

    /**
     * Register a custom enchantment in the server registry.<br>
     * Note: when a plugin is disabled, all registry entries for said plugin should be un-registered
     * @param plugin Plugin for which the enchantment will be registered
     * @param enchantment Enchantment to register
     * @return An instance of {@link EnchantmentRegistryEntry} if registration was successful, otherwise returns null
     * @see #unRegisterEnchantment(Plugin, EnchantmentRegistryEntry)
     */
    public static <T extends Enchantment> EnchantmentRegistryEntry<T> registerEnchantment(
            final Plugin plugin,
            final T enchantment
    ) {
        return registerEnchantment(plugin, enchantment, enchantment.getKey());
    }

    /**
     * Un-register a custom enchantment from the server registry
     * @param plugin Plugin to un-register the enchantment for
     * @param entry Registry entry, returned from registration, representing the registered enchantment
     * @see #registerEnchantment(Plugin, Enchantment, NamespacedKey)
     */
    public static void unRegisterEnchantment(final Plugin plugin, final EnchantmentRegistryEntry entry) {
        if (!ensureReflection()) {
            logError(String.format("Skipping un-registration of enchantment: %s", entry.getNsKey().toString()));
            return;
        }
        
        if (!ensureExists(plugin, entry))
            return;

        try {
            ((Map<?, ?>) field_byKey.get(null)).remove(entry.getNsKey());

            final Enchantment enchantment = entry.getEnchantment();

            @SuppressWarnings("unchecked") final Map<String, ?> byName = (Map<String, ?>) field_ByName.get(null);
            byName.keySet().stream().filter(key -> enchantment.equals(byName.get(key))).forEach(byName::remove);
        } catch(Throwable ignored) {
            // TOCTOU: If this ever happens, you have bigger problems than a stale enchantment registration
            return;
        }

        getPluginRegistry(plugin).remove(entry);
    }


    private static List<EnchantmentRegistryEntry> getPluginRegistry(final Plugin plugin) {
        return enchantmentRegistry.computeIfAbsent(plugin, k -> new LinkedList<>());
    }

    private static boolean ensureExists(final Plugin plugin, final EnchantmentRegistryEntry entry) {
        final boolean exists = getPluginRegistry(plugin).contains(entry);
        if (!exists)
            logError(String.format("Could not find plugin enchantment (it should be registered): %s", entry.getNsKey().toString()));

        return exists;
    }

    private static boolean ensureNotExists(final Plugin plugin, final EnchantmentRegistryEntry entry) {
        final boolean exists = getPluginRegistry(plugin).contains(entry);
        if (exists)
            logError(String.format("Found registered plugin enchantment (it should not be registered): %s", entry.getNsKey().toString()));

        return !exists;
    }

    private static boolean ensureReflection() {
        // Something went wrong in static initialization: error has already been logged
        if (field_acceptingNew == null || field_byKey == null || field_ByName == null)
            return false;

        try {
            // Ensure that the fields are actually accessible
            // This is probably unnecessary, but Java 9+ is kinda quirky,
            // so you can never be too safe
            return field_byKey.get(null) instanceof Map<?, ?> &&
                    field_ByName.get(null) instanceof Map<?, ?> &&
                    field_acceptingNew.getBoolean(null);
        } catch (final Throwable reason) {
            if (!loggedFailReason) {
                logError("Could not access necessary field for enchantments! Logging error once:");
                reason.printStackTrace();
                
                loggedFailReason = true;
            }
        }

        return false;
    }
}
