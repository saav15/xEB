package org.xeb.xeb;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.xeb.xeb.attribute.ModAttributes;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.buff.impl.combat.*;
import org.xeb.xeb.buff.impl.defense.*;
import org.xeb.xeb.buff.impl.utility.*;
import org.xeb.xeb.buff.impl.special.*;
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

    @SuppressWarnings("deprecation")
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
        org.xeb.xeb.block.ModBlocks.register(modEventBus);
        org.xeb.xeb.fluid.ModFluids.register(modEventBus);
        org.xeb.xeb.fluid.ModFluidTypes.FLUID_TYPES.register(modEventBus);

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
        registerAllEssences();

        LOGGER.info("xEB (xd Elite Buffs) loaded!");
    }

    @SuppressWarnings("deprecation")
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
        // NOTE: Curios slot registration via IMC+Reflection is deprecated and fragile in 1.20.1.
        // The 'ultimate' slot should be registered via a Curios Datapack (data/curios/slots/ultimate.json)
        // or via a pack.mcmeta-compatible resource pack included in the modpack.
        // Example JSON for data/curios/slots/ultimate.json:
        // { "size": 1, "operation": "SET", "icon": "curios:slot/empty_charm_slot" }
        //
        // If Curios API is on the classpath at compile time, use:
        //   top.theillusivec4.curios.api.CuriosApi.enqueueSlotType(event, SlotTypePreset.CHARM.getIdentifier());
        // For a custom slot, use their SlotTypeMessage builder directly (no reflection needed).
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            LOGGER.info("xEB: Curios detected. Register the 'ultimate' slot via Datapack (data/curios/slots/ultimate.json).");
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
            event.register(org.xeb.xeb.client.ModKeyMappings.FLOURISH_KEY);
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
            event.registerEntityRenderer(ModEntities.MECHA_VULCAN_PROJECTILE.get(),
                    org.xeb.xeb.client.renderer.MechaVulcanProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.HOMING_MISSILE.get(),
                    net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        }

        private static final java.util.Set<Object> patchedRenderers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        @SuppressWarnings({"unchecked", "rawtypes"})
        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            // Helper lambda: patch a single LivingEntityRenderer
            java.util.function.Consumer<EntityRenderer<?>> patchRenderer = (renderer) -> {
                if (renderer == null || !patchedRenderers.add(renderer)) return;

                // --- CASO 1: Entidades nativas de otros mods que USAN GeckoLib ---
                // GeoEntityRenderer tiene 1 param de tipo en GeckoLib 2.x (1.20.1)
                if (renderer instanceof software.bernie.geckolib.renderer.GeoEntityRenderer<?> geoEntity) {
                    geoEntity.addRenderLayer(new MedallionGeoLayer(geoEntity));
                    geoEntity.addRenderLayer(new MobColorGeoLayer(geoEntity));
                    geoEntity.addRenderLayer(new GlowEyeGeoLayer(geoEntity));
                }
                // --- CASO 2: Entidades Vanilla reemplazadas por GeckoLib (ej: Better Animals) ---
                else if (renderer instanceof software.bernie.geckolib.renderer.GeoReplacedEntityRenderer<?, ?> geoReplaced) {
                    geoReplaced.addRenderLayer(new MedallionGeoLayer(geoReplaced));
                    geoReplaced.addRenderLayer(new MobColorGeoLayer(geoReplaced));
                    geoReplaced.addRenderLayer(new GlowEyeGeoLayer(geoReplaced));
                }
                // --- CASO 3: Entidades Vanilla o de mods que NO usan GeckoLib ---
                // Tipos raw necesarios: javac no puede unificar dos capturas wildcard independientes
                // con diamond <>. El @SuppressWarnings({"unchecked","rawtypes"}) del método lo cubre.
                else if (renderer instanceof LivingEntityRenderer) {
                    LivingEntityRenderer livingRenderer = (LivingEntityRenderer) renderer;
                    livingRenderer.addLayer(new MedallionRenderLayer(livingRenderer));
                    livingRenderer.addLayer(new MobColorOverlay(livingRenderer));
                    livingRenderer.addLayer(new GlowEyeOverlay(livingRenderer));
                    livingRenderer.addLayer(new org.xeb.xeb.render.DoomfistRenderLayer(livingRenderer));
                    livingRenderer.addLayer(new org.xeb.xeb.render.OpticBlastPlayerLayer(livingRenderer));
                    livingRenderer.addLayer(new org.xeb.xeb.render.HolyDualityRenderLayer(livingRenderer));
                }
            };

            // Add layers to player skins
            for (String skin : event.getSkins()) {
                patchRenderer.accept(event.getSkin(skin));
            }

            // Parchear los mobs vanilla clave (garantizamos el orden; patchedRenderers evita duplicados)
            @SuppressWarnings("unchecked")
            EntityType<? extends LivingEntity>[] vanillaTypes = new EntityType[] {
                EntityType.CREEPER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
                EntityType.CAVE_SPIDER, EntityType.ENDERMAN, EntityType.BLAZE, EntityType.WITCH,
                EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.WITHER, EntityType.WARDEN,
                EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.HOGLIN, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY, EntityType.WITHER_SKELETON,
                EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.GHAST,
                EntityType.MAGMA_CUBE, EntityType.SLIME, EntityType.SHULKER, EntityType.PHANTOM,
                EntityType.EVOKER, EntityType.ZOGLIN,
                ModEntities.WITHERFIST.get(), ModEntities.TANKWITHERFIST.get()
            };
            for (EntityType<? extends LivingEntity> type : vanillaTypes) {
                try { patchRenderer.accept(event.getRenderer(type)); } catch (Exception ignored) {}
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
                    // Ignore non-living entity types or types without renderers
                }
            }
        }
    }

    private void registerAllEssences() {
        String[] buffIds = {
            // combat
            "spiky", "reactive", "damaging", "flaming", "creepy", "mad", "mirror", "plow",
            "resonant", "sandy", "static",
            // defense
            "absorbent", "hardy", "healthy", "protected", "shielded", "tough", "undying",
            // utility
            "bouncy", "evolving", "lucky", "mega", "speedy", "sticky", "twin",
            // special
            "depressing", "infested", "slightly_depressing"
        };
        for (String id : buffIds) {
            org.xeb.xeb.loot.EssenceRegistry.register(id);
        }
        LOGGER.info("xEB: Registered {} essences", buffIds.length);
    }
}
