package com.example.client;

import com.example.ExampleMod;
import com.example.item.ExcaliburItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

public final class ExcaliburChargeEffects {
	private ExcaliburChargeEffects() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(ExcaliburChargeEffects::tick);
	}

	private static void tick(Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
			return;
		}

		for (Player player : minecraft.level.players()) {
			if (!player.isUsingItem()
					|| !player.getUseItem().is(ExampleMod.EXCALIBUR)
					|| player.distanceToSqr(minecraft.player) > 32.0D * 32.0D) {
				continue;
			}

			float charge = ExcaliburItem.getChargeProgress(player.getTicksUsingItem());
			boolean firstPersonSword = player == minecraft.player
					&& minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
			int particleCount = 4 + Math.round(charge * 8.0F);
			for (int index = 0; index < particleCount; index++) {
				Vec3 swordTarget = getSwordTarget(minecraft, player);
				spawnConvergingParticle(minecraft, swordTarget, firstPersonSword);
			}
		}
	}

	private static Vec3 getSwordTarget(Minecraft minecraft, Player player) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (right.lengthSqr() < 0.0001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			right = right.normalize();
		}
		Vec3 up = right.cross(look).normalize();
		HumanoidArm usedArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
				? player.getMainArm()
				: player.getMainArm().getOpposite();
		double handSide = usedArm == HumanoidArm.RIGHT ? 1.0D : -1.0D;

		if (player == minecraft.player
				&& minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
			// During the spear use animation the first-person blade is pulled high
			// across the screen. Target the visible blade line, rather than the hand
			// position below it. Spreading targets along this line makes the energy
			// visibly enter the blade instead of collapsing into an empty point.
			double bladePosition = 0.15D + minecraft.level.random.nextDouble() * 0.85D;
			Vec3 bladeBase = minecraft.gameRenderer.getMainCamera().getPosition()
					.add(look.scale(0.62D))
					.add(right.scale(0.10D * handSide))
					.add(up.scale(0.16D));
			Vec3 bladeAxis = right.scale(0.10D * handSide)
					.add(up.scale(0.24D));
			return bladeBase.add(bladeAxis.scale(bladePosition));
		}

		return player.getEyePosition()
				.add(look.scale(0.28D))
				.add(right.scale(0.43D * handSide))
				.add(up.scale(0.05D));
	}

	private static void spawnConvergingParticle(
			Minecraft minecraft,
			Vec3 swordCenter,
			boolean cameraRelative
	) {
		Vec3 randomOffset = new Vec3(
				minecraft.level.random.nextGaussian(),
				minecraft.level.random.nextGaussian(),
				minecraft.level.random.nextGaussian()
		);
		if (randomOffset.lengthSqr() < 0.0001D) {
			randomOffset = new Vec3(1.0D, 0.0D, 0.0D);
		}

		double radius = 0.42D + minecraft.level.random.nextDouble() * 0.42D;
		Vec3 start = swordCenter.add(randomOffset.normalize().scale(radius));
		Vec3 velocity = swordCenter.subtract(start).scale(1.0D / 8.0D);
		if (cameraRelative) {
			ExcaliburChargeParticle.prepareCameraRelativeSpawn(start, swordCenter);
		}
		try {
			minecraft.level.addParticle(
					ExampleMod.EXCALIBUR_CHARGE_PARTICLE,
					start.x, start.y, start.z,
					velocity.x, velocity.y, velocity.z
			);
		} finally {
			ExcaliburChargeParticle.clearPreparedSpawn();
		}
	}
}
