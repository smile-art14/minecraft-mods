package com.shenmi.yoyo.entity;

import com.shenmi.yoyo.YoYoMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Network-visible visual anchor for the strangling rope. It follows the
 * target's neck and lets the client draw a rope from the attacker's hand to
 * that point without trusting client-side combat state.
 */
public final class StrangleTetherEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            StrangleTetherEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(
            StrangleTetherEntity.class,
            EntityDataSerializers.INT
    );

    public StrangleTetherEntity(EntityType<? extends StrangleTetherEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public StrangleTetherEntity(Level level, Player owner, LivingEntity target) {
        this(YoYoMod.STRANGLE_TETHER, level);
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(TARGET_ID, target.getId());
        moveToNeck(target);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(TARGET_ID, -1);
    }

    public Entity getOwnerEntity() {
        int id = entityData.get(OWNER_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    public LivingEntity getTargetEntity() {
        int id = entityData.get(TARGET_ID);
        Entity entity = id < 0 ? null : level().getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setNoGravity(true);

        LivingEntity target = getTargetEntity();
        Entity owner = getOwnerEntity();
        if (target == null || owner == null || !target.isAlive() || !owner.isAlive()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        moveToNeck(target);
    }

    private void moveToNeck(LivingEntity target) {
        Vec3 neck = target.position().add(0.0D, target.getBbHeight() * 0.78D, 0.0D);
        setPos(neck.x, neck.y, neck.z);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
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
