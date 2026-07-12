package org.xeb.xeb.loot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.Config;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;

import java.util.List;

@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EliteLootHandler {

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

        for (MedallionData m : medallions) {
            // ── BITS: 1 bit por medallión (con chance), tier = tier del medallón ──
            double bitChance = switch (m.getTier()) {
                case COMMON -> Config.bronzeBitDropChance;
                case RARE -> Config.silverBitDropChance;
                case LEGENDARY -> Config.goldBitDropChance;
            };
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

            // ── ESSENCE: chance raro, 1 essence del buff tipo ──
            double essenceChance = Config.essenceDropChance * switch (m.getTier()) {
                case COMMON -> 0.5;
                case RARE -> 1.0;
                case LEGENDARY -> 1.5;
            };
            if (isBoss) essenceChance = Math.min(1.0, essenceChance + 0.15);

            if (random.nextDouble() < essenceChance) {
                ItemStack essence = EssenceRegistry.createStack(m.getBuff().getId(), m.getTier());
                if (!essence.isEmpty()) {
                    ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), essence);
                    event.getDrops().add(drop);
                }
            }
        }
    }
}
