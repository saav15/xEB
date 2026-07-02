package org.xeb.xeb.entity;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.xeb.xeb.item.ModItems;
import java.lang.reflect.Method;

public class HotPotatoProjectileEntity extends ThrowableItemProjectile {
    private static final Logger LOGGER = LogUtils.getLogger();

    public HotPotatoProjectileEntity(EntityType<? extends HotPotatoProjectileEntity> type, Level level) {
        super(type, level);
    }

    public HotPotatoProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.HOT_POTATO_PROJECTILE.get(), shooter, level);
    }

    public HotPotatoProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.HOT_POTATO_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.HOT_POTATO.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target && !this.level().isClientSide()) {
            boolean equipped = false;
            // 1. Try to auto-equip in Curios slot
            try {
                Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Method getHelper = apiClass.getMethod("getCuriosHelper");
                Object helper = getHelper.invoke(null);
                if (helper != null) {
                    Method getCuriosHandler = helper.getClass().getMethod("getCuriosHandler", LivingEntity.class);
                    Object lazyOpt = getCuriosHandler.invoke(helper, target);
                    if (lazyOpt != null) {
                        Method isPresent = lazyOpt.getClass().getMethod("isPresent");
                        if ((Boolean) isPresent.invoke(lazyOpt)) {
                            Method get = lazyOpt.getClass().getMethod("orElseThrow");
                            Object handlerObj = get.invoke(lazyOpt);
                            if (handlerObj != null) {
                                Method getCurios = handlerObj.getClass().getMethod("getCurios");
                                java.util.Map<String, Object> curiosMap = (java.util.Map<String, Object>) getCurios.invoke(handlerObj);
                                ItemStack potatoStack = new ItemStack(ModItems.HOT_POTATO.get());
                                for (Object stacksHandler : curiosMap.values()) {
                                    Method getStacks = stacksHandler.getClass().getMethod("getStacks");
                                    Object stacksObj = getStacks.invoke(stacksHandler);
                                    if (stacksObj != null) {
                                        Method getSlots = stacksObj.getClass().getMethod("getSlots");
                                        int slotsCount = (Integer) getSlots.invoke(stacksObj);
                                        Method getStackInSlot = stacksObj.getClass().getMethod("getStackInSlot", int.class);
                                        Method insertItem = stacksObj.getClass().getMethod("insertItem", int.class, ItemStack.class, boolean.class);
                                        for (int i = 0; i < slotsCount; i++) {
                                            ItemStack existing = (ItemStack) getStackInSlot.invoke(stacksObj, i);
                                            if (existing == null || existing.isEmpty()) {
                                                ItemStack remainder = (ItemStack) insertItem.invoke(stacksObj, i, potatoStack, false);
                                                if (remainder == null || remainder.isEmpty()) {
                                                    equipped = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (equipped) break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Curios auto-equip failed for Hot Potato on entity {}: {}", target.getId(), e.getMessage());
            }

            // 2. If not equipped, put in inventory (or hand, or drop)
            if (!equipped) {
                ItemStack potatoStack = new ItemStack(ModItems.HOT_POTATO.get());
                if (target instanceof Player player) {
                    if (!player.getInventory().add(potatoStack)) {
                        player.drop(potatoStack, false);
                    }
                } else {
                    if (target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty()) {
                        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, potatoStack);
                    } else if (target.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).isEmpty()) {
                        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, potatoStack);
                    } else {
                        target.spawnAtLocation(potatoStack);
                    }
                }
            }

            // Play fire charge sound on target
            this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            if (result.getType() == HitResult.Type.BLOCK) {
                this.spawnAtLocation(new ItemStack(ModItems.HOT_POTATO.get()));
                this.discard();
            }
        }
    }
}
