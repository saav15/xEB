package org.xeb.xeb;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.xeb.xeb.attribute.ModAttributes;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.buff.impl.*;
import org.xeb.xeb.compat.ModCompatManager;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.entity.ModEntities;
import org.xeb.xeb.entity.EliteFlyEntity;
import org.xeb.xeb.entity.EliteFlyModel;
import org.xeb.xeb.network.XEBNetwork;
import org.xeb.xeb.render.*;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(Xeb.MODID)
public class Xeb {
    public static final String MODID = "xeb";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Xeb() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register configuration
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, Config.SPEC);

        // Register custom systems
        ModAttributes.register(modEventBus);
        ModEffects.register(modEventBus);
        ModEntities.register(modEventBus);
        org.xeb.xeb.item.ModItems.register(modEventBus);
        org.xeb.xeb.item.ModCreativeModeTabs.register(modEventBus);

        // Register lifecycle listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::onAttributeModification);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register network channel
        XEBNetwork.register();

        // Register all 28 buffs
        registerAllBuffs();

        LOGGER.info("xEB (xd Elite Buffs) loaded!");
    }

    private void registerAllBuffs() {
        EliteBuffRegistry.register(new SpikyBuff());
        EliteBuffRegistry.register(new ReactiveBuff());
        EliteBuffRegistry.register(new DamagingBuff());
        EliteBuffRegistry.register(new ToughBuff());
        EliteBuffRegistry.register(new ShieldedBuff());
        EliteBuffRegistry.register(new ProtectedBuff());
        EliteBuffRegistry.register(new SpeedyBuff());
        EliteBuffRegistry.register(new FlamingBuff());
        EliteBuffRegistry.register(new CreepyBuff());
        EliteBuffRegistry.register(new LuckyBuff());
        EliteBuffRegistry.register(new StaticBuff());
        EliteBuffRegistry.register(new BouncyBuff());
        EliteBuffRegistry.register(new MirrorBuff());
        EliteBuffRegistry.register(new ResonantBuff());
        EliteBuffRegistry.register(new UndyingBuff());
        EliteBuffRegistry.register(new HealthyBuff());
        EliteBuffRegistry.register(new SandyBuff());
        EliteBuffRegistry.register(new InfestedBuff());
        EliteBuffRegistry.register(new AbsorbentBuff());
        EliteBuffRegistry.register(new DepressingBuff());
        EliteBuffRegistry.register(new SlightlyDepressingBuff());
        EliteBuffRegistry.register(new EvolvingBuff());
        EliteBuffRegistry.register(new PlowBuff());
        EliteBuffRegistry.register(new MegaBuff());
        EliteBuffRegistry.register(new MadBuff());
        EliteBuffRegistry.register(new TwinBuff());
        EliteBuffRegistry.register(new HardyBuff());
        EliteBuffRegistry.register(new StickyBuff());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize cross-mod compatibility hooks
            ModCompatManager.init();
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        org.xeb.xeb.command.XebCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ELITE_FLY.get(), EliteFlyEntity.createAttributes().build());
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<?> entityType : event.getTypes()) {
            EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) entityType;
            if (event.has(livingType, Attributes.MAX_HEALTH)) {
                event.add(livingType, ModAttributes.MANA.get(), 20.0D);
            }
        }
    }

    public static LivingEntity getEntityFromReplacedRenderer(software.bernie.geckolib.renderer.GeoReplacedEntityRenderer<?, ?> renderer) {
        try {
            Class<?> currentClass = renderer.getClass();
            while (currentClass != null) {
                for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                    if (net.minecraft.world.entity.Entity.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object val = field.get(renderer);
                        if (val instanceof LivingEntity living) {
                            return living;
                        }
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to extract entity from GeoReplacedEntityRenderer: {}", e.getMessage());
        }
        return null;
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("xEB Client Setup Complete.");
        }

        @SubscribeEvent
        public static void registerKeyMappings(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(org.xeb.xeb.client.ModKeyMappings.ACTUAR_1_KEY);
            event.register(org.xeb.xeb.client.ModKeyMappings.ACTUAR_2_KEY);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // Register client entity renderer for EliteFly using software.bernie.geckolib.renderer.GeoEntityRenderer
            event.registerEntityRenderer(ModEntities.ELITE_FLY.get(),
                    org.xeb.xeb.render.EliteFlyRenderer::new);
            // Sparkle arrowhead renderer with trail
            event.registerEntityRenderer(ModEntities.SPARKLE.get(),
                    org.xeb.xeb.render.SparkleRenderer::new);
            event.registerEntityRenderer(ModEntities.HOT_POTATO_PROJECTILE.get(),
                    net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
            event.registerEntityRenderer(ModEntities.DEMON_CORE.get(),
                    net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        }

        private static final java.util.Set<Object> patchedRenderers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        @SuppressWarnings({"unchecked", "rawtypes"})
        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            // Helper lambda: patch a single LivingEntityRenderer
            java.util.function.Consumer<EntityRenderer<?>> patchRenderer = (renderer) -> {
                if (renderer == null || !patchedRenderers.add(renderer)) return;
                if (renderer instanceof software.bernie.geckolib.renderer.GeoRenderer geoRenderer) {
                    try {
                        java.lang.reflect.Method addRenderLayerMethod = null;
                        Class<?> c = geoRenderer.getClass();
                        while (c != null && addRenderLayerMethod == null) {
                            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                                if (m.getName().equals("addRenderLayer") && m.getParameterCount() == 1) {
                                    addRenderLayerMethod = m;
                                    break;
                                }
                            }
                            c = c.getSuperclass();
                        }
                        if (addRenderLayerMethod != null) {
                            addRenderLayerMethod.setAccessible(true);
                            addRenderLayerMethod.invoke(geoRenderer, new MedallionGeoLayer(geoRenderer));
                            addRenderLayerMethod.invoke(geoRenderer, new MobColorGeoLayer(geoRenderer));
                            addRenderLayerMethod.invoke(geoRenderer, new GlowEyeGeoLayer(geoRenderer));
                        }
                    } catch (Exception e) {
                        LOGGER.warn("xEB: Failed to add GeckoLib layers: " + e.getMessage());
                    }
                } else if (renderer instanceof LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new MedallionRenderLayer<>(livingRenderer));
                    livingRenderer.addLayer(new MobColorOverlay<>(livingRenderer));
                    livingRenderer.addLayer(new GlowEyeOverlay<>(livingRenderer));
                    livingRenderer.addLayer(new org.xeb.xeb.render.DoomfistRenderLayer<>(livingRenderer));
                }
            };

            // Add layers to player skins
            for (String skin : event.getSkins()) {
                patchRenderer.accept(event.getSkin(skin));
            }

            // Explicitly patch key vanilla mobs that may be missed by the generic loop
            EntityType<?>[] vanillaMobs = {
                EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                EntityType.CAVE_SPIDER, EntityType.ENDERMAN, EntityType.BLAZE, EntityType.WITCH,
                EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.WITHER, EntityType.WARDEN,
                EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY, EntityType.WITHER_SKELETON,
                EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.GHAST,
                EntityType.MAGMA_CUBE, EntityType.SLIME, EntityType.SHULKER, EntityType.PHANTOM,
                EntityType.EVOKER, EntityType.ZOGLIN
            };
            for (EntityType<?> mobType : vanillaMobs) {
                try {
                    patchRenderer.accept(event.getRenderer((EntityType<? extends LivingEntity>) mobType));
                } catch (Exception e) {
                    LOGGER.debug("[xEB] Could not patch renderer for vanilla mob {}: {}", mobType, e.getMessage());
                }
            }

            // Generic loop for all remaining registered living entity renderers (modded mobs)
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
                try {
                    EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
                    EntityRenderer<?> renderer = null;
                    try {
                        renderer = event.getRenderer(livingType);
                    } catch (Exception e) {
                        continue;
                    }
                    patchRenderer.accept(renderer);
                } catch (Exception e) {
                    LOGGER.debug("[xEB] Skipping entity type during layer patching: {}", e.getMessage());
                }
            }
        }
    }
}
