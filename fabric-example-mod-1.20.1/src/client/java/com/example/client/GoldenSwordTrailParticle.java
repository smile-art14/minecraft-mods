package com.example.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class GoldenSwordTrailParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	private GoldenSwordTrailParticle(
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
		this.lifetime = 9 + level.random.nextInt(4);
		this.quadSize = 0.20F + level.random.nextFloat() * 0.10F;
		this.gravity = 0.0F;
		this.rCol = 1.0F;
		this.gCol = 0.56F + level.random.nextFloat() * 0.18F;
		this.bCol = 0.02F + level.random.nextFloat() * 0.06F;
		this.alpha = 0.72F;
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
		xd *= 0.88D;
		yd *= 0.88D;
		zd *= 0.88D;
		quadSize *= 0.94F;
		alpha = 0.72F * (1.0F - age / (float) lifetime);
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
			return new GoldenSwordTrailParticle(
					level, x, y, z, velocityX, velocityY, velocityZ, sprites
			);
		}
	}
}
