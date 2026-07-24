package org.xeb.xeb.compat.tconstruct;

import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import org.xeb.xeb.Xeb;

public class XEBTinkersModifiers {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Xeb.MODID);

    public static final StaticModifier<Modifier> SPIKY = MODIFIERS.register("spiky", Modifier::new);
    public static final StaticModifier<Modifier> REACTIVE = MODIFIERS.register("reactive", Modifier::new);
    public static final StaticModifier<Modifier> DAMAGING = MODIFIERS.register("damaging", Modifier::new);
    public static final StaticModifier<Modifier> FLAMING = MODIFIERS.register("flaming", Modifier::new);
    public static final StaticModifier<Modifier> CREEPY = MODIFIERS.register("creepy", Modifier::new);
    public static final StaticModifier<Modifier> MAD = MODIFIERS.register("mad", Modifier::new);
    public static final StaticModifier<Modifier> MIRROR = MODIFIERS.register("mirror", Modifier::new);
    public static final StaticModifier<Modifier> PLOW = MODIFIERS.register("plow", Modifier::new);
    public static final StaticModifier<Modifier> RESONANT = MODIFIERS.register("resonant", Modifier::new);
    public static final StaticModifier<Modifier> SANDY = MODIFIERS.register("sandy", Modifier::new);
    public static final StaticModifier<Modifier> STATIC = MODIFIERS.register("static", Modifier::new);
    public static final StaticModifier<Modifier> ABSORBENT = MODIFIERS.register("absorbent", Modifier::new);
    public static final StaticModifier<Modifier> HARDY = MODIFIERS.register("hardy", Modifier::new);
    public static final StaticModifier<Modifier> HEALTHY = MODIFIERS.register("healthy", Modifier::new);
    public static final StaticModifier<Modifier> PROTECTED = MODIFIERS.register("protected", Modifier::new);
    public static final StaticModifier<Modifier> SHIELDED = MODIFIERS.register("shielded", Modifier::new);
    public static final StaticModifier<Modifier> TOUGH = MODIFIERS.register("tough", Modifier::new);
    public static final StaticModifier<Modifier> UNDYING = MODIFIERS.register("undying", Modifier::new);
    public static final StaticModifier<Modifier> BOUNCY = MODIFIERS.register("bouncy", Modifier::new);
    public static final StaticModifier<Modifier> EVOLVING = MODIFIERS.register("evolving", Modifier::new);
    public static final StaticModifier<Modifier> LUCKY = MODIFIERS.register("lucky", Modifier::new);
    public static final StaticModifier<Modifier> MEGA = MODIFIERS.register("mega", Modifier::new);
    public static final StaticModifier<Modifier> SPEEDY = MODIFIERS.register("speedy", Modifier::new);
    public static final StaticModifier<Modifier> STICKY = MODIFIERS.register("sticky", Modifier::new);
    public static final StaticModifier<Modifier> TWIN = MODIFIERS.register("twin", Modifier::new);
    public static final StaticModifier<Modifier> DEPRESSING = MODIFIERS.register("depressing", Modifier::new);
    public static final StaticModifier<Modifier> INFESTED = MODIFIERS.register("infested", Modifier::new);
    public static final StaticModifier<Modifier> SLIGHTLY_DEPRESSING = MODIFIERS.register("slightly_depressing", Modifier::new);

    public static void register(IEventBus modEventBus) {
        MODIFIERS.register(modEventBus);
    }
}
