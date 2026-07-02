package org.xeb.xeb.weapon;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.xeb.xeb.Config;
import org.xeb.xeb.weapon.classifier.HeuristicClassifier;

import java.util.Collection;

/**
 * Safe wrapper around ModList to avoid crashing when Forge is not initialized (e.g., unit tests).
 */
final class ModListSafe {
    static boolean isLoaded(String modId) {
        try {
            return net.minecraftforge.fml.ModList.get().isLoaded(modId);
        } catch (Exception | LinkageError e) {
            return false;
        }
    }
}

/**
 * Calculates a weapon score for player hotbar slots under Madness.
 */
public class WeaponScoringEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HeuristicClassifier HEURISTIC = new HeuristicClassifier();

    /**
     * Scores a given ItemStack based on proximity, attributes, durability, enchantments, and mod context.
     */
    public static double calculateScore(ItemStack stack, boolean inMeleeRange, Player player) {
        if (stack == null || stack.isEmpty()) return -100.0;

        WeaponClass classification = WeaponClassificationEngine.classify(stack);
        if (classification == WeaponClass.NON_WEAPON) {
            return -100.0;
        }

        // Durability check
        if (stack.isDamageableItem()) {
            int maxDmg = stack.getMaxDamage();
            int currentDmg = stack.getDamageValue();
            int remaining = maxDmg - currentDmg;
            double pct = maxDmg > 0 ? (double) remaining / maxDmg : 1.0;
            if (pct <= 0.0) {
                return -1000.0; // Broken weapon
            }
        }

        // 1. Base Damage from attributes
        double baseDamage = 1.0;
        try {
            Collection<AttributeModifier> modifiers = stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(Attributes.ATTACK_DAMAGE);
            if (modifiers != null && !modifiers.isEmpty()) {
                double totalMod = 0.0;
                for (AttributeModifier mod : modifiers) {
                    totalMod += mod.getAmount();
                }
                if (totalMod > 0) {
                    baseDamage = totalMod;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to read attack damage attributes for {}: {}", stack.getItem(), e.getMessage());
        }

        double score = baseDamage;

        // 2. Tier Bonus
        double tierBonus = 0.0;
        if (stack.getItem() instanceof TieredItem tieredItem) {
            Tier tier = tieredItem.getTier();
            if (tier == null) {
                tierBonus = 0.0;
            } else if (tier == Tiers.WOOD || tier == Tiers.GOLD) {
                tierBonus = 0.0;
            } else if (tier == Tiers.STONE) {
                tierBonus = 1.0;
            } else if (tier == Tiers.IRON) {
                tierBonus = 2.0;
            } else if (tier == Tiers.DIAMOND) {
                tierBonus = 3.0;
            } else if (tier == Tiers.NETHERITE) {
                tierBonus = 4.0;
            } else {
                // Modded tier
                String tierStr = tier.toString().toLowerCase();
                tierBonus = getModMaterialTier(tierStr);
                if (tierBonus == 0.0) {
                    tierBonus = tier.getAttackDamageBonus();
                }
            }
        }
        score += tierBonus;

        // 3. Keyword Bonus (+15.0 if name matches heuristics)
        if (HEURISTIC.classify(stack) != WeaponClass.NON_WEAPON) {
            score += 15.0;
        }

        // 4. Classification Bonus (+5.0 for matching current combat range)
        if (inMeleeRange) {
            if (classification == WeaponClass.MELEE || classification == WeaponClass.HYBRID) {
                score += 5.0;
            }
        } else {
            if (classification == WeaponClass.RANGED || classification == WeaponClass.MAGIC) {
                score += 5.0;
            }
        }

        // 5. Special Ability Bonus (+10.0 if weapon has known features)
        boolean hasSpecial = false;
        try {
            if (stack.getEnchantmentLevel(Enchantments.FIRE_ASPECT) > 0 ||
                stack.getEnchantmentLevel(Enchantments.KNOCKBACK) > 0 ||
                stack.getEnchantmentLevel(Enchantments.MOB_LOOTING) > 0) {
                hasSpecial = true;
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to check enchantments for {}: {}", stack.getItem(), e.getMessage());
        }

        if (!hasSpecial && ModListSafe.isLoaded("bettercombat")) {
            try {
                var styleOpt = org.xeb.xeb.bettercombat.BetterCombatWeaponAnalyzer.getWeaponStyle(stack);
                if (styleOpt.isPresent() && !styleOpt.get().getSpecialAbilities().isEmpty()) {
                    hasSpecial = true;
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to check BetterCombat special abilities: {}", e.getMessage());
            }
        }
        if (hasSpecial) {
            score += 10.0;
        }

        // 6. Enchantment Bonus (Sum of levels * 2.0)
        try {
            int totalEnchantmentLevels = 0;
            var enchantments = EnchantmentHelper.getEnchantments(stack);
            for (int level : enchantments.values()) {
                totalEnchantmentLevels += level;
            }
            score += totalEnchantmentLevels * 2.0;
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to read enchantment levels for {}: {}", stack.getItem(), e.getMessage());
        }

        // 7. Ranged Penalty (-20.0 for pure RANGED in melee range)
        if (inMeleeRange && classification == WeaponClass.RANGED) {
            score -= 20.0;
        }

        // 8. Ammo check for ranged items
        if (classification == WeaponClass.RANGED) {
            Item item = stack.getItem();
            if (item instanceof BowItem || item instanceof CrossbowItem) {
                boolean hasAmmo = player.isCreative() || !player.getProjectile(stack).isEmpty();
                if (!hasAmmo) {
                    score += Config.noAmmoRangedPenalty;
                }
            }
        }

        // 9. Cooldown Penalty (-cooldownPercent * 3.0)
        try {
            double cd = player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F);
            score -= cd * 3.0;
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to check cooldown for {}: {}", stack.getItem(), e.getMessage());
        }

        // 10. Better Combat Combo Bonus (+8.0 if combo > 1)
        if (ModListSafe.isLoaded("bettercombat")) {
            try {
                var styleOpt = org.xeb.xeb.bettercombat.BetterCombatWeaponAnalyzer.getWeaponStyle(stack);
                if (styleOpt.isPresent() && styleOpt.get().getComboLength() > 1) {
                    score += 8.0;
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to check BetterCombat combo length: {}", e.getMessage());
            }
        }

        // 11. Low Durability Penalty (-5.0)
        if (stack.isDamageableItem()) {
            int maxDmg = stack.getMaxDamage();
            int currentDmg = stack.getDamageValue();
            int remaining = maxDmg - currentDmg;
            double pct = maxDmg > 0 ? (double) remaining / maxDmg : 1.0;
            if (pct < Config.lowDurabilityThreshold) {
                score -= 5.0;
            }
        }

        return score;
    }

    private static double getModMaterialTier(String tierName) {
        String mapping = Config.modMaterialTierMapping;
        if (mapping == null || mapping.isEmpty()) return 0.0;
        try {
            String[] pairs = mapping.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    String key = kv[0].trim().toLowerCase();
                    if (tierName.contains(key)) {
                        return Double.parseDouble(kv[1].trim());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to parse mod material tier mapping: {}", e.getMessage());
        }
        return 0.0;
    }
}
