package org.xeb.xeb.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.xeb.xeb.network.StaticPortalSyncPacket;
import org.xeb.xeb.network.XEBNetwork;

public class StaticPortalEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(StaticPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPAWN_INTERVAL =
            SynchedEntityData.defineId(StaticPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_SPAWNS =
            SynchedEntityData.defineId(StaticPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPAWNS_COUNT =
            SynchedEntityData.defineId(StaticPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GLITCH_TICKS =
            SynchedEntityData.defineId(StaticPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_ENTITY_TYPE =
            SynchedEntityData.defineId(StaticPortalEntity.class, EntityDataSerializers.STRING);

    public StaticPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, -1);
        this.entityData.define(DATA_SPAWN_INTERVAL, 60);
        this.entityData.define(DATA_MAX_SPAWNS, 0);
        this.entityData.define(DATA_SPAWNS_COUNT, 0);
        this.entityData.define(DATA_GLITCH_TICKS, 0);
        this.entityData.define(DATA_ENTITY_TYPE, "xeb:steven_boss");
    }

    public int getPortalLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setPortalLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, lifetime);
    }

    public int getSpawnInterval() {
        return this.entityData.get(DATA_SPAWN_INTERVAL);
    }

    public void setSpawnInterval(int interval) {
        this.entityData.set(DATA_SPAWN_INTERVAL, Math.max(1, interval));
    }

    public int getMaxSpawns() {
        return this.entityData.get(DATA_MAX_SPAWNS);
    }

    public void setMaxSpawns(int maxSpawns) {
        this.entityData.set(DATA_MAX_SPAWNS, maxSpawns);
    }

    public int getSpawnsCount() {
        return this.entityData.get(DATA_SPAWNS_COUNT);
    }

    public void setSpawnsCount(int count) {
        this.entityData.set(DATA_SPAWNS_COUNT, count);
    }

    public int getGlitchTicks() {
        return this.entityData.get(DATA_GLITCH_TICKS);
    }

    public void setGlitchTicks(int glitchTicks) {
        this.entityData.set(DATA_GLITCH_TICKS, glitchTicks);
    }

    public String getPortalEntityType() {
        return this.entityData.get(DATA_ENTITY_TYPE);
    }

    public void setPortalEntityType(String entityType) {
        this.entityData.set(DATA_ENTITY_TYPE, entityType);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("xebPortalLifetime")) setPortalLifetime(tag.getInt("xebPortalLifetime"));
        if (tag.contains("xebPortalSpawnInterval")) setSpawnInterval(tag.getInt("xebPortalSpawnInterval"));
        if (tag.contains("xebPortalMaxSpawns")) setMaxSpawns(tag.getInt("xebPortalMaxSpawns"));
        if (tag.contains("xebPortalSpawnsCount")) setSpawnsCount(tag.getInt("xebPortalSpawnsCount"));
        if (tag.contains("xebPortalGlitchTicks")) setGlitchTicks(tag.getInt("xebPortalGlitchTicks"));
        if (tag.contains("xebPortalEntityType")) setPortalEntityType(tag.getString("xebPortalEntityType"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("xebPortalLifetime", getPortalLifetime());
        tag.putInt("xebPortalSpawnInterval", getSpawnInterval());
        tag.putInt("xebPortalMaxSpawns", getMaxSpawns());
        tag.putInt("xebPortalSpawnsCount", getSpawnsCount());
        tag.putInt("xebPortalGlitchTicks", getGlitchTicks());
        tag.putString("xebPortalEntityType", getPortalEntityType());
    }

    @Override
    public void tick() {
        super.tick();

        int lifetime = getPortalLifetime();
        if (lifetime > 0) {
            lifetime--;
            setPortalLifetime(lifetime);
        }

        int glitch = getGlitchTicks();
        if (glitch > 0) {
            setGlitchTicks(glitch - 1);
        }

        // Ambient beacon sound every 20 ticks
        if (this.tickCount % 20 == 1) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.6F, 0.3F);
        }

        // Random Glitch trigger every 20-40 ticks
        if (!this.level().isClientSide() && this.tickCount % (20 + this.random.nextInt(20)) == 0) {
            setGlitchTicks(3);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + (random.nextDouble() - 0.5D) * 1.5D,
                        this.getY() + random.nextDouble() * 2.5D,
                        this.getZ() + (random.nextDouble() - 0.5D) * 1.5D,
                        4, 0.2D, 0.4D, 0.2D, 0.05D);
            }
        }

        // Spawn interval tick check
        if (!this.level().isClientSide() && getSpawnInterval() > 0 && this.tickCount % getSpawnInterval() == 0) {
            int maxSpawns = getMaxSpawns();
            int count = getSpawnsCount();
            if (maxSpawns == -1 || count < maxSpawns) {
                spawnTargetEntity();
            }
        }

        // Periodic network sync every 10 ticks
        if (!this.level().isClientSide() && this.tickCount % 10 == 0) {
            XEBNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new StaticPortalSyncPacket(getId(), getPortalLifetime(), getSpawnInterval(), getMaxSpawns(), getSpawnsCount(), getGlitchTicks(), getPortalEntityType())
            );
        }

        // Discard when lifetime reaches 0
        if (lifetime == 0 && !this.level().isClientSide()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.5D, this.getZ(), 25, 0.8D, 1.2D, 0.8D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.5D, this.getZ(), 15, 0.6D, 1.0D, 0.6D, 0.1D);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.2F, 0.5F);
            this.discard();
        }
    }

    private void spawnTargetEntity() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        String typeId = getPortalEntityType();
        if (typeId == null || typeId.isEmpty()) return;

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(typeId));
        if (type != null) {
            Entity spawned = type.create(serverLevel);
            if (spawned != null) {
                spawned.moveTo(this.getX(), this.getY() + 0.1D, this.getZ(), this.getYRot(), this.getXRot());
                spawned.getPersistentData().putInt("xebMaterializingTicks", 10);
                if (spawned instanceof Mob mob) {
                    mob.setNoAi(true);
                }
                spawned.setInvulnerable(true);
                serverLevel.addFreshEntity(spawned);
                setSpawnsCount(getSpawnsCount() + 1);

                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2D, this.getZ(), 20, 0.5D, 0.8D, 0.5D, 0.1D);
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 1.2F, 1.5F);
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
