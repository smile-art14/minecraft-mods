package com.shenmi.yoyo.mixin;

import com.shenmi.yoyo.api.PlayerStandingState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds one real SynchedEntityData boolean to Player. Unlike potion effects,
 * this data is distributed to every client that tracks the player, which is
 * exactly what the remote standing-pose renderer needs.
 */
@Mixin(Player.class)
public abstract class PlayerStandingDataMixin implements PlayerStandingState {
    @Unique
    private static final EntityDataAccessor<Boolean> SHENMI_YOYO_STANDING =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Boolean> SHENMI_YOYO_SITTING =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<Integer> SHENMI_YOYO_STANDING_TARGET =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void shenmiYoyo$defineStandingData(CallbackInfo ci) {
        ((Player) (Object) this).getEntityData().define(SHENMI_YOYO_STANDING, false);
        ((Player) (Object) this).getEntityData().define(SHENMI_YOYO_SITTING, false);
        ((Player) (Object) this).getEntityData().define(SHENMI_YOYO_STANDING_TARGET, -1);
    }

    @Override
    public boolean shenmiYoyo$isStandingOnPlayer() {
        return ((Player) (Object) this).getEntityData().get(SHENMI_YOYO_STANDING);
    }

    @Override
    public void shenmiYoyo$setStandingOnPlayer(boolean standing) {
        ((Player) (Object) this).getEntityData().set(SHENMI_YOYO_STANDING, standing);
    }

    @Override
    public boolean shenmiYoyo$isSittingOnPlayer() {
        return ((Player) (Object) this).getEntityData().get(SHENMI_YOYO_SITTING);
    }

    @Override
    public void shenmiYoyo$setSittingOnPlayer(boolean sitting) {
        ((Player) (Object) this).getEntityData().set(SHENMI_YOYO_SITTING, sitting);
    }

    @Override
    public int shenmiYoyo$getStandingTargetId() {
        return ((Player) (Object) this).getEntityData().get(SHENMI_YOYO_STANDING_TARGET);
    }

    @Override
    public void shenmiYoyo$setStandingTargetId(int entityId) {
        ((Player) (Object) this).getEntityData().set(SHENMI_YOYO_STANDING_TARGET, entityId);
    }
}
