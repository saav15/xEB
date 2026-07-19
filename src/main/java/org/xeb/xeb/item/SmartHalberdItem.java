package org.xeb.xeb.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.xeb.xeb.entity.SpikeProjectileEntity;
import org.xeb.xeb.network.HalberdSpikeSyncPacket;
import org.xeb.xeb.network.XEBNetwork;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class SmartHalberdItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // === CONSTANTES ===
    public static final int MAX_SPIKES = 8;
    public static final int TICKS_PER_SPIKE = 5;       // 0.25s por pico
    public static final double SPIKE_SPACING = 2.5D;    // distancia entre picos
    public static final double SPIKE_ZIGZAG_AMPLITUDE = 1.2D; // altura del zigzag
    public static final double SPIKE_LAUNCH_SPEED = 2.5D;
    public static final double SPIKE_MAX_REACH = 30.0D;
    public static final float BIG_SPIKE_DAMAGE = 14.0F;  // balanceado
    public static final float SMALL_BARB_DAMAGE = 4.0F; // balanceado
    public static final int BARB_COUNT = 6;             // puas pequeñas por impacto
    public static final double BARB_SPREAD_RADIUS = 3.0D;

    public SmartHalberdItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, event -> {
            net.minecraft.world.entity.Entity entity = event.getData(software.bernie.geckolib.constant.DataTickets.ENTITY);
            LivingEntity wielder = resolveWielder(entity);
            if (wielder instanceof Player player) {
                boolean isLowHP = player.getHealth() / player.getMaxHealth() <= 0.20F;
                if (isLowHP) {
                    player.getPersistentData().remove("xebHalberdEyeAnim");
                    return event.setAndContinue(RawAnimation.begin().thenLoop("Eye5"));
                } else {
                    long now = player.level().getGameTime();
                    long lastCheck = player.getPersistentData().getLong("xebHalberdEyeTime");
                    String currentEye = player.getPersistentData().getString("xebHalberdEyeAnim");
                    
                    if (now - lastCheck >= 100 || currentEye.isEmpty() || "Eye5".equals(currentEye) || !isEyeAnim(currentEye)) {
                        player.getPersistentData().putLong("xebHalberdEyeTime", now);
                        String[] eyeAnims = {"Idle", "Eye2", "Eye3", "Eye4"};
                        currentEye = eyeAnims[player.getRandom().nextInt(eyeAnims.length)];
                        player.getPersistentData().putString("xebHalberdEyeAnim", currentEye);
                    }
                    return event.setAndContinue(RawAnimation.begin().thenLoop(currentEye));
                }
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("Idle"));
        }));
    }

    private boolean isEyeAnim(String anim) {
        return "Idle".equals(anim) || "Eye2".equals(anim) || "Eye3".equals(anim) || "Eye4".equals(anim);
    }

    @Nullable
    private LivingEntity resolveWielder(net.minecraft.world.entity.Entity entityFromTicket) {
        if (entityFromTicket instanceof LivingEntity living && isHolding(living)) {
            return living;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && isHolding(mc.player)) {
                return mc.player;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private boolean isHolding(LivingEntity entity) {
        return entity.getMainHandItem().getItem() == this
                || entity.getOffhandItem().getItem() == this;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.xeb.smart_halberd.desc1"));
        tooltip.add(Component.translatable("item.xeb.smart_halberd.desc2"));
        tooltip.add(Component.translatable("item.xeb.smart_halberd.lore"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || stack.isEnchanted();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return org.xeb.xeb.enchantment.ModEnchantments.isModEnchantment(enchantment) 
            || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private org.xeb.xeb.client.renderer.SmartHalberdGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new org.xeb.xeb.client.renderer.SmartHalberdGeoRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> builder = com.google.common.collect.ImmutableMultimap.builder();
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
            builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 8.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            builder.put(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(), new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.fromString("c6d982b6-dc82-4df4-8d48-8df0c6f50b4f"), "Weapon modifier", 1.5D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        
        // Inicializar estado de carga
        player.getPersistentData().putInt("xebHalberdChargeTicks", 0);
        player.getPersistentData().putInt("xebHalberdSpikeCount", 0);
        
        if (!level.isClientSide()) {
            level.playSound(null, player, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.5F, 1.5F);
            // Sincronizar inicio de carga
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new HalberdSpikeSyncPacket(player.getId(), new ArrayList<>(), true));
        }
        
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPEAR; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!(entity instanceof Player player)) return;
        
        int ticksUsed = this.getUseDuration(stack) - count;
        player.getPersistentData().putInt("xebHalberdChargeTicks", ticksUsed);
        
        // Calcular cuántos picos deberían existir
        int expectedSpikes = Math.min(MAX_SPIKES, ticksUsed / TICKS_PER_SPIKE);
        int currentSpikes = player.getPersistentData().getInt("xebHalberdSpikeCount");
        
        if (expectedSpikes > currentSpikes) {
            int spikeIndex = currentSpikes; // 0-indexed
            player.getPersistentData().putInt("xebHalberdSpikeCount", expectedSpikes);
            
            if (!level.isClientSide()) {
                // Calcular posición del pico en el patrón de serpiente
                Vec3 lookDir = player.getLookAngle();
                
                // Zigzag: pares arriba, impares abajo
                double zigzag = (spikeIndex % 2 == 0) ? SPIKE_ZIGZAG_AMPLITUDE : -SPIKE_ZIGZAG_AMPLITUDE;
                
                // Posición del pico: avanzar en la dirección de la mira + offset zigzag en Y
                double distance = (spikeIndex + 1) * SPIKE_SPACING;
                Vec3 spikePos = player.getEyePosition(1.0F)
                        .add(lookDir.scale(distance))
                        .add(0, zigzag, 0);
                
                // Guardar la posición del pico en NBT
                String key = "xebHalberdSpike" + spikeIndex;
                player.getPersistentData().putDouble(key + "X", spikePos.x);
                player.getPersistentData().putDouble(key + "Y", spikePos.y);
                player.getPersistentData().putDouble(key + "Z", spikePos.z);
                
                // Sincronizar picos actuales al cliente
                List<Vec3> currentPositions = new ArrayList<>();
                for (int i = 0; i <= spikeIndex; i++) {
                    String k = "xebHalberdSpike" + i;
                    double px = player.getPersistentData().getDouble(k + "X");
                    double py = player.getPersistentData().getDouble(k + "Y");
                    double pz = player.getPersistentData().getDouble(k + "Z");
                    currentPositions.add(new Vec3(px, py, pz));
                }
                
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new HalberdSpikeSyncPacket(player.getId(), currentPositions, true));
                
                // Sonido de aparición del pico
                level.playSound(null, spikePos.x, spikePos.y, spikePos.z,
                        SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.5F, 1.5F + spikeIndex * 0.1F);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        
        int spikeCount = player.getPersistentData().getInt("xebHalberdSpikeCount");
        
        if (spikeCount <= 0) {
            // Cancelado sin picos
            cleanupChargeState(player);
            if (!level.isClientSide()) {
                XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new HalberdSpikeSyncPacket(player.getId(), new ArrayList<>(), false));
            }
            return;
        }
        
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Vec3 lookDir = player.getLookAngle();
            int levelSpikeFrenzy = stack.getEnchantmentLevel(org.xeb.xeb.enchantment.ModEnchantments.SPIKE_FRENZY.get());
            
            for (int i = 0; i < spikeCount; i++) {
                String key = "xebHalberdSpike" + i;
                double x = player.getPersistentData().getDouble(key + "X");
                double y = player.getPersistentData().getDouble(key + "Y");
                double z = player.getPersistentData().getDouble(key + "Z");
                
                Vec3 spikePos = new Vec3(x, y, z);
                
                // Spawn spike projectile entity
                SpikeProjectileEntity spike = new SpikeProjectileEntity(level, serverPlayer);
                spike.moveTo(spikePos.x, spikePos.y, spikePos.z, player.getYRot(), player.getXRot());
                
                // Velocidad: hacia adelante en la dirección de la mira
                spike.setDeltaMovement(lookDir.scale(SPIKE_LAUNCH_SPEED));
                spike.setOwner(serverPlayer);
                spike.setSpikeIndex(i);
                level.addFreshEntity(spike);

                // Spike Frenzy: Spawn extra adjacent spikes
                if (levelSpikeFrenzy > 0) {
                    Vec3 right = new Vec3(-lookDir.z, 0.0D, lookDir.x).normalize();
                    for (int extra = 1; extra <= levelSpikeFrenzy; extra++) {
                        for (double side : new double[]{-0.8D * extra, 0.8D * extra}) {
                            SpikeProjectileEntity extraSpike = new SpikeProjectileEntity(level, serverPlayer);
                            Vec3 extraSpikePos = spikePos.add(right.scale(side));
                            extraSpike.moveTo(extraSpikePos.x, extraSpikePos.y, extraSpikePos.z, player.getYRot(), player.getXRot());
                            extraSpike.setDeltaMovement(lookDir.scale(SPIKE_LAUNCH_SPEED));
                            extraSpike.setOwner(serverPlayer);
                            extraSpike.setSpikeIndex(i);
                            level.addFreshEntity(extraSpike);
                        }
                    }
                }
            }
            
            // Sonido de lanzamiento
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.5F, 0.5F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
            
            // Sincronizar fin de carga
            XEBNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new HalberdSpikeSyncPacket(player.getId(), new ArrayList<>(), false));
        }
        
        cleanupChargeState(player);
    }
    
    private void cleanupChargeState(Player player) {
        var data = player.getPersistentData();
        data.remove("xebHalberdChargeTicks");
        int count = data.getInt("xebHalberdSpikeCount");
        for (int i = 0; i < count; i++) {
            String key = "xebHalberdSpike" + i;
            data.remove(key + "X");
            data.remove(key + "Y");
            data.remove(key + "Z");
        }
        data.remove("xebHalberdSpikeCount");
    }
}
