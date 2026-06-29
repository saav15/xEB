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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
