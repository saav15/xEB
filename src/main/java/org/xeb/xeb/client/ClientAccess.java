package org.xeb.xeb.client;

import net.minecraft.client.Minecraft;
import org.xeb.xeb.client.gui.EnigmaBiosScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientAccess {
    public static void openEnigmaBiosScreen() {
        Minecraft.getInstance().setScreen(new EnigmaBiosScreen());
    }
}
