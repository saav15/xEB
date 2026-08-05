package org.xeb.xeb.loot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.Config;
import org.xeb.xeb.enchantment.ModEnchantments;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EliteLootHandler {

    private static final Set<String> GROUP_A_BUFFS = Set.of(
            "absorbent", "damaging", "flaming", "sandy", "infested", "shielded",
            "tough", "speedy", "spiky", "reactive", "plow", "hardy"
    );

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide()) return;
        if (!Config.enabled || !Config.lootDropsEnabled) return;

        List<MedallionData> medallions = MedallionManager.getMedallions(entity);
        if (medallions == null || medallions.isEmpty()) return;

        boolean isBoss = MedallionManager.isBoss(entity);
        int looting = event.getLootingLevel();
        ServerLevel level = (ServerLevel) entity.level();
        RandomSource random = level.getRandom();

        // Check for Medallero enchantment level on killer's weapon
        int medalleroLvl = 0;
        if (event.getSource().getEntity() instanceof Player killer) {
            medalleroLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.MEDALLERO.get(), killer.getMainHandItem());
            if (medalleroLvl == 0) {
                medalleroLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.MEDALLERO.get(), killer.getOffhandItem());
            }
        }

        for (MedallionData m : medallions) {
            // ── BITS: 1 bit por medallón (con chance), tier = tier del medallón ──
            double bitChance = switch (m.getTier()) {
                case COMMON -> Config.bronzeBitDropChance;
                case RARE -> Config.silverBitDropChance;
                case LEGENDARY -> Config.goldBitDropChance;
            };
            if (medalleroLvl > 0) {
                bitChance += 0.15D * medalleroLvl;
            }
            if (isBoss && Config.bossBitGuaranteed) {
                bitChance = Math.min(1.0, bitChance + 0.20);
            }

            if (random.nextDouble() < bitChance) {
                int count = 1;
                if (random.nextDouble() < looting * 0.15) count++;
                if (m.getTier() == MedallionType.LEGENDARY && random.nextDouble() < 0.30) count++;

                Item bitItem = switch (m.getTier()) {
                    case COMMON -> ModItems.BRONZE_ELITE_BIT.get();
                    case RARE -> ModItems.SILVER_ELITE_BIT.get();
                    case LEGENDARY -> ModItems.GOLD_ELITE_BIT.get();
                };
                ItemStack stack = new ItemStack(bitItem, count);
                ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
                event.getDrops().add(drop);
            }

            // ── ESSENCE: Group A (2%, 3%, 6%) vs Group B (1%, 2%, 3%) + Medallero Bonus (+1.5%, +3.0%, +5.0%) ──
            String buffId = m.getBuff().getId();
            boolean isGroupA = GROUP_A_BUFFS.contains(buffId);

            double baseChance;
            if (isGroupA) {
                baseChance = switch (m.getTier()) {
                    case COMMON -> 0.02D;
                    case RARE -> 0.03D;
                    case LEGENDARY -> 0.06D;
                };
            } else {
                baseChance = switch (m.getTier()) {
                    case COMMON -> 0.01D;
                    case RARE -> 0.02D;
                    case LEGENDARY -> 0.03D;
                };
            }

            double medalleroBonus = switch (medalleroLvl) {
                case 1 -> 0.015D;
                case 2 -> 0.030D;
                case 3 -> 0.050D;
                default -> 0.0D;
            };

            double essenceChance = baseChance + medalleroBonus;
            if (isBoss) {
                essenceChance += 0.15D;
            }

            if (random.nextDouble() < essenceChance) {
                ItemStack essence = EssenceRegistry.createStack(buffId, m.getTier());
                if (!essence.isEmpty()) {
                    ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), essence);
                    event.getDrops().add(drop);
                }
            }
        }
    }
}
