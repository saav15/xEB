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

        // Register client config screen factory
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> registerClientConfigScreen());

        // Register custom systems
        ModAttributes.register(modEventBus);
        ModEffects.register(modEventBus);
        ModEntities.register(modEventBus);
        org.xeb.xeb.sound.ModSounds.register(modEventBus);
        org.xeb.xeb.item.ModItems.register(modEventBus);
        org.xeb.xeb.item.ModCreativeModeTabs.register(modEventBus);

        // Register lifecycle listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);
        modEventBus.addListener(this::onAttributeModification);
        modEventBus.addListener(this::enqueueIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register network channel
        XEBNetwork.register();

        // Register all 28 buffs
        registerAllBuffs();

        LOGGER.info("xEB (xd Elite Buffs) loaded!");
    }

    private static void registerClientConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new org.xeb.xeb.client.gui.HUDPositionScreen(parent)
                )
        );
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
            net.minecraft.world.entity.SpawnPlacements.register(
                    ModEntities.HOT_POKER.get(),
                    net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    org.xeb.xeb.entity.HotPokerEntity::checkHotPokerSpawnRules
            );
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        org.xeb.xeb.command.XebCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ELITE_FLY.get(), EliteFlyEntity.createAttributes().build());
        event.put(ModEntities.WITHERFIST.get(), org.xeb.xeb.entity.WitherfistEntity.createAttributes().build());
        event.put(ModEntities.TANKWITHERFIST.get(), org.xeb.xeb.entity.TankWitherfistEntity.createAttributes().build());
        event.put(ModEntities.CRAZY_DIAMOND.get(), org.xeb.xeb.entity.CrazyDiamondEntity.createAttributes().build());
        event.put(ModEntities.HOT_POKER.get(), org.xeb.xeb.entity.HotPokerEntity.createAttributes().build());
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

    private void enqueueIMC(final net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent event) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            try {
                Class<?> builderClass = Class.forName("top.theillusivec4.curios.api.SlotTypeMessage$Builder");
                Object builder = builderClass.getConstructor(String.class).newInstance("ultimate");
                builderClass.getMethod("size", int.class).invoke(builder, 1);
                builderClass.getMethod("icon", net.minecraft.resources.ResourceLocation.class).invoke(builder, new net.minecraft.resources.ResourceLocation("curios", "slot/empty_charm_slot"));
                Object message = builderClass.getMethod("build").invoke(builder);
                net.minecraftforge.fml.InterModComms.sendTo("curios", "register_type", () -> message);
                LOGGER.info("Successfully registered custom Curios slot 'ultimate' via IMC.");
            } catch (Exception e) {
                LOGGER.error("Failed to register custom Curios slot 'ultimate' via IMC", e);
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
            // Ignore
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
            event.register(org.xeb.xeb.client.ModKeyMappings.ACTIVA_1_KEY);
            event.register(org.xeb.xeb.client.ModKeyMappings.ACTIVA_2_KEY);
            event.register(org.xeb.xeb.client.ModKeyMappings.ACTIVA_3_KEY);
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
                    org.xeb.xeb.render.DemonCoreRenderer::new);
            event.registerEntityRenderer(ModEntities.WITHERFIST.get(),
                    net.minecraft.client.renderer.entity.WitherSkeletonRenderer::new);
            event.registerEntityRenderer(ModEntities.TANKWITHERFIST.get(),
                    net.minecraft.client.renderer.entity.WitherSkeletonRenderer::new);
            // Mini-laser projectile (Optic Blast left-click)
            event.registerEntityRenderer(ModEntities.MINI_LASER.get(),
                    org.xeb.xeb.client.renderer.MiniLaserRenderer::new);
            event.registerEntityRenderer(ModEntities.FLOWER_PROJECTILE.get(),
                    org.xeb.xeb.client.renderer.FlowerProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.FLOWER_PELLET.get(),
                    org.xeb.xeb.client.renderer.FlowerPelletRenderer::new);
            event.registerEntityRenderer(ModEntities.CRAZY_DIAMOND.get(),
                    org.xeb.xeb.client.renderer.CrazyDiamondRenderer::new);
            event.registerEntityRenderer(ModEntities.RESTORE_PROJECTILE.get(),
                    org.xeb.xeb.client.renderer.RestoreProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.TEARS_PROJECTILE.get(),
                    org.xeb.xeb.client.renderer.TearsProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.HOT_POKER.get(),
                    org.xeb.xeb.client.renderer.HotPokerGeoRenderer::new);
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
                    livingRenderer.addLayer(new org.xeb.xeb.render.OpticBlastPlayerLayer<>(livingRenderer));
                }
            };

            // Add layers to player skins
            for (String skin : event.getSkins()) {
                patchRenderer.accept(event.getSkin(skin));
            }

            // Explicitly patch key vanilla mobs that may be missed by the generic loop
            try { patchRenderer.accept(event.getRenderer(EntityType.CREEPER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.ZOMBIE)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.SKELETON)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.SPIDER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.CAVE_SPIDER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.ENDERMAN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.BLAZE)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.WITCH)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.GUARDIAN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.ELDER_GUARDIAN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.WITHER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.WARDEN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.PIGLIN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.PIGLIN_BRUTE)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.HOGLIN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.ZOMBIFIED_PIGLIN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.DROWNED)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.HUSK)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.STRAY)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.WITHER_SKELETON)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.VINDICATOR)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.PILLAGER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.RAVAGER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.GHAST)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.MAGMA_CUBE)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.SLIME)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.SHULKER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.PHANTOM)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.EVOKER)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(EntityType.ZOGLIN)); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(ModEntities.WITHERFIST.get())); } catch (Exception ignored) {}
            try { patchRenderer.accept(event.getRenderer(ModEntities.TANKWITHERFIST.get())); } catch (Exception ignored) {}

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
                    // Ignore non-living entity types or types without renderers
                }
            }
        }
    }
}
