package dev.w1zzrd.spigot.wizcompat;

import dev.w1zzrd.spigot.wizcompat.serialization.UUIDList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public class WizCompatPlugin extends JavaPlugin {
    public static void logError(final String message) {
        Bukkit.getLogger().warning(String.format("[WizCompat] %s", message));
    }


    @Override
    public void onEnable() {
        super.onEnable();

        // Register serializers
        ConfigurationSerialization.registerClass(UUIDList.class);
    }

    @Override
    public void onDisable() {
        super.onDisable();

        // Un-register serializers
        ConfigurationSerialization.unregisterClass(UUIDList.class);
    }
}
