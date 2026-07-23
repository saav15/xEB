package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class StevenPortalEntity extends Entity {
    private int lifetime = 40;

    public StevenPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Lifetime")) {
            this.lifetime = tag.getInt("Lifetime");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.lifetime);
    }

    @Override
    public void tick() {
        super.tick();
        this.lifetime--;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.2D, this.getZ(), 8, 0.6, 0.8, 0.6, 0.05);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.2D, this.getZ(), 5, 0.4, 0.6, 0.4, 0.02);

            if (this.tickCount == 1) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 1.2F);
            }
        }

        if (this.lifetime <= 0 && !this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
