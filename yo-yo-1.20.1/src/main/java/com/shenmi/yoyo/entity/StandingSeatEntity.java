package com.shenmi.yoyo.entity;

import com.shenmi.yoyo.YoYoMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Invisible intermediary vehicle used only for the "stand on another player"
 * pose. The anchor itself rides the target player while the standing player
 * rides this anchor. A client mixin suppresses the vanilla seated model pose
 * when the player's vehicle is this entity.
 */
public final class StandingSeatEntity extends Entity {
    public StandingSeatEntity(EntityType<? extends StandingSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public StandingSeatEntity(Level level) {
        this(YoYoMod.STANDING_SEAT, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setNoGravity(true);

        if (!level().isClientSide && (!isPassenger() || getVehicle() == null || getPassengers().isEmpty())) {
            discard();
        }
    }

    @Override
    public double getMyRidingOffset() {
        Entity vehicle = getVehicle();
        if (vehicle == null) {
            return 0.0D;
        }

        // Entity.positionRider adds vehicle.getPassengersRidingOffset() and
        // this value. Cancel the vanilla offset and place the anchor just
        // above the target's current collision-box height.
        return vehicle.getBbHeight() + 0.12D - vehicle.getPassengersRidingOffset();
    }

    @Override
    public double getPassengersRidingOffset() {
        // Player.getMyRidingOffset() is -0.35 in 1.20.1. This cancels it so
        // the standing player's feet line up with the anchor position.
        return 0.35D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
