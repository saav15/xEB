package org.xeb.xeb.event;

import org.xeb.xeb.Xeb;
import org.xeb.xeb.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HolyShieldClientHandler {

    public static boolean isHolyShieldActive(LivingEntity entity) {
        if (entity == null) return false;
        // Potion effect presence is synchronized to the client and represents the active shield state
        return entity.hasEffect(ModEffects.HOLY_SHIELD.get());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    // Update doomfist client-side dash timer and particles
                    net.minecraft.nbt.CompoundTag tag = living.getPersistentData();
                    if (tag.getBoolean("xebDoomfistDashing")) {
                        int timer = tag.getInt("xebDoomfistDashTimer");
                        if (timer > 0) {
                            tag.putInt("xebDoomfistDashTimer", timer - 1);
                        } else {
                            tag.remove("xebDoomfistDashing");
                            tag.remove("xebDoomfistDashTimer");
                            tag.remove("xebDoomfistChargeRatio");
                        }

                        // Spawn cool blue trail particles behind the player
                        for (int i = 0; i < 2; i++) {
                            double px = living.getX() + (living.getRandom().nextDouble() - 0.5D) * living.getBbWidth();
                            double py = living.getY() + living.getRandom().nextDouble() * living.getBbHeight();
                            double pz = living.getZ() + (living.getRandom().nextDouble() - 0.5D) * living.getBbWidth();

                            // Motion opposite to velocity
                            net.minecraft.world.phys.Vec3 vel = living.getDeltaMovement();
                            double mx = -vel.x * 0.2D + (living.getRandom().nextDouble() - 0.5D) * 0.05D;
                            double my = -vel.y * 0.2D + (living.getRandom().nextDouble() - 0.5D) * 0.05D;
                            double mz = -vel.z * 0.2D + (living.getRandom().nextDouble() - 0.5D) * 0.05D;

                            // Spawn red flames for Doomfist V2, blue flames for Doomfist V1
                            boolean holdsV2 = living.getMainHandItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get()) || living.getOffhandItem().is(org.xeb.xeb.item.ModItems.DOOMFIST_V2.get());
                            net.minecraft.core.particles.SimpleParticleType flameType = holdsV2 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;

                            mc.level.addParticle(flameType, px, py, pz, mx, my, mz);
                            if (living.getRandom().nextFloat() < 0.4F) {
                                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, mx, my, mz);
                            }
                        }
                    }

                    // --- Doomfist ability client-side ticks ---
                    if (tag.contains("xebUppercutFloatTicks")) {
                        int floatTicks = tag.getInt("xebUppercutFloatTicks");
                        if (floatTicks > 0) {
                            tag.putInt("xebUppercutFloatTicks", floatTicks - 1);
                            if (!living.onGround()) {
                                net.minecraft.world.phys.Vec3 motion = living.getDeltaMovement();
                                // Only lock Y velocity on client once target reaches the jump peak (velocity <= 0.0D)
                                if (motion.y <= 0.0D) {
                                    living.setDeltaMovement(motion.x, 0.0D, motion.z);
                                }
                            } else {
                                // Prevent early clearing on launch tick
                                if (floatTicks < 35) {
                                    tag.remove("xebUppercutFloatTicks");
                                }
                            }
                        } else {
                            tag.remove("xebUppercutFloatTicks");
                        }
                    }

                    if (living instanceof net.minecraft.world.entity.player.Player player) {
                        // Tick client-side cooldowns for HUD display
                        if (tag.contains("xebUppercutCooldownTicks")) {
                            int cd = tag.getInt("xebUppercutCooldownTicks");
                            if (cd > 0) {
                                tag.putInt("xebUppercutCooldownTicks", cd - 1);
                            } else {
                                tag.remove("xebUppercutCooldownTicks");
                            }
                        }
                        if (tag.contains("xebSlamCooldownTicks")) {
                            int cd = tag.getInt("xebSlamCooldownTicks");
                            if (cd > 0) {
                                tag.putInt("xebSlamCooldownTicks", cd - 1);
                            } else {
                                tag.remove("xebSlamCooldownTicks");
                            }
                        }

                        if (tag.contains("xebSlamState")) {
                            int slamState = tag.getInt("xebSlamState");
                            if (slamState == 1) { // Casting
                                int timer = tag.getInt("xebSlamTimer");
                                if (timer > 0) {
                                    tag.putInt("xebSlamTimer", timer - 1);
                                    player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                                }
                                if (player == mc.player) {
                                    drawHologram(player, mc.level);
                                }
                            } else if (slamState == 2) { // Slamming downward
                                if (tag.contains("xebSlamTargetX")) {
                                    double targetX = tag.getDouble("xebSlamTargetX");
                                    double targetY = tag.getDouble("xebSlamTargetY");
                                    double targetZ = tag.getDouble("xebSlamTargetZ");
                                    net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(targetX, targetY, targetZ);
                                    net.minecraft.world.phys.Vec3 slamVec = targetPos.subtract(player.position());
                                    
                                    // Guide player on client to exactly match server path and prevent rubberbanding
                                    if (slamVec.lengthSqr() > 0.01D) {
                                        net.minecraft.world.phys.Vec3 dir = slamVec.normalize();
                                        player.setDeltaMovement(dir.scale(1.8D));
                                    }
                                }
                            }
                        }
                    }

                    if (isHolyShieldActive(living)) {
                        double time = (living.tickCount + mc.getFrameTime()) * 0.15D;
                        double radius = living.getBbWidth() * 0.7D;

                        // 1. Rotating circle particle: GLOW (diffused cyan/blue) at the feet
                        double xOffset = Math.cos(time) * radius;
                        double zOffset = Math.sin(time) * radius;
                        mc.level.addParticle(ParticleTypes.GLOW,
                                living.getX() + xOffset, living.getY() + 0.05D, living.getZ() + zOffset,
                                0.0D, 0.0D, 0.0D);

                        // 2. Secondary white sparkle: END_ROD at the opposite side of the circle
                        if (living.getRandom().nextFloat() < 0.15F) {
                            double xOffset2 = Math.cos(time + Math.PI) * radius;
                            double zOffset2 = Math.sin(time + Math.PI) * radius;
                            mc.level.addParticle(ParticleTypes.END_ROD,
                                    living.getX() + xOffset2, living.getY() + 0.05D, living.getZ() + zOffset2,
                                    0.0D, 0.01D, 0.0D);
                        }
                    }
                }
            }
        }
    }

    private static void drawHologram(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level) {
        net.minecraft.world.phys.Vec3 start = player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = start.add(look.scale(15.0D));
        
        net.minecraft.world.phys.BlockHitResult raycast = level.clip(new net.minecraft.world.level.ClipContext(
                start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        
        net.minecraft.world.phys.Vec3 landPos;
        if (raycast.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            landPos = raycast.getLocation();
        } else {
            // Missed block, project downward from the end point to stick to the floor
            net.minecraft.world.phys.Vec3 downEnd = new net.minecraft.world.phys.Vec3(end.x, end.y - 30.0D, end.z);
            net.minecraft.world.phys.BlockHitResult groundRay = level.clip(new net.minecraft.world.level.ClipContext(
                    end, downEnd, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            landPos = groundRay.getLocation();
        }
        
        net.minecraft.world.phys.Vec3 horizLook = new net.minecraft.world.phys.Vec3(look.x, 0.0D, look.z).normalize();
        double angleRange = Math.toRadians(30.0D);
        
        // Dotted cone line drawing scaled to 6.0 blocks
        for (double dist = 0.5D; dist <= 6.0D; dist += 0.5D) {
            net.minecraft.world.phys.Vec3 leftOffset = rotateY(horizLook, -angleRange).scale(dist);
            spawnHologramParticle(level, landPos.add(leftOffset));
            
            net.minecraft.world.phys.Vec3 rightOffset = rotateY(horizLook, angleRange).scale(dist);
            spawnHologramParticle(level, landPos.add(rightOffset));
            
            net.minecraft.world.phys.Vec3 centerOffset = horizLook.scale(dist);
            spawnHologramParticle(level, landPos.add(centerOffset));
        }
        
        // Arc at the end of the cone (dist = 6.0)
        for (double a = -angleRange; a <= angleRange; a += Math.toRadians(10.0D)) {
            net.minecraft.world.phys.Vec3 arcOffset = rotateY(horizLook, a).scale(6.0D);
            spawnHologramParticle(level, landPos.add(arcOffset));
        }
    }

    private static net.minecraft.world.phys.Vec3 rotateY(net.minecraft.world.phys.Vec3 vec, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new net.minecraft.world.phys.Vec3(vec.x * cos - vec.z * sin, vec.y, vec.x * sin + vec.z * cos);
    }

    private static void spawnHologramParticle(net.minecraft.world.level.Level level, net.minecraft.world.phys.Vec3 pos) {
        net.minecraft.core.BlockPos blockPos = new net.minecraft.core.BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        double groundY = pos.y;
        for (int dy = 2; dy >= -3; dy--) {
            net.minecraft.core.BlockPos check = blockPos.above(dy);
            if (level.getBlockState(check).isCollisionShapeFullBlock(level, check) && !level.getBlockState(check.above()).isCollisionShapeFullBlock(level, check.above())) {
                groundY = check.getY() + 1.05D;
                break;
            }
        }
        // Using ELECTRIC_SPARK as a lightweight, clean blue particle model instead of flamas
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, groundY, pos.z, 0.0D, 0.0D, 0.0D);
    }
}
