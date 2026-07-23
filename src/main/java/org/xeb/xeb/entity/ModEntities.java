package org.xeb.xeb.entity;

import org.xeb.xeb.Xeb;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Xeb.MODID);

    public static final RegistryObject<EntityType<EliteFlyEntity>> ELITE_FLY = ENTITY_TYPES.register("elite_fly",
            () -> EntityType.Builder.of(EliteFlyEntity::new, MobCategory.MONSTER)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .build("elite_fly")
    );

    public static final RegistryObject<EntityType<SparkleEntity>> SPARKLE = ENTITY_TYPES.register("sparkle",
            () -> EntityType.Builder.<SparkleEntity>of(SparkleEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .build("sparkle")
    );

    public static final RegistryObject<EntityType<HotPotatoProjectileEntity>> HOT_POTATO_PROJECTILE = ENTITY_TYPES.register("hot_potato_projectile",
            () -> EntityType.Builder.<HotPotatoProjectileEntity>of(HotPotatoProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("hot_potato_projectile")
    );

    public static final RegistryObject<EntityType<DemonCoreEntity>> DEMON_CORE = ENTITY_TYPES.register("demon_core",
            () -> EntityType.Builder.<DemonCoreEntity>of(DemonCoreEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("demon_core")
    );

    public static final RegistryObject<EntityType<WitherfistEntity>> WITHERFIST = ENTITY_TYPES.register("witherfist",
            () -> EntityType.Builder.of(WitherfistEntity::new, MobCategory.MONSTER)
                    .sized(0.72F, 2.59F) // Same as vanilla WitherSkeleton
                    .clientTrackingRange(10)
                    .build("witherfist")
    );

    public static final RegistryObject<EntityType<TankWitherfistEntity>> TANKWITHERFIST = ENTITY_TYPES.register("tankwitherfist",
            () -> EntityType.Builder.of(TankWitherfistEntity::new, MobCategory.MONSTER)
                    .sized(0.72F, 2.59F) // Same as vanilla WitherSkeleton
                    .clientTrackingRange(10)
                    .build("tankwitherfist")
    );

    public static final RegistryObject<EntityType<MiniLaserProjectileEntity>> MINI_LASER = ENTITY_TYPES.register("mini_laser",
            () -> EntityType.Builder.<MiniLaserProjectileEntity>of(MiniLaserProjectileEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("mini_laser")
    );

    public static final RegistryObject<EntityType<FlowerProjectileEntity>> FLOWER_PROJECTILE = ENTITY_TYPES.register("flower_projectile",
            () -> EntityType.Builder.<FlowerProjectileEntity>of(FlowerProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("flower_projectile")
    );

    public static final RegistryObject<EntityType<FlowerPelletEntity>> FLOWER_PELLET = ENTITY_TYPES.register("flower_pellet",
            () -> EntityType.Builder.<FlowerPelletEntity>of(FlowerPelletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("flower_pellet")
    );

    public static final RegistryObject<EntityType<CrazyDiamondEntity>> CRAZY_DIAMOND = ENTITY_TYPES.register("crazy_diamond",
            () -> EntityType.Builder.of(CrazyDiamondEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("crazy_diamond")
    );

    public static final RegistryObject<EntityType<RestoreProjectileEntity>> RESTORE_PROJECTILE = ENTITY_TYPES.register("restore_projectile",
            () -> EntityType.Builder.<RestoreProjectileEntity>of(RestoreProjectileEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("restore_projectile")
    );

    public static final RegistryObject<EntityType<org.xeb.xeb.entity.TearsProjectileEntity>> TEARS_PROJECTILE = ENTITY_TYPES.register("tears_projectile",
            () -> EntityType.Builder.<org.xeb.xeb.entity.TearsProjectileEntity>of(org.xeb.xeb.entity.TearsProjectileEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("tears_projectile")
    );

    public static final RegistryObject<EntityType<HotPokerEntity>> HOT_POKER = ENTITY_TYPES.register("hot_poker",
            () -> EntityType.Builder.of(HotPokerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("hot_poker")
    );

    public static final RegistryObject<EntityType<MechaVulcanProjectileEntity>> MECHA_VULCAN_PROJECTILE = ENTITY_TYPES.register("mecha_vulcan_projectile",
            () -> EntityType.Builder.<MechaVulcanProjectileEntity>of(MechaVulcanProjectileEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("mecha_vulcan_projectile")
    );

    public static final RegistryObject<EntityType<HomingMissileEntity>> HOMING_MISSILE = ENTITY_TYPES.register("homing_missile",
            () -> EntityType.Builder.<HomingMissileEntity>of(HomingMissileEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("homing_missile")
    );

    public static final RegistryObject<EntityType<SpikeProjectileEntity>> SPIKE_PROJECTILE = ENTITY_TYPES.register("spike_projectile",
            () -> EntityType.Builder.<SpikeProjectileEntity>of(SpikeProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 1.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("spike_projectile"));

    public static final RegistryObject<EntityType<ShatteredRiftEntity>> SHATTERED_RIFT = ENTITY_TYPES.register("shattered_rift",
            () -> EntityType.Builder.<ShatteredRiftEntity>of(ShatteredRiftEntity::new, MobCategory.MISC)
                    .sized(3.0F, 0.2F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("shattered_rift"));

    public static final RegistryObject<EntityType<StevenBossEntity>> STEVEN_BOSS = ENTITY_TYPES.register("steven_boss",
            () -> EntityType.Builder.of(StevenBossEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(10)
                    .build("steven_boss")
    );

    public static final RegistryObject<EntityType<StevenPortalEntity>> STEVEN_PORTAL = ENTITY_TYPES.register("steven_portal",
            () -> EntityType.Builder.<StevenPortalEntity>of(StevenPortalEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .build("steven_portal")
    );

    public static final RegistryObject<EntityType<StevenCloneEntity>> STEVEN_CLONE = ENTITY_TYPES.register("steven_clone",
            () -> EntityType.Builder.<StevenCloneEntity>of(StevenCloneEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(10)
                    .build("steven_clone")
    );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
