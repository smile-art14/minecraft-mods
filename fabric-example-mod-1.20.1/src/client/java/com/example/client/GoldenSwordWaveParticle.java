package com.example.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class GoldenSwordWaveParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	private GoldenSwordWaveParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteSet sprites
	) {
		super(level, x, y, z, velocityX, velocityY, velocityZ);
		this.sprites = sprites;
		this.xd = velocityX;
		this.yd = velocityY;
		this.zd = velocityZ;
		this.lifetime = 4 + level.random.nextInt(2);
		this.quadSize = 0.25F + level.random.nextFloat() * 0.10F;
		this.gravity = 0.0F;
		this.rCol = 1.0F;
		if (level.random.nextFloat() < 0.58F) {
			this.gCol = 0.96F;
			this.bCol = 0.62F;
		} else {
			this.gCol = 0.78F + level.random.nextFloat() * 0.14F;
			this.bCol = 0.12F + level.random.nextFloat() * 0.10F;
		}
		this.alpha = 1.0F;
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
		move(xd, yd, zd);
		quadSize *= 0.97F;
		alpha = Math.max(0.0F, 1.0F - age / (float) lifetime);
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
			return new GoldenSwordWaveParticle(
					level, x, y, z, velocityX, velocityY, velocityZ, sprites
			);
		}
	}
}
