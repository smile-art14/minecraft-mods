package com.shenmi.seadis.mixin;

import com.shenmi.seadis.SeaDisdainFishingMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
	private static final List<EntityType<?>> OTHER_HOSTILE_CATCHES = BuiltInRegistries.ENTITY_TYPE.stream()
			.filter(type -> type.getCategory() == MobCategory.MONSTER)
			.filter(type -> type != EntityType.ENDER_DRAGON)
			.filter(type -> type != EntityType.WITHER)
			.filter(type -> type != EntityType.WARDEN)
			.toList();

	@Shadow
	private int nibble;

	@Shadow
	private int timeUntilLured;

	@Shadow
	private int timeUntilHooked;

	@Shadow
	private Entity hookedIn;

	@Shadow
	private void pullEntity(Entity entity) {
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void seaDisdain$speedUpFishing(CallbackInfo callback) {
		FishingHook hook = (FishingHook) (Object) this;
		if (!(hook.level() instanceof ServerLevel)
				|| !(hook.getOwner() instanceof Player player)
				|| !seaDisdain$isHoldingEnchantedRod(player)) {
			return;
		}

		if (timeUntilLured > 1) {
			timeUntilLured = Math.max(1, timeUntilLured - 2);
		}
		if (timeUntilHooked > 1) {
			timeUntilHooked = Math.max(1, timeUntilHooked - 2);
		}
	}

	@Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
	private void seaDisdain$replaceLootWithDangerousCatch(
			ItemStack fishingRod,
			CallbackInfoReturnable<Integer> callback
	) {
		FishingHook hook = (FishingHook) (Object) this;
		if (!(hook.level() instanceof ServerLevel serverLevel)
				|| !(hook.getOwner() instanceof Player player)
				|| hookedIn != null
				|| nibble <= 0
				|| EnchantmentHelper.getItemEnchantmentLevel(
						SeaDisdainFishingMod.SEA_DISDAIN, fishingRod) <= 0
				|| OTHER_HOSTILE_CATCHES.isEmpty()) {
			return;
		}

		Entity caught = seaDisdain$createCatch(serverLevel, hook, player);
		if (caught == null) {
			return;
		}

		caught.moveTo(hook.getX(), hook.getY(), hook.getZ(),
				serverLevel.getRandom().nextFloat() * 360.0F, 0.0F);
		if (caught instanceof Warden warden) {
			// A raw Warden has no sculk-shrieker emergence cooldown and can enter
			// its digging/removal behavior immediately. Keep fishing catches alive.
			warden.setPersistenceRequired();
			warden.getBrain().setMemoryWithExpiry(
					MemoryModuleType.DIG_COOLDOWN,
					Unit.INSTANCE,
					1200L
			);
		}
		if (!serverLevel.addFreshEntity(caught)) {
			return;
		}

		if (caught instanceof DragonFireball dragonFireball) {
			seaDisdain$launchDragonFireballAtPlayer(dragonFireball, player);
		} else {
			pullEntity(caught);
		}
		if (caught instanceof MinecartTNT) {
			seaDisdain$launchMinecartAtPlayer(caught, player);
		}
		if (caught instanceof LivingEntity livingCaught) {
			livingCaught.setLastHurtByMob(player);
		}
		if (caught instanceof Warden warden) {
			warden.increaseAngerAt(player, 150, true);
		}
		hook.discard();
		callback.setReturnValue(1);
	}

	private static boolean seaDisdain$isHoldingEnchantedRod(Player player) {
		return EnchantmentHelper.getItemEnchantmentLevel(
				SeaDisdainFishingMod.SEA_DISDAIN, player.getMainHandItem()) > 0
				|| EnchantmentHelper.getItemEnchantmentLevel(
				SeaDisdainFishingMod.SEA_DISDAIN, player.getOffhandItem()) > 0;
	}

	private static Entity seaDisdain$createCatch(ServerLevel level, FishingHook hook, Player player) {
		if (level.getRandom().nextBoolean()) {
			return switch (level.getRandom().nextInt(7)) {
				case 0 -> EntityType.ENDER_DRAGON.create(level);
				case 1 -> EntityType.WITHER.create(level);
				case 2 -> EntityType.WARDEN.create(level);
				case 3 -> seaDisdain$createPrimedTnt(level, player);
				case 4 -> seaDisdain$createPrimedTntMinecart(level);
				case 5 -> seaDisdain$createHarmingPotion(level, player);
				default -> seaDisdain$createDragonFireball(level, hook, player);
			};
		}

		EntityType<?> caughtType = OTHER_HOSTILE_CATCHES.get(
				level.getRandom().nextInt(OTHER_HOSTILE_CATCHES.size())
		);
		return caughtType.create(level);
	}

	private static PrimedTnt seaDisdain$createPrimedTnt(ServerLevel level, Player player) {
		PrimedTnt tnt = new PrimedTnt(level, 0.0D, 0.0D, 0.0D, player);
		tnt.setFuse(20);
		return tnt;
	}

	private static MinecartTNT seaDisdain$createPrimedTntMinecart(ServerLevel level) {
		MinecartTNT minecart = EntityType.TNT_MINECART.create(level);
		if (minecart != null) {
			minecart.primeFuse();
			((MinecartTNTAccessor) minecart).seaDisdain$setFuse(20);
		}
		return minecart;
	}

	private static void seaDisdain$launchMinecartAtPlayer(Entity minecart, Player player) {
		Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
		Vec3 offset = target.subtract(minecart.position());
		if (offset.lengthSqr() < 0.0001D) {
			return;
		}

		// Cover the hook-to-player distance in roughly seven ticks. The upward
		// component compensates for minecart gravity so it reaches the torso.
		minecart.setDeltaMovement(offset.scale(1.0D / 7.0D).add(0.0D, 0.22D, 0.0D));
		minecart.hurtMarked = true;
	}

	private static ThrownPotion seaDisdain$createHarmingPotion(ServerLevel level, Player player) {
		ThrownPotion potion = new ThrownPotion(level, player);
		potion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.HARMING));
		return potion;
	}

	private static DragonFireball seaDisdain$createDragonFireball(
			ServerLevel level,
			FishingHook hook,
			Player player
	) {
		Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
		Vec3 direction = target.subtract(hook.position()).normalize();
		DragonFireball fireball = new DragonFireball(level, player, direction.x, direction.y, direction.z);
		fireball.addTag(SeaDisdainFishingMod.CAUGHT_DRAGON_FIREBALL_TAG);
		return fireball;
	}

	private static void seaDisdain$launchDragonFireballAtPlayer(
			DragonFireball fireball,
			Player player
	) {
		Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
		Vec3 direction = target.subtract(fireball.position()).normalize();
		fireball.setDeltaMovement(direction.scale(1.5D));
		fireball.hurtMarked = true;
	}
}
