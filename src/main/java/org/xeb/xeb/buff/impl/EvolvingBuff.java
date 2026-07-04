package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.network.BuffParticlePacket;
import org.xeb.xeb.network.XEBNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EvolvingBuff extends EliteBuff {
    private static final String TICKS_KEY = "xebEvolvingTicks";

    public EvolvingBuff() {
        super("evolving", "Evolving", BuffType.ENEMY_ONLY, 0x7B68EE, 10.0D, false);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        entity.getPersistentData().putInt(TICKS_KEY, 0);
    }

    @Override
    public void onDetach(LivingEntity entity) {
        entity.getPersistentData().remove(TICKS_KEY);
    }

    @Override
    public void onServerTick(LivingEntity entity, ServerLevel level) {
        CompoundTag tag = entity.getPersistentData();
        int ticks = tag.getInt(TICKS_KEY) + 1;
        
        if (ticks >= 600) { // 30 seconds (600 ticks)
            ticks = 0;

            List<MedallionData> current = MedallionManager.getMedallions(entity);
            if (current.size() < 5) { // Cap at 5 total medallions
                List<String> excludeIds = new ArrayList<>();
                excludeIds.add(this.getId());
                for (MedallionData m : current) {
                    if (!m.getBuff().isStackable()) {
                        excludeIds.add(m.getBuff().getId());
                    }
                }
                excludeIds.addAll(MedallionManager.getConflictingBuffIds(current));

                EliteBuff newBuff = EliteBuffRegistry.getRandomByWeight(entity.getRandom(), MedallionManager.isBoss(entity), excludeIds);
                if (newBuff != null) {
                    MedallionManager.attachMedallion(entity, new MedallionData(newBuff, MedallionType.COMMON, UUID.randomUUID()));

                    // Check for evolved_champion advancement
                    List<MedallionData> updated = MedallionManager.getMedallions(entity);
                    if (updated.size() >= 5) {
                        level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, entity.getBoundingBox().inflate(32.0D)).forEach(player -> {
                            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                                org.xeb.xeb.event.MedallionSpawnHandler.grantAdvancement(sp, "xeb:evolved_champion");
                            }
                        });
                    }

                    // Particles
                    BuffParticlePacket packet = new BuffParticlePacket(entity.getX(), entity.getY(), entity.getZ(), "evolve", 15);
                    XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
                }
            }
        }
        tag.putInt(TICKS_KEY, ticks);
    }
}
