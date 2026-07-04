package org.xeb.xeb.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setDirty();
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public void setTicksRemaining(int ticksRemaining) {
        this.ticksRemaining = ticksRemaining;
        setDirty();
    }

    public boolean isForceNextNight() {
        return forceNextNight;
    }

    public void setForceNextNight(boolean forceNextNight) {
        this.forceNextNight = forceNextNight;
        setDirty();
    }
}
