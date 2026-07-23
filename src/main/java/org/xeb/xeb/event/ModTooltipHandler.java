package org.xeb.xeb.event;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.extremeburst.ExtremeBurstRegistry;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModTooltipHandler {

    public static boolean isExtremeBurstCurio(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.DOGMA.get()
                || item == ModItems.OMEGA_FLOWERY.get()
                || item == ModItems.QUANTUM_CAT_BARRAGE.get();
    }

    public static boolean isModWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.GOLDEN_FLOWER.get()
                || item == ModItems.DOOMFIST.get()
                || item == ModItems.DOOMFIST_V2.get()
                || item == ModItems.OPTIC_BLAST.get()
                || item == ModItems.HOLY_DUALITY_BLADE.get()
                || item == ModItems.MECHA_OVERDRIVE.get()
                || item == ModItems.BROKEN_DIAMOND.get()
                || item == ModItems.THE_TEARS.get()
                || item == ModItems.SMART_HALBERD.get();
    }

    private static class WeaponStats {
        final double damage;
        final double speed;
        final double range;

        WeaponStats(double damage, double speed, double range) {
            this.damage = damage;
            this.speed = speed;
            this.range = range;
        }
    }

    private static WeaponStats getStats(Item item) {
        if (item == ModItems.GOLDEN_FLOWER.get()) return new WeaponStats(2.0D, 1.5D, 2.0D);
        if (item == ModItems.DOOMFIST.get()) return new WeaponStats(10.0D, 2.0D, 2.0D);
        if (item == ModItems.DOOMFIST_V2.get()) return new WeaponStats(8.0D, 2.0D, 2.0D);
        if (item == ModItems.OPTIC_BLAST.get()) return new WeaponStats(3.0D, 4.0D, 3.0D);
        if (item == ModItems.HOLY_DUALITY_BLADE.get()) return new WeaponStats(8.0D, 1.6D, 2.0D);
        if (item == ModItems.MECHA_OVERDRIVE.get()) return new WeaponStats(10.0D, 1.4D, 2.0D);
        if (item == ModItems.BROKEN_DIAMOND.get()) return new WeaponStats(8.0D, 1.6D, 2.0D);
        if (item == ModItems.THE_TEARS.get()) return new WeaponStats(4.0D, 2.5D, 2.0D);
        if (item == ModItems.SMART_HALBERD.get()) return new WeaponStats(9.0D, 1.0D, 4.5D);
        return new WeaponStats(0.0D, 0.0D, 0.0D);
    }

    private static int getEstimatedDps(Item item) {
        if (item == ModItems.GOLDEN_FLOWER.get()) return 15;
        if (item == ModItems.DOOMFIST.get()) return 38;
        if (item == ModItems.DOOMFIST_V2.get()) return 32;
        if (item == ModItems.OPTIC_BLAST.get()) return 45;
        if (item == ModItems.HOLY_DUALITY_BLADE.get()) return 42;
        if (item == ModItems.MECHA_OVERDRIVE.get()) return 58;
        if (item == ModItems.BROKEN_DIAMOND.get()) return 50;
        if (item == ModItems.THE_TEARS.get()) return 36;
        if (item == ModItems.SMART_HALBERD.get()) return 22;
        return 0;
    }

    private static class WeaponKeys {
        final String desc1;
        final String desc2;
        final String desc4;
        final String desc5;
        final String lore;

        WeaponKeys(String name, boolean isSpecialKeys) {
            this.desc1 = "item.xeb." + name + ".desc1";
            this.desc2 = "item.xeb." + name + ".desc2";
            if (name.equals("smart_halberd")) {
                this.desc4 = null;
                this.desc5 = null;
                this.lore = "item.xeb.smart_halberd.lore";
            } else if (isSpecialKeys) {
                this.desc4 = "item.xeb." + name + ".activa1";
                this.desc5 = "item.xeb." + name + ".activa2";
                this.lore = "item.xeb." + name + ".lore";
            } else {
                this.desc4 = "item.xeb." + name + ".desc4";
                this.desc5 = "item.xeb." + name + ".desc5";
                this.lore = name.equals("optic_blast") ? "item.xeb.optic_blast.desc6" : "item.xeb." + name + ".desc3";
            }
        }
    }

    private static WeaponKeys getKeys(Item item) {
        if (item == ModItems.GOLDEN_FLOWER.get()) return new WeaponKeys("golden_flower", false);
        if (item == ModItems.DOOMFIST.get()) return new WeaponKeys("doomfist", false);
        if (item == ModItems.DOOMFIST_V2.get()) return new WeaponKeys("doomfist_v2", false);
        if (item == ModItems.OPTIC_BLAST.get()) return new WeaponKeys("optic_blast", false);
        if (item == ModItems.HOLY_DUALITY_BLADE.get()) return new WeaponKeys("holy_duality_blade", false);
        if (item == ModItems.MECHA_OVERDRIVE.get()) return new WeaponKeys("mecha_overdrive", false);
        if (item == ModItems.BROKEN_DIAMOND.get()) return new WeaponKeys("broken_diamond", true);
        if (item == ModItems.THE_TEARS.get()) return new WeaponKeys("the_tears", true);
        if (item == ModItems.SMART_HALBERD.get()) return new WeaponKeys("smart_halberd", false);
        return null;
    }

    private static Component createWaveName(String nameText) {
        MutableComponent result = Component.empty();
        long time = System.currentTimeMillis();
        for (int i = 0; i < nameText.length(); i++) {
            char c = nameText.charAt(i);
            // Color wave phase offset based on character index
            double phase = ((time % 2000) / 2000.0 * 2.0 * Math.PI) - (i * 0.25);
            // Cycle between deep crimson red and bright orange-gold
            int r = (int) (210 + 45 * Math.sin(phase));
            int g = (int) (40 + 40 * Math.sin(phase));
            int b = (int) (20 + 20 * Math.sin(phase));
            int rgb = (r << 16) | (g << 8) | b;
            result.append(Component.literal(String.valueOf(c))
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)).withBold(true)));
        }
        return result;
    }

    private static Component createAquaWaveName(String nameText) {
        MutableComponent result = Component.empty();
        long time = System.currentTimeMillis();
        for (int i = 0; i < nameText.length(); i++) {
            char c = nameText.charAt(i);
            // Wave phase offset based on character index
            double phase = ((time % 2000) / 2000.0 * 2.0 * Math.PI) - (i * 0.25);
            // Cycle between bright aqua and soft mint-cyan
            int r = (int) (10 + 10 * Math.sin(phase));
            int g = (int) (225 + 30 * Math.sin(phase));
            int b = (int) (195 + 30 * Math.sin(phase));
            int rgb = (r << 16) | (g << 8) | b;
            result.append(Component.literal(String.valueOf(c))
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)).withBold(true)));
        }
        return result;
    }

    private static Component createPurpleWaveName(String nameText) {
        MutableComponent result = Component.empty();
        long time = System.currentTimeMillis();
        for (int i = 0; i < nameText.length(); i++) {
            char c = nameText.charAt(i);
            double phase = ((time % 2000) / 2000.0 * 2.0 * Math.PI) - (i * 0.25);
            int r = (int) (185 + 55 * Math.sin(phase));
            int g = (int) (50 + 30 * Math.sin(phase));
            int b = (int) (210 + 30 * Math.sin(phase));
            int rgb = (r << 16) | (g << 8) | b;
            result.append(Component.literal(String.valueOf(c))
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)).withBold(true)));
        }
        return result;
    }

    private static Component createGoldWaveName(String nameText) {
        MutableComponent result = Component.empty();
        long time = System.currentTimeMillis();
        for (int i = 0; i < nameText.length(); i++) {
            char c = nameText.charAt(i);
            double phase = ((time % 2000) / 2000.0 * 2.0 * Math.PI) - (i * 0.25);
            int r = 255;
            int g = (int) (200 + 50 * Math.sin(phase));
            int b = (int) (30 + 30 * Math.sin(phase));
            int rgb = (r << 16) | (g << 8) | b;
            result.append(Component.literal(String.valueOf(c))
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)).withBold(true)));
        }
        return result;
    }

    private static String getWeaponName(String id) {
        return switch (id) {
            case "the_tears"     -> Component.translatable("item.xeb.the_tears").getString();
            case "golden_flower" -> Component.translatable("item.xeb.golden_flower").getString();
            default              -> id;
        };
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (isModWeapon(stack)) {
            List<Component> tooltip = event.getToolTip();
            if (tooltip.isEmpty()) return;

            // 1. Save and apply animated wave style to the weapon name
            Component originalName = tooltip.get(0);
            Component customName;
            if (stack.is(ModItems.SMART_HALBERD.get())) {
                customName = createAquaWaveName(originalName.getString());
            } else {
                customName = createWaveName(originalName.getString());
            }

            tooltip.clear();
            tooltip.add(customName);

            WeaponKeys keys = getKeys(stack.getItem());
            WeaponStats stats = getStats(stack.getItem());
            if (keys == null) return;

            if (!Screen.hasShiftDown()) {
                // Default: only show lore and Shift prompt
                tooltip.add(Component.translatable(keys.lore));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.shift_prompt"));
            } else {
                // Shift held: full technical details
                // Description
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.description"));
                tooltip.add(Component.translatable(keys.desc1).withStyle(ChatFormatting.GRAY));

                // Stats
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.stats"));
                tooltip.add(Component.translatable("gui.xeb.tooltip.base_damage")
                        .append(Component.literal(String.valueOf((int) stats.damage))));
                tooltip.add(Component.translatable("gui.xeb.tooltip.attack_speed")
                        .append(Component.literal(String.valueOf(stats.speed))));
                tooltip.add(Component.translatable("gui.xeb.tooltip.attack_range")
                        .append(Component.literal(String.valueOf(stats.range))));
                tooltip.add(Component.translatable("gui.xeb.tooltip.dps")
                        .append(Component.literal(String.valueOf(getEstimatedDps(stack.getItem())))));

                // Abilities
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.abilities"));
                
                // 1. Right Click Ability (using desc2)
                tooltip.add(Component.translatable("gui.xeb.tooltip.right_click_prefix")
                        .append(Component.translatable(keys.desc2).withStyle(ChatFormatting.GRAY)));
                
                // 2. Active Abilities (if present, using desc4 and desc5)
                if (keys.desc4 != null && keys.desc5 != null) {
                    tooltip.add(Component.literal("  §b• ").append(Component.translatable(keys.desc4, Component.keybind("key.xeb.activa_1")).withStyle(ChatFormatting.YELLOW)));
                    tooltip.add(Component.literal("  §b• ").append(Component.translatable(keys.desc5, Component.keybind("key.xeb.activa_2")).withStyle(ChatFormatting.YELLOW)));
                }

                // Enchantments (Better formatted, clean bullet points without stars)
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
                if (!enchantments.isEmpty()) {
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("gui.xeb.tooltip.enchantments"));
                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        Component fullName = entry.getKey().getFullname(entry.getValue());
                        tooltip.add(Component.literal("  §d• ").append(fullName.copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
                    }
                }

                // Lore
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable(keys.lore));
            }

            // Advanced tooltips compatibility
            if (event.getFlags().isAdvanced()) {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (rl != null) {
                    tooltip.add(Component.literal(rl.toString()).withStyle(ChatFormatting.DARK_GRAY));
                }
                if (stack.hasTag()) {
                    int size = stack.getTag().getAllKeys().size();
                    tooltip.add(Component.literal("NBT: " + size + " tag(s)").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        } else if (isExtremeBurstCurio(stack)) {
            List<Component> tooltip = event.getToolTip();
            if (tooltip.isEmpty()) return;

            Component originalName = tooltip.get(0);
            Component customName = createPurpleWaveName(originalName.getString());

            tooltip.clear();
            tooltip.add(customName);

            Item curioItem = stack.getItem();
            ExtremeBurstRegistry.ExtremeBurstEntry entry = ExtremeBurstRegistry.getEntry(curioItem);
            if (entry == null) return;

            String desc1Key = "";
            String desc2Key = "";
            String requiresKey = "";
            String loreKey = "";

            if (curioItem == ModItems.DOGMA.get()) {
                desc1Key = "item.xeb.dogma.desc1";
                desc2Key = "item.xeb.dogma.desc2";
                requiresKey = "item.xeb.dogma.requires";
                loreKey = "item.xeb.dogma.enigma_lore";
            } else if (curioItem == ModItems.OMEGA_FLOWERY.get()) {
                desc1Key = "item.xeb.omega_flowery.desc1";
                desc2Key = "item.xeb.omega_flowery.desc2";
                requiresKey = "item.xeb.omega_flowery.requires";
                loreKey = "item.xeb.omega_flowery.enigma_lore";
            } else if (curioItem == ModItems.QUANTUM_CAT_BARRAGE.get()) {
                desc1Key = "item.xeb.quantum_cat_barrage.desc1";
                desc2Key = "item.xeb.quantum_cat_barrage.desc2";
                loreKey = "item.xeb.quantum_cat_barrage.enigma_lore";
            }

            if (!Screen.hasShiftDown()) {
                if (!loreKey.isEmpty()) {
                    tooltip.add(Component.translatable(loreKey).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                    tooltip.add(Component.literal(""));
                }
                tooltip.add(Component.translatable("gui.xeb.tooltip.shift_prompt"));
            } else {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.description"));
                
                String typeStr = entry.type == ExtremeBurstRegistry.BurstType.UNIVERSAL 
                        ? Component.translatable("gui.xeb.tooltip.extreme_burst.accessibility.universal").getString() 
                        : Component.translatable("gui.xeb.tooltip.extreme_burst.accessibility.limited", 
                                entry.requiredWeaponName != null ? getWeaponName(entry.requiredWeaponName) : "").getString();
                
                String verStr = entry.version == ExtremeBurstRegistry.BurstVersion.INSTANT 
                        ? Component.translatable("gui.xeb.tooltip.extreme_burst.type.instant").getString() 
                        : Component.translatable("gui.xeb.tooltip.extreme_burst.type.instance").getString();
                
                tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.description_label")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                        .append(Component.literal(typeStr + " / " + verStr).withStyle(ChatFormatting.LIGHT_PURPLE)));

                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.stats"));
                
                double cdSec = entry.cooldownTicks / 20.0;
                tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.cooldown_label")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                        .append(Component.translatable("gui.xeb.tooltip.extreme_burst.seconds_value", cdSec).withStyle(ChatFormatting.LIGHT_PURPLE)));
                
                if (entry.durationTicks > 0) {
                    double durSec = entry.durationTicks / 20.0;
                    tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.duration_label")
                            .withStyle(ChatFormatting.DARK_PURPLE)
                            .append(Component.translatable("gui.xeb.tooltip.extreme_burst.seconds_value", durSec).withStyle(ChatFormatting.LIGHT_PURPLE)));
                }

                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.abilities"));
                
                if (curioItem == ModItems.QUANTUM_CAT_BARRAGE.get()) {
                    tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.effects.cat").withStyle(ChatFormatting.LIGHT_PURPLE));
                } else if (curioItem == ModItems.DOGMA.get()) {
                    tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.effects.dogma").withStyle(ChatFormatting.LIGHT_PURPLE));
                } else if (curioItem == ModItems.OMEGA_FLOWERY.get()) {
                    tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.effects.flowery").withStyle(ChatFormatting.LIGHT_PURPLE));
                    tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.effects.flowery.act1").withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.effects.flowery.act2").withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.translatable("gui.xeb.tooltip.extreme_burst.effects.flowery.act3").withStyle(ChatFormatting.AQUA));
                }

                if (!loreKey.isEmpty()) {
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable(loreKey).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                }
            }

            if (event.getFlags().isAdvanced()) {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (rl != null) {
                    tooltip.add(Component.literal(rl.toString()).withStyle(ChatFormatting.DARK_GRAY));
                }
                if (stack.hasTag()) {
                    int size = stack.getTag().getAllKeys().size();
                    tooltip.add(Component.literal("NBT: " + size + " tag(s)").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        } else if (stack.is(ModItems.ENIGMA_BIOS.get())) {
            List<Component> tooltip = event.getToolTip();
            if (tooltip.isEmpty()) return;

            Component originalName = tooltip.get(0);
            Component customName = createGoldWaveName(originalName.getString());

            tooltip.clear();
            tooltip.add(customName);

            if (!Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("item.xeb.enigma_bios.lore").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.shift_prompt"));
            } else {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.description"));
                tooltip.add(Component.translatable("item.xeb.enigma_bios.desc").withStyle(ChatFormatting.GRAY));

                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.abilities"));
                tooltip.add(Component.translatable("gui.xeb.tooltip.right_click_prefix")
                        .append(Component.translatable("gui.xeb.tooltip.enigma_bios.open").withStyle(ChatFormatting.GRAY)));
                tooltip.add(Component.literal("  §b• ")
                        .append(Component.translatable("gui.xeb.tooltip.enigma_bios.quick_access", 
                                Component.keybind("key.xeb.enigma").withStyle(ChatFormatting.LIGHT_PURPLE)).withStyle(ChatFormatting.YELLOW)));

                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.xeb.tooltip.stats"));
                tooltip.add(Component.translatable("gui.xeb.tooltip.enigma_bios.history").withStyle(ChatFormatting.GOLD));

                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    net.minecraft.nbt.CompoundTag pTag = mc.player.getPersistentData();
                    net.minecraft.nbt.ListTag recentList = pTag.getList("xebRecentMedallions", 10);
                    if (recentList.isEmpty()) {
                        tooltip.add(Component.translatable("gui.xeb.tooltip.enigma_bios.no_records").withStyle(ChatFormatting.DARK_GRAY));
                    } else {
                        for (int i = 0; i < Math.min(3, recentList.size()); i++) {
                            net.minecraft.nbt.CompoundTag entry = recentList.getCompound(i);
                            String name = entry.getString("name");
                            String tier = entry.getString("tier");
                            ChatFormatting tierColor = switch (tier) {
                                case "LEGENDARY" -> ChatFormatting.AQUA;
                                case "RARE" -> ChatFormatting.LIGHT_PURPLE;
                                default -> ChatFormatting.WHITE;
                            };
                            tooltip.add(Component.literal("  ")
                                    .append(Component.literal("[" + tier + "] ").withStyle(tierColor))
                                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW)));
                        }
                    }
                }

                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("item.xeb.enigma_bios.lore").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            }

            if (event.getFlags().isAdvanced()) {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (rl != null) {
                    tooltip.add(Component.literal(rl.toString()).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        if (isModWeapon(stack) || isExtremeBurstCurio(stack) || stack.is(ModItems.ENIGMA_BIOS.get())) {
            long time = System.currentTimeMillis();
            double phaseStart = (time % 1500) / 1500.0 * 2.0 * Math.PI;
            double phaseEnd = phaseStart + Math.PI;

            if (stack.is(ModItems.ENIGMA_BIOS.get())) {
                // Set dynamic animated golden yellow border glow and dark amber background
                int rStart = 255;
                int gStart = (int) (200 + 50 * Math.sin(phaseStart));
                int bStart = (int) (30 + 30 * Math.sin(phaseStart));
                int borderStart = 0xFF000000 | (rStart << 16) | (gStart << 8) | bStart;

                int rEnd = 255;
                int gEnd = (int) (200 + 50 * Math.sin(phaseEnd));
                int bEnd = (int) (30 + 30 * Math.sin(phaseEnd));
                int borderEnd = 0xFF000000 | (rEnd << 16) | (gEnd << 8) | bEnd;

                event.setBorderStart(borderStart);
                event.setBorderEnd(borderEnd);
                event.setBackground(0xF21C1602); // Dark amber-gold background
            } else if (isExtremeBurstCurio(stack)) {
                // Purple/magenta glow border and dark purple background
                int rStart = (int) (185 + 55 * Math.sin(phaseStart));
                int gStart = (int) (50 + 30 * Math.sin(phaseStart));
                int bStart = (int) (210 + 30 * Math.sin(phaseStart));
                int borderStart = 0xFF000000 | (rStart << 16) | (gStart << 8) | bStart;

                int rEnd = (int) (185 + 55 * Math.sin(phaseEnd));
                int gEnd = (int) (50 + 30 * Math.sin(phaseEnd));
                int bEnd = (int) (210 + 30 * Math.sin(phaseEnd));
                int borderEnd = 0xFF000000 | (rEnd << 16) | (gEnd << 8) | bEnd;

                event.setBorderStart(borderStart);
                event.setBorderEnd(borderEnd);
                event.setBackground(0xF2120215); // Dark purple background
            } else if (stack.is(ModItems.SMART_HALBERD.get())) {
                // Set dynamic animated legendary aqua border and dark teal background glow
                int rStart = (int) (10 + 10 * Math.sin(phaseStart));
                int gStart = (int) (225 + 30 * Math.sin(phaseStart));
                int bStart = (int) (195 + 30 * Math.sin(phaseStart));
                int borderStart = 0xFF000000 | (rStart << 16) | (gStart << 8) | bStart;

                int rEnd = (int) (10 + 10 * Math.sin(phaseEnd));
                int gEnd = (int) (150 + 30 * Math.sin(phaseEnd));
                int bEnd = (int) (120 + 30 * Math.sin(phaseEnd));
                int borderEnd = 0xFF000000 | (rEnd << 16) | (gEnd << 8) | bEnd;

                event.setBorderStart(borderStart);
                event.setBorderEnd(borderEnd);
                event.setBackground(0xF2001510);
            } else {
                // Set dynamic animated mythic red border and dark red background glow
                int rStart = (int) (200 + 55 * Math.sin(phaseStart));
                int gStart = (int) (20 + 20 * Math.sin(phaseStart));
                int bStart = (int) (30 + 30 * Math.sin(phaseStart));
                int borderStart = 0xFF000000 | (rStart << 16) | (gStart << 8) | bStart;

                int rEnd = (int) (200 + 55 * Math.sin(phaseEnd));
                int gEnd = (int) (20 + 20 * Math.sin(phaseEnd));
                int bEnd = (int) (30 + 30 * Math.sin(phaseEnd));
                int borderEnd = 0xFF000000 | (rEnd << 16) | (gEnd << 8) | bEnd;

                event.setBorderStart(borderStart);
                event.setBorderEnd(borderEnd);
                event.setBackground(0xF2150505);
            }
        }
    }
}
