package org.xeb.xeb.bettercombat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.xeb.xeb.compat.ModCompatAdapter;
import org.xeb.xeb.weapon.WeaponClass;
import org.xeb.xeb.weapon.WeaponStyleData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BetterCombatCompatAdapter implements ModCompatAdapter {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("xeb-compat");
    private static final String MOD_ID = "bettercombat";
    private final boolean loaded;
    private static final java.util.Set<String> WARNED_ITEMS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.Set<String> LOGGED_ITEMS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public BetterCombatCompatAdapter() {
        boolean modLoaded = false;
        try {
            modLoaded = ModList.get() != null && 
                       (ModList.get().isLoaded("bettercombat") || ModList.get().isLoaded("better_combat"));
        } catch (Exception | LinkageError ignored) {}
        this.loaded = modLoaded;
    }

    @Override
    public String modId() {
        return "bettercombat";
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public WeaponClass classifyWeapon(ItemStack stack) {
        if (!loaded) return WeaponClass.NON_WEAPON;
        Optional<WeaponStyleData> style = getWeaponStyle(stack);
        return style.isPresent() ? WeaponClass.MELEE : WeaponClass.NON_WEAPON;
    }

    @Override
    public boolean isBoss(LivingEntity entity) {
        return false;
    }

    @Override
    public Optional<WeaponStyleData> getWeaponStyle(ItemStack stack) {
        if (!loaded || stack.isEmpty()) return Optional.empty();
        try {
            Class<?> registryClass = null;
            for (String name : new String[]{"net.bettercombat.logic.WeaponRegistry", "net.bettercombat.api.WeaponRegistry"}) {
                try {
                    registryClass = Class.forName(name);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (registryClass == null) {
                LOGGER.error("[xEB] Better Combat WeaponRegistry class not found in classpath candidates!");
                return Optional.empty();
            }

            // Resolve Kotlin companion object / object instance if present
            Object instance = null;
            try {
                java.lang.reflect.Field instanceField = registryClass.getField("INSTANCE");
                instance = instanceField.get(null);
            } catch (Exception ignored) {}

            // Try resolving and invoking getAttributes with multiple parameter signatures
            java.lang.reflect.Method getAttributes = null;
            Object attributes = null;

            // Candidate 1: ItemStack
            try {
                getAttributes = registryClass.getMethod("getAttributes", ItemStack.class);
                getAttributes.setAccessible(true);
                attributes = getAttributes.invoke(instance, stack);
            } catch (Throwable t) {
                LOGGER.error("[xEB] Error invoking getAttributes(ItemStack) reflectively", t);
            }

            // Candidate 2: Item (if candidate 1 failed or returned null)
            if (attributes == null) {
                try {
                    getAttributes = registryClass.getMethod("getAttributes", net.minecraft.world.item.Item.class);
                    getAttributes.setAccessible(true);
                    attributes = getAttributes.invoke(instance, stack.getItem());
                } catch (Throwable t) {
                    LOGGER.error("[xEB] Error invoking getAttributes(Item) reflectively", t);
                }
            }

            // Candidate 3: ResourceLocation (if candidates 1 and 2 failed or returned null)
            if (attributes == null) {
                try {
                    getAttributes = registryClass.getMethod("getAttributes", net.minecraft.resources.ResourceLocation.class);
                    getAttributes.setAccessible(true);
                    net.minecraft.resources.ResourceLocation loc = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                    attributes = getAttributes.invoke(instance, loc);
                } catch (Throwable t) {
                    LOGGER.error("[xEB] Error invoking getAttributes(ResourceLocation) reflectively", t);
                }
            }

            String registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (LOGGED_ITEMS.add(registryName)) {
                if (attributes == null) {
                    LOGGER.info("[xEB] getWeaponStyle item: " + registryName + " resolved to NULL attributes");
                } else {
                    LOGGER.info("[xEB] getWeaponStyle item: " + registryName + " resolved to attributes: " + attributes.getClass().getName());
                }
            }

            if (attributes == null) {
                boolean isLikelyWeapon = registryName.contains("sword") || registryName.contains("axe") || 
                                         registryName.contains("knuckle") || registryName.contains("doomfist") ||
                                         registryName.contains("mace") || registryName.contains("dagger") ||
                                         registryName.contains("scythe") || registryName.contains("spear");
                
                if (isLikelyWeapon) {
                    if (WARNED_ITEMS.add(registryName)) {
                        LOGGER.warn("[xEB] Better Combat WeaponRegistry resolved, but getAttributes returned null for weapon: " + registryName);
                        for (java.lang.reflect.Method m : registryClass.getMethods()) {
                            if (m.getName().toLowerCase().contains("attribute")) {
                                LOGGER.info("[xEB] Candidate method: " + m.toString());
                            }
                        }
                    }
                }
                return Optional.empty();
            }

            // Parse Range
            double attackRange = 3.0D;
            try {
                java.lang.reflect.Method rangeMethod = null;
                try {
                    rangeMethod = attributes.getClass().getMethod("range");
                } catch (NoSuchMethodException e) {
                    rangeMethod = attributes.getClass().getMethod("getRange");
                }
                attackRange = ((Number) rangeMethod.invoke(attributes)).doubleValue();
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field rangeField = attributes.getClass().getField("range");
                    attackRange = rangeField.getDouble(attributes);
                } catch (Exception e2) {
                    LOGGER.debug("[xEB] Failed to read BC range field fallback: {}", e2.getMessage());
                }
            }

            // Parse Two Handed
            boolean twoHanded = false;
            try {
                java.lang.reflect.Method twoHandedMethod = null;
                try {
                    twoHandedMethod = attributes.getClass().getMethod("isTwoHanded");
                } catch (NoSuchMethodException e) {
                    try {
                        twoHandedMethod = attributes.getClass().getMethod("getIsTwoHanded");
                    } catch (NoSuchMethodException e2) {
                        twoHandedMethod = attributes.getClass().getMethod("twoHanded");
                    }
                }
                twoHanded = (Boolean) twoHandedMethod.invoke(attributes);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field twoHandedField = attributes.getClass().getField("isTwoHanded");
                    twoHanded = twoHandedField.getBoolean(attributes);
                } catch (Exception e2) {
                    LOGGER.debug("[xEB] Failed to read BC twoHanded field fallback: {}", e2.getMessage());
                }
            }

            // Parse Attacks / Combo steps.
            // We collect PER-STEP data so the combo driver can use the correct cadence
            // and ability for each swing, matching BetterCombat's attacks[] array.
            int comboLength = 1;
            double attackSpeed = 1.6D; // fallback average
            List<String> abilities = new ArrayList<>();
            List<Double> perStepSpeeds = new ArrayList<>();
            List<String> perStepAbilities = new ArrayList<>();
            try {
                java.lang.reflect.Method attacksMethod = null;
                try {
                    attacksMethod = attributes.getClass().getMethod("attacks");
                } catch (NoSuchMethodException e) {
                    attacksMethod = attributes.getClass().getMethod("getAttacks");
                }
                Object[] attacks = (Object[]) attacksMethod.invoke(attributes);
                if (attacks != null && attacks.length > 0) {
                    comboLength = attacks.length;
                    double speedSum = 0;
                    int speedCount = 0;
                    for (Object attack : attacks) {
                        // Per-attack speed (field "speed" or method "speed()").
                        double stepSpeed = 0;
                        try {
                            java.lang.reflect.Method speedMethod = null;
                            try {
                                speedMethod = attack.getClass().getMethod("speed");
                            } catch (NoSuchMethodException e1) {
                                speedMethod = attack.getClass().getMethod("getSpeed");
                            }
                            stepSpeed = ((Number) speedMethod.invoke(attack)).doubleValue();
                        } catch (Exception e1) {
                            try {
                                java.lang.reflect.Field speedField = attack.getClass().getField("speed");
                                stepSpeed = speedField.getDouble(attack);
                            } catch (Exception e) {
                                LOGGER.debug("[xEB] Failed to read BC per-step speed: {}", e.getMessage());
                            }
                        }
                        perStepSpeeds.add(stepSpeed);
                        if (stepSpeed > 0) { speedSum += stepSpeed; speedCount++; }

                        // Detect the single dominant ability from this step's animation name.
                        // We pick at most one keyword per step (first match wins) so the
                        // combo driver can trigger the right special on the right swing.
                        String stepAbility = null;
                        try {
                            java.lang.reflect.Method animMethod = null;
                            try {
                                animMethod = attack.getClass().getMethod("animation");
                            } catch (NoSuchMethodException e1) {
                                animMethod = attack.getClass().getMethod("getAnimation");
                            }
                            String anim = (String) animMethod.invoke(attack);
                            if (anim != null) {
                                String lower = anim.toLowerCase();
                                if (lower.contains("leap") || lower.contains("jump")) stepAbility = "leap";
                                else if (lower.contains("thrust") || lower.contains("stab")) stepAbility = "thrust";
                                else if (lower.contains("parry") || lower.contains("block")) stepAbility = "parry";
                                else if (lower.contains("sweep") || lower.contains("spin") || lower.contains("whirl")) stepAbility = "sweep";

                                if (stepAbility != null && !abilities.contains(stepAbility)) {
                                    abilities.add(stepAbility);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.debug("[xEB] Failed to read BC animation name: {}", e.getMessage());
                        }
                        perStepAbilities.add(stepAbility);
                    }
                    // Average speed across steps drives the legacy fallback cadence.
                    if (speedCount > 0) attackSpeed = speedSum / speedCount;
                }
            } catch (Exception e) {
                LOGGER.debug("[xEB] Failed to parse BC attacks/combo data: {}", e.getMessage());
            }

            return Optional.of(new WeaponStyleData(attackRange, comboLength, attackSpeed, abilities, twoHanded,
                    perStepSpeeds, perStepAbilities));
        } catch (Exception e) {
            LOGGER.debug("[xEB] Failed to resolve BC weapon style: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean isItemNonWeapon(ItemStack stack) {
        return false;
    }
}
