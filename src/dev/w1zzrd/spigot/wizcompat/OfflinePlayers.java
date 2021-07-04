package dev.w1zzrd.spigot.wizcompat;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OfflinePlayers {
    private OfflinePlayers(){ throw new UnsupportedOperationException("Functional class"); }

    private static int totalFiles = -1;
    private static Set<UUID> fileUUIDs = null;

    private static Set<UUID> getFileUUIDsAggressive(final Server server) {
        return server.getWorlds().stream().flatMap(world -> {
            File[] playerDataFiles = new File(world.getWorldFolder(), "playerdata").listFiles();

            if (playerDataFiles == null)
                return Stream.empty();

            return Arrays.stream(playerDataFiles)
                    .filter(file -> file.getName().endsWith(".dat"))
                    .map(file -> {
                        try {
                            return UUID.fromString(file.getName().substring(0, file.getName().indexOf('.')));
                        } catch (IllegalArgumentException badName) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull);
        }).collect(Collectors.toSet());
    }

    private static Set<UUID> getFileUUIDsLazy(final Server server) {
        int totalFiles = 0;
        for (final World world : server.getWorlds()) {
            final File worldFolder = new File(world.getWorldFolder(), "playerdata");
            final String[] fileNames = worldFolder.list();

            if (fileNames != null)
                totalFiles += fileNames.length;
        }

        // Assume no data files have been deleted
        // I.e. if there is not change in the dat file count, we assume no new players have been seen
        if (OfflinePlayers.totalFiles != totalFiles || fileUUIDs == null) {
            final Set<UUID> found = getFileUUIDsAggressive(server);
            OfflinePlayers.totalFiles = totalFiles;

            fileUUIDs = found;
            return found;
        }

        return fileUUIDs;
    }

    /**
     * Get UUIDs of all players known to this server
     * @param server Server to get player UUIDs for
     * @return Array of all UUIDs of players on the server
     */
    public static UUID[] getAllKnownPlayers(final Server server, final boolean lazyFileLoading) {
        final Set<UUID> fileUUIDS = lazyFileLoading ? getFileUUIDsLazy(server) : getFileUUIDsAggressive(server);

        // Add online players, in case (for some reason idk) the online player data hasn't yet been saved to a file
        // This is probably unnecessary
        fileUUIDS.addAll(server.getOnlinePlayers().stream().map(Entity::getUniqueId).collect(Collectors.toSet()));

        return fileUUIDS.toArray(new UUID[0]);
    }

    /**
     * Find a player on the server by name
     * @param server Server to find player for
     * @param playerName Name of player to find
     * @param ignoreCase Whether or not to respect case-sensitivity
     * @return Matching player instance, or null if server does not know of a player with the given name
     */
    public static OfflinePlayer getKnownPlayer(final Server server, final String playerName, final boolean ignoreCase, final boolean lazyFileLoading) {
        for (final UUID uuid : getAllKnownPlayers(server, lazyFileLoading)) {
            final OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            final String name = player.getName();

            if (name != null && (ignoreCase && name.equalsIgnoreCase(playerName) || !ignoreCase && name.equals(playerName)))
                return player;
        }

        return null;
    }

    /**
     * Find a player on the server by name
     * @param server Server to find player for
     * @param playerName Name of player to find
     * @return Matching player instance, or null if server does not know of a player with the given name
     */
    public static OfflinePlayer getKnownPlayer(final Server server, final String playerName) {
        return getKnownPlayer(server, playerName, true, true);
    }
}