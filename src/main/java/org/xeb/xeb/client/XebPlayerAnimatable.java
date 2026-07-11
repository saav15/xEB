package org.xeb.xeb.client;

import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Wrapper que permite a Player ser animado por GeckoLib.
 * El XebPlayerRenderer usa este wrapper internamente.
 */
public class XebPlayerAnimatable implements GeoAnimatable {
    private Player player;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public XebPlayerAnimatable(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (player == null) {
                return PlayState.STOP;
            }
            String animName = XebPlayerAnimator.getAnimationName(player);
            if (animName == null) {
                return PlayState.STOP;
            }
            return state.setAndContinue(RawAnimation.begin().thenPlay(animName));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return player != null ? player.tickCount : 0;
    }
}
