package org.xeb.xeb.event;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;

import java.util.List;
import java.util.UUID;

public class DeathMessageHelper {
    public static Component modifyDeathMessage(Component original, LivingEntity victim) {
        if (victim.hasEffect(org.xeb.xeb.effect.ModEffects.DELAYED_PAIN.get()) ||
            victim.getPersistentData().getBoolean("xebDelayedPainDied")) {
            // Clear the temporary flag
            victim.getPersistentData().remove("xebDelayedPainDied");
            return Component.translatable("death.attack.xeb.delayed_pain", original);
        }

        // Custom weapon-specific death messages (N23)
        long currentTick = victim.level().getGameTime();
        if (victim.getPersistentData().contains("xebLastAttackWeapon") && victim.getPersistentData().contains("xebLastAttackTime")) {
            long lastTime = victim.getPersistentData().getLong("xebLastAttackTime");
            if (currentTick - lastTime <= 100) {
                String weapon = victim.getPersistentData().getString("xebLastAttackWeapon");
                String type = victim.getPersistentData().getString("xebLastAttackType");
                String translationKey = "death.attack.xeb." + weapon + "." + type;
                
                LivingEntity killer = victim.getKillCredit();
                Component killerName = killer != null ? killer.getDisplayName() : Component.literal("unknown");
                MutableComponent customMsg = Component.translatable(translationKey, victim.getDisplayName(), killerName);
                
                victim.getPersistentData().remove("xebLastAttackWeapon");
                victim.getPersistentData().remove("xebLastAttackType");
                victim.getPersistentData().remove("xebLastAttackTime");
                
                if (killer != null) {
                    List<MedallionData> medallions = MedallionManager.getMedallions(killer);
                    if (!medallions.isEmpty()) {
                        MutableComponent hoverText = Component.literal("§6§l" + killer.getDisplayName().getString() + "'s Medallions:§r\n");
                        for (MedallionData m : medallions) {
                            String tierColor = switch (m.getTier()) {
                                case COMMON -> "§c";
                                case RARE -> "§7";
                                case LEGENDARY -> "§6";
                            };
                            hoverText.append(Component.literal(tierColor + "● §r"))
                                     .append(m.getBuff().getDisplayName())
                                     .append(Component.literal(" (" + tierColor))
                                     .append(m.getTier().getDisplayName())
                                     .append(Component.literal("§r)\n"));
                            String desc = getVagueDescription(m.getBuff().getId());
                            hoverText.append("  §8§o" + desc + "§r\n");
                        }
                        
                        MutableComponent mutableArg = killerName.copy();
                        mutableArg.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
                        customMsg = Component.translatable(translationKey, victim.getDisplayName(), mutableArg);
                    }
                }
                
                return customMsg;
            }
        }

        LivingEntity killer = victim.getKillCredit();
        if (killer == null) return original;

        List<MedallionData> medallions = MedallionManager.getMedallions(killer);
        if (medallions.isEmpty()) return original;

        // Construct the hover text for the medallions
        MutableComponent hoverText = Component.literal("§6§l" + killer.getDisplayName().getString() + "'s Medallions:§r\n");
        for (MedallionData m : medallions) {
            String tierColor = switch (m.getTier()) {
                case COMMON -> "§c"; // Bronze (Red/Orange-ish)
                case RARE -> "§7"; // Silver (Gray)
                case LEGENDARY -> "§6"; // Gold (Gold/Yellow)
            };
            
            // Add medallion 3D-like icon representation (e.g. ● or a colored bracketed icon) and name
            hoverText.append(Component.literal(tierColor + "● §r"))
                     .append(m.getBuff().getDisplayName())
                     .append(Component.literal(" (" + tierColor))
                     .append(m.getTier().getDisplayName())
                     .append(Component.literal("§r)\n"));
            
            // Add a vague description of the effect
            String desc = getVagueDescription(m.getBuff().getId());
            hoverText.append("  §8§o" + desc + "§r\n");
        }

        // Apply hover to the killer name argument in translatable contents
        if (original.getContents() instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean modified = false;
            
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component c) {
                    if (c.getString().contains(killer.getDisplayName().getString())) {
                        MutableComponent mutableArg = c.copy();
                        mutableArg.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
                        newArgs[i] = mutableArg;
                        modified = true;
                        continue;
                    }
                }
                newArgs[i] = args[i];
            }
            
            if (modified) {
                MutableComponent newComponent = Component.translatable(translatable.getKey(), newArgs);
                newComponent.setStyle(original.getStyle());
                for (Component sibling : original.getSiblings()) {
                    newComponent.append(sibling.copy());
                }
                return newComponent;
            }
        }

        // Fallback: apply hover to the whole death message if not a translatable component
        MutableComponent fallback = original.copy();
        fallback.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
        return fallback;
    }

    private static String getVagueDescription(String id) {
        return switch (id) {
            case "spiky" -> "Deals damage back to attackers.";
            case "reactive" -> "Emits a sonic shockwave when damaged.";
            case "damaging" -> "Grants increased attack damage.";
            case "tough" -> "Grants increased armor.";
            case "shielded" -> "Has a temporary shield that absorbs damage.";
            case "protected" -> "Holy shield blocks the first incoming hit.";
            case "speedy" -> "Moves significantly faster.";
            case "flaming" -> "Immune to fire, leaves a trail of fire.";
            case "creepy" -> "Inflicts poison on melee hits and nearby entities.";
            case "lucky" -> "Chance to double damage or dodge attacks.";
            case "static" -> "Releases chain lightning after moving.";
            case "bouncy" -> "Knocks back attackers and itself.";
            case "mirror" -> "Reflects projectiles back to shooter.";
            case "resonant" -> "Gains power and heals when items are used near it.";
            case "undying" -> "Revives with 50% health once upon death.";
            case "healthy" -> "Gains passive health regeneration and max health.";
            case "sandy" -> "Chance to dodge, creates a sandstorm on death.";
            case "infested" -> "Spawns hostile flies when killed.";
            case "absorbent" -> "Siphons mana and deals damage to nearby targets.";
            case "depressing" -> "Weakens attributes of all nearby players.";
            case "slightly_depressing" -> "Slightly weakens attributes of adjacent players.";
            case "evolving" -> "Gains new random medallions over time.";
            case "plow" -> "Tramples entities by walking through them.";
            case "mega" -> "Large size, highly increased health and damage.";
            case "mad" -> "Attacks everyone nearby in a frenzy, gaining power and healing on kills.";
            case "twin" -> "Spawns with an identical twin with the same buffs.";
            case "hardy" -> "Immune to all forms of crowd control.";
            case "sticky" -> "Tarres attackers, slowing their movement.";
            default -> "Grants mysterious powers.";
        };
    }
}
