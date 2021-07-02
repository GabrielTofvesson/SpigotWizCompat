package dev.w1zzrd.spigot.wizcompat.packet;

import org.bukkit.entity.Player;

import static dev.w1zzrd.spigot.wizcompat.packet.Players.getEntityFromPlayer;
import static dev.w1zzrd.spigot.wizcompat.packet.Reflect.reflectGetField;
import static dev.w1zzrd.spigot.wizcompat.packet.Reflect.reflectInvoke;

public class Packets {
    public static void sendPacket(final Player target, final Object packet) {
        reflectInvoke(reflectGetField(getEntityFromPlayer(target), "playerConnection", "networkManager"), new String[]{ "sendPacket" }, packet);
    }
}
