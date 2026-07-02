package org.xeb.xeb.bot;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.xeb.xeb.Config;
import org.xeb.xeb.bettercombat.BetterCombatAttackController;
import org.xeb.xeb.bettercombat.BetterCombatWeaponAnalyzer;
import org.xeb.xeb.bettercombat.SpecialAbilityHandler;
import org.xeb.xeb.boss.UniversalBossDetector;
import org.xeb.xeb.effect.ModEffects;
import org.xeb.xeb.weapon.*;

import java.util.List;

/**
 * formalizes player bot controls under Madness using state transitions.
 */
@OnlyIn(Dist.CLIENT)
public class PlayerBotStateMachine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static BotState currentState = BotState.IDLE;
    private static int stateTicks = 0;
    private static final HotbarScanner HOTBAR_SCANNER = new HotbarScanner();
    private static LivingEntity currentTarget = null;
    private static int hybridChargeTicks = 0;
    private static int hybridSpecialCooldown = 0;
    private static int rangedDrawTicks = 0;
    
    private static int currentComboStep = 0;
    private static int comboTicksRemaining = 0;
    private static int targetUnreachableTicks = 0;

    private static final java.util.Map<java.util.UUID, java.lang.Long> BLACKLISTED_TARGETS = new java.util.concurrent.ConcurrentHashMap<>();
    private static LivingEntity lastAttackedTarget = null;
    private static int attackAttempts = 0;
    private static float lastTargetHealth = -1.0F;
    private static int lastHurtTimeDetectedTicks = 0;

    private static int meleeSpellCastTicks = 0;
    private static net.minecraft.client.KeyMapping ironCastKey = null;
    private static boolean checkedIronCastKey = false;

    private static int eatingSlot = -1;
    private static boolean isEating = false;

    private static int enderPearlCooldown = 0;
    private static int potionCooldown = 0;
    private static int pearlThrowTicks = 0;
    private static int potionThrowTicks = 0;

    public static BotState getCurrentState() {
        return currentState;
    }

    public static LivingEntity getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Resets the entire state machine state and releases all key bindings.
     */
    public static void reset() {
        currentState = BotState.IDLE;
        stateTicks = 0;
        currentTarget = null;
        hybridChargeTicks = 0;
        hybridSpecialCooldown = 0;
        rangedDrawTicks = 0;
        currentComboStep = 0;
        comboTicksRemaining = 0;
        targetUnreachableTicks = 0;
        HOTBAR_SCANNER.reset();
        lastAttackedTarget = null;
        attackAttempts = 0;
        lastTargetHealth = -1.0F;
        lastHurtTimeDetectedTicks = 0;
        BLACKLISTED_TARGETS.clear();
        meleeSpellCastTicks = 0;
        isEating = false;
        eatingSlot = -1;
        enderPearlCooldown = 0;
        potionCooldown = 0;
        pearlThrowTicks = 0;
        potionThrowTicks = 0;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null) {
                mc.options.keyUse.setDown(false);
                mc.options.keyAttack.setDown(false);
                mc.options.keyJump.setDown(false);
            }
            if (mc != null && mc.player != null && mc.player.isUsingItem() && mc.gameMode != null) {
                mc.gameMode.releaseUsingItem(mc.player);
            }
            // Reset BetterCombat attack controller state and any right-click state.
            BetterCombatAttackController.reset(mc);
            SpecialAbilityHandler.reset();
            KeyMapping castKey = getIronCastKey();
            if (castKey != null) {
                castKey.setDown(false);
            }
        } catch (Exception | LinkageError e) {
            LOGGER.debug("[xEB] Bot reset skipped (unit test or missing Minecraft): {}", e.getMessage());
        }
    }

    /**
     * Ticks the bot state machine, executing actions and performing transitions.
     */
    public static void tick(Minecraft mc, LocalPlayer player) {
        if (player == null || !player.isAlive() || !player.hasEffect(ModEffects.MADNESS.get())) {
            if (currentState != BotState.IDLE) {
                reset();
            }
            return;
        }

        stateTicks++;
        if (hybridSpecialCooldown > 0) hybridSpecialCooldown--;
        if (comboTicksRemaining > -100) comboTicksRemaining--;
        if (enderPearlCooldown > 0) enderPearlCooldown--;
        if (potionCooldown > 0) potionCooldown--;

        // Check if we are currently charging a Doomfist or Incinerator, and release them if fully charged
        if (player.isUsingItem()) {
            ItemStack usingItem = player.getUseItem();
            if (isDoomfistItem(usingItem)) {
                int ticksCharged = usingItem.getUseDuration() - player.getUseItemRemainingTicks();
                if (ticksCharged >= 50) {
                    pressKey(mc.options.keyUse, false); // Release to punch!
                    if (mc.gameMode != null) {
                        mc.gameMode.releaseUsingItem(player);
                    }
                }
            } else {
                String usingStr = usingItem.getItem().toString();
                if (usingStr.contains("the_incinerator")) {
                    int ticksCharged = usingItem.getUseDuration() - player.getUseItemRemainingTicks();
                    if (ticksCharged >= 35) {
                        pressKey(mc.options.keyUse, false); // Release flame sweep!
                        if (mc.gameMode != null) {
                            mc.gameMode.releaseUsingItem(player);
                        }
                    }
                }
            }
        }

        // State Machine transitions
        switch (currentState) {
            case IDLE:
                currentState = BotState.SCANNING;
                stateTicks = 0;
                break;

            case SCANNING:
                findTarget(player);
                if (currentTarget != null) {
                    currentState = BotState.APPROACHING;
                    stateTicks = 0;
                }
                break;

            case APPROACHING:
                validateTarget(player);
                if (currentTarget == null) {
                    currentState = BotState.SCANNING;
                    stateTicks = 0;
                    break;
                }

                double dist = player.distanceTo(currentTarget);
                double reach = getWeaponReach(player);

                // Scan hotbar slots and adjust selected slot
                boolean inMeleeRange = dist <= reach;
                updateStrategicInventory(mc, player, dist, inMeleeRange);

                if (inMeleeRange) {
                    currentState = BotState.ATTACKING_MELEE;
                    stateTicks = 0;
                } else if (dist <= 24.0D && (canUseAtRange(player.getMainHandItem()) || hasChargedFistEffect(player))) {
                    currentState = BotState.ATTACKING_RANGED;
                    stateTicks = 0;
                }
                break;

            case ATTACKING_MELEE:
                validateTarget(player);
                if (currentTarget == null) {
                    currentState = BotState.SCANNING;
                    stateTicks = 0;
                    break;
                }

                double distMelee = player.distanceTo(currentTarget);
                double reachMelee = getWeaponReach(player);

                updateStrategicInventory(mc, player, distMelee, distMelee <= reachMelee);
                if (isEating || hasChargedFistEffect(player)) {
                    currentState = BotState.ATTACKING_RANGED;
                    stateTicks = 0;
                    break;
                }

                if (distMelee > reachMelee) {
                    if (canUseAtRange(player.getMainHandItem())) {
                        currentState = BotState.ATTACKING_RANGED;
                    } else {
                        currentState = BotState.APPROACHING;
                    }
                    stateTicks = 0;
                    break;
                }

                executeMeleeCombat(mc, player);
                break;

            case ATTACKING_RANGED:
                validateTarget(player);
                if (currentTarget == null) {
                    currentState = BotState.SCANNING;
                    stateTicks = 0;
                    break;
                }

                double distRanged = player.distanceTo(currentTarget);
                double reachRanged = getWeaponReach(player);

                updateStrategicInventory(mc, player, distRanged, distRanged <= reachRanged);
                if (isEating || hasChargedFistEffect(player)) {
                    executeRangedCombat(mc, player);
                    break;
                }

                if (distRanged <= reachRanged) {
                    currentState = BotState.ATTACKING_MELEE;
                    stateTicks = 0;
                    break;
                } else if (!canUseAtRange(player.getMainHandItem())) {
                    currentState = BotState.APPROACHING;
                    stateTicks = 0;
                    break;
                }

                executeRangedCombat(mc, player);
                break;
        }

        // Apply chaotic view orientation
        if (currentTarget != null) {
            lookAtTarget(player, currentTarget);
            try {
                mc.gameRenderer.pick(1.0F);
            } catch (Throwable e) {
                LOGGER.debug("[xEB] Failed to pick target for bot: {}", e.getMessage());
            }
        }
    }

    private static void findTarget(LocalPlayer player) {
        long now = System.currentTimeMillis();
        BLACKLISTED_TARGETS.entrySet().removeIf(entry -> entry.getValue() < now);

        double range = 16.0D;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(
                LivingEntity.class, searchBox,
                t -> t != player && t.isAlive() && player.hasLineOfSight(t) &&
                     !UniversalBossDetector.isBlacklisted(t) &&
                     !BLACKLISTED_TARGETS.containsKey(t.getUUID()) &&
                     !(t instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()))
        );

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity e : entities) {
            double dist = player.distanceToSqr(e);
            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }
        currentTarget = closest;
    }

    private static void validateTarget(LocalPlayer player) {
        if (currentTarget == null || !currentTarget.isAlive() || player.distanceToSqr(currentTarget) > 360.0D) {
            currentTarget = null;
            targetUnreachableTicks = 0;
            return;
        }

        double dist = player.distanceTo(currentTarget);
        double reach = getWeaponReach(player);
        boolean hasSight = player.hasLineOfSight(currentTarget);

        boolean withinReach = false;
        if (canUseAtRange(player.getMainHandItem())) {
            withinReach = dist <= 24.0D;
        } else {
            withinReach = dist <= reach;
        }

        if (!hasSight || !withinReach) {
            targetUnreachableTicks++;
            if (targetUnreachableTicks >= 1200) { // 1 minute (1200 ticks)
                currentTarget = null;
                targetUnreachableTicks = 0;
            }
        } else {
            targetUnreachableTicks = 0;
        }
    }

    private static void switchToSlot(Minecraft mc, LocalPlayer player, int slot) {
        ItemStack prevStack = player.getMainHandItem();
        ItemStack newStack = player.getInventory().getItem(slot);

        if (isBCLoaded() && comboTicksRemaining > 0) {
            double prevScore = WeaponScoringEngine.calculateScore(prevStack, player.distanceTo(currentTarget) <= getWeaponReach(player), player);
            double newScore = WeaponScoringEngine.calculateScore(newStack, player.distanceTo(currentTarget) <= getWeaponReach(player), player);
            if (newScore - prevScore < Config.emergencySwitchScoreThreshold) {
                return; // Wait for current combo to end
            }
        }

        player.getInventory().selected = slot;
        rangedDrawTicks = 0;
        hybridChargeTicks = 0;
        currentComboStep = 0;
        comboTicksRemaining = 0;
        mc.options.keyUse.setDown(false);
        if (player.isUsingItem() && mc.gameMode != null) {
            mc.gameMode.releaseUsingItem(player);
        }

        // Two-handed offhand swap logic
        if (isBCLoaded()) {
            var styleOpt = BetterCombatWeaponAnalyzer.getWeaponStyle(newStack);
            if (styleOpt.isPresent() && styleOpt.get().isTwoHanded()) {
                ItemStack offhand = player.getOffhandItem();
                if (WeaponClassificationEngine.classify(offhand) != WeaponClass.NON_WEAPON) {
                    for (int i = 0; i < 9; i++) {
                        if (i != slot && player.getInventory().getItem(i).isEmpty()) {
                            swapHotbarToOffhand(mc, player, i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void swapHotbarToOffhand(Minecraft mc, Player player, int hotbarSlot) {
        int prevSelected = player.getInventory().selected;
        player.getInventory().selected = hotbarSlot;
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                net.minecraft.core.BlockPos.ZERO,
                net.minecraft.core.Direction.DOWN
            ));
        }
        player.getInventory().selected = prevSelected;
    }

    private static double getWeaponReach(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        double reach = player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        if (reach <= 0) reach = 3.0;

        WeaponClass wc = WeaponClassificationEngine.classify(mainHand);
        if (wc == WeaponClass.MELEE) {
            reach += 0.5D;
        }

        if (isBCLoaded() && Config.enableBetterCombatIntegration) {
            var styleOpt = BetterCombatWeaponAnalyzer.getWeaponStyle(mainHand);
            if (styleOpt.isPresent()) {
                reach = styleOpt.get().getAttackRange();
            }
        }

        return reach + Config.meleeRangeBufferBlocks;
    }

    private static void registerAttackAttempt(LivingEntity target) {
        if (target == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double reach = getWeaponReach(mc.player);
        if (mc.player.distanceTo(target) > reach) {
            return;
        }

        if (lastAttackedTarget != target) {
            lastAttackedTarget = target;
            attackAttempts = 1;
            lastTargetHealth = target.getHealth();
            lastHurtTimeDetectedTicks = stateTicks;
        } else {
            attackAttempts++;
            if (target.hurtTime > 0 || target.getHealth() < lastTargetHealth) {
                lastTargetHealth = target.getHealth();
                lastHurtTimeDetectedTicks = stateTicks;
                attackAttempts = 0;
            } else {
                if (attackAttempts >= 30 && (stateTicks - lastHurtTimeDetectedTicks) > 120) {
                    try {
                        net.minecraft.network.chat.Component name = target.getName();
                        if (name != null) {
                            org.apache.logging.log4j.LogManager.getLogger("xeb-bot")
                                .info("[xEB] Target " + name.getString() + " appears invulnerable/protected. Blacklisting for 30s.");
                        }
                    } catch (Throwable e) {
                        LOGGER.debug("[xEB] Failed to log blacklisted target name: {}", e.getMessage());
                    }
                    BLACKLISTED_TARGETS.put(target.getUUID(), System.currentTimeMillis() + 30000L); // 30 second blacklist
                    currentTarget = null;
                    lastAttackedTarget = null;
                    attackAttempts = 0;
                }
            }
        }
    }

    private static void lookAtTarget(LocalPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dy = (target.getY() + target.getEyeHeight() * 0.75D) - (player.getY() + player.getEyeHeight());
        double dz = target.getZ() - player.getZ();
        double dh = Math.sqrt(dx * dx + dz * dz);
        float baseYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float basePitch = (float) -(Math.atan2(dy, dh) * 180.0D / Math.PI);

        float yawNoise = (float) (Math.sin(stateTicks * 0.38) * 8.0);
        float pitchNoise = (float) (Math.sin(stateTicks * 0.27) * 4.0);

        float finalYaw = baseYaw + yawNoise;
        float finalPitch = Math.max(-80.0F, Math.min(80.0F, basePitch + pitchNoise));

        player.setYRot(finalYaw);
        player.setXRot(finalPitch);
        player.yRotO = finalYaw;
        player.xRotO = finalPitch;
        player.yHeadRot = finalYaw;
        player.yHeadRotO = finalYaw;
    }

    private static void executeMeleeCombat(Minecraft mc, LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        WeaponClass wc = WeaponClassificationEngine.classify(mainHand);

        if (mc.options.keyUse.isDown() || player.isUsingItem()) {
            ItemStack using = player.getUseItem();
            if (using.getItem().isEdible()) {
                mc.options.keyUse.setDown(false);
                if (mc.gameMode != null) {
                    mc.gameMode.releaseUsingItem(player);
                }
            }
        }

        // Handle casting imbued spells during melee combat
        if (hasActiveIronSpell(player)) {
            Object spell = getSelectedIronSpell();
            if (spell != null && isSpellOnCooldown(spell)) {
                switchToReadySpell();
            }
            meleeSpellCastTicks++;
            KeyMapping castKey = getIronCastKey();
            if (castKey != null) {
                int mod = meleeSpellCastTicks % 80;
                if (mod < 15) {
                    pressKey(castKey, true);
                } else {
                    pressKey(castKey, false);
                }
            }
        } else {
            meleeSpellCastTicks = 0;
            KeyMapping castKey = getIronCastKey();
            if (castKey != null) {
                pressKey(castKey, false);
            }
        }

        // 1) Prioritize Better Combat integration if active and weapon attributes are mapped
        boolean isBetterCombat = isBCLoaded() && Config.enableBetterCombatIntegration;
        if (isBetterCombat) {
            var styleOpt = BetterCombatWeaponAnalyzer.getWeaponStyle(mainHand);
            if (styleOpt.isPresent()) {
                var style = styleOpt.get();

                // Per-step ability for the CURRENT combo step (leap/thrust/parry/sweep)
                SpecialAbilityHandler.executeSpecialAbilities(mc, player, currentTarget, style, currentComboStep);

                // Respect BetterCombat's own in-progress animation
                boolean bcBusy = BetterCombatAttackController.isAttackInProgress(mc, player);
                if (bcBusy && comboTicksRemaining < -40) {
                    bcBusy = false; // Safety fallback timeout to prevent getting permanently stuck
                }
                if (!bcBusy && comboTicksRemaining <= 0) {
                    // Trigger an attack the way BetterCombat expects (real key press → BC combo).
                    BetterCombatAttackController.triggerAttack(mc, player, currentTarget);
                    registerAttackAttempt(currentTarget);

                    // Cadence for the NEXT step. Use the per-step speed when available.
                    double stepSpeed = style.getSpeedForStep(currentComboStep);
                    comboTicksRemaining = (int) Math.max(5.0, (20.0 / stepSpeed));
                    currentComboStep = (currentComboStep + 1) % style.getComboLength();
                } else {
                    // Not attacking this tick: make sure we don't leave the attack key held.
                    mc.options.keyAttack.setDown(false);
                }

                // Right-click special abilities for modded weapons (Cataclysm charge-swords, staves, etc.)
                handleRightClickSpecials(mc, player, mainHand);
                return;
            }
        }

        // 2) Fallback to hybrid/use-action melee weapons
        if (wc == WeaponClass.HYBRID) {
            handleHybridMeleeLogic(mc, player);
            return;
        }

        // Detect MELEE weapons that have a right-click special (modded staves, charge swords, etc.)
        if (wc == WeaponClass.MELEE || wc == WeaponClass.MAGIC) {
            try {
                net.minecraft.world.item.UseAnim useAnim = mainHand.getItem().getUseAnimation(mainHand);
                int useDuration = mainHand.getItem().getUseDuration(mainHand);
                if (useAnim != net.minecraft.world.item.UseAnim.NONE && useDuration > 0) {
                    handleHybridMeleeLogic(mc, player);
                    return;
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to check melee weapon use animation: {}", e.getMessage());
            }
        }

        // 3) Fallback to vanilla melee
        mc.options.keyAttack.setDown(false);
        if (player.getAttackStrengthScale(0.0F) >= 0.9F) {
            BetterCombatAttackController.triggerAttack(mc, player, currentTarget);
            registerAttackAttempt(currentTarget);
        }
    }

    /**
     * Routes a weapon's right-click (use) special ability through {@link SpecialAbilityHandler},
     * respecting the configured right-click mode (PERIODIC / TACTICAL / DISABLED).
     * Only invoked for weapons that have a real use action (modded charge-swords, staves, etc.).
     */
    private static void handleRightClickSpecials(Minecraft mc, LocalPlayer player, ItemStack mainHand) {
        if (Config.rightClickMode.equals("DISABLED")) return;
        try {
            net.minecraft.world.item.UseAnim useAnim = mainHand.getItem().getUseAnimation(mainHand);
            if (useAnim == net.minecraft.world.item.UseAnim.NONE) return;
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to check weapon use animation for right-click specials: {}", e.getMessage());
            return;
        }
        SpecialAbilityHandler.executeRightClickAbilities(mc, player, currentTarget, Config.rightClickMode);
    }

    private static void handleHybridMeleeLogic(Minecraft mc, LocalPlayer player) {
        if (player.getAttackStrengthScale(0.0F) >= 0.9F) {
            BetterCombatAttackController.triggerAttack(mc, player, currentTarget);
        }

        if (hybridSpecialCooldown <= 0 && hybridChargeTicks == 0) {
            if (player.getRandom().nextFloat() < 0.05F) {
                hybridChargeTicks = 1;
            }
        }

        if (hybridChargeTicks > 0) {
            ItemStack heldStack = player.getMainHandItem();
            int rawUse = heldStack.getItem().getUseDuration(heldStack);
            int drawDuration = (rawUse > 0 && rawUse < 200) ? Math.max(10, rawUse) : 20;
            if (hybridChargeTicks < drawDuration) {
                pressKey(mc.options.keyUse, true);
                hybridChargeTicks++;
            } else {
                pressKey(mc.options.keyUse, false);
                if (player.isUsingItem() && mc.gameMode != null) {
                    mc.gameMode.releaseUsingItem(player);
                }
                hybridChargeTicks = 0;
                hybridSpecialCooldown = 100 + player.getRandom().nextInt(80);
            }
        }
    }

    private static void executeRangedCombat(Minecraft mc, LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        mc.options.keyAttack.setDown(false);

        String id = "";
        try {
            id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString().toLowerCase();
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to get item registry name for ranged combat: {}", e.getMessage());
        }

        if (id.contains("terra_blade") || id.contains("starfury") || id.contains("star_wrath")) {
            // Swing at target at range to fire mana beams/stars!
            if (stateTicks % 5 == 0) {
                BetterCombatAttackController.triggerAttack(mc, player, currentTarget);
            }
            return;
        }

        if (hasChargedFistEffect(player)) {
            // Swing at the target at range to trigger AirSwingPacket (Sparkles!)
            if (stateTicks % 3 == 0) {
                BetterCombatAttackController.triggerAttack(mc, player, currentTarget);
            }
            return;
        }

        // Scan/cycle spell if on cooldown
        if (hasActiveIronSpell(player)) {
            Object spell = getSelectedIronSpell();
            if (spell != null && isSpellOnCooldown(spell)) {
                switchToReadySpell();
            }
        }

        if (hasActiveIronSpell(player)) {
            executeIronSpellCast(mc, player);
            return;
        }

        if (mainHand.getItem() instanceof CrossbowItem) {
            boolean charged = CrossbowItem.isCharged(mainHand);
            if (charged) {
                if (rangedDrawTicks >= 0) {
                    pressKey(mc.options.keyUse, false);
                    rangedDrawTicks = -5;
                } else if (rangedDrawTicks == -1) {
                    pressKey(mc.options.keyUse, true);
                    rangedDrawTicks = 0;
                    registerAttackAttempt(currentTarget);
                } else {
                    pressKey(mc.options.keyUse, false);
                    rangedDrawTicks++;
                }
            } else {
                pressKey(mc.options.keyUse, true);
                rangedDrawTicks = 1;
            }
        } else {
            int rawUse = mainHand.getItem().getUseDuration(mainHand);

            if (rawUse <= 5) {
                // Instant-use item (throwables, instant spells, etc.) — quick press-release (3 ticks on, 7 off)
                if (rangedDrawTicks < 0) {
                    pressKey(mc.options.keyUse, false);
                    rangedDrawTicks++;
                } else if (rangedDrawTicks < 3) {
                    if (rangedDrawTicks == 0) {
                        registerAttackAttempt(currentTarget);
                    }
                    pressKey(mc.options.keyUse, true);
                    rangedDrawTicks++;
                } else {
                    pressKey(mc.options.keyUse, false);
                    rangedDrawTicks = -7;
                }
            } else {
                // Charge-and-release item (bows, staves, tridents, etc.)
                // rawUse >= 200 (e.g. bows use 72000) means hold-until-release — default to 20 ticks
                int drawDuration = (rawUse < 200) ? Math.max(10, rawUse) : 20;
                if (rangedDrawTicks < 0) {
                    pressKey(mc.options.keyUse, false);
                    rangedDrawTicks++;
                } else if (rangedDrawTicks < drawDuration) {
                    pressKey(mc.options.keyUse, true);
                    rangedDrawTicks++;
                } else {
                    pressKey(mc.options.keyUse, false);
                    if (player.isUsingItem() && mc.gameMode != null) {
                        mc.gameMode.releaseUsingItem(player);
                    }
                    registerAttackAttempt(currentTarget);
                    rangedDrawTicks = -5;
                }
            }
        }
    }

    /**
     * Updates simulated input vectors based on the current state.
     */
    public static void applyMovementInput(Input input, LocalPlayer player) {
        if (currentTarget == null) {
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            return;
        }

        double dist = player.distanceTo(currentTarget);
        ItemStack mainHand = player.getMainHandItem();
        WeaponClass wc = WeaponClassificationEngine.classify(mainHand);
        double reach = getWeaponReach(player);

        boolean isRanged = false;
        if (isEating || hasChargedFistEffect(player)) {
            isRanged = true;
        } else if (wc == WeaponClass.RANGED || wc == WeaponClass.MAGIC) {
            isRanged = true;
        } else if (wc == WeaponClass.HYBRID) {
            isRanged = (currentState == BotState.ATTACKING_RANGED);
        }

        float fwd = 0.0F;
        float side = 0.0F;

        if (isEating) {
            // Eating safety spacing: keep 12.0 to 16.0 blocks
            if (dist > 16.0D) {
                fwd = 0.8F;
            } else if (dist < 12.0D) {
                fwd = -1.0F;
            } else {
                fwd = 0.0F;
                side = (float) (Math.sin(stateTicks * 0.15) * 0.8);
            }
        } else if (isRanged) {
            // Ranged/Magic spacing: keep a safe distance of 9.0 to 14.0 blocks
            if (dist > 14.0D) {
                fwd = 1.0F;
            } else if (dist < 9.0D) {
                fwd = -1.0F;
            } else {
                fwd = 0.0F;
                side = (float) (Math.sin(stateTicks * 0.15) * 0.8);
            }
        } else {
            // Melee spacing (footsies)
            double lowBound = Math.max(1.5D, reach * 0.50D);
            double highBound = Math.max(2.5D, reach * 0.95D);

            if (dist > highBound) {
                fwd = 1.0F;
            } else if (dist < lowBound) {
                fwd = -0.8F;
                side = (float) (Math.sin(stateTicks * 0.2) * 0.5);
            } else {
                fwd = 0.2F; // slow forward movement to stay engaged
                int direction = (stateTicks / 40) % 2 == 0 ? 1 : -1;
                side = (float) (direction * 0.8);
            }
        }

        input.forwardImpulse = fwd;
        input.leftImpulse = side;
        input.up = fwd > 0.01F;
        input.down = fwd < -0.01F;
        input.left = side > 0.01F;
        input.right = side < -0.01F;

        if (player.horizontalCollision || (stateTicks % 40 == 0 && player.onGround())) {
            input.jumping = true;
        }

        // Sprinting Optimization
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            boolean shouldSprint = dist > 4.0D && !player.isUsingItem() && fwd > 0.8F;
            mc.options.keySprint.setDown(shouldSprint);
            if (shouldSprint) {
                player.setSprinting(true);
            }
        }
    }

    /**
     * Returns true if the weapon can be used offensively at range:
     * RANGED, HYBRID, and MAGIC weapons qualify.
     * MELEE weapons with a right-click use action also qualify (e.g., modded staves classified as MELEE).
     */
    private static boolean canUseAtRange(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isSpellbookItem(stack)) return true;

        // Custom registry name heuristics for modded range items
        try {
            String registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
            if (registryName.contains("bow") || registryName.contains("gun") || registryName.contains("pistol") ||
                registryName.contains("rifle") || registryName.contains("blaster") || registryName.contains("cannon") ||
                registryName.contains("wand") || registryName.contains("staff") || registryName.contains("spellbook") ||
                registryName.contains("launcher") || registryName.contains("shoulder_weapon") || registryName.contains("throwable") ||
                registryName.contains("assault") || registryName.contains("grenade") || registryName.contains("terra_blade") ||
                registryName.contains("starfury") || registryName.contains("star_wrath")) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to get item registry name for range check: {}", e.getMessage());
        }

        WeaponClass wc = WeaponClassificationEngine.classify(stack);
        if (wc == WeaponClass.RANGED || wc == WeaponClass.HYBRID || wc == WeaponClass.MAGIC) return true;
        // Also allow items that have a right-click use action regardless of class
        try {
            net.minecraft.world.item.UseAnim anim = stack.getItem().getUseAnimation(stack);
            int duration = stack.getItem().getUseDuration(stack);
            return anim != net.minecraft.world.item.UseAnim.NONE && duration > 0;
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to check item use animation for range detection: {}", e.getMessage());
        }
        return false;
    }

    private static boolean isBCLoaded() {
        try {
            return net.minecraftforge.fml.ModList.get() != null &&
                   (net.minecraftforge.fml.ModList.get().isLoaded("bettercombat") ||
                    net.minecraftforge.fml.ModList.get().isLoaded("better_combat"));
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    private static boolean hasActiveIronSpell(LocalPlayer player) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) return false;
        try {
            Class<?> clientMagicDataClass = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            java.lang.reflect.Method getMgr = clientMagicDataClass.getMethod("getSpellSelectionManager");
            Object mgr = getMgr.invoke(null);
            if (mgr != null) {
                java.lang.reflect.Method getSelected = mgr.getClass().getMethod("getSelectedSpellData");
                Object selectedData = getSelected.invoke(mgr);
                if (selectedData != null) {
                    java.lang.reflect.Method getSpell = selectedData.getClass().getMethod("getSpell");
                    Object spell = getSpell.invoke(selectedData);
                    if (spell != null) {
                        java.lang.reflect.Method getSpellId = spell.getClass().getMethod("getSpellId");
                        String id = (String) getSpellId.invoke(spell);
                        return id != null && !id.equalsIgnoreCase("none") && !id.isEmpty();
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to check Iron's Spells active spell: {}", e.getMessage());
        }
        return false;
    }

    private static KeyMapping getIronCastKey() {
        if (!checkedIronCastKey) {
            checkedIronCastKey = true;
            try {
                Class<?> keyMappingsClass = Class.forName("io.redspace.ironsspellbooks.player.KeyMappings");
                java.lang.reflect.Field field = keyMappingsClass.getField("SPELLBOOK_CAST_ACTIVE_KEYMAP");
                ironCastKey = (KeyMapping) field.get(null);
            } catch (Throwable e1) {
                try {
                    Class<?> keyMappingsClass = Class.forName("io.redspace.ironsspellbooks.player.KeyMappings");
                    java.lang.reflect.Field field = keyMappingsClass.getField("QUICK_CAST_MAPPINGS");
                    java.util.List<?> list = (java.util.List<?>) field.get(null);
                    if (list != null && !list.isEmpty()) {
                        ironCastKey = (KeyMapping) list.get(0);
                    }
                } catch (Throwable e2) {
                    LOGGER.debug("[xEB] Iron's Spells cast key not found: {}", e2.getMessage());
                }
            }
        }
        return ironCastKey;
    }

    private static void executeIronSpellCast(Minecraft mc, LocalPlayer player) {
        KeyMapping castKey = getIronCastKey();
        if (castKey == null) {
            pressKey(mc.options.keyUse, true);
            return;
        }

        int rawUse = 20; // Default spell hold duration
        if (rangedDrawTicks < 0) {
            pressKey(castKey, false);
            rangedDrawTicks++;
        } else if (rangedDrawTicks < rawUse) {
            pressKey(castKey, true);
            rangedDrawTicks++;
        } else {
            pressKey(castKey, false);
            registerAttackAttempt(currentTarget);
            rangedDrawTicks = -10; // Pause ticks
        }
    }

    private static void pressKey(KeyMapping key, boolean down) {
        if (key == null) return;
        if (down) {
            if (!key.isDown()) {
                key.setDown(true);
                incrementClickCount(key);
            }
        } else {
            key.setDown(false);
        }
    }

    private static void incrementClickCount(KeyMapping key) {
        try {
            java.lang.reflect.Field field = null;
            for (String name : new String[]{"clickCount", "f_90815_"}) {
                try {
                    field = KeyMapping.class.getDeclaredField(name);
                    break;
                } catch (Throwable e) {
                    LOGGER.debug("[xEB] KeyMapping field '{}' not found", name);
                }
            }
            if (field != null) {
                field.setAccessible(true);
                int current = field.getInt(key);
                field.setInt(key, current + 1);
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to increment click count: {}", e.getMessage());
        }
    }

    private static Object getSelectedIronSpell() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) return null;
        try {
            Class<?> clientMagicDataClass = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            java.lang.reflect.Method getMgr = clientMagicDataClass.getMethod("getSpellSelectionManager");
            Object mgr = getMgr.invoke(null);
            if (mgr != null) {
                java.lang.reflect.Method getSelected = mgr.getClass().getMethod("getSelectedSpellData");
                Object selectedData = getSelected.invoke(mgr);
                if (selectedData != null) {
                    java.lang.reflect.Method getSpell = selectedData.getClass().getMethod("getSpell");
                    return getSpell.invoke(selectedData);
                }
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to get selected Iron's spell: {}", e.getMessage());
        }
        return null;
    }

    private static boolean isSpellOnCooldown(Object spell) {
        try {
            Class<?> clientMagicDataClass = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            java.lang.reflect.Method getCooldowns = clientMagicDataClass.getMethod("getCooldowns");
            Object cooldowns = getCooldowns.invoke(null);
            if (cooldowns != null) {
                java.lang.reflect.Method foundMethod = null;
                for (java.lang.reflect.Method m : cooldowns.getClass().getMethods()) {
                    if (m.getName().equals("isOnCooldown") && m.getParameterCount() == 1) {
                        foundMethod = m;
                        break;
                    }
                }
                if (foundMethod != null) {
                    return (Boolean) foundMethod.invoke(cooldowns, spell);
                }
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to check Iron's spell cooldown: {}", e.getMessage());
        }
        return false;
    }

    private static void switchToReadySpell() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) return;
        try {
            Class<?> clientMagicDataClass = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            java.lang.reflect.Method getMgr = clientMagicDataClass.getMethod("getSpellSelectionManager");
            Object mgr = getMgr.invoke(null);
            if (mgr != null) {
                java.lang.reflect.Method getSpellCount = mgr.getClass().getMethod("getSpellCount");
                int count = (Integer) getSpellCount.invoke(mgr);
                
                java.lang.reflect.Method getSpellData = mgr.getClass().getMethod("getSpellData", int.class);
                java.lang.reflect.Method makeSelection = mgr.getClass().getMethod("makeSelection", int.class);
                
                for (int i = 0; i < count; i++) {
                    Object spellData = getSpellData.invoke(mgr, i);
                    if (spellData != null) {
                        java.lang.reflect.Method getSpell = spellData.getClass().getMethod("getSpell");
                        Object spell = getSpell.invoke(spellData);
                        if (spell != null) {
                            java.lang.reflect.Method getSpellId = spell.getClass().getMethod("getSpellId");
                            String id = (String) getSpellId.invoke(spell);
                            if (id != null && !id.equalsIgnoreCase("none") && !id.isEmpty()) {
                                if (!isSpellOnCooldown(spell)) {
                                    makeSelection.invoke(mgr, i);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.debug("[xEB] Failed to switch Iron's ready spell: {}", e.getMessage());
        }
    }

    private static boolean isSpellbookItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String className = stack.getItem().getClass().getName();
        return className.contains("Spellbook") || className.contains("spellbook");
    }

    private static boolean hasChargedFistEffect(LocalPlayer player) {
        return player.hasEffect(ModEffects.CHARGED_FIST.get());
    }

    private static boolean isDoomfistItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getItem().getClass().getName();
        return name.contains("Doomfist") || stack.getItem().toString().contains("doomfist");
    }

    private static boolean isStaffItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String className = stack.getItem().getClass().getName();
        return className.contains("Staff") || className.contains("stave") || stack.getItem().toString().contains("staff");
    }

    private static void updateStrategicInventory(Minecraft mc, LocalPlayer player, double dist, boolean inMeleeRange) {
        float healthPct = player.getHealth() / player.getMaxHealth();

        // A. Curios Auto-Equip (Holy Mantle / Brass Knuckles)
        if (stateTicks % 40 == 0) {
            boolean hasHolyMantleEquipped = org.xeb.xeb.compat.ModCompatManager.hasCurioOrOffhand(player, org.xeb.xeb.item.ModItems.HOLY_MANTLE.get());
            if (!hasHolyMantleEquipped) {
                int mantleSlot = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.is(org.xeb.xeb.item.ModItems.HOLY_MANTLE.get())) {
                        mantleSlot = i;
                        break;
                    }
                }
                if (mantleSlot != -1) {
                    if (player.getInventory().selected != mantleSlot) {
                        switchToSlot(mc, player, mantleSlot);
                    }
                    pressKey(mc.options.keyUse, true);
                    potionThrowTicks = 1;
                    return;
                }
            }

            boolean hasBrassKnucklesEquipped = org.xeb.xeb.compat.ModCompatManager.hasCurioOrOffhand(player, org.xeb.xeb.item.ModItems.BRASS_KNUCKLES.get());
            if (!hasBrassKnucklesEquipped) {
                int knucklesSlot = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.is(org.xeb.xeb.item.ModItems.BRASS_KNUCKLES.get())) {
                        knucklesSlot = i;
                        break;
                    }
                }
                if (knucklesSlot != -1) {
                    if (player.getInventory().selected != knucklesSlot) {
                        switchToSlot(mc, player, knucklesSlot);
                    }
                    pressKey(mc.options.keyUse, true);
                    potionThrowTicks = 1;
                    return;
                }
            }
        }

        // B. Iron's Spells staff synergy
        if (hasActiveIronSpell(player)) {
            int staffSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (isStaffItem(stack)) {
                    staffSlot = i;
                    break;
                }
            }
            if (staffSlot != -1) {
                if (player.getInventory().selected != staffSlot) {
                    switchToSlot(mc, player, staffSlot);
                }
                return;
            }
        }

        // 1. Potion throwing (healing / splash)
        if (potionCooldown <= 0 && healthPct < 0.40F) {
            int potionSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && (stack.getItem() instanceof net.minecraft.world.item.ThrowablePotionItem)) {
                    potionSlot = i;
                    break;
                }
            }
            if (potionSlot != -1) {
                if (player.getInventory().selected != potionSlot) {
                    switchToSlot(mc, player, potionSlot);
                }
                player.setXRot(90.0F);
                player.xRotO = 90.0F;
                pressKey(mc.options.keyUse, true);
                potionThrowTicks = 1;
                potionCooldown = 100; // 5-second cooldown
                return;
            }
        }
        
        if (potionThrowTicks > 0) {
            potionThrowTicks--;
            if (potionThrowTicks == 0) {
                pressKey(mc.options.keyUse, false);
            }
            return;
        }

        // 2. Ender Pearl Escape / Approach
        if (enderPearlCooldown <= 0) {
            int pearlSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.ENDER_PEARL)) {
                    pearlSlot = i;
                    break;
                }
            }

            if (pearlSlot != -1) {
                if (healthPct < 0.25F && dist <= 10.0D) {
                    if (player.getInventory().selected != pearlSlot) {
                        switchToSlot(mc, player, pearlSlot);
                    }
                    float escapeYaw = player.getYRot() + 180.0F;
                    player.setYRot(escapeYaw);
                    player.yRotO = escapeYaw;
                    player.setXRot(-35.0F);
                    player.xRotO = -35.0F;
                    pressKey(mc.options.keyUse, true);
                    pearlThrowTicks = 1;
                    enderPearlCooldown = 120; // 6-second cooldown
                    return;
                }
                else if (dist > 22.0D) {
                    if (player.getInventory().selected != pearlSlot) {
                        switchToSlot(mc, player, pearlSlot);
                    }
                    lookAtTarget(player, currentTarget);
                    player.setXRot(player.getXRot() - 15.0F);
                    pressKey(mc.options.keyUse, true);
                    pearlThrowTicks = 1;
                    enderPearlCooldown = 120;
                    return;
                }
            }
        }

        if (pearlThrowTicks > 0) {
            pearlThrowTicks--;
            if (pearlThrowTicks == 0) {
                pressKey(mc.options.keyUse, false);
            }
            return;
        }

        // 3. Totem of Undying auto-equip
        if (healthPct < 0.35F && !player.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
            int totemSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
                    totemSlot = i;
                    break;
                }
            }
            if (totemSlot != -1) {
                swapHotbarToOffhand(mc, player, totemSlot);
            }
        }

        // 4. Health check & eating food (preferring Golden Apples)
        if (healthPct < 0.45F || (isEating && healthPct < 0.85F)) {
            int foodSlot = -1;
            if (healthPct < 0.60F) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && (stack.is(net.minecraft.world.item.Items.GOLDEN_APPLE) || stack.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE))) {
                        foodSlot = i;
                        break;
                    }
                }
            }
            if (foodSlot == -1) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.getItem().isEdible()) {
                        foodSlot = i;
                        break;
                    }
                }
            }

            if (foodSlot != -1) {
                isEating = true;
                eatingSlot = foodSlot;
                if (player.getInventory().selected != foodSlot) {
                    switchToSlot(mc, player, foodSlot);
                }
                pressKey(mc.options.keyUse, true);
                return;
            }
        }
        
        if (isEating) {
            isEating = false;
            eatingSlot = -1;
            pressKey(mc.options.keyUse, false);
        }

        // 5. Shield Tactical Blocking
        boolean hasShieldEquipped = player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
        if (!hasShieldEquipped) {
            int shieldSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                    shieldSlot = i;
                    break;
                }
            }
            if (shieldSlot != -1) {
                swapHotbarToOffhand(mc, player, shieldSlot);
                hasShieldEquipped = true;
            }
        }

        if (hasShieldEquipped) {
            boolean targetIsAttacking = currentTarget != null && currentTarget.swingTime > 0;
            boolean shouldBlock = inMeleeRange && (targetIsAttacking || comboTicksRemaining > 0 || stateTicks % 40 < 12);
            if (shouldBlock) {
                pressKey(mc.options.keyUse, true);
                return;
            } else {
                pressKey(mc.options.keyUse, false);
            }
        }

        // 6. Charged Fist ranged attacks
        if (hasChargedFistEffect(player)) {
            int meleeSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && !canUseAtRange(stack)) {
                    meleeSlot = i;
                    break;
                }
            }
            if (meleeSlot != -1 && player.getInventory().selected != meleeSlot) {
                switchToSlot(mc, player, meleeSlot);
            }
            return;
        }

        // 7. The Doomfist combo strategy
        int doomfistSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isDoomfistItem(stack)) {
                doomfistSlot = i;
                break;
            }
        }

        if (doomfistSlot != -1) {
            ItemStack doomfistStack = player.getInventory().getItem(doomfistSlot);
            boolean isChargedFistOnCooldown = player.getCooldowns().isOnCooldown(doomfistStack.getItem());
            
            if (!isChargedFistOnCooldown && dist <= 15.0D) {
                if (player.getInventory().selected != doomfistSlot) {
                    switchToSlot(mc, player, doomfistSlot);
                }
                pressKey(mc.options.keyUse, true);
                return;
            }
        }

        // 7.5. The Incinerator combo strategy
        int incineratorSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem().toString().contains("the_incinerator")) {
                incineratorSlot = i;
                break;
            }
        }

        if (incineratorSlot != -1) {
            ItemStack incStack = player.getInventory().getItem(incineratorSlot);
            boolean isOnCooldown = player.getCooldowns().isOnCooldown(incStack.getItem());
            if (!isOnCooldown && dist <= 12.0D) {
                if (player.getInventory().selected != incineratorSlot) {
                    switchToSlot(mc, player, incineratorSlot);
                }
                pressKey(mc.options.keyUse, true);
                return;
            }
        }

        // 8. Default: delegate to scanner
        int bestSlot = HOTBAR_SCANNER.getBestWeaponSlot(player, dist, inMeleeRange);
        if (bestSlot != player.getInventory().selected) {
            switchToSlot(mc, player, bestSlot);
        }
    }
}
