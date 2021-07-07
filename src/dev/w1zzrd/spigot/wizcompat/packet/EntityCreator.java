package dev.w1zzrd.spigot.wizcompat.packet;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

import static dev.w1zzrd.spigot.wizcompat.packet.Reflect.*;

public final class EntityCreator {
    private EntityCreator() { throw new UnsupportedOperationException("Functional class"); }

    private static Object getMinecraftServerFromWorld(final Object worldServer) {
        return reflectGetField(worldServer, "server", "D");
    }

    private static Object getMonsterEntityType(final Class<?> entityClass) {
        final Class<?> type_EntityTypes = entityClass.getDeclaredConstructors()[0].getParameterTypes()[0];

        return reflectGetGenericStaticField(type_EntityTypes, type_EntityTypes, entityClass);
    }

    public static Object createFakeMonster(final Player target, final String entityClassName) {
        final Package versionPackage = getNativeMonsterPackage(target);
        final Class<?> type_Entity = loadClass(versionPackage, entityClassName);

        final Object nativeWorld = Players.getWorldServerFromPlayer(target);
        assert type_Entity != null;
        final Object entityType = getMonsterEntityType(type_Entity);

        return reflectConstruct(type_Entity, entityType, nativeWorld);
    }

    public static Object createFakeSlime(final Player target) {
        return createFakeMonster(target, "EntitySlime");
    }

    public static Object createFakeShulker(final Player target) {
        return createFakeMonster(target, "EntityShulker");
    }

    public static void sendEntitySpawnPacket(final Player target, final Object entity) {
        final Package versionPackage = getNativePacketPackage(target);
        Packets.sendPacket(target, reflectConstruct(loadClass(versionPackage, "PacketPlayOutSpawnEntityLiving", "game.PacketPlayOutSpawnEntityLiving"), entity));
    }

    public static void sendEntityMetadataPacket(final Player target, final Object entity) {
        final Package versionPackage = getNativePacketPackage(target);

        Object constr1;
        try {
            constr1 = reflectConstruct(
                    loadClass(versionPackage, "PacketPlayOutEntityMetadata", "game.PacketPlayOutEntityMetadata"),
                    getEntityID(entity),
                    reflectGetField(entity, "dataWatcher", "Y")
            );
        } catch (Throwable t) {
            constr1 = reflectConstruct(
                    loadClass(versionPackage, "PacketPlayOutEntityMetadata", "game.PacketPlayOutEntityMetadata"),
                    getEntityID(entity),
                    reflectGetField(entity, "dataWatcher", "Y"),
                    true
            );
        }

        Packets.sendPacket(
                target,
                constr1
        );
    }

    public static void sendEntityDespawnPacket(final Player target, final int entityID) {
        sendEntityDespawnPackets(target, entityID);
    }

    public static void sendEntityDespawnPackets(final Player target, final int... entityIDs) {
        final Package versionPackage = getNativePacketPackage(target);
        try {
            Packets.sendPacket(target, reflectConstruct(loadClass(versionPackage, "PacketPlayOutEntityDestroy", "game.PacketPlayOutEntityDestroy"), new Object[]{ entityIDs }));
        } catch (Throwable t) {
            for (int entityID : entityIDs)
                Packets.sendPacket(target, reflectConstruct(loadClass(versionPackage, "PacketPlayOutEntityDestroy", "game.PacketPlayOutEntityDestroy"), entityID));
        }
    }

    public static int getEntityID(final Object entity) {
        return (Integer)reflectInvoke(entity, new String[]{ "getId" });
    }

    public static void setEntityInvisible(final Object entity, final boolean invisible) {
        reflectInvoke(entity, new String[]{ "setInvisible" }, invisible);
    }

    public static void setEntityInvulnerable(final Object entity, final boolean invulnerable) {
        reflectInvoke(entity, new String[]{ "setInvulnerable" }, invulnerable);
    }

    public static void setEntityGlowing(final Object entity, final boolean isGlowing) {
        reflectInvoke(entity, new String[]{ "setGlowingTag", "i" }, isGlowing);
    }

    public static void setEntityLocation(final Object entity, final double x, final double y, final double z, final float yaw, final float pitch) {
        reflectInvoke(entity, new String[]{ "setLocation" }, x, y, z, yaw, pitch);
    }

    public static void setEntityCollision(final Object entity, final boolean collision) {
        reflectSetField(entity, boolean.class, collision, "collides");
    }

    public static void setEntityCustomName(final Object entity, final String name) {
        final Package versionPackage = entity.getClass().getPackage();
        final Method setCustomName = findDeclaredMethod(entity.getClass(), new String[]{ "setCustomName" }, new Object[]{ null });

        final Package chatPackage = setCustomName.getParameterTypes()[0].getPackage();

        setEntityCustomName(entity, reflectConstruct(loadClass(chatPackage, "ChatComponentText"), name));
    }

    public static void setEntityCustomName(final Object entity, final Object chatBaseComponent) {
        reflectInvoke(entity, new String[]{ "setCustomName" }, chatBaseComponent);
    }

    public static void setEntityCustomNameVisible(final Object entity, final boolean visible) {
        reflectInvoke(entity, new String[] { "setCustomNameVisible" }, visible);
    }

    public static void setSlimeSize(final Object slime, final int size) {
        reflectInvoke(slime, new String[]{ "setSize" }, size, true);
    }
}
