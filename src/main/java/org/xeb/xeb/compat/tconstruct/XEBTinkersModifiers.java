package org.xeb.xeb.compat.tconstruct;

import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import org.xeb.xeb.Xeb;

public class XEBTinkersModifiers {
    public static final ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(Xeb.MODID);

    public static final StaticModifier<Modifier> SPIKY               = MODIFIERS.register("spiky",               () -> new XebModifier(0x800000));
    public static final StaticModifier<Modifier> REACTIVE            = MODIFIERS.register("reactive",            () -> new XebModifier(0xFF8C00));
    public static final StaticModifier<Modifier> DAMAGING            = MODIFIERS.register("damaging",            () -> new XebModifier(0xDC143C));
    public static final StaticModifier<Modifier> FLAMING             = MODIFIERS.register("flaming",             () -> new XebModifier(0xFF4500));
    public static final StaticModifier<Modifier> CREEPY              = MODIFIERS.register("creepy",              () -> new XebModifier(0x00FF00));
    public static final StaticModifier<Modifier> MAD                 = MODIFIERS.register("mad",                 () -> new XebModifier(0xB22222));
    public static final StaticModifier<Modifier> MIRROR              = MODIFIERS.register("mirror",              () -> new XebModifier(0xC0C0C0));
    public static final StaticModifier<Modifier> PLOW                = MODIFIERS.register("plow",                () -> new XebModifier(0x8B4513));
    public static final StaticModifier<Modifier> RESONANT            = MODIFIERS.register("resonant",            () -> new XebModifier(0x00BFFF));
    public static final StaticModifier<Modifier> SANDY               = MODIFIERS.register("sandy",               () -> new XebModifier(0xE6C280));
    public static final StaticModifier<Modifier> STATIC              = MODIFIERS.register("static",              () -> new XebModifier(0x00FFFF));
    public static final StaticModifier<Modifier> ABSORBENT           = MODIFIERS.register("absorbent",           () -> new XebModifier(0x9400D3));
    public static final StaticModifier<Modifier> HARDY               = MODIFIERS.register("hardy",               () -> new XebModifier(0x4682B4));
    public static final StaticModifier<Modifier> HEALTHY             = MODIFIERS.register("healthy",             () -> new XebModifier(0xFF1493));
    public static final StaticModifier<Modifier> PROTECTED           = MODIFIERS.register("protected",           () -> new XebModifier(0xFFD700));
    public static final StaticModifier<Modifier> SHIELDED            = MODIFIERS.register("shielded",            () -> new XebModifier(0x708090));
    public static final StaticModifier<Modifier> TOUGH               = MODIFIERS.register("tough",               () -> new XebModifier(0x2F4F4F));
    public static final StaticModifier<Modifier> UNDYING             = MODIFIERS.register("undying",             () -> new XebModifier(0x00FA9A));
    public static final StaticModifier<Modifier> BOUNCY              = MODIFIERS.register("bouncy",              () -> new XebModifier(0x32CD32));
    public static final StaticModifier<Modifier> EVOLVING            = MODIFIERS.register("evolving",            () -> new XebModifier(0x1E90FF));
    public static final StaticModifier<Modifier> LUCKY               = MODIFIERS.register("lucky",               () -> new XebModifier(0xFFA500));
    public static final StaticModifier<Modifier> MEGA                = MODIFIERS.register("mega",                () -> new XebModifier(0x9932CC));
    public static final StaticModifier<Modifier> SPEEDY              = MODIFIERS.register("speedy",              () -> new XebModifier(0xFFFF00));
    public static final StaticModifier<Modifier> STICKY              = MODIFIERS.register("sticky",              () -> new XebModifier(0x2E8B57));
    public static final StaticModifier<Modifier> TWIN                = MODIFIERS.register("twin",                () -> new XebModifier(0xFF00FF));
    public static final StaticModifier<Modifier> DEPRESSING          = MODIFIERS.register("depressing",          () -> new XebModifier(0x4B0082));
    public static final StaticModifier<Modifier> INFESTED            = MODIFIERS.register("infested",            () -> new XebModifier(0x556B2F));
    public static final StaticModifier<Modifier> SLIGHTLY_DEPRESSING = MODIFIERS.register("slightly_depressing", () -> new XebModifier(0x7B68EE));

    public static void register(IEventBus modEventBus) {
        MODIFIERS.register(modEventBus);
    }
}
