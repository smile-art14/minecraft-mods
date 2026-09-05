package com.example.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Networked visual anchor for one complete Excalibur slash texture.
 * Damage and collision remain server-authoritative in ExcaliburItem.
 */
public final class GoldenSwordWaveEntity extends Entity {
	private static final EntityDataAccessor<Float> HALF_WIDTH =
			SynchedEntityData.defineId(
					GoldenSwordWaveEntity.class,
					EntityDataSerializers.FLOAT
			);
	private static final EntityDataAccessor<Float> DIRECTION_X =
			SynchedEntityData.defineId(
					GoldenSwordWaveEntity.class,
					EntityDataSerializers.FLOAT
			);
	private static final EntityDataAccessor<Float> DIRECTION_Y =
			SynchedEntityData.defineId(
					GoldenSwordWaveEntity.class,
					EntityDataSerializers.FLOAT
			);
	private static final EntityDataAccessor<Float> DIRECTION_Z =
			SynchedEntityData.defineId(
					GoldenSwordWaveEntity.class,
					EntityDataSerializers.FLOAT
			);

	public GoldenSwordWaveEntity(
			EntityType<? extends GoldenSwordWaveEntity> entityType,
			Level level
	) {
		super(entityType, level);
		noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(HALF_WIDTH, 1.0F);
		entityData.define(DIRECTION_X, 0.0F);
		entityData.define(DIRECTION_Y, 0.0F);
		entityData.define(DIRECTION_Z, 1.0F);
	}

	public float getHalfWidth() {
		return entityData.get(HALF_WIDTH);
	}

	public void setHalfWidth(double halfWidth) {
		entityData.set(HALF_WIDTH, (float) Math.max(0.1D, halfWidth));
	}

	public Vec3 getTravelDirection() {
		Vec3 direction = new Vec3(
				entityData.get(DIRECTION_X),
				entityData.get(DIRECTION_Y),
				entityData.get(DIRECTION_Z)
		);
		return direction.lengthSqr() < 0.0001D
				? new Vec3(0.0D, 0.0D, 1.0D)
				: direction.normalize();
	}

	public void setTravelDirection(Vec3 direction) {
		Vec3 normalized = direction.lengthSqr() < 0.0001D
				? new Vec3(0.0D, 0.0D, 1.0D)
				: direction.normalize();
		entityData.set(DIRECTION_X, (float) normalized.x);
		entityData.set(DIRECTION_Y, (float) normalized.y);
		entityData.set(DIRECTION_Z, (float) normalized.z);
	}

	@Override
	public void tick() {
		super.tick();
		noPhysics = true;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		setHalfWidth(tag.getFloat("HalfWidth"));
		setTravelDirection(new Vec3(
				tag.getDouble("DirectionX"),
				tag.getDouble("DirectionY"),
				tag.getDouble("DirectionZ")
		));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("HalfWidth", getHalfWidth());
		Vec3 direction = getTravelDirection();
		tag.putDouble("DirectionX", direction.x);
		tag.putDouble("DirectionY", direction.y);
		tag.putDouble("DirectionZ", direction.z);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 16384.0D;
	}
}
