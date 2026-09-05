package com.example.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

public final class ExcaliburChargeParticle extends TextureSheetParticle {
	private static final ThreadLocal<CameraRelativeSpawn> PREPARED_SPAWN = new ThreadLocal<>();
	private final SpriteSet sprites;
	private final CameraRelativeSpawn cameraRelativeSpawn;

	private ExcaliburChargeParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteSet sprites,
			CameraRelativeSpawn cameraRelativeSpawn
	) {
		super(level, x, y, z, velocityX, velocityY, velocityZ);
		this.sprites = sprites;
		this.cameraRelativeSpawn = cameraRelativeSpawn;
		this.xd = velocityX;
		this.yd = velocityY;
		this.zd = velocityZ;
		this.lifetime = 8;
		this.quadSize = 0.035F + level.random.nextFloat() * 0.025F;
		this.gravity = 0.0F;
		// Full-bright warm gold: the converging energy now matches Excalibur's
		// charged blade instead of reading as unrelated white smoke.
		this.rCol = 1.0F;
		this.gCol = 0.84F + level.random.nextFloat() * 0.12F;
		this.bCol = 0.20F + level.random.nextFloat() * 0.12F;
		this.alpha = 0.96F;
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		if (age++ >= lifetime) {
			remove();
			return;
		}
		if (cameraRelativeSpawn == null) {
			move(xd, yd, zd);
		} else {
			float progress = age / (float) lifetime;
			Vec3 localPosition = cameraRelativeSpawn.start().lerp(
					cameraRelativeSpawn.target(), progress
			);
			Vec3 worldPosition = cameraLocalToWorld(localPosition);
			setPos(worldPosition.x, worldPosition.y, worldPosition.z);
		}
		quadSize *= 0.92F;
		alpha = 0.96F * (1.0F - age / (float) lifetime);
		setSpriteFromAge(sprites);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	protected int getLightColor(float partialTick) {
		return 0xF000F0;
	}

	static void prepareCameraRelativeSpawn(Vec3 start, Vec3 target) {
		PREPARED_SPAWN.set(new CameraRelativeSpawn(
				worldToCameraLocal(start),
				worldToCameraLocal(target)
		));
	}

	static void clearPreparedSpawn() {
		PREPARED_SPAWN.remove();
	}

	private static Vec3 worldToCameraLocal(Vec3 worldPosition) {
		Minecraft minecraft = Minecraft.getInstance();
		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		CameraBasis basis = getCameraBasis(minecraft);
		Vec3 offset = worldPosition.subtract(cameraPosition);
		return new Vec3(
				offset.dot(basis.right()),
				offset.dot(basis.up()),
				offset.dot(basis.look())
		);
	}

	private static Vec3 cameraLocalToWorld(Vec3 localPosition) {
		Minecraft minecraft = Minecraft.getInstance();
		Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
		CameraBasis basis = getCameraBasis(minecraft);
		return cameraPosition
				.add(basis.right().scale(localPosition.x))
				.add(basis.up().scale(localPosition.y))
				.add(basis.look().scale(localPosition.z));
	}

	private static CameraBasis getCameraBasis(Minecraft minecraft) {
		Vec3 look = minecraft.player == null
				? new Vec3(0.0D, 0.0D, 1.0D)
				: minecraft.player.getViewVector(1.0F).normalize();
		Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (right.lengthSqr() < 0.0001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			right = right.normalize();
		}
		return new CameraBasis(look, right, right.cross(look).normalize());
	}

	private record CameraRelativeSpawn(Vec3 start, Vec3 target) {
	}

	private record CameraBasis(Vec3 look, Vec3 right, Vec3 up) {
	}

	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(
				SimpleParticleType type,
				ClientLevel level,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ
		) {
			CameraRelativeSpawn cameraRelativeSpawn = PREPARED_SPAWN.get();
			PREPARED_SPAWN.remove();
			return new ExcaliburChargeParticle(
					level, x, y, z, velocityX, velocityY, velocityZ, sprites,
					cameraRelativeSpawn
			);
		}
	}
}
