package org.xeb.xeb.beamstruggle;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Permite que otros mods registren sus beams para participar en Beam Struggles.
 */
public final class ExternalBeamRegistry {

    private ExternalBeamRegistry() {}

    public static void init() {
        // Trigger static block loading
    }

    public record ExternalBeamData(UUID ownerUUID, Vec3 start, Vec3 end, int color) {}

    private static final ConcurrentHashMap<String, Function<LivingEntity, ExternalBeamData>> PROVIDERS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<ExternalBeamData> CURRENT_TICK_BEAMS = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Class<?>, MethodCache> METHOD_CACHE = new ConcurrentHashMap<>();

    private static class MethodCache {
        Method isFiringBeam;
        Method getBeamEnd;
        boolean initialized = false;
    }

    public static void registerBeamProvider(String modId, Function<LivingEntity, ExternalBeamData> provider) {
        PROVIDERS.put(modId, provider);
    }

    /**
     * Llamado por xEB cada tick del server para recolectar beams de mods externos.
     */
    public static void collectBeams(net.minecraft.server.level.ServerLevel level) {
        CURRENT_TICK_BEAMS.clear();
        if (PROVIDERS.isEmpty()) return;

        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living) {
                for (Function<LivingEntity, ExternalBeamData> provider : PROVIDERS.values()) {
                    try {
                        ExternalBeamData data = provider.apply(living);
                        if (data != null) {
                            CURRENT_TICK_BEAMS.add(data);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public static CopyOnWriteArrayList<ExternalBeamData> getCurrentTickBeams() {
        return CURRENT_TICK_BEAMS;
    }

    private static void registerTremorzillaProvider() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("alexscaves")) return;

        registerBeamProvider("tremorzilla", entity -> {
            String className = entity.getClass().getName().toLowerCase();
            if (!className.contains("tremorzilla")) return null;
            MethodCache cache = METHOD_CACHE.computeIfAbsent(entity.getClass(), k -> new MethodCache());
            if (!cache.initialized) {
                initMethodCache(cache, entity.getClass(),
                        new String[]{
                            "isFiringBeam", "isBeamActive", "isLaserActive", "isShooting",
                            "isBeamFiring", "isFiringLaser", "getBeamActive", "isCharging",
                            "isShootingBeam", "isUsingBeam"
                        },
                        new String[]{
                            "getBeamEndpoint", "getBeamEnd", "getLaserTarget", "getBeamTarget",
                            "getTargetPosition", "getBeamPos", "getLaserEnd", "getTargetPos",
                            "getAimPos", "getLookTarget"
                        });
                cache.initialized = true;
            }
            ExternalBeamData data = extractBeamData(entity, cache, 0x40FF80);
            if (data == null) {
                // Fallback: detectar via NBT
                data = detectViaNBT(entity, 0x40FF80, "Tremorzilla", "BeamActive", "BeamEnd");
            }
            return data;
        });

        // Fallback adicional: scan de entidades LaserBeam cerca del Tremorzilla
        registerBeamProvider("tremorzilla_laser_entity", entity -> {
            if (!entity.getClass().getName().toLowerCase().contains("tremorzilla")) return null;
            AABB searchBox = entity.getBoundingBox().inflate(20.0D);
            List<net.minecraft.world.entity.Entity> nearby = entity.level().getEntities(entity, searchBox);
            for (net.minecraft.world.entity.Entity e : nearby) {
                String eClassName = e.getClass().getName().toLowerCase();
                if (eClassName.contains("laser") || eClassName.contains("beam")) {
                    if (e.getUUID().equals(entity.getUUID()) ||
                        (e.getPersistentData().hasUUID("Owner") && e.getPersistentData().getUUID("Owner").equals(entity.getUUID())) ||
                        (e.getPersistentData().hasUUID("owner") && e.getPersistentData().getUUID("owner").equals(entity.getUUID()))) {
                        Vec3 start = entity.getEyePosition(1.0F);
                        Vec3 end = e.position();
                        return new ExternalBeamData(entity.getUUID(), start, end, 0x40FF80);
                    }
                }
            }
            return null;
        });
    }

    private static ExternalBeamData detectViaNBT(LivingEntity entity, int color, String entityName,
                                                  String firingTag, String endTagPrefix) {
        if (entity.getPersistentData().contains(firingTag) && entity.getPersistentData().getBoolean(firingTag)) {
            Vec3 start = entity.getEyePosition(1.0F);
            Vec3 end = start.add(entity.getLookAngle().scale(40.0D));
            if (entity.getPersistentData().contains(endTagPrefix + "X")) {
                end = new Vec3(
                    entity.getPersistentData().getDouble(endTagPrefix + "X"),
                    entity.getPersistentData().getDouble(endTagPrefix + "Y"),
                    entity.getPersistentData().getDouble(endTagPrefix + "Z")
                );
            }
            return new ExternalBeamData(entity.getUUID(), start, end, color);
        }
        return null;
    }

    private static void registerCataclysmProviders() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("cataclysm")) return;

        // The Harbinger
        registerBeamProvider("the_harbinger", entity -> {
            String name = entity.getClass().getName().toLowerCase();
            if (!name.contains("harbinger")) return null;
            MethodCache cache = METHOD_CACHE.computeIfAbsent(entity.getClass(), k -> new MethodCache());
            if (!cache.initialized) {
                initMethodCache(cache, entity.getClass(),
                        new String[]{
                            "isLaserActive", "isBeamActive", "isFiringBeam", "isShootingLaser",
                            "isLaserCharging", "isBlastingLaser", "isShooting", "getLaserActive"
                        },
                        new String[]{
                            "getLaserTarget", "getBeamTarget", "getBeamEnd", "getTargetPos",
                            "getLaserEnd", "getAimTarget", "getBlastTarget"
                        });
                cache.initialized = true;
            }
            ExternalBeamData data = extractBeamData(entity, cache, 0xB000FF);
            if (data == null) data = detectViaNBT(entity, 0xB000FF, "Harbinger", "LaserActive", "LaserEnd");
            return data;
        });

        // The Leviathan
        registerBeamProvider("leviathan", entity -> {
            String name = entity.getClass().getName().toLowerCase();
            if (!name.contains("leviathan")) return null;
            MethodCache cache = METHOD_CACHE.computeIfAbsent(entity.getClass(), k -> new MethodCache());
            if (!cache.initialized) {
                initMethodCache(cache, entity.getClass(),
                        new String[]{
                            "isBeamFiring", "isBeamActive", "isLaserActive", "isShooting",
                            "isFiringBeam", "isBlastActive", "isShootingBeam"
                        },
                        new String[]{
                            "getBeamTarget", "getBeamEnd", "getLaserTarget", "getTargetPos",
                            "getBlastTarget", "getBeamEnd", "getAimPos"
                        });
                cache.initialized = true;
            }
            ExternalBeamData data = extractBeamData(entity, cache, 0x00DDFF);
            if (data == null) data = detectViaNBT(entity, 0x00DDFF, "Leviathan", "BeamActive", "BeamEnd");
            return data;
        });

        // Generic LaserBeam entity scan for Cataclysm
        registerBeamProvider("cataclysm_laser_scan", entity -> {
            String className = entity.getClass().getName().toLowerCase();
            if (!className.contains("laser") && !className.contains("beam")) return null;
            try {
                Method getOwner = entity.getClass().getMethod("getOwner");
                Object owner = getOwner.invoke(entity);
                if (owner instanceof LivingEntity livingOwner) {
                    Vec3 start = entity.position();
                    Vec3 end = start.add(entity.getDeltaMovement().scale(40.0D));
                    try {
                        Method getEnd = entity.getClass().getMethod("getEnd");
                        Object endResult = getEnd.invoke(entity);
                        if (endResult instanceof Vec3 v) end = v;
                    } catch (Exception ignored) {}
                    return new ExternalBeamData(livingOwner.getUUID(), start, end, 0xB000FF);
                }
            } catch (Exception ignored) {}
            return null;
        });
    }

    private static void initMethodCache(MethodCache cache, Class<?> clazz,
                                         String[] firingMethodNames, String[] endpointMethodNames) {
        for (String name : firingMethodNames) {
            try {
                cache.isFiringBeam = clazz.getMethod(name);
                break;
            } catch (NoSuchMethodException ignored) {}
        }
        for (String name : endpointMethodNames) {
            try {
                cache.getBeamEnd = clazz.getMethod(name);
                break;
            } catch (NoSuchMethodException ignored) {}
        }
    }

    private static ExternalBeamData extractBeamData(LivingEntity entity, MethodCache cache, int color) {
        if (cache.isFiringBeam == null) return null;
        try {
            Object firingResult = cache.isFiringBeam.invoke(entity);
            boolean firing = false;
            if (firingResult instanceof Boolean b) firing = b;
            else if (firingResult instanceof Integer i) firing = i > 0;
            if (!firing) return null;

            Vec3 start = entity.getEyePosition(1.0F);
            Vec3 end = start.add(entity.getLookAngle().scale(40.0D));
            if (cache.getBeamEnd != null) {
                Object endpointResult = cache.getBeamEnd.invoke(entity);
                if (endpointResult instanceof Vec3 v) end = v;
                else if (endpointResult instanceof net.minecraft.core.BlockPos bp) end = new Vec3(bp.getX(), bp.getY(), bp.getZ());
            }
            return new ExternalBeamData(entity.getUUID(), start, end, color);
        } catch (Exception ignored) {
            return null;
        }
    }

    static {
        registerTremorzillaProvider();
        registerCataclysmProviders();
    }
}
