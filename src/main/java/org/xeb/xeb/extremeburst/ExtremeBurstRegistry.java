package org.xeb.xeb.extremeburst;

import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.xeb.xeb.item.ModItems;

import java.util.*;

/**
 * Registry that maps Curios item → Extreme Burst entry.
 *
 * <p>An Extreme Burst is an ultimate ability tied to the "extreme_burst" Curios slot.
 * It is activated by the Activa 3 key (button=3, tecla N) regardless of which weapon
 * the player holds — following the same structural pattern as the Quantum Cat Barrage.</p>
 *
 * <h3>Types:</h3>
 * <ul>
 *   <li>{@link BurstType#UNIVERSAL} — usable regardless of held weapon.</li>
 *   <li>{@link BurstType#LIMITED} — requires a specific weapon in main/off hand.</li>
 * </ul>
 *
 * <h3>Versions:</h3>
 * <ul>
 *   <li>{@link BurstVersion#INSTANT} — fires immediately; {@code durationTicks} is irrelevant.</li>
 *   <li>{@link BurstVersion#INSTANCE} — enters a timed transformed state for {@code durationTicks}.</li>
 * </ul>
 */
public class ExtremeBurstRegistry {

    public enum BurstType    { UNIVERSAL, LIMITED }
    public enum BurstVersion { INSTANT,   INSTANCE }

    public static final class ExtremeBurstEntry {
        public final Item curioItem;
        public final BurstType type;
        public final BurstVersion version;
        /** Null for {@link BurstType#UNIVERSAL}. One of: "the_tears", "golden_flower". */
        public final String requiredWeaponName;
        public final int cooldownTicks;
        /** 0 for {@link BurstVersion#INSTANT}. */
        public final int durationTicks;

        public ExtremeBurstEntry(Item curioItem, BurstType type, BurstVersion version,
                                 String requiredWeaponName, int cooldownTicks, int durationTicks) {
            this.curioItem         = curioItem;
            this.type              = type;
            this.version           = version;
            this.requiredWeaponName = requiredWeaponName;
            this.cooldownTicks     = cooldownTicks;
            this.durationTicks     = durationTicks;
        }
    }

    private static final Map<Item, ExtremeBurstEntry> REGISTRY = new LinkedHashMap<>();

    public static void register(ExtremeBurstEntry entry) {
        REGISTRY.put(entry.curioItem, entry);
    }

    public static ExtremeBurstEntry getEntry(Item curioItem) {
        return REGISTRY.get(curioItem);
    }

    public static Collection<ExtremeBurstEntry> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * Finds the active Extreme Burst entry for the given entity.
     *
     * <p>Search order:
     * <ol>
     *   <li><b>Curios slots</b> (primary) — equipping the item here is the intended use.</li>
     *   <li><b>Offhand</b> (fallback) — allows the HUD to show even if Curios reflection
     *       fails transiently on the client.</li>
     *   <li><b>Full inventory</b> (fallback) — same reason; keeps the HUD visible so the
     *       player knows they own the item.</li>
     * </ol>
     *
     * <p><b>Important:</b> a non-null return does NOT mean the item is equippable.
     * Use {@link #isInCurioSlot(LivingEntity, ExtremeBurstEntry)} on the server to
     * verify the item is actually in a Curios slot before allowing activation.</p>
     */
    public static ExtremeBurstEntry findActiveBurst(LivingEntity entity) {
        // 1. Primary: Curios slots
        for (net.minecraft.world.item.ItemStack stack : org.xeb.xeb.compat.ModCompatManager.getCuriosItems(entity)) {
            ExtremeBurstEntry entry = REGISTRY.get(stack.getItem());
            if (entry != null) return entry;
        }
        return null;
    }

    /**
     * Returns {@code true} if the burst item ({@code entry.curioItem}) is currently
     * equipped in a Curios slot on the given entity.
     *
     * <p>Used server-side by {@code ActuarKeyPacket} to enforce that activation
     * requires the item to be properly equipped — not just sitting in the inventory.</p>
     */
    public static boolean isInCurioSlot(LivingEntity entity, ExtremeBurstEntry entry) {
        for (net.minecraft.world.item.ItemStack stack : org.xeb.xeb.compat.ModCompatManager.getCuriosItems(entity)) {
            if (stack.is(entry.curioItem)) return true;
        }
        return false;
    }

    /**
     * Returns true if the player has a registered burst item in their inventory or offhand
     * but NOT in a Curios slot. Used to show a helpful "equip in curio" message.
     */
    public static boolean hasBurstOnlyInInventory(Player player) {
        // If it's in a curio slot, it's NOT "only in inventory"
        ExtremeBurstEntry curio = null;
        for (net.minecraft.world.item.ItemStack stack : org.xeb.xeb.compat.ModCompatManager.getCuriosItems(player)) {
            if (REGISTRY.containsKey(stack.getItem())) { curio = REGISTRY.get(stack.getItem()); break; }
        }
        if (curio != null) return false;
        // Check offhand and inventory
        if (REGISTRY.containsKey(player.getOffhandItem().getItem())) return true;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = player.getInventory().getItem(i);
            if (REGISTRY.containsKey(s.getItem())) return true;
        }
        return false;
    }


    /**
     * Returns {@code true} if the player satisfies the weapon requirement for this burst.
     * For {@link BurstType#UNIVERSAL} always returns {@code true}.
     */
    public static boolean canActivate(Player player, ExtremeBurstEntry entry) {
        if (entry.type == BurstType.UNIVERSAL) return true;
        if (entry.requiredWeaponName == null) return false;

        Item requiredItem = switch (entry.requiredWeaponName) {
            case "the_tears"     -> ModItems.THE_TEARS.get();
            case "golden_flower" -> ModItems.GOLDEN_FLOWER.get();
            default              -> null;
        };
        if (requiredItem == null) return false;

        return player.getMainHandItem().is(requiredItem)
                || player.getOffhandItem().is(requiredItem);
    }

    /**
     * Called from {@link org.xeb.xeb.Xeb#commonSetup} after all items are registered.
     */
    public static void init() {
        // ── Quantum Cat Barrage — Universal, Instant ──────────────────────────
        register(new ExtremeBurstEntry(
                ModItems.QUANTUM_CAT_BARRAGE.get(),
                BurstType.UNIVERSAL,
                BurstVersion.INSTANT,
                null,
                3600, // 180 s cooldown
                0
        ));

        // ── Dogma — Limited (The Tears), Instant ─────────────────────────────
        register(new ExtremeBurstEntry(
                ModItems.DOGMA.get(),
                BurstType.LIMITED,
                BurstVersion.INSTANT,
                "the_tears",
                4000, // 200 s cooldown
                200   // 10 s active (handled by DogmaBurstHandler)
        ));

        // ── Omega Flowery — Limited (Golden Flower), Instance ─────────────────
        register(new ExtremeBurstEntry(
                ModItems.OMEGA_FLOWERY.get(),
                BurstType.LIMITED,
                BurstVersion.INSTANCE,
                "golden_flower",
                6000, // 300 s cooldown (5 min)
                400   // 20 s duration
        ));
    }
}
