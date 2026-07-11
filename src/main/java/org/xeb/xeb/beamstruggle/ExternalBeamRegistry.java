package org.xeb.xeb.beamstruggle;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
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
            if (!entity.getClass().getName().toLowerCase().contains("tremorzilla")) return null;
            MethodCache cache = METHOD_CACHE.computeIfAbsent(entity.getClass(), k -> new MethodCache());
            if (!cache.initialized) {
                initMethodCache(cache, entity.getClass(),
                        new String[]{"isFiringBeam", "isBeamActive", "isLaserActive", "isShooting"},
                        new String[]{"getBeamEndpoint", "getBeamEnd", "getLaserTarget", "getBeamTarget", "getTargetPosition"});
                cache.initialized = true;
            }
            return extractBeamData(entity, cache, 0x40FF80); // green
        });

        registerBeamProvider("tremorzilla_alt", entity -> {
            if (!entity.getClass().getName().toLowerCase().contains("tremorzilla")) return null;
            if (entity.getPersistentData().contains("BeamActive") && entity.getPersistentData().getBoolean("BeamActive")) {
                Vec3 start = entity.getEyePosition(1.0F);
                Vec3 end = start.add(entity.getLookAngle().scale(40.0D));
                if (entity.getPersistentData().contains("BeamEndX")) {
                    end = new Vec3(
                            entity.getPersistentData().getDouble("BeamEndX"),
                            entity.getPersistentData().getDouble("BeamEndY"),
                            entity.getPersistentData().getDouble("BeamEndZ")
                    );
                }
                return new ExternalBeamData(entity.getUUID(), start, end, 0x40FF80);
            }
            return null;
        });
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
                        new String[]{"isLaserActive", "isBeamActive", "isFiringBeam", "isShootingLaser"},
                        new String[]{"getLaserTarget", "getBeamTarget", "getBeamEnd", "getTargetPos"});
                cache.initialized = true;
            }
            return extractBeamData(entity, cache, 0xB000FF); // purple
        });

        // The Leviathan
        registerBeamProvider("leviathan", entity -> {
            String name = entity.getClass().getName().toLowerCase();
            if (!name.contains("leviathan")) return null;
            MethodCache cache = METHOD_CACHE.computeIfAbsent(entity.getClass(), k -> new MethodCache());
            if (!cache.initialized) {
                initMethodCache(cache, entity.getClass(),
                        new String[]{"isBeamFiring", "isBeamActive", "isLaserActive", "isShooting"},
                        new String[]{"getBeamTarget", "getBeamEnd", "getLaserTarget", "getTargetPos"});
                cache.initialized = true;
            }
            return extractBeamData(entity, cache, 0x00DDFF); // cyan
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
