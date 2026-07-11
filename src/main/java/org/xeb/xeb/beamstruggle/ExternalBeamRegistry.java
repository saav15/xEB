package org.xeb.xeb.beamstruggle;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Permite que otros mods registren sus beams para participar en Beam Struggles.
 */
public final class ExternalBeamRegistry {

    private ExternalBeamRegistry() {}

    public record ExternalBeamData(UUID ownerUUID, Vec3 start, Vec3 end, int color) {}

    private static final ConcurrentHashMap<String, Function<LivingEntity, ExternalBeamData>> PROVIDERS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<ExternalBeamData> CURRENT_TICK_BEAMS = new CopyOnWriteArrayList<>();

    public static void registerBeamProvider(String modId, Function<LivingEntity, ExternalBeamData> provider) {
        PROVIDERS.put(modId, provider);
    }

    /**
     * Llamado por xEB cada tick del server para recolectar beams de mods externos.
     * No iterar entidades si no hay providers registrados.
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

    static {
        // Register Tremorzilla hook via reflection
        registerBeamProvider("alexscaves", entity -> {
            if (entity.getClass().getName().endsWith("TremorzillaEntity")) {
                try {
                    java.lang.reflect.Method isFiring = entity.getClass().getMethod("isFiringBeam");
                    boolean firing = (boolean) isFiring.invoke(entity);
                    if (firing) {
                        java.lang.reflect.Method getBeamEndpoint = entity.getClass().getMethod("getBeamEndpoint");
                        Vec3 endpoint = (Vec3) getBeamEndpoint.invoke(entity);
                        return new ExternalBeamData(
                            entity.getUUID(),
                            entity.getEyePosition(1.0F),
                            endpoint,
                            0x40FF80  // Tremorzilla green beam color
                        );
                    }
                } catch (Exception ignored) {}
            }
            return null;
        });
    }
}
