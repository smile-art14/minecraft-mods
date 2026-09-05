package com.shenmi.yoyo.entity;

import com.shenmi.yoyo.YoYoMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class YoYoProjectileEntity extends ThrowableItemProjectile {
    private static final int MAX_OUTBOUND_TICKS = 15;
    private static final double MAX_DISTANCE_SQR = 13.0D * 13.0D;
    private static final double RETURN_SPEED = 1.65D;
    private boolean returning;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public YoYoProjectileEntity(
            EntityType<? extends YoYoProjectileEntity> type,
            Level level
    ) {
        super(type, level);
        setNoGravity(true);
    }

    public YoYoProjectileEntity(Level level, LivingEntity owner) {
        super(YoYoMod.YOYO_PROJECTILE, owner, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return YoYoMod.YOYO;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);

        if (level().isClientSide) {
            if ((tickCount & 1) == 0) {
                level().addParticle(
                        ParticleTypes.CRIT,
                        getX(), getY(), getZ(),
                        0.0D, 0.0D, 0.0D
                );
            }
            return;
        }

        Entity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            discard();
            return;
        }

        if (!returning && (tickCount >= MAX_OUTBOUND_TICKS || distanceToSqr(owner) >= MAX_DISTANCE_SQR)) {
            returning = true;
        }

        if (returning) {
            Vec3 handTarget = owner.getEyePosition().add(0.0D, -0.38D, 0.0D);
            Vec3 toOwner = handTarget.subtract(position());
            if (toOwner.lengthSqr() <= 1.10D) {
                discard();
                return;
            }

            Vec3 returnVelocity = toOwner.normalize().scale(RETURN_SPEED)
                    .add(owner.getDeltaMovement().scale(0.10D));
            setDeltaMovement(returnVelocity);
            hasImpulse = true;
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Once the yo-yo is returning, collisions must not push it away from
        // its owner. Otherwise it can oscillate forever against a mob/player
        // standing between the yo-yo and the thrower.
        if (returning) {
            return;
        }

        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }

        Entity hit = result.getEntity();
        Entity owner = getOwner();
        if (hit == owner || hitEntityIds.contains(hit.getId())) {
            return;
        }

        hitEntityIds.add(hit.getId());
        hit.hurt(damageSources().thrown(this, owner), 8.0F);

        int piercingLevel = EnchantmentHelper.getItemEnchantmentLevel(
                YoYoMod.PIERCING,
                getItem()
        );
        int maxTargets = 1 + piercingLevel;

        if (hitEntityIds.size() >= maxTargets) {
            returning = true;
            setDeltaMovement(getDeltaMovement().scale(-0.25D));
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // During the return flight the yo-yo is tethered to the player and
        // should pass through terrain. The old behavior reversed its velocity
        // on every collision tick, which made it vibrate and become stuck on
        // walls, trees and other obstacles.
        if (returning) {
            return;
        }

        super.onHitBlock(result);
        if (!level().isClientSide) {
            returning = true;
            setDeltaMovement(getDeltaMovement().scale(-0.25D));
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !returning
                && entity != getOwner()
                && !hitEntityIds.contains(entity.getId())
                && super.canHitEntity(entity);
    }
}
