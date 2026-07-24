package org.xeb.xeb.client;

import net.minecraft.client.Minecraft;
import org.xeb.xeb.client.gui.EnigmaBiosScreen;
import org.xeb.xeb.client.gui.HUDPositionScreen;
import org.xeb.xeb.client.gui.XebCompletionToast;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientAccess {
    public static void openEnigmaBiosScreen() {
        Minecraft.getInstance().setScreen(new EnigmaBiosScreen());
    }

    public static void showCompletionToast(int newlyUnlocked) {
        XebCompletionToast.show(newlyUnlocked);
    }

    @SuppressWarnings("deprecation")
    public static void registerClientConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new HUDPositionScreen(parent)
                )
        );
    }
}
