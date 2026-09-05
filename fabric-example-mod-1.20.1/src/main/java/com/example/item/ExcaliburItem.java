package com.example.item;

import com.example.ExampleMod;
import com.example.entity.GoldenSwordWaveEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public final class ExcaliburItem extends SwordItem {
	private static final int MIN_CHARGE_TICKS = 8;
	private static final int MAX_CHARGE_TICKS = 60;
	private static final double WAVE_SPEED = 1.5D;
	private static final int WAVE_RIBBON_LAYERS = 5;
	private static final List<GoldenSwordWave> ACTIVE_WAVES = new ArrayList<>();
	private static boolean eventsInitialized;

	public ExcaliburItem(Properties properties) {
		// The same attack damage and speed modifiers used by a diamond sword.
		super(Tiers.DIAMOND, 3, -2.4F, properties);
	}

	public static void initialize() {
		if (eventsInitialized) {
			return;
		}
		eventsInitialized = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> tickWaves());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE_WAVES.clear());
	}

	public static float getChargeProgress(int chargeTicks) {
		return Math.min(Math.max(chargeTicks, 0), MAX_CHARGE_TICKS)
				/ (float) MAX_CHARGE_TICKS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(player.getItemInHand(hand));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		// The spear pose pulls the sword back into a readable charging stance in
		// both first and third person, instead of making it look like a drawn bow.
		return UseAnim.SPEAR;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
		int chargeTicks = getUseDuration(stack) - timeLeft;
		if (!(level instanceof ServerLevel serverLevel)
				|| !(user instanceof Player player)
				|| chargeTicks < MIN_CHARGE_TICKS) {
			return;
		}

		float charge = getChargeProgress(chargeTicks);
		float damage = 10.0F + 90.0F * charge;
		double halfWidth = 0.75D + 2.75D * charge;
		double range = 9.0D + 23.0D * charge;
		int waveLengthLevel = EnchantmentHelper.getItemEnchantmentLevel(
				ExampleMod.SWORD_WAVE_LENGTH,
				stack
		);
		int waveDistanceLevel = EnchantmentHelper.getItemEnchantmentLevel(
				ExampleMod.SWORD_WAVE_DISTANCE,
				stack
		);
		halfWidth *= 1.0D + waveLengthLevel * 0.25D;
		range += waveDistanceLevel * 8.0D;
		float explosionPower = 1.5F + 3.5F * charge;
		boolean explosion = EnchantmentHelper.getItemEnchantmentLevel(ExampleMod.EXPLOSION, stack) > 0;
		int destructionLevel = EnchantmentHelper.getItemEnchantmentLevel(ExampleMod.DESTRUCTION, stack);
		Vec3 direction = player.getLookAngle().normalize();
		Vec3 start = player.getEyePosition().add(direction.scale(1.2D));

		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, stack) > 0) {
			double verticalSpacing = 1.5D + charge * 1.5D;
			Vec3 verticalAxis = getWaveVerticalAxis(player, direction);
			addWave(serverLevel, player, start.add(verticalAxis.scale(verticalSpacing)),
					direction, damage, halfWidth, range, explosion, destructionLevel, explosionPower);
			addWave(serverLevel, player, start,
					direction, damage, halfWidth, range, explosion, destructionLevel, explosionPower);
			addWave(serverLevel, player, start.subtract(verticalAxis.scale(verticalSpacing)),
					direction, damage, halfWidth, range, explosion, destructionLevel, explosionPower);
		} else {
			addWave(serverLevel, player, start,
					direction, damage, halfWidth, range, explosion, destructionLevel, explosionPower);
		}

		serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, 1.0F, 0.75F + charge * 0.55F);
		serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
				SoundSource.PLAYERS, 0.8F, 1.2F + charge * 0.6F);
		player.getCooldowns().addCooldown(this, 10);
		boolean mainHand = player.getMainHandItem() == stack;
		player.swing(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
		EquipmentSlot slot = mainHand
				? EquipmentSlot.MAINHAND
				: EquipmentSlot.OFFHAND;
		stack.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(slot));
	}

	private static void addWave(
			ServerLevel level,
			Player owner,
			Vec3 start,
			Vec3 direction,
			float damage,
			double halfWidth,
			double range,
			boolean explosion,
			int destructionLevel,
			float explosionPower
	) {
		ACTIVE_WAVES.add(new GoldenSwordWave(
				level, owner, start, direction, damage, halfWidth, range,
				explosion, destructionLevel, explosionPower
		));
	}

	private static Vec3 getWaveVerticalAxis(Player player, Vec3 direction) {
		Vec3 horizontalAxis = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (horizontalAxis.lengthSqr() < 0.0001D) {
			double yaw = Math.toRadians(player.getYRot());
			horizontalAxis = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
		} else {
			horizontalAxis = horizontalAxis.normalize();
		}
		return horizontalAxis.cross(direction).normalize();
	}

	private static void tickWaves() {
		ListIterator<GoldenSwordWave> iterator = ACTIVE_WAVES.listIterator();
		while (iterator.hasNext()) {
			GoldenSwordWave wave = iterator.next();
			if (!wave.tick()) {
				wave.discardVisual();
				iterator.remove();
			}
		}
	}

	private static final class GoldenSwordWave {
		private final ServerLevel level;
		private final Player owner;
		private final Vec3 direction;
		private final Vec3 horizontalAxis;
		private final float damage;
		private final double halfWidth;
		private final double maxRange;
		private final boolean explosion;
		private final int destructionLevel;
		private final float explosionPower;
		private final Set<Integer> hitEntityIds = new HashSet<>();
		private final GoldenSwordWaveEntity visual;
		private Vec3 center;
		private double distanceTravelled;

		private GoldenSwordWave(
				ServerLevel level,
				Player owner,
				Vec3 center,
				Vec3 direction,
				float damage,
				double halfWidth,
				double maxRange,
				boolean explosion,
				int destructionLevel,
				float explosionPower
		) {
			this.level = level;
			this.owner = owner;
			this.center = center;
			this.direction = direction;
			Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
			this.horizontalAxis = side.lengthSqr() < 0.0001D
					? new Vec3(1.0D, 0.0D, 0.0D)
					: side.normalize();
			this.damage = damage;
			this.halfWidth = halfWidth;
			this.maxRange = maxRange;
			this.explosion = explosion;
			this.destructionLevel = destructionLevel;
			this.explosionPower = explosionPower;
			this.visual = new GoldenSwordWaveEntity(
					ExampleMod.GOLDEN_SWORD_WAVE_ENTITY,
					level
			);
			this.visual.setPos(center);
			this.visual.setTravelDirection(direction);
			this.visual.setHalfWidth(getCurrentHalfWidth());
			level.addFreshEntity(this.visual);
		}

		private boolean tick() {
			if (!owner.isAlive() || owner.level() != level || distanceTravelled >= maxRange) {
				return false;
			}

			Vec3 previous = center;
			center = center.add(direction.scale(WAVE_SPEED));
			distanceTravelled += WAVE_SPEED;
			double currentHalfWidth = getCurrentHalfWidth();
			List<Vec3> wavePoints = createWavePoints(currentHalfWidth);
			visual.setPos(center.x, center.y, center.z);
			visual.setHalfWidth(currentHalfWidth);
			spawnAccentParticles(currentHalfWidth);

			if (explosion || destructionLevel > 0) {
				List<BlockHitResult> explosionHits = explosion
						? findTouchedBlocks(wavePoints, 0)
						: List.of();
				if (destructionLevel > 0) {
					int verticalRadius = destructionLevel - 1;
					List<BlockHitResult> destructionHits = explosion && verticalRadius == 0
							? explosionHits
							: findTouchedBlocks(wavePoints, verticalRadius);
					destroyTouchedBlocks(destructionHits);
				}
				if (!explosionHits.isEmpty()) {
					// Every distinct block touched across the whole sword-wave arc gets
					// its own blast before this wave is removed.
					for (BlockHitResult hit : explosionHits) {
						explodeAt(hit.getLocation());
					}
					return false;
				}
			}

			AABB searchBox = new AABB(previous, center).inflate(
					currentHalfWidth + 0.75D,
					currentHalfWidth * 0.5D + 0.75D,
					currentHalfWidth + 0.75D
			);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
				if (target == owner
						|| !target.isAlive()
						|| hitEntityIds.contains(target.getId())
						|| !intersectsWave(target.getBoundingBox(), wavePoints, currentHalfWidth)) {
					continue;
				}
				hitEntityIds.add(target.getId());
				if (target.hurt(level.damageSources().playerAttack(owner), damage)) {
					target.knockback(0.25D + currentHalfWidth * 0.12D, -direction.x, -direction.z);
					spawnImpactParticles(target);
				}
				if (explosion) {
					explodeAt(target.getBoundingBox().getCenter());
					return false;
				}
			}
			return distanceTravelled < maxRange;
		}

		private double getCurrentHalfWidth() {
			double distanceProgress = Math.min(1.0D, distanceTravelled / maxRange);
			// A linear increase is visually cancelled by perspective at long range.
			// Quadratic expansion accelerates as the wave travels, so its apparent
			// size also grows on screen instead of merely growing in world space.
			double distanceScale = 0.15D
					+ distanceProgress * distanceProgress * 3.85D;
			return halfWidth * distanceScale;
		}

		private List<Vec3> createWavePoints(double currentHalfWidth) {
			List<Vec3> points = new ArrayList<>();
			addParticleSlice(points, center, currentHalfWidth);
			return points;
		}

		private void addParticleSlice(
				List<Vec3> points,
				Vec3 sliceCenter,
				double currentHalfWidth
		) {
			int pointCount = Math.max(7, (int) Math.ceil(currentHalfWidth * 4.0D));
			for (int index = 0; index < pointCount; index++) {
				double offset = -currentHalfWidth
						+ (currentHalfWidth * 2.0D * index) / (pointCount - 1);
				double normalizedOffset = offset / currentHalfWidth;
				double pointedTaper = Math.pow(
						Math.max(0.0D, 1.0D - Math.abs(normalizedOffset)),
						0.58D
				);
				// The bright cutting edge bows forward while the inner edge is
				// pulled backwards. Both edges meet at the tips, producing the
				// thick, flame-like crescent silhouette from the reference.
				double frontDepth = (1.0D - normalizedOffset * normalizedOffset)
						* currentHalfWidth * 0.62D;
				double ribbonThickness = currentHalfWidth * 0.72D * pointedTaper;
				double backDepth = frontDepth - ribbonThickness;
				Vec3 lateralPoint = sliceCenter.add(horizontalAxis.scale(offset));
				for (int layer = 0; layer < WAVE_RIBBON_LAYERS; layer++) {
					double layerProgress = layer / (double) (WAVE_RIBBON_LAYERS - 1);
					// Bias samples towards the front to make the white-gold edge
					// denser than the translucent golden tail.
					double depthProgress = 1.0D
							- Math.pow(1.0D - layerProgress, 1.35D);
					double depth = backDepth
							+ (frontDepth - backDepth) * depthProgress;
					points.add(lateralPoint.add(direction.scale(depth)));
				}
			}
		}

		private boolean intersectsWave(
				AABB entityBounds,
				List<Vec3> wavePoints,
				double currentHalfWidth
		) {
			// Adjacent sample volumes overlap, forming the same continuous curved
			// ribbon that the player sees from the particle points.
			double thickness = 0.38D + currentHalfWidth * 0.045D;
			for (Vec3 point : wavePoints) {
				if (new AABB(point, point).inflate(thickness).intersects(entityBounds)) {
					return true;
				}
			}
			return false;
		}

		private List<BlockHitResult> findTouchedBlocks(
				List<Vec3> wavePoints,
				int verticalRadius
		) {
			List<BlockHitResult> hits = new ArrayList<>();
			Set<BlockPos> hitPositions = new LinkedHashSet<>();
			Vec3 backwards = direction.scale(WAVE_SPEED);
			for (Vec3 point : wavePoints) {
				for (int verticalOffset = -verticalRadius;
						verticalOffset <= verticalRadius;
						verticalOffset++) {
					Vec3 raisedPoint = point.add(0.0D, verticalOffset, 0.0D);
					BlockHitResult hit = level.clip(new ClipContext(
							raisedPoint.subtract(backwards),
							raisedPoint,
							ClipContext.Block.COLLIDER,
							ClipContext.Fluid.NONE,
							owner
					));
					if (hit.getType() == HitResult.Type.BLOCK
							&& hitPositions.add(hit.getBlockPos().immutable())) {
						hits.add(hit);
					}
				}
			}
			return hits;
		}

		private void destroyTouchedBlocks(List<BlockHitResult> blockHits) {
			for (BlockHitResult hit : blockHits) {
				BlockPos position = hit.getBlockPos();
				BlockState state = level.getBlockState(position);
				if (state.isAir()
						|| state.getDestroySpeed(level, position) < 0.0F
						|| !level.mayInteract(owner, position)) {
					continue;
				}
				level.destroyBlock(position, true, owner);
			}
		}

		private void explodeAt(Vec3 position) {
			level.explode(
					owner,
					position.x,
					position.y,
					position.z,
					explosionPower,
					Level.ExplosionInteraction.TNT
			);
		}

		private void discardVisual() {
			if (!visual.isRemoved()) {
				visual.discard();
			}
		}

		private void spawnAccentParticles(double currentHalfWidth) {
			// The entity texture remains the complete sword-wave silhouette.
			// These few particles only add moving sparks to its cutting edge and
			// translucent embers behind it.
			for (int index = 0; index < 3; index++) {
				double lateralOffset = (level.random.nextDouble() * 2.0D - 1.0D)
						* currentHalfWidth * 0.82D;
				double travelOffset = (level.random.nextDouble() - 0.55D)
						* currentHalfWidth * 1.35D;
				Vec3 sparkPosition = center
						.add(horizontalAxis.scale(lateralOffset))
						.add(direction.scale(travelOffset));
				level.sendParticles(
						ExampleMod.GOLDEN_SWORD_TRAIL_PARTICLE,
						sparkPosition.x,
						sparkPosition.y,
						sparkPosition.z,
						0,
						direction.x,
						direction.y,
						direction.z,
						WAVE_SPEED * (0.10D + index * 0.035D)
				);
			}

			double edgeOffset = (level.random.nextDouble() * 2.0D - 1.0D)
					* currentHalfWidth * 0.68D;
			Vec3 cuttingEdge = center
					.add(horizontalAxis.scale(edgeOffset))
					.add(direction.scale(currentHalfWidth * 0.72D));
			level.sendParticles(
					ExampleMod.GOLDEN_SWORD_WAVE_PARTICLE,
					cuttingEdge.x,
					cuttingEdge.y,
					cuttingEdge.z,
					0,
					direction.x,
					direction.y,
					direction.z,
					WAVE_SPEED * 0.22D
			);
		}

		private void spawnImpactParticles(LivingEntity target) {
			double impactY = target.getY() + target.getBbHeight() * 0.5D;
			level.sendParticles(
					ExampleMod.GOLDEN_SWORD_WAVE_PARTICLE,
					target.getX(),
					impactY,
					target.getZ(),
					7,
					0.30D,
					0.30D,
					0.30D,
					0.16D
			);
			level.sendParticles(
					ExampleMod.GOLDEN_SWORD_TRAIL_PARTICLE,
					target.getX(),
					impactY,
					target.getZ(),
					5,
					0.24D,
					0.24D,
					0.24D,
					0.10D
			);
		}

	}
}
