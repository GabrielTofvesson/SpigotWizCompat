package dev.w1zzrd.spigot.wizcompat.packet;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static dev.w1zzrd.spigot.wizcompat.packet.Reflect.*;

public final class Players {
    private Players() { throw new UnsupportedOperationException("Functional class"); }

    public static void sendPlayerGameModePacket(final Player target, final GameMode gameMode) {
        final Object entity = getEntityFromPlayer(target);

        final Package versionPackage = getNativePacketPackage(target);
        final Class<?> type_PacketPlayOutPlayerInfo = loadClass(versionPackage, "PacketPlayOutPlayerInfo", "game.PacketPlayOutPlayerInfo");
        final Class<?> type_EnumPlayerInfoAction = loadClass(versionPackage, "PacketPlayOutPlayerInfo$EnumPlayerInfoAction", "game.PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
        final Class<?> type_PlayerInfoData = loadClass(versionPackage, "PacketPlayOutPlayerInfo$PlayerInfoData", "game.PacketPlayOutPlayerInfo$PlayerInfoData");

        assert type_PacketPlayOutPlayerInfo != null;
        assert type_EnumPlayerInfoAction != null;
        assert type_PlayerInfoData != null;

        final Class<?> type_GameMode = type_PlayerInfoData.getDeclaredConstructors()[0].getParameterTypes()[2];

        final Object profile = reflectInvoke(entity, new String[]{ "getProfile" });
        final Integer ping = reflectGetField(entity, int.class, "ping", "e");

        Object nativeGameMode = null;
        for (final Enum<?> e : (Enum<?>[])reflectInvokeStatic(type_GameMode, new String[]{ "values" }))
            if (e.name().equalsIgnoreCase(gameMode.name())) {
                nativeGameMode = e;
                break;
            }

        // Use deprecated implementation as a last resort
        if (nativeGameMode == null)
            nativeGameMode = reflectInvokeStatic(type_GameMode, new String[]{ "getById" }, gameMode.getValue());

        Object listName = reflectInvoke(entity, new String[]{ "getPlayerListName" });
        if (listName == null)
            listName = reflectGetField(entity, "listName");

        // Syntactic sugar can go suck a fat one
        // I didn't realize the constructor took an array type, because I completely missed the "..."
        final Object entityArray = Array.newInstance(entity.getClass(), 1);
        Array.set(entityArray, 0, entity);

        final Object packet = reflectConstruct(type_PacketPlayOutPlayerInfo, reflectGetStaticField(type_EnumPlayerInfoAction, "UPDATE_GAME_MODE", "b"), entityArray);

        assert packet != null;

        final List<Object> playerInfo = new ArrayList<>();
        playerInfo.add(reflectConstruct(type_PlayerInfoData, profile, ping, nativeGameMode, listName));

        reflectSetField(packet, List.class, playerInfo);

        Packets.sendPacket(target, packet);
    }

    static Object getWorldServerFromPlayer(final Player from) {
        return reflectGetField(from.getWorld(), "world");
    }

    public static Object getEntityFromPlayer(final Player player) {
        try {
            return reflectInvoke(player, new String[]{ "getHandle" });
        } catch(Throwable t) {
            t.printStackTrace();
        }

        return reflectGetField(player, "entity");
    }

    public static void openSignEditor(final Player target, final Location where) {
        // Must edit a non-editable sign
        final BlockState targetState = where.getBlock().getState();
        if (!(targetState instanceof Sign) || ((Sign)targetState).isEditable())
            return;

        final Object tileSign = reflectGetField(targetState, "tileEntity");

        // Make editable by player
        reflectInvoke(tileSign, new String[]{ "a" }, target.getUniqueId());
        reflectInvoke(tileSign, new String[]{ "a" }, true);

        final Class<?> type_PacketPlayOutOpenSignEditor = loadClass(
                getNativePacketPackage(target),
                "PacketPlayOutOpenSignEditor", "game.PacketPlayOutOpenSignEditor"
        );
        assert type_PacketPlayOutOpenSignEditor != null;

        Packets.sendPacket(
                target,
                reflectConstruct(
                        type_PacketPlayOutOpenSignEditor,
                        reflectConstruct(
                                type_PacketPlayOutOpenSignEditor.getDeclaredFields()[0].getType(), // Only declares one field
                                where.getBlockX(),
                                where.getBlockY(),
                                where.getBlockZ()
                        )));
    }

}
