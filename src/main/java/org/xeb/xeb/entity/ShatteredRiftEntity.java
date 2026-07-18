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
import java.util.List;

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
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (this.level().isClientSide()) {
            // Client side: spawn rotating dust particles and rising sparkles
            int difficulty = this.getDifficulty();
            double time = (this.tickCount) * 0.15D;
            
            float r = 1.0F, g = 1.0F, b = 1.0F;
            if (difficulty == 0) { // Blue
                r = 0.1F; g = 0.4F; b = 1.0F;
            } else if (difficulty == 1) { // Green
                r = 0.1F; g = 0.9F; b = 0.3F;
            } else if (difficulty == 2) { // Red
                r = 1.0F; g = 0.1F; b = 0.1F;
            } else { // Rainbow: cycling HSL
                double cTime = (System.currentTimeMillis() % 2000) / 2000.0 * 2.0 * Math.PI;
                r = (float) (0.5D + 0.5D * Math.sin(cTime));
                g = (float) (0.5D + 0.5D * Math.sin(cTime + 2.0D * Math.PI / 3.0D));
                b = (float) (0.5D + 0.5D * Math.sin(cTime + 4.0D * Math.PI / 3.0D));
            }
            
            // Spawn 3 rotating dust particles at Y = position.y + 0.05
            for (int i = 0; i < 3; i++) {
                double offset = i * (2.0D * Math.PI / 3.0D);
                double radius = 1.0D + 0.2D * Math.sin(time * 0.5D);
                double px = this.getX() + Math.cos(time + offset) * radius;
                double pz = this.getZ() + Math.sin(time + offset) * radius;
                
                net.minecraft.core.particles.DustParticleOptions dust = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(r, g, b), 1.2F);
                this.level().addParticle(dust, px, this.getY() + 0.05D, pz, 0.0D, 0.0D, 0.0D);
            }
            
            // Random rising end rods inside the 3x3 area
            if (this.random.nextFloat() < 0.15F) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, 
                    this.getX() + (this.random.nextDouble() - 0.5D) * 2.5D,
                    this.getY() + 0.05D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 2.5D,
                    0.0D, 0.02D, 0.0D
                );
            }
        } else {
            // Server side: proximity sneak detection within 2.5 blocks (inflate bounding box)
            List<Player> nearby = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(1.0D), 
                    p -> p.isAlive() && !p.isSpectator());
            for (Player player : nearby) {
                boolean isSneaking = player.isShiftKeyDown();
                String wasSneakingKey = "xebRiftWasSneaking_" + this.getId();
                boolean wasSneaking = player.getPersistentData().getBoolean(wasSneakingKey);
                
                if (isSneaking && !wasSneaking) {
                    // Sneak transition (false -> true)
                    int crouchCount = player.getPersistentData().getInt("xebRiftCrouchCount");
                    long lastCrouchTime = player.getPersistentData().getLong("xebLastCrouchTime");
                    long gameTime = this.level().getGameTime();
                    
                    if (gameTime - lastCrouchTime < 30) { // must be within 1.5 seconds
                        crouchCount++;
                    } else {
                        crouchCount = 1;
                    }
                    
                    player.getPersistentData().putInt("xebRiftCrouchCount", crouchCount);
                    player.getPersistentData().putLong("xebLastCrouchTime", gameTime);
                    
                    if (crouchCount >= 3) {
                        // Reset player state
                        player.getPersistentData().remove("xebRiftCrouchCount");
                        player.getPersistentData().remove("xebLastCrouchTime");
                        player.getPersistentData().remove(wasSneakingKey);
                        // Trigger activation
                        this.interact(player, InteractionHand.MAIN_HAND);
                        break;
                    }
                }
                player.getPersistentData().putBoolean(wasSneakingKey, isSneaking);
            }
        }
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
                
                // Fetch player's actual Elite Meter level and scale boss target level
                int playerLevel = org.xeb.xeb.medallion.MedallionManager.getEliteMeterLevel(player);
                int bossTargetLevel = switch (difficulty) {
                    case 1 -> playerLevel + 6;   // Green (+6)
                    case 2 -> playerLevel + 8;   // Red (+8)
                    case 3 -> playerLevel + 12;  // Rainbow (+12)
                    case 0 -> playerLevel + 4;   // Blue (+4)
                    default -> playerLevel + 4;
                };

                // Generate a randomized epic boss name matching the spawned entity type
                String randomName = getEpicBossName(boss);
                String colorCode = switch (difficulty) {
                    case 1 -> "§2"; // Green
                    case 2 -> "§c"; // Red
                    case 3 -> "§d"; // Pink/Rainbow
                    case 0 -> "§b"; // Cyan/Blue
                    default -> "§7";
                };
                
                boss.setCustomName(Component.literal(colorCode + "[Elite +" + bossTargetLevel + "] " + randomName));
                boss.setCustomNameVisible(true);

                // Apply attributes, equipment and medallions based on calculated bossTargetLevel
                equipAndBuffBoss(boss, bossTargetLevel, difficulty, serverLevel);

                // Add to world
                serverLevel.addFreshEntity(boss);
            }

            // 3. Remove rift
            this.discard();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    private static final String[] EPIC_FIRST_NAMES = {
        "Balthazar", "Ragnar", "Morgath", "Kalthor", "Malakar", 
        "Vorn", "Tenebris", "Aethelgard", "Vulgard", "Ignis", 
        "Xerxes", "Valerius", "Grom", "Zul'Gar", "Drakar",
        "Krag'Maw", "Zephyrus", "Gorthaur", "Sarkon", "Ymir",
        "Vorash", "Kaelen", "Grendel", "Vandor", "Thorgar"
    };

    private String getEpicBossName(Mob boss) {
        String firstName = EPIC_FIRST_NAMES[boss.getRandom().nextInt(EPIC_FIRST_NAMES.length)];
        net.minecraft.resources.ResourceLocation key = EntityType.getKey(boss.getType());
        String typeId = key != null ? key.toString() : "";
        
        String title = switch (typeId) {
            case "minecraft:wither_skeleton" -> ", the Decay Bringer";
            case "minecraft:piglin_brute" -> ", the Golden Berserker";
            case "minecraft:husk" -> ", the Desert Dread";
            case "minecraft:stray" -> ", the Frozen Archer";
            case "minecraft:spider", "minecraft:cave_spider" -> ", the Brood Mother";
            case "minecraft:witch" -> ", the Cursed Alchemist";
            case "minecraft:zombie" -> ", the Undying Fiend";
            case "minecraft:skeleton" -> ", the Bone Warlord";
            default -> ", the Shattered Sentinel";
        };
        return firstName + title;
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

    private void equipAndBuffBoss(Mob boss, int bossTargetLevel, int difficulty, ServerLevel level) {
        // Continuous multiplier scaling based on calculated boss target level
        double hpMultiplier = 1.0D + (bossTargetLevel * 0.45D);   // +45% HP per level
        double dmgMultiplier = 1.0D + (bossTargetLevel * 0.15D);  // +15% damage per level

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
        boss.getPersistentData().putInt("xebRiftTargetLevel", bossTargetLevel);

        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        ItemStack[] armorPieces = new ItemStack[4];

        if (bossTargetLevel >= 18) {
            // High progression: Netherite
            armorPieces[0] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET);
            armorPieces[1] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE);
            armorPieces[2] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS);
            armorPieces[3] = new ItemStack(net.minecraft.world.item.Items.NETHERITE_BOOTS);
            for (ItemStack piece : armorPieces) {
                piece.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
                piece.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
            }
        } else if (bossTargetLevel >= 12) {
            // Mid-high progression: Diamond
            armorPieces[0] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET);
            armorPieces[1] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            armorPieces[2] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS);
            armorPieces[3] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS);
            for (ItemStack piece : armorPieces) {
                piece.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 3);
            }
        } else if (bossTargetLevel >= 6) {
            // Mid progression: Iron / Diamond chest
            armorPieces[0] = new ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
            armorPieces[1] = new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            armorPieces[2] = new ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS);
            armorPieces[3] = new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS);
        } else {
            // Low progression: Iron
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
            if (bossTargetLevel >= 10) {
                weapon.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 3);
            }
            boss.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            boss.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
        }

        // Attach actual Elite Medallions to the boss so they render, apply buffs, and trigger proper loot logic
        assignRiftBossMedallions(boss, bossTargetLevel, level);
    }

    private void assignRiftBossMedallions(Mob boss, int targetLevel, ServerLevel level) {
        // Determine medallion count based on target level, scaling past 4 up to 9
        int count = 1;
        if (targetLevel >= 25) count = 9;
        else if (targetLevel >= 22) count = 8;
        else if (targetLevel >= 19) count = 7;
        else if (targetLevel >= 16) count = 6;
        else if (targetLevel >= 13) count = 5;
        else if (targetLevel >= 10) count = 4;
        else if (targetLevel >= 7) count = 3;
        else if (targetLevel >= 4) count = 2;

        List<org.xeb.xeb.medallion.MedallionData> rolled = new java.util.ArrayList<>();
        java.util.Set<String> excludeSet = new java.util.HashSet<>();
        net.minecraft.util.RandomSource random = boss.getRandom();

        for (int i = 0; i < count; i++) {
            excludeSet.addAll(org.xeb.xeb.medallion.MedallionManager.getConflictingBuffSet(rolled));
            
            org.xeb.xeb.buff.EliteBuff buff = org.xeb.xeb.buff.EliteBuffRegistry.getRandomByWeight(
                net.minecraft.util.RandomSource.create(random.nextLong()), true, new java.util.ArrayList<>(excludeSet)
            );
            
            if (buff != null) {
                org.xeb.xeb.medallion.MedallionType tier;
                double roll = random.nextDouble();
                if (targetLevel >= 8) {
                    tier = roll < 0.85 ? org.xeb.xeb.medallion.MedallionType.LEGENDARY : org.xeb.xeb.medallion.MedallionType.RARE;
                } else if (targetLevel >= 6) {
                    tier = roll < 0.60 ? org.xeb.xeb.medallion.MedallionType.RARE : (roll < 0.85 ? org.xeb.xeb.medallion.MedallionType.LEGENDARY : org.xeb.xeb.medallion.MedallionType.COMMON);
                } else {
                    tier = roll < 0.65 ? org.xeb.xeb.medallion.MedallionType.COMMON : org.xeb.xeb.medallion.MedallionType.RARE;
                }
                
                rolled.add(new org.xeb.xeb.medallion.MedallionData(buff, tier, java.util.UUID.randomUUID()));
                if (!buff.isStackable()) {
                    excludeSet.add(buff.getId());
                }
            }
        }

        if (!rolled.isEmpty()) {
            org.xeb.xeb.medallion.MedallionManager.saveMedallions(boss, rolled);
            for (org.xeb.xeb.medallion.MedallionData m : rolled) {
                try {
                    m.getBuff().onAttach(boss, m.getUniqueId());
                } catch (Exception e) {
                    System.err.println("Failed to attach rift boss medallion: " + m.getBuff().getId());
                    e.printStackTrace();
                }
            }
            org.xeb.xeb.medallion.MedallionManager.refreshDimensionsIfNeeded(boss, rolled);
            org.xeb.xeb.medallion.MedallionManager.syncToTracking(boss);
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
