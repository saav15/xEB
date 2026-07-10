package org.xeb.xeb.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists the Permanight event state across server restarts.
 *
 * <p><b>Performance fix (N11):</b> The original called {@link #setDirty()} inside
 * {@code setTicksRemaining()}, which is invoked every server tick during the 24 000‑tick
 * Permanight cycle.  That saturated the SavedData I/O queue with 24 000 dirty marks
 * per Permanight.  {@code ticksRemaining} is now updated in-memory without marking
 * dirty; {@link #markDirtyTick()} is called by {@link org.xeb.xeb.event.PermanightLootHandler}
 * every 200 ticks (~10 s) to flush a reasonably up-to-date snapshot to disk without
 * I/O spam.  State-changing operations ({@link #setActive}, {@link #setForceNextNight})
 * still call {@link #setDirty()} immediately because they are rare and critical.</p>
 */
public class PermanightSavedData extends SavedData {
    private static final String DATA_NAME = "xeb_permanight";

    private boolean active = false;
    private int ticksRemaining = 0;
    private boolean forceNextNight = false;

    public PermanightSavedData() {}

    public static PermanightSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            PermanightSavedData::load,
            PermanightSavedData::new,
            DATA_NAME
        );
    }

    public static PermanightSavedData load(CompoundTag nbt) {
        PermanightSavedData data = new PermanightSavedData();
        data.active = nbt.getBoolean("active");
        data.ticksRemaining = nbt.getInt("ticksRemaining");
        data.forceNextNight = nbt.getBoolean("forceNextNight");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putBoolean("active", this.active);
        nbt.putInt("ticksRemaining", this.ticksRemaining);
        nbt.putBoolean("forceNextNight", this.forceNextNight);
        return nbt;
    }

    // ── State-change methods — call setDirty() immediately (rare, critical) ──

    public boolean isActive() { return active; }

    public void setActive(boolean active) {
        this.active = active;
        setDirty(); // immediate flush — start/end of Permanight must survive a crash
    }

    public boolean isForceNextNight() { return forceNextNight; }

    public void setForceNextNight(boolean forceNextNight) {
        this.forceNextNight = forceNextNight;
        setDirty(); // immediate flush — admin command, must persist
    }

    // ── Tick countdown — updated in-memory; caller flushes periodically ───────

    public int getTicksRemaining() { return ticksRemaining; }

    /**
     * Decrements the remaining tick counter WITHOUT calling {@link #setDirty()}.
     * The caller ({@link org.xeb.xeb.event.PermanightLootHandler}) flushes via
     * {@link #markDirtyTick()} every 200 ticks so disk writes stay under 120/Permanight.
     */
    public void setTicksRemaining(int ticksRemaining) {
        this.ticksRemaining = ticksRemaining;
        // No setDirty() — handled externally at a throttled interval
    }

    /**
     * Explicitly marks this data as dirty so the next auto-save will persist
     * the current {@code ticksRemaining} value.
     * Called by {@link org.xeb.xeb.event.PermanightLootHandler} every 200 ticks.
     */
    public void markDirtyTick() {
        setDirty();
    }
}
