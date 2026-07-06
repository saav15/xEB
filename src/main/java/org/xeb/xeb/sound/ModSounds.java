package org.xeb.xeb.sound;

import org.xeb.xeb.Xeb;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Xeb.MODID);

    public static final RegistryObject<SoundEvent> JARONA1 = SOUNDS.register("jarona1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "jarona1")));
    
    public static final RegistryObject<SoundEvent> JARONA2 = SOUNDS.register("jarona2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "jarona2")));
    
    public static final RegistryObject<SoundEvent> JARONA3 = SOUNDS.register("jarona3",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "jarona3")));
    
    public static final RegistryObject<SoundEvent> JARONA4 = SOUNDS.register("jarona4",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "jarona4")));
    
    public static final RegistryObject<SoundEvent> JARONA_FAKE = SOUNDS.register("jaronafake",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "jaronafake")));

    public static final RegistryObject<SoundEvent> ABSORB_FLOWER_CLONE = SOUNDS.register("absorbflowerclone",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "absorbflowerclone")));

    public static final RegistryObject<SoundEvent> FLOWER_DANCE1 = SOUNDS.register("flowerdance1",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "flowerdance1")));

    public static final RegistryObject<SoundEvent> FLOWER_DANCE2 = SOUNDS.register("flowerdance2",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "flowerdance2")));

    public static final RegistryObject<SoundEvent> FLOWER_DANCE_FAKE = SOUNDS.register("flowerdancefake",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Xeb.MODID, "flowerdancefake")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
