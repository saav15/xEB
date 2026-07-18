package org.xeb.xeb.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.network.NetworkHooks;
import org.xeb.xeb.item.ModItems;

public class ShatteredRiftEntity extends Entity {
    private static final EntityDataAccessor<Integer> DIFFICULTY = SynchedEntityData.defineId(ShatteredRiftEntity.class, EntityDataSerializers.INT);

    public ShatteredRiftEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ShatteredRiftEntity(Level level, double x, double y, double z, int difficulty) {
        this(ModEntities.SHATTERED_RIFT.get(), level);
        this.setPos(x, y, z);
        this.entityData.set(DIFFICULTY, difficulty);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DIFFICULTY, 0);
    }

    public int getDifficulty() {
        return this.entityData.get(DIFFICULTY);
    }

    public void setDifficulty(int difficulty) {
        this.entityData.set(DIFFICULTY, difficulty);
    }

    @Override
    public void tick() {
        super.tick();
        // Client side particles or general ticking can be placed here if needed.
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int difficulty = this.getDifficulty();
            BlockPos spawnPos = this.blockPosition();

            // 1. Play dramatic explosion sounds
            serverLevel.playSound(null, spawnPos, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
            serverLevel.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.7F);

            // 2. Select appropriate mob based on biome
            EntityType<? extends Mob> mobType = getBossMobType(serverLevel, spawnPos);
            Mob boss = mobType.create(serverLevel);
            if (boss != null) {
                boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
                
                // Set name based on color / difficulty
                String namePrefix = switch (difficulty) {
                    case 1 -> "§2[Elite +6] ";
                    case 2 -> "§c[Elite +8] ";
                    case 3 -> "§d[Elite +12] ";
                    case 0 -> "§1[Elite +4] ";
                    default -> "§7[Elite] ";
                };
                boss.setCustomName(Component.literal(namePrefix).append(boss.getType().getDescription()));
                boss.setCustomNameVisible(true);

                // Apply attributes and gear
                equipAndBuffBoss(boss, difficulty, serverLevel);

                // Add to world
                serverLevel.addFreshEntity(boss);
            }

            // 3. Remove rift
            this.discard();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    private EntityType<? extends Mob> getBossMobType(Level level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);
        if (biomeHolder.is(BiomeTags.IS_NETHER)) {
            return level.random.nextFloat() < 0.6F ? EntityType.WITHER_SKELETON : EntityType.PIGLIN_BRUTE;
        }
        if (biomeHolder.is(BiomeTags.HAS_DESERT_PYRAMID) || biomeHolder.toString().contains("desert") || biomeHolder.toString().contains("sandy")) {
            return level.random.nextFloat() < 0.7F ? EntityType.HUSK : EntityType.SPIDER;
        }
        if (biomeHolder.is(BiomeTags.IS_TAIGA) || biomeHolder.is(BiomeTags.IS_HILL) || biomeHolder.toString().contains("cold") || biomeHolder.toString().contains("snow")) {
            return level.random.nextFloat() < 0.7F ? EntityType.STRAY : EntityType.ZOMBIE;
        }
        if (pos.getY() < 50) {
            float r = level.random.nextFloat();
            if (r < 0.4F) return EntityType.CAVE_SPIDER;
            if (r < 0.7F) return EntityType.SKELETON;
            return EntityType.ZOMBIE;
        }
        float r = level.random.nextFloat();
        if (r < 0.45F) return EntityType.ZOMBIE;
        if (r < 0.90F) return EntityType.SKELETON;
        return EntityType.WITCH;
    }

    private void equipAndBuffBoss(Mob boss, int difficulty, ServerLevel level) {
        double hpMultiplier = switch (difficulty) {
            case 1 -> 3.0D;
            case 2 -> 4.0D;
            case 3 -> 8.0D;
            case 0 -> 2.0D;
            default -> 1.0D;
        };
        double dmgMultiplier = switch (difficulty) {
            case 1 -> 1.6D;
            case 2 -> 2.0D;
            case 3 -> 3.0D;
            case 0 -> 1.3D;
            default -> 1.0D;
        };

        var maxHpAttr = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            double baseVal = maxHpAttr.getBaseValue();
            maxHpAttr.setBaseValue(baseVal * hpMultiplier);
            boss.setHealth((float) (baseVal * hpMultiplier));
        }

        var dmgAttr = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            double baseVal = dmgAttr.getBaseValue();
            dmgAttr.setBaseValue(baseVal * dmgMultiplier);
        }

        var followAttr = boss.getAttribute(Attributes.FOLLOW_RANGE);
        if (followAttr != null) {
            followAttr.setBaseValue(48.0D);
        }

        boss.getPersistentData().putBoolean("xebRiftBoss", true);
        boss.getPersistentData().putInt("xebRiftDifficulty", difficulty);

        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        ItemStack[] armorPieces = new ItemStack[4];

        if (difficulty == 3) {
            armorPieces[0] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET);
            armorPieces[1] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE);
            armorPieces[2] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS);
            armorPieces[3] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_BOOTS);
            for (ItemStack piece : armorPieces) {
                piece.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
                piece.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
            }
        } else if (difficulty == 2) {
            armorPieces[0] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET);
            armorPieces[1] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            armorPieces[2] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS);
            armorPieces[3] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS);
            for (ItemStack piece : armorPieces) {
                piece.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 3);
            }
        } else if (difficulty == 1) {
            armorPieces[0] = new ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
            armorPieces[1] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            armorPieces[2] = new ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS);
            armorPieces[3] = new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS);
        } else {
            armorPieces[0] = new ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
            armorPieces[1] = new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
            armorPieces[2] = new ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS);
            armorPieces[3] = new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS);
        }

        for (int i = 0; i < 4; i++) {
            boss.setItemSlot(armorSlots[i], armorPieces[i]);
            boss.setDropChance(armorSlots[i], 0.0F);
        }

        ItemStack weapon;
        boolean hasSmartHalberd = level.random.nextFloat() < 0.20F;
        if (hasSmartHalberd) {
            weapon = new ItemStack(ModItems.SMART_HALBERD.get());
            boss.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            boss.setDropChance(EquipmentSlot.MAINHAND, 0.01F); // Drops with 1.0% base chance, increased by Medallero
        } else {
            net.minecraft.world.item.Item[] weapons = {
                net.minecraft.world.item.Items.IRON_SWORD,
                net.minecraft.world.item.Items.IRON_AXE,
                net.minecraft.world.item.Items.DIAMOND_SWORD,
                net.minecraft.world.item.Items.DIAMOND_AXE
            };
            weapon = new ItemStack(weapons[level.random.nextInt(weapons.length)]);
            if (difficulty >= 2) {
                weapon.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, difficulty + 1);
            }
            boss.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            boss.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDifficulty(tag.getInt("Difficulty"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Difficulty", getDifficulty());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
