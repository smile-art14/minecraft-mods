package com.shenmi.yoyo.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Hidden synchronized marker used to distinguish the standing-on-head pose
 * from ordinary vanilla player riding. Mob effects are synchronized by the
 * server, so every tracking client receives the same pose state.
 */
public final class StandingOnPlayerEffect extends MobEffect {
    public StandingOnPlayerEffect() {
        super(MobEffectCategory.NEUTRAL, 0x6EC8FF);
    }
}
