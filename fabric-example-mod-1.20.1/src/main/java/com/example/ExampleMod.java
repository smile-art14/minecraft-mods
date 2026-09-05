package com.example;

import com.example.entity.GoldenSwordWaveEntity;
import com.example.item.ExcaliburItem;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.SweepingEdgeEnchantment;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "mcdemo";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final List<ScheduledLightning> SCHEDULED_LIGHTNING = new ArrayList<>();
	private static final List<FrozenEntity> FROZEN_ENTITIES = new ArrayList<>();
	private static final List<WindEye> ACTIVE_WIND_EYES = new ArrayList<>();
	private static final List<HookPull> ACTIVE_HOOK_PULLS = new ArrayList<>();
	private static final Map<UUID, Integer> SHIELD_DASH_COOLDOWNS = new HashMap<>();
	private static final double SHIELD_DASH_RANGE = 4.5D;
	private static final double SHIELD_DASH_MIN_CLOSING_SPEED = 0.035D;
	private static final int SHIELD_DASH_COOLDOWN_TICKS = 8;
	public static final String SWALLOW_ACTIVE_TAG = "ShenmiSwallowActive";
	public static final String SWALLOW_COMPRESSED_TAG = "ShenmiSwallowCompressed";
	public static final String SWALLOW_STAGE_TAG = "ShenmiSwallowStage";
	public static final int SWALLOW_STAGE_COUNT = 5;
	public static final SimpleParticleType GOLDEN_SWORD_WAVE_PARTICLE = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE,
			id("golden_sword_wave"),
			FabricParticleTypes.simple()
	);
	public static final SimpleParticleType GOLDEN_SWORD_TRAIL_PARTICLE = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE,
			id("golden_sword_trail"),
			FabricParticleTypes.simple()
	);
	public static final SimpleParticleType EXCALIBUR_CHARGE_PARTICLE = Registry.register(
			BuiltInRegistries.PARTICLE_TYPE,
			id("excalibur_charge"),
			FabricParticleTypes.simple()
	);
	public static final EntityType<GoldenSwordWaveEntity> GOLDEN_SWORD_WAVE_ENTITY =
			Registry.register(
					BuiltInRegistries.ENTITY_TYPE,
					id("golden_sword_wave"),
					EntityType.Builder.<GoldenSwordWaveEntity>of(
									GoldenSwordWaveEntity::new,
									MobCategory.MISC
							)
							.sized(0.1F, 0.1F)
							.clientTrackingRange(10)
							.updateInterval(1)
							.build("golden_sword_wave")
			);
	private static final List<Tornado> ACTIVE_TORNADOES = new ArrayList<>();
	public static final Item MYSTIC_CRYSTAL = registerItem(
			"mystic_crystal",
			new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
	);
	public static final Item THUNDER_WAND = registerItem(
			"thunder_wand",
			new ThunderWandItem(new Item.Properties().stacksTo(1).durability(128).rarity(Rarity.RARE))
	);
	public static final Enchantment THUNDER_CHANNELING = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("channeling"),
			new ThunderChannelingEnchantment()
	);
	public static final Item FLAME_WAND = registerItem(
			"flame_wand",
			new FlameWandItem(new Item.Properties().stacksTo(1).durability(128).rarity(Rarity.RARE))
	);
	public static final Item ICE_WAND = registerItem(
			"ice_wand",
			new IceWandItem(new Item.Properties().stacksTo(1).durability(128).rarity(Rarity.RARE))
	);
	public static final Item HURRICANE_WAND = registerItem(
			"hurricane_wand",
			new HurricaneWandItem(new Item.Properties().stacksTo(1).durability(128).rarity(Rarity.RARE))
	);
	public static final Item ENDER_WAND = registerItem(
			"ender_wand",
			new EnderWandItem(new Item.Properties().stacksTo(1).durability(128).rarity(Rarity.EPIC))
	);
	public static final Item EXCALIBUR = registerItem(
			"excalibur",
			new ExcaliburItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
	);
	public static final Enchantment EXPLOSION = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("explosion"),
			new ExcaliburEnchantment(Enchantment.Rarity.RARE, 20, 0, 30, 1)
	);
	public static final Enchantment DESTRUCTION = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("destruction"),
			new ExcaliburEnchantment(Enchantment.Rarity.VERY_RARE, 10, 10, 30, 3)
	);
	public static final Enchantment SWORD_WAVE_LENGTH = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("sword_wave_length"),
			new ExcaliburEnchantment(Enchantment.Rarity.RARE, 10, 10, 30, 3)
	);
	public static final Enchantment SWORD_WAVE_DISTANCE = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("sword_wave_distance"),
			new ExcaliburEnchantment(Enchantment.Rarity.RARE, 10, 10, 30, 3)
	);
	public static final Enchantment WIND_EYE = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("wind_eye"),
			new WindEyeEnchantment()
	);
	public static final Enchantment TORNADO = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("tornado"),
			new TornadoEnchantment()
	);
	public static final Enchantment FREEZING = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("freezing"),
			new FreezingEnchantment()
	);
	public static final Enchantment HOOK_RETURN = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("hook_return"),
			new HookReturnEnchantment()
	);
	public static final Enchantment SHIELD_DASH = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("shield_dash"),
			new ShieldDashEnchantment()
	);
	public static final Enchantment SWALLOW_SWORD = Registry.register(
			BuiltInRegistries.ENCHANTMENT,
			id("swallow_sword"),
			new SwallowSwordEnchantment()
	);
	public static final MobEffect FROZEN_TINT = Registry.register(
			BuiltInRegistries.MOB_EFFECT,
			id("frozen_tint"),
			new FrozenTintEffect()
	);
	public static final Block MYSTIC_BLOCK = Registry.register(
			BuiltInRegistries.BLOCK,
			id("mystic_block"),
			new Block(BlockBehaviour.Properties.of()
					.mapColor(MapColor.GOLD)
					.strength(3.0F, 6.0F)
					.sound(SoundType.AMETHYST)
					.lightLevel(state -> 10))
	);
	public static final Item MYSTIC_BLOCK_ITEM = registerItem(
			"mystic_block",
			new BlockItem(MYSTIC_BLOCK, new Item.Properties())
	);
	public static final CreativeModeTab WAND_ITEM_GROUP = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			id("wands"),
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.mcdemo.wands"))
					.icon(() -> new ItemStack(THUNDER_WAND))
					.displayItems((parameters, output) -> {
						output.accept(THUNDER_WAND);
						output.accept(FLAME_WAND);
						output.accept(ICE_WAND);
						output.accept(HURRICANE_WAND);
						output.accept(ENDER_WAND);
						output.accept(EXCALIBUR);
						output.accept(MYSTIC_CRYSTAL);
						output.accept(MYSTIC_BLOCK_ITEM);
						output.accept(createThunderChannelingBook(1));
						output.accept(createThunderChannelingBook(2));
						output.accept(createThunderChannelingBook(3));
						output.accept(createWindEyeBook(1));
						output.accept(createWindEyeBook(2));
						output.accept(createWindEyeBook(3));
						output.accept(createTornadoBook());
						output.accept(createFreezingBook());
						output.accept(createHookReturnBook());
						output.accept(createShieldDashBook());
						output.accept(createSwallowSwordBook(1));
						output.accept(createSwallowSwordBook(2));
						output.accept(createSwallowSwordBook(3));
						output.accept(createExplosionBook());
						output.accept(createDestructionBook(1));
						output.accept(createDestructionBook(2));
						output.accept(createDestructionBook(3));
						output.accept(createSwordWaveLengthBook(1));
						output.accept(createSwordWaveLengthBook(2));
						output.accept(createSwordWaveLengthBook(3));
						output.accept(createSwordWaveDistanceBook(1));
						output.accept(createSwordWaveDistanceBook(2));
						output.accept(createSwordWaveDistanceBook(3));
					})
					.build()
	);

	@Override
	public void onInitialize() {
		ExcaliburItem.initialize();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("shenmi_kit")
						.requires(source -> source.hasPermission(2))
						.executes(context -> {
							var player = context.getSource().getPlayerOrException();
							player.getInventory().placeItemBackInInventory(new ItemStack(MYSTIC_CRYSTAL, 8));
							player.getInventory().placeItemBackInInventory(new ItemStack(THUNDER_WAND));
							player.getInventory().placeItemBackInInventory(new ItemStack(FLAME_WAND));
							player.getInventory().placeItemBackInInventory(new ItemStack(ICE_WAND));
							player.getInventory().placeItemBackInInventory(new ItemStack(HURRICANE_WAND));
							player.getInventory().placeItemBackInInventory(new ItemStack(ENDER_WAND));
							player.getInventory().placeItemBackInInventory(new ItemStack(EXCALIBUR));
							player.getInventory().placeItemBackInInventory(new ItemStack(MYSTIC_BLOCK, 4));
							context.getSource().sendSuccess(
									() -> Component.translatable("command.mcdemo.kit.success"),
									false
							);
							return Command.SINGLE_SUCCESS;
						}))
		);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickScheduledLightning();
			tickFrozenEntities();
			tickWindEyes();
			tickTornadoes();
			tickHookPulls();
			tickShieldDash(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SCHEDULED_LIGHTNING.clear();
			FROZEN_ENTITIES.clear();
			ACTIVE_WIND_EYES.clear();
			ACTIVE_TORNADOES.clear();
			ACTIVE_HOOK_PULLS.clear();
			SHIELD_DASH_COOLDOWNS.clear();
		});

		LOGGER.info("Shenmi Workshop initialized.");
	}

	private static Item registerItem(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, id(name), item);
	}

	private static ItemStack createThunderChannelingBook(int level) {
		ItemStack book = EnchantedBookItem.createForEnchantment(
				new EnchantmentInstance(THUNDER_CHANNELING, level)
		);
		book.setHoverName(Component.translatable("item.mcdemo.lightning_book." + level));
		return book;
	}

	private static ItemStack createFreezingBook() {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(FREEZING, 1));
		book.setHoverName(Component.translatable("item.mcdemo.freezing_book"));
		return book;
	}

	private static ItemStack createHookReturnBook() {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(HOOK_RETURN, 1));
		book.setHoverName(Component.translatable("item.mcdemo.hook_return_book"));
		return book;
	}

	private static ItemStack createShieldDashBook() {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(SHIELD_DASH, 1));
		book.setHoverName(Component.translatable("item.mcdemo.shield_dash_book"));
		return book;
	}

	private static ItemStack createSwallowSwordBook(int level) {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(SWALLOW_SWORD, level));
		book.setHoverName(Component.translatable("item.mcdemo.swallow_sword_book." + level));
		return book;
	}

	private static ItemStack createExplosionBook() {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(EXPLOSION, 1));
		book.setHoverName(Component.translatable("item.mcdemo.explosion_book"));
		return book;
	}

	private static ItemStack createDestructionBook(int level) {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(DESTRUCTION, level));
		book.setHoverName(Component.translatable("item.mcdemo.destruction_book." + level));
		return book;
	}

	private static ItemStack createSwordWaveLengthBook(int level) {
		ItemStack book = EnchantedBookItem.createForEnchantment(
				new EnchantmentInstance(SWORD_WAVE_LENGTH, level)
		);
		book.setHoverName(Component.translatable("item.mcdemo.sword_wave_length_book." + level));
		return book;
	}

	private static ItemStack createSwordWaveDistanceBook(int level) {
		ItemStack book = EnchantedBookItem.createForEnchantment(
				new EnchantmentInstance(SWORD_WAVE_DISTANCE, level)
		);
		book.setHoverName(Component.translatable("item.mcdemo.sword_wave_distance_book." + level));
		return book;
	}

	private static ItemStack createWindEyeBook(int level) {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(WIND_EYE, level));
		book.setHoverName(Component.translatable("item.mcdemo.wind_eye_book." + level));
		return book;
	}

	private static ItemStack createTornadoBook() {
		ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(TORNADO, 1));
		book.setHoverName(Component.translatable("item.mcdemo.tornado_book"));
		return book;
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static boolean isSwallowSword(ItemStack stack) {
		return stack.getItem() instanceof SwordItem
				&& EnchantmentHelper.getItemEnchantmentLevel(SWALLOW_SWORD, stack) > 0;
	}

	public static int getSwallowSwordUseDuration(ItemStack stack) {
		int swallowLevel = Math.max(1, Math.min(3,
				EnchantmentHelper.getItemEnchantmentLevel(SWALLOW_SWORD, stack)));
		int quickChargeLevel = Math.max(0, Math.min(3,
				EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack)));
		int duration = 36 - (swallowLevel - 1) * 8 - quickChargeLevel * 4;
		return Math.max(8, duration);
	}

	public static boolean isSwallowCompressed(ItemStack stack) {
		return stack.hasTag() && stack.getTag().getBoolean(SWALLOW_COMPRESSED_TAG);
	}

	public static int getSwallowStage(ItemStack stack) {
		if (!stack.hasTag()) return 0;
		return Math.max(0, Math.min(SWALLOW_STAGE_COUNT, stack.getTag().getInt(SWALLOW_STAGE_TAG)));
	}

	public static void beginSwallow(ItemStack stack) {
		stack.getOrCreateTag().putBoolean(SWALLOW_ACTIVE_TAG, true);
		if (!isSwallowCompressed(stack)) {
			stack.getOrCreateTag().putInt(SWALLOW_STAGE_TAG, 0);
		}
	}

	public static void updateSwallowStage(ItemStack stack, int remainingTicks) {
		if (!isSwallowSword(stack) || isSwallowCompressed(stack)) return;
		int total = getSwallowSwordUseDuration(stack);
		int elapsed = Math.max(0, total - remainingTicks);
		int stage = Math.min(SWALLOW_STAGE_COUNT,
				(int)Math.floor((double)elapsed * SWALLOW_STAGE_COUNT / Math.max(1, total)));
		stack.getOrCreateTag().putInt(SWALLOW_STAGE_TAG, stage);
	}

	public static void finishSwallow(ItemStack stack) {
		var tag = stack.getOrCreateTag();
		tag.putBoolean(SWALLOW_ACTIVE_TAG, false);
		tag.putBoolean(SWALLOW_COMPRESSED_TAG, true);
		tag.putInt(SWALLOW_STAGE_TAG, SWALLOW_STAGE_COUNT);
	}

	public static void emitSwallowParticles(Player player, ItemStack stack, int remainingTicks) {
		if (!(player.level() instanceof ServerLevel serverLevel) || remainingTicks % 4 != 0) return;
		Vec3 mouth = player.getEyePosition().add(player.getLookAngle().scale(0.32D)).add(0.0D, -0.16D, 0.0D);
		serverLevel.sendParticles(
				new ItemParticleOption(ParticleTypes.ITEM, stack.copy()),
				mouth.x, mouth.y, mouth.z,
				4,
				0.10D, 0.08D, 0.10D,
				0.035D
		);
		serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT,
				SoundSource.PLAYERS, 0.35F, 1.15F + serverLevel.random.nextFloat() * 0.15F);
	}

	public static void cancelSwallow(ItemStack stack) {
		if (!stack.hasTag() || isSwallowCompressed(stack)) return;
		stack.getTag().remove(SWALLOW_ACTIVE_TAG);
		stack.getTag().remove(SWALLOW_STAGE_TAG);
	}

	public static void popSpringKnife(Player player, ItemStack stack) {
		if (!isSwallowSword(stack) || !isSwallowCompressed(stack)) return;
		var tag = stack.getOrCreateTag();
		tag.remove(SWALLOW_ACTIVE_TAG);
		tag.remove(SWALLOW_COMPRESSED_TAG);
		tag.remove(SWALLOW_STAGE_TAG);
		stack.setHoverName(Component.literal("弹簧刀"));
		if (!player.level().isClientSide) {
			performSpringKnifeSweep(player, stack);
		}
	}

	private static void performSpringKnifeSweep(Player player, ItemStack stack) {
		if (!(stack.getItem() instanceof SwordItem swordItem)) return;
		int sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, stack);
		int knockbackLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack);
		int fireAspectLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
		double radius = 2.75D + sweepingLevel * 1.25D;
		float baseAttackDamage = 1.0F + swordItem.getDamage();
		float sweepRatio = sweepingLevel > 0
				? SweepingEdgeEnchantment.getSweepingDamageRatio(sweepingLevel)
				: 0.0F;

		List<LivingEntity> targets = new ArrayList<>(player.level().getEntitiesOfClass(
				LivingEntity.class,
				player.getBoundingBox().inflate(radius, 1.75D, radius),
				target -> target != player && target.isAlive() && target.isAttackable() && !player.isAlliedTo(target)
		));
		targets.removeIf(target -> {
			double dx = target.getX() - player.getX();
			double dz = target.getZ() - player.getZ();
			return dx * dx + dz * dz > radius * radius || Math.abs(target.getY() - player.getY()) > 2.5D;
		});
		targets.sort(Comparator.comparingDouble(player::distanceToSqr));

		boolean hitAnything = false;
		for (int index = 0; index < targets.size(); index++) {
			LivingEntity target = targets.get(index);
			float enchantmentBonus = EnchantmentHelper.getDamageBonus(stack, target.getMobType());
			float fullDamage = baseAttackDamage + enchantmentBonus;
			float damage = index == 0 ? fullDamage : 1.0F + sweepRatio * fullDamage;
			if (!target.hurt(player.damageSources().playerAttack(player), damage)) {
				continue;
			}
			hitAnything = true;
			if (fireAspectLevel > 0) {
				target.setSecondsOnFire(fireAspectLevel * 4);
			}
			Vec3 away = new Vec3(target.getX() - player.getX(), 0.0D, target.getZ() - player.getZ());
			if (away.lengthSqr() > 1.0E-6D) {
				away = away.normalize();
				double push = 0.40D + knockbackLevel * 0.35D;
				target.push(away.x * push, 0.10D, away.z * push);
				target.hurtMarked = true;
			}
			EnchantmentHelper.doPostHurtEffects(target, player);
			EnchantmentHelper.doPostDamageEffects(player, target);
		}

		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, 1.0F, 1.0F);
		player.sweepAttack();
		if (hitAnything) {
			stack.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(InteractionHand.OFF_HAND));
			player.causeFoodExhaustion(0.1F);
		}
	}

	public static void startHookPull(LivingEntity owner, LivingEntity target) {
		if (owner == target || !owner.isAlive() || !target.isAlive() || owner.level().isClientSide) {
			return;
		}
		ACTIVE_HOOK_PULLS.removeIf(pull -> pull.target() == target);
		ACTIVE_HOOK_PULLS.add(new HookPull(owner, target, 16));
	}

	private static void tickHookPulls() {
		ListIterator<HookPull> iterator = ACTIVE_HOOK_PULLS.listIterator();
		while (iterator.hasNext()) {
			HookPull pull = iterator.next();
			LivingEntity owner = pull.owner();
			LivingEntity target = pull.target();
			if (!owner.isAlive() || !target.isAlive() || owner.level() != target.level()) {
				iterator.remove();
				continue;
			}

			Vec3 look = owner.getLookAngle();
			Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
			if (horizontalLook.lengthSqr() < 1.0E-6D) {
				horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
			} else {
				horizontalLook = horizontalLook.normalize();
			}
			Vec3 destination = owner.position().add(horizontalLook.scale(1.8D)).add(0.0D, 0.15D, 0.0D);
			Vec3 offset = destination.subtract(target.position());
			double distance = offset.length();
			int ticksRemaining = pull.ticksRemaining() - 1;

			if (distance <= 1.05D || ticksRemaining <= 0) {
				target.setDeltaMovement(target.getDeltaMovement().scale(0.25D));
				target.hurtMarked = true;
				iterator.remove();
				continue;
			}

			double speed = Math.min(2.8D, Math.max(0.85D, distance * 0.52D));
			Vec3 velocity = offset.normalize().scale(speed);
			target.setDeltaMovement(velocity.x, Math.max(-0.55D, velocity.y), velocity.z);
			target.hurtMarked = true;
			iterator.set(new HookPull(owner, target, ticksRemaining));
		}
	}

	private static void tickShieldDash(MinecraftServer server) {
		SHIELD_DASH_COOLDOWNS.replaceAll((uuid, ticks) -> ticks - 1);
		SHIELD_DASH_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= 0);

		for (var player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || player.isSpectator() || SHIELD_DASH_COOLDOWNS.containsKey(player.getUUID())) {
				continue;
			}

			ItemStack mainHand = player.getMainHandItem();
			ItemStack offHand = player.getOffhandItem();
			boolean active = (mainHand.is(Items.SHIELD) && EnchantmentHelper.getItemEnchantmentLevel(SHIELD_DASH, mainHand) > 0)
					|| (offHand.is(Items.SHIELD) && EnchantmentHelper.getItemEnchantmentLevel(SHIELD_DASH, offHand) > 0);
			if (!active) {
				continue;
			}

			Vec3 look = player.getLookAngle();
			Vec3 forward = new Vec3(look.x, 0.0D, look.z);
			if (forward.lengthSqr() < 1.0E-6D) {
				continue;
			}
			forward = forward.normalize();
			boolean triggered = false;
			AABB area = player.getBoundingBox().inflate(SHIELD_DASH_RANGE, 2.0D, SHIELD_DASH_RANGE);
			for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
				if (target == player || !target.isAlive()) {
					continue;
				}

				Vec3 horizontalOffset = new Vec3(
						target.getX() - player.getX(),
						0.0D,
						target.getZ() - player.getZ()
				);
				double distance = horizontalOffset.length();
				if (distance < 0.05D || distance > SHIELD_DASH_RANGE) {
					continue;
				}

				Vec3 direction = horizontalOffset.scale(1.0D / distance);
				if (forward.dot(direction) < 0.15D) {
					continue;
				}

				Vec3 previousHorizontalOffset = new Vec3(
						target.xo - player.xo,
						0.0D,
						target.zo - player.zo
				);
				double previousDistance = previousHorizontalOffset.length();
				double distanceClosingSpeed = previousDistance - distance;

				Vec3 playerVelocity = new Vec3(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
				Vec3 targetVelocity = new Vec3(target.getDeltaMovement().x, 0.0D, target.getDeltaMovement().z);
				double velocityClosingSpeed = playerVelocity.subtract(targetVelocity).dot(direction);
				double closingSpeed = Math.max(distanceClosingSpeed, velocityClosingSpeed);
				if (closingSpeed < SHIELD_DASH_MIN_CLOSING_SPEED) {
					continue;
				}

				float damage = (float) Math.min(10.0D, 4.0D + closingSpeed * 6.0D);
				target.invulnerableTime = 0;
				target.hurt(player.level().damageSources().playerAttack(player), damage);

				double strength = Math.min(3.0D, 1.15D + closingSpeed * 4.8D);
				Vec3 currentVelocity = target.getDeltaMovement();
				target.setDeltaMovement(
						currentVelocity.x * 0.18D + direction.x * strength,
						Math.max(currentVelocity.y, 0.38D + Math.min(0.30D, closingSpeed)),
						currentVelocity.z * 0.18D + direction.z * strength
				);
				target.hurtMarked = true;
				triggered = true;
			}

			if (triggered) {
				SHIELD_DASH_COOLDOWNS.put(player.getUUID(), SHIELD_DASH_COOLDOWN_TICKS);
				player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
						SoundSource.PLAYERS, 1.0F, 0.72F);
			}
		}
	}

	private static void tickWindEyes() {
		ListIterator<WindEye> iterator = ACTIVE_WIND_EYES.listIterator();
		while (iterator.hasNext()) {
			WindEye windEye = iterator.next();
			ServerLevel level = windEye.level();
			Vec3 center = windEye.center();
			int ticksRemaining = windEye.ticksRemaining() - 1;
			int age = windEye.totalDuration() - ticksRemaining;

			// Two moving cloud points make the five-block wind eye easy to locate
			// without flooding the client with hundreds of particles every tick.
			for (int arm = 0; arm < 2; arm++) {
				double angle = age * 0.28D + arm * Math.PI;
				double radius = 1.0D + (age % 20) * 0.19D;
				level.sendParticles(
						ParticleTypes.CLOUD,
						center.x + Math.cos(angle) * radius,
						center.y + 0.25D,
						center.z + Math.sin(angle) * radius,
						2,
						0.08D,
						0.05D,
						0.08D,
						0.01D
				);
			}

			AABB area = new AABB(center, center).inflate(5.0D);
			boolean damageTick = ticksRemaining % 20 == 0;
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
				if (!entity.isAlive() || entity == windEye.owner()) {
					continue;
				}
				Vec3 offset = center.subtract(entity.getBoundingBox().getCenter());
				double distance = offset.length();
				if (distance > 5.0D) {
					continue;
				}

				if (distance > 0.2D) {
					Vec3 pull = offset.normalize();
					double horizontalPull = windEye.pullStrength();
					entity.push(
							pull.x * horizontalPull,
							pull.y * horizontalPull * 0.45D,
							pull.z * horizontalPull
					);
					entity.hurtMarked = true;
				}
				if (damageTick) {
					entity.hurt(level.damageSources().magic(), 2.0F);
				}
			}

			if (ticksRemaining <= 0) {
				iterator.remove();
			} else {
				iterator.set(new WindEye(
						level,
						center,
						windEye.owner(),
						ticksRemaining,
						windEye.totalDuration(),
						windEye.pullStrength()
				));
			}
		}
	}

	private static void tickTornadoes() {
		ListIterator<Tornado> iterator = ACTIVE_TORNADOES.listIterator();
		while (iterator.hasNext()) {
			Tornado tornado = iterator.next();
			ServerLevel level = tornado.level();
			Vec3 center = tornado.center().add(tornado.velocity());
			Vec3 attractionCenter = center.add(0.0D, 1.6D, 0.0D);
			int ticksRemaining = tornado.ticksRemaining() - 1;
			int age = 100 - ticksRemaining;

			// Stack rotating cloud rings into a tapered, moving funnel.
			for (int layer = 0; layer < 5; layer++) {
				double height = layer * 0.72D;
				double radius = 0.35D + layer * 0.48D;
				double angle = age * 0.48D + layer * 1.37D;
				level.sendParticles(
						ParticleTypes.CLOUD,
						center.x + Math.cos(angle) * radius,
						center.y + height,
						center.z + Math.sin(angle) * radius,
						3,
						0.12D,
						0.10D,
						0.12D,
						0.02D
				);
			}
			level.sendParticles(
					ParticleTypes.POOF,
					attractionCenter.x,
					attractionCenter.y,
					attractionCenter.z,
					2,
					0.45D,
					0.8D,
					0.45D,
					0.04D
			);

			AABB area = new AABB(attractionCenter, attractionCenter).inflate(4.0D);
			boolean damageTick = ticksRemaining % 10 == 0;
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
				if (!entity.isAlive() || entity == tornado.owner()) {
					continue;
				}
				Vec3 offset = attractionCenter.subtract(entity.getBoundingBox().getCenter());
				double distance = offset.length();
				if (distance > 4.0D) {
					continue;
				}

				if (distance > 0.15D) {
					Vec3 pull = offset.normalize();
					entity.push(pull.x * 0.32D, pull.y * 0.22D, pull.z * 0.32D);
					entity.hurtMarked = true;
				}
				if (damageTick) {
					// The requested half-second cadence is shorter than vanilla's
					// normal hurt immunity, so reset it for each tornado damage pulse.
					entity.invulnerableTime = 0;
					entity.hurt(level.damageSources().magic(), 3.0F);
				}
			}

			if (ticksRemaining % 20 == 0) {
				level.playSound(
						null,
						center.x,
						center.y,
						center.z,
						SoundEvents.TRIDENT_RIPTIDE_3,
						SoundSource.PLAYERS,
						0.8F,
						0.7F
				);
			}

			if (ticksRemaining <= 0) {
				iterator.remove();
			} else {
				iterator.set(new Tornado(level, center, tornado.velocity(), tornado.owner(), ticksRemaining));
			}
		}
	}

	public static final class EnderWandItem extends Item {
		private static final double USE_RANGE = 20.0D;

		public EnderWandItem(Properties properties) {
			super(properties);
		}

		@Override
		public int getEnchantmentValue() {
			return 15;
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			Player player = context.getPlayer();
			if (player == null || player.getEyePosition().distanceToSqr(context.getClickLocation())
					> USE_RANGE * USE_RANGE) {
				return InteractionResult.FAIL;
			}
			BlockPos destination = context.getClickedPos().relative(context.getClickedFace());
			return teleportToBlock(
					context.getLevel(),
					player,
					context.getItemInHand(),
					context.getHand(),
					destination
			);
		}

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack wand = player.getItemInHand(hand);
			BlockHitResult blockHit = (BlockHitResult) player.pick(USE_RANGE, 1.0F, false);
			if (blockHit.getType() == HitResult.Type.MISS) {
				return InteractionResultHolder.pass(wand);
			}

			BlockPos destination = blockHit.getBlockPos().relative(blockHit.getDirection());
			InteractionResult result = teleportToBlock(level, player, wand, hand, destination);
			return new InteractionResultHolder<>(result, wand);
		}

		private static InteractionResult teleportToBlock(
				Level level,
				Player player,
				ItemStack wand,
				InteractionHand hand,
				BlockPos requestedDestination
		) {
			if (player.getCooldowns().isOnCooldown(wand.getItem())) {
				return InteractionResult.FAIL;
			}
			if (!(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.SUCCESS;
			}

			Vec3 destination = findSafeDestination(serverLevel, player, requestedDestination);
			if (destination == null) {
				player.displayClientMessage(Component.translatable("message.mcdemo.ender_wand.no_space"), true);
				return InteractionResult.FAIL;
			}

			Vec3 origin = player.position();
			serverLevel.sendParticles(
					ParticleTypes.PORTAL,
					origin.x,
					origin.y + player.getBbHeight() * 0.5D,
					origin.z,
					64,
					0.45D,
					player.getBbHeight() * 0.45D,
					0.45D,
					0.35D
			);
			serverLevel.playSound(
					null,
					origin.x,
					origin.y,
					origin.z,
					SoundEvents.ENDERMAN_TELEPORT,
					SoundSource.PLAYERS,
					1.0F,
					1.0F
			);

			player.teleportTo(destination.x, destination.y, destination.z);
			player.fallDistance = 0.0F;
			serverLevel.sendParticles(
					ParticleTypes.REVERSE_PORTAL,
					destination.x,
					destination.y + player.getBbHeight() * 0.5D,
					destination.z,
					64,
					0.45D,
					player.getBbHeight() * 0.45D,
					0.45D,
					0.12D
			);
			serverLevel.playSound(
					null,
					destination.x,
					destination.y,
					destination.z,
					SoundEvents.ENDERMAN_TELEPORT,
					SoundSource.PLAYERS,
					1.0F,
					1.0F
			);

			player.getCooldowns().addCooldown(wand.getItem(), 20);
			wand.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
			return InteractionResult.CONSUME;
		}

		private static Vec3 findSafeDestination(ServerLevel level, Player player, BlockPos requested) {
			for (int offsetY = 0; offsetY <= 3; offsetY++) {
				BlockPos candidateBlock = requested.above(offsetY);
				if (!level.getWorldBorder().isWithinBounds(candidateBlock)) {
					continue;
				}
				Vec3 candidate = Vec3.atBottomCenterOf(candidateBlock);
				AABB movedBounds = player.getBoundingBox().move(
						candidate.x - player.getX(),
						candidate.y - player.getY(),
						candidate.z - player.getZ()
				);
				if (level.noCollision(player, movedBounds) && !level.containsAnyLiquid(movedBounds)) {
					return candidate;
				}
			}
			return null;
		}
	}

	public static final class HurricaneWandItem extends Item {
		private static final double USE_RANGE = 15.0D;

		public HurricaneWandItem(Properties properties) {
			super(properties);
		}

		@Override
		public int getEnchantmentValue() {
			return 15;
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			Player player = context.getPlayer();
			Vec3 center = Vec3.atCenterOf(context.getClickedPos()).add(
					Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(0.5D)
			);
			if (player == null || player.getEyePosition().distanceToSqr(center) > USE_RANGE * USE_RANGE) {
				return InteractionResult.FAIL;
			}
			return createWindEye(context.getLevel(), player, context.getItemInHand(), context.getHand(), center);
		}

		@Override
		public InteractionResult interactLivingEntity(
				ItemStack stack,
				Player player,
				LivingEntity target,
				InteractionHand hand
		) {
			if (player.distanceToSqr(target) > USE_RANGE * USE_RANGE) {
				return InteractionResult.FAIL;
			}
			return createWindEye(player.level(), player, stack, hand, target.getBoundingBox().getCenter());
		}

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack wand = player.getItemInHand(hand);
			if (EnchantmentHelper.getItemEnchantmentLevel(TORNADO, wand) > 0) {
				InteractionResult result = createWindEye(
						level,
						player,
						wand,
						hand,
						player.getEyePosition().add(player.getLookAngle().scale(1.5D))
				);
				return new InteractionResultHolder<>(result, wand);
			}
			Vec3 start = player.getEyePosition();
			Vec3 look = player.getViewVector(1.0F);
			Vec3 end = start.add(look.scale(USE_RANGE));
			BlockHitResult blockHit = (BlockHitResult) player.pick(USE_RANGE, 1.0F, false);
			double nearestDistance = blockHit.getType() == HitResult.Type.MISS
					? USE_RANGE * USE_RANGE
					: start.distanceToSqr(blockHit.getLocation());
			AABB searchBox = player.getBoundingBox().expandTowards(look.scale(USE_RANGE)).inflate(1.0D);
			EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
					player,
					start,
					end,
					searchBox,
					entity -> entity != player && !entity.isSpectator() && entity.isPickable(),
					nearestDistance
			);

			InteractionResult result;
			if (entityHit != null) {
				result = createWindEye(level, player, wand, hand, entityHit.getEntity().getBoundingBox().getCenter());
			} else if (blockHit.getType() != HitResult.Type.MISS) {
				result = createWindEye(level, player, wand, hand, blockHit.getLocation());
			} else {
				return InteractionResultHolder.pass(wand);
			}
			return new InteractionResultHolder<>(result, wand);
		}

		private static InteractionResult createWindEye(
				Level level,
				Player player,
				ItemStack wand,
				InteractionHand hand,
				Vec3 center
		) {
			if (player.getCooldowns().isOnCooldown(wand.getItem())) {
				return InteractionResult.FAIL;
			}
			if (!(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.SUCCESS;
			}
			if (EnchantmentHelper.getItemEnchantmentLevel(TORNADO, wand) > 0) {
				// Preserve all three components of the view vector so the tornado
				// follows the crosshair upward, downward, horizontally or diagonally.
				Vec3 direction = player.getLookAngle().normalize();
				if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, wand) > 0) {
					spawnTornado(serverLevel, player, spreadDirection(direction, -20.0D));
					spawnTornado(serverLevel, player, direction);
					spawnTornado(serverLevel, player, spreadDirection(direction, 20.0D));
				} else {
					spawnTornado(serverLevel, player, direction);
				}
				Vec3 soundPosition = player.position().add(direction.scale(1.5D));
				serverLevel.playSound(
						null,
						soundPosition.x,
						soundPosition.y,
						soundPosition.z,
						SoundEvents.TRIDENT_RIPTIDE_3,
						SoundSource.PLAYERS,
						1.2F,
						0.65F
				);
				player.getCooldowns().addCooldown(wand.getItem(), 100);
				wand.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
				return InteractionResult.CONSUME;
			}

			int windEyeLevel = Math.min(3, EnchantmentHelper.getItemEnchantmentLevel(WIND_EYE, wand));
			int duration = 100 + windEyeLevel * 40;
			double pullStrength = 0.04D + windEyeLevel * 0.015D;
			ACTIVE_WIND_EYES.add(new WindEye(
					serverLevel,
					center,
					player,
					duration,
					duration,
					pullStrength
			));
			serverLevel.playSound(
					null,
					center.x,
					center.y,
					center.z,
					SoundEvents.TRIDENT_RIPTIDE_1,
					SoundSource.PLAYERS,
					1.0F,
					0.8F
			);
			player.getCooldowns().addCooldown(wand.getItem(), duration);
			wand.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
			return InteractionResult.CONSUME;
		}

		private static void spawnTornado(ServerLevel level, Player player, Vec3 direction) {
			Vec3 normalizedDirection = direction.normalize();
			Vec3 velocity = normalizedDirection.scale(0.35D);
			Vec3 spawnPosition = player.position().add(normalizedDirection.scale(1.5D));
			ACTIVE_TORNADOES.add(new Tornado(level, spawnPosition, velocity, player, 100));
		}

		private static Vec3 spreadDirection(Vec3 direction, double degrees) {
			Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
			if (side.lengthSqr() < 1.0E-6D) {
				side = new Vec3(1.0D, 0.0D, 0.0D);
			} else {
				side = side.normalize();
			}
			double radians = Math.toRadians(degrees);
			return direction.scale(Math.cos(radians)).add(side.scale(Math.sin(radians))).normalize();
		}
	}

	public static final class ThunderWandItem extends Item {
		private static final double ATTACK_RANGE = 10.0D;

		public ThunderWandItem(Properties properties) {
			super(properties);
		}

		@Override
		public int getEnchantmentValue() {
			return 15;
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			Player player = context.getPlayer();
			if (player == null || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(context.getClickedPos()))
					> ATTACK_RANGE * ATTACK_RANGE) {
				return InteractionResult.FAIL;
			}

			BlockPos strikePos = context.getClickedPos().relative(context.getClickedFace());
			return summonLightning(
					context.getLevel(),
					player,
					context.getItemInHand(),
					context.getHand(),
					strikePos.getX() + 0.5D,
					strikePos.getY(),
					strikePos.getZ() + 0.5D
			);
		}

		@Override
		public InteractionResult interactLivingEntity(
				ItemStack stack,
				Player player,
				LivingEntity target,
				InteractionHand hand
		) {
			if (player.distanceToSqr(target) > ATTACK_RANGE * ATTACK_RANGE) {
				return InteractionResult.FAIL;
			}

			return summonLightning(
					player.level(),
					player,
					stack,
					hand,
					target.getX(),
					target.getY(),
					target.getZ()
			);
		}

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack wand = player.getItemInHand(hand);
			Vec3 start = player.getEyePosition();
			Vec3 look = player.getViewVector(1.0F);
			Vec3 end = start.add(look.scale(ATTACK_RANGE));
			BlockHitResult blockHit = (BlockHitResult) player.pick(ATTACK_RANGE, 1.0F, false);
			double nearestDistance = blockHit.getType() == HitResult.Type.MISS
					? ATTACK_RANGE * ATTACK_RANGE
					: start.distanceToSqr(blockHit.getLocation());
			AABB searchBox = player.getBoundingBox().expandTowards(look.scale(ATTACK_RANGE)).inflate(1.0D);
			EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
					player,
					start,
					end,
					searchBox,
					entity -> entity != player && !entity.isSpectator() && entity.isPickable(),
					nearestDistance
			);

			InteractionResult result;
			if (entityHit != null) {
				Entity target = entityHit.getEntity();
				result = summonLightning(level, player, wand, hand, target.getX(), target.getY(), target.getZ());
			} else if (blockHit.getType() != HitResult.Type.MISS) {
				BlockPos strikePos = blockHit.getBlockPos().relative(blockHit.getDirection());
				result = summonLightning(
						level,
						player,
						wand,
						hand,
						strikePos.getX() + 0.5D,
						strikePos.getY(),
						strikePos.getZ() + 0.5D
				);
			} else {
				return InteractionResultHolder.pass(wand);
			}

			return new InteractionResultHolder<>(result, wand);
		}

		private static InteractionResult summonLightning(
				Level level,
				Player player,
				ItemStack wand,
				InteractionHand hand,
				double x,
				double y,
				double z
		) {
			if (player.getCooldowns().isOnCooldown(wand.getItem())) {
				return InteractionResult.FAIL;
			}
			if (!(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.SUCCESS;
			}

			if (!spawnLightning(serverLevel, x, y, z)) {
				return InteractionResult.FAIL;
			}

			int channelingLevel = Math.min(3, Math.max(
					EnchantmentHelper.getItemEnchantmentLevel(THUNDER_CHANNELING, wand),
					EnchantmentHelper.getItemEnchantmentLevel(Enchantments.CHANNELING, wand)
			));
			for (int strike = 1; strike <= channelingLevel * 2; strike++) {
				SCHEDULED_LIGHTNING.add(new ScheduledLightning(serverLevel, x, y, z, strike * 4));
			}
			player.getCooldowns().addCooldown(wand.getItem(), 20);
			wand.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));

			return InteractionResult.CONSUME;
		}
	}

	private static boolean spawnLightning(ServerLevel level, double x, double y, double z) {
		AABB damageArea = new AABB(x - 3.0D, y - 3.0D, z - 3.0D, x + 3.0D, y + 6.0D, z + 3.0D);
		level.getEntitiesOfClass(LivingEntity.class, damageArea)
				.forEach(entity -> entity.invulnerableTime = 0);

		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
		if (lightning == null) {
			return false;
		}
		lightning.moveTo(x, y, z);
		level.addFreshEntity(lightning);
		return true;
	}

	private static void tickScheduledLightning() {
		ListIterator<ScheduledLightning> iterator = SCHEDULED_LIGHTNING.listIterator();
		while (iterator.hasNext()) {
			ScheduledLightning scheduled = iterator.next();
			int ticksRemaining = scheduled.ticksRemaining() - 1;
			if (ticksRemaining <= 0) {
				spawnLightning(scheduled.level(), scheduled.x(), scheduled.y(), scheduled.z());
				iterator.remove();
			} else {
				iterator.set(new ScheduledLightning(
						scheduled.level(),
						scheduled.x(),
						scheduled.y(),
						scheduled.z(),
						ticksRemaining
				));
			}
		}
	}

	private static void freezeEntity(LivingEntity entity, int ticks) {
		ListIterator<FrozenEntity> iterator = FROZEN_ENTITIES.listIterator();
		while (iterator.hasNext()) {
			FrozenEntity frozen = iterator.next();
			if (frozen.entity().getUUID().equals(entity.getUUID())) {
				iterator.set(new FrozenEntity(entity, Math.max(frozen.ticksRemaining(), ticks), frozen.previousFrozenTicks()));
				return;
			}
		}
		FROZEN_ENTITIES.add(new FrozenEntity(entity, ticks, entity.getTicksFrozen()));
	}

	private static void tickFrozenEntities() {
		ListIterator<FrozenEntity> iterator = FROZEN_ENTITIES.listIterator();
		while (iterator.hasNext()) {
			FrozenEntity frozen = iterator.next();
			LivingEntity entity = frozen.entity();
			if (!entity.isAlive()) {
				iterator.remove();
				continue;
			}

			entity.setTicksFrozen(entity.getTicksRequiredToFreeze());
			int ticksRemaining = frozen.ticksRemaining() - 1;
			if (ticksRemaining <= 0) {
				entity.setTicksFrozen(frozen.previousFrozenTicks());
				iterator.remove();
			} else {
				iterator.set(new FrozenEntity(entity, ticksRemaining, frozen.previousFrozenTicks()));
			}
		}
	}

	private record ScheduledLightning(ServerLevel level, double x, double y, double z, int ticksRemaining) {
	}

	private record FrozenEntity(LivingEntity entity, int ticksRemaining, int previousFrozenTicks) {
	}

	private record HookPull(LivingEntity owner, LivingEntity target, int ticksRemaining) {
	}

	private record WindEye(
			ServerLevel level,
			Vec3 center,
			LivingEntity owner,
			int ticksRemaining,
			int totalDuration,
			double pullStrength
	) {
	}

	private record Tornado(
			ServerLevel level,
			Vec3 center,
			Vec3 velocity,
			LivingEntity owner,
			int ticksRemaining
	) {
	}

	private static final class ExcaliburEnchantment extends Enchantment {
		private final int baseMinCost;
		private final int costPerLevel;
		private final int costRange;
		private final int maxLevel;

		private ExcaliburEnchantment(
				Rarity rarity,
				int baseMinCost,
				int costPerLevel,
				int costRange,
				int maxLevel
		) {
			super(rarity, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
			this.baseMinCost = baseMinCost;
			this.costPerLevel = costPerLevel;
			this.costRange = costRange;
			this.maxLevel = maxLevel;
		}

		@Override
		public int getMinCost(int level) {
			return baseMinCost + (level - 1) * costPerLevel;
		}

		@Override
		public int getMaxCost(int level) {
			return getMinCost(level) + costRange;
		}

		@Override
		public int getMaxLevel() {
			return maxLevel;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.is(EXCALIBUR);
		}
	}

	private static final class ThunderChannelingEnchantment extends Enchantment {
		private ThunderChannelingEnchantment() {
			super(Enchantment.Rarity.RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 10 + (level - 1) * 15;
		}

		@Override
		public int getMaxCost(int level) {
			return getMinCost(level) + 30;
		}

		@Override
		public int getMaxLevel() {
			return 3;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.is(THUNDER_WAND);
		}
	}

	private static final class WindEyeEnchantment extends Enchantment {
		private WindEyeEnchantment() {
			super(Enchantment.Rarity.RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 10 + (level - 1) * 15;
		}

		@Override
		public int getMaxCost(int level) {
			return getMinCost(level) + 30;
		}

		@Override
		public int getMaxLevel() {
			return 3;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.is(HURRICANE_WAND);
		}

		@Override
		protected boolean checkCompatibility(Enchantment other) {
			return super.checkCompatibility(other) && other != TORNADO;
		}
	}

	private static final class TornadoEnchantment extends Enchantment {
		private TornadoEnchantment() {
			super(Enchantment.Rarity.VERY_RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 30;
		}

		@Override
		public int getMaxCost(int level) {
			return 60;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.is(HURRICANE_WAND);
		}

		@Override
		protected boolean checkCompatibility(Enchantment other) {
			return super.checkCompatibility(other) && other != WIND_EYE;
		}
	}

	private static final class HookReturnEnchantment extends Enchantment {
		private HookReturnEnchantment() {
			super(Enchantment.Rarity.RARE, EnchantmentCategory.TRIDENT, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 18;
		}

		@Override
		public int getMaxCost(int level) {
			return 48;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.is(Items.TRIDENT);
		}
	}

	private static final class ShieldDashEnchantment extends Enchantment {
		private ShieldDashEnchantment() {
			super(Enchantment.Rarity.RARE, EnchantmentCategory.BREAKABLE,
					new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 16;
		}

		@Override
		public int getMaxCost(int level) {
			return 46;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.is(Items.SHIELD);
		}
	}

	private static final class SwallowSwordEnchantment extends Enchantment {
		private SwallowSwordEnchantment() {
			super(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON,
					new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 12 + (level - 1) * 12;
		}

		@Override
		public int getMaxCost(int level) {
			return getMinCost(level) + 28;
		}

		@Override
		public int getMaxLevel() {
			return 3;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.getItem() instanceof SwordItem;
		}
	}

	private static final class FreezingEnchantment extends Enchantment {
		private FreezingEnchantment() {
			super(Enchantment.Rarity.RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
		}

		@Override
		public int getMinCost(int level) {
			return 20;
		}

		@Override
		public int getMaxCost(int level) {
			return 50;
		}

		@Override
		public boolean canEnchant(ItemStack stack) {
			return stack.is(ICE_WAND);
		}
	}

	private static final class FrozenTintEffect extends MobEffect {
		private FrozenTintEffect() {
			super(MobEffectCategory.HARMFUL, 0x76D7FF);
		}
	}

	public static final class FlameWandItem extends Item {
		public FlameWandItem(Properties properties) {
			super(properties);
		}

		@Override
		public int getEnchantmentValue() {
			return 15;
		}

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack wand = player.getItemInHand(hand);
			if (!level.isClientSide) {
				Vec3 direction = player.getLookAngle();
				int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, wand);
				int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, wand);
				int flameLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, wand);
				int quickChargeLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, wand);
				int multishotLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, wand);
				int piercingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, wand);
				float directDamage = 10.0F + powerLevel * 1.25F;
				int burnSeconds = 5 + flameLevel * 5;

				if (multishotLevel > 0) {
					spawnFireball(level, player, direction.yRot((float) Math.toRadians(-10.0D)), directDamage, burnSeconds, punchLevel, piercingLevel);
					spawnFireball(level, player, direction, directDamage, burnSeconds, punchLevel, piercingLevel);
					spawnFireball(level, player, direction.yRot((float) Math.toRadians(10.0D)), directDamage, burnSeconds, punchLevel, piercingLevel);
				} else {
					spawnFireball(level, player, direction, directDamage, burnSeconds, punchLevel, piercingLevel);
				}

				level.playSound(
						null,
						player.blockPosition(),
						SoundEvents.BLAZE_SHOOT,
						SoundSource.PLAYERS,
						1.0F,
						1.0F
				);

				player.getCooldowns().addCooldown(this, Math.max(5, 20 - quickChargeLevel * 5));
				wand.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
			}

			return InteractionResultHolder.sidedSuccess(wand, level.isClientSide());
		}

		private static void spawnFireball(
				Level level,
				Player player,
				Vec3 direction,
				float directDamage,
				int burnSeconds,
				int punchLevel,
				int piercingLevel
		) {
			WandFireball fireball = new WandFireball(
					level,
					player,
					direction.x,
					direction.y,
					direction.z,
					directDamage,
					burnSeconds,
					punchLevel,
					piercingLevel
			);
			fireball.setPos(
					player.getX() + direction.x,
					player.getEyeY() - 0.1D,
					player.getZ() + direction.z
			);
			level.addFreshEntity(fireball);
		}
	}

	public static final class WandFireball extends SmallFireball {
		private final float directDamage;
		private final int burnSeconds;
		private final int punchLevel;
		private final int piercingLevel;
		private final Set<Integer> piercedEntityIds = new HashSet<>();

		public WandFireball(
				Level level,
				LivingEntity owner,
				double xPower,
				double yPower,
				double zPower,
				float directDamage,
				int burnSeconds,
				int punchLevel,
				int piercingLevel
		) {
			super(level, owner, xPower, yPower, zPower);
			this.directDamage = directDamage;
			this.burnSeconds = burnSeconds;
			this.punchLevel = punchLevel;
			this.piercingLevel = piercingLevel;
			this.xPower *= 2.0D;
			this.yPower *= 2.0D;
			this.zPower *= 2.0D;
		}

		@Override
		protected void onHit(HitResult hitResult) {
			if (hitResult instanceof EntityHitResult entityHitResult) {
				if (!piercedEntityIds.add(entityHitResult.getEntity().getId())) {
					return;
				}
				onHitEntity(entityHitResult);
				if (!level().isClientSide && piercedEntityIds.size() > piercingLevel) {
					discard();
				}
			} else if (hitResult instanceof BlockHitResult blockHitResult) {
				onHitBlock(blockHitResult);
				if (!level().isClientSide) {
					discard();
				}
			}
		}

		@Override
		protected void onHitEntity(EntityHitResult hitResult) {
			if (level().isClientSide) {
				return;
			}

			Entity target = hitResult.getEntity();
			Entity owner = getOwner();
			int previousFireTicks = target.getRemainingFireTicks();
			target.setSecondsOnFire(burnSeconds);

			if (!target.hurt(damageSources().fireball(this, owner), directDamage)) {
				target.setRemainingFireTicks(previousFireTicks);
			} else {
				if (owner instanceof LivingEntity livingOwner) {
					doEnchantDamageEffects(livingOwner, target);
				}
				if (punchLevel > 0) {
					Vec3 movement = getDeltaMovement();
					double horizontalLength = movement.horizontalDistance();
					if (horizontalLength > 0.0D) {
						target.push(
								movement.x / horizontalLength * punchLevel * 0.6D,
								0.1D,
								movement.z / horizontalLength * punchLevel * 0.6D
						);
					}
				}
			}
		}
	}

	public static final class IceWandItem extends Item {
		public IceWandItem(Properties properties) {
			super(properties);
		}

		@Override
		public int getEnchantmentValue() {
			return 15;
		}

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack wand = player.getItemInHand(hand);
			if (!level.isClientSide) {
				Vec3 direction = player.getLookAngle();
				int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, wand);
				int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, wand);
				int flameLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, wand);
				int quickChargeLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, wand);
				int multishotLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MULTISHOT, wand);
				int piercingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, wand);
				boolean freezing = EnchantmentHelper.getItemEnchantmentLevel(FREEZING, wand) > 0;
				float directDamage = 5.0F + powerLevel * 1.25F;
				int flameSeconds = flameLevel * 5;

				if (multishotLevel > 0) {
					spawnIceBall(level, player, direction.yRot((float) Math.toRadians(-10.0D)), directDamage, flameSeconds, punchLevel, piercingLevel, freezing);
					spawnIceBall(level, player, direction, directDamage, flameSeconds, punchLevel, piercingLevel, freezing);
					spawnIceBall(level, player, direction.yRot((float) Math.toRadians(10.0D)), directDamage, flameSeconds, punchLevel, piercingLevel, freezing);
				} else {
					spawnIceBall(level, player, direction, directDamage, flameSeconds, punchLevel, piercingLevel, freezing);
				}

				level.playSound(
						null,
						player.blockPosition(),
						SoundEvents.SNOW_GOLEM_SHOOT,
						SoundSource.PLAYERS,
						1.0F,
						0.8F
				);
				player.getCooldowns().addCooldown(this, Math.max(5, 20 - quickChargeLevel * 5));
				wand.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
			}

			return InteractionResultHolder.sidedSuccess(wand, level.isClientSide());
		}

		private static void spawnIceBall(
				Level level,
				Player player,
				Vec3 direction,
				float directDamage,
				int flameSeconds,
				int punchLevel,
				int piercingLevel,
				boolean freezing
		) {
			WandIceBall iceBall = new WandIceBall(
					level,
					player,
					directDamage,
					flameSeconds,
					punchLevel,
					piercingLevel,
					freezing
			);
			iceBall.setPos(
					player.getX() + direction.x,
					player.getEyeY() - 0.1D,
					player.getZ() + direction.z
			);
			iceBall.shoot(direction.x, direction.y, direction.z, 2.0F, 0.0F);
			level.addFreshEntity(iceBall);
		}
	}

	public static final class WandIceBall extends Snowball {
		private final float directDamage;
		private final int flameSeconds;
		private final int punchLevel;
		private final int piercingLevel;
		private final boolean freezing;
		private final Set<Integer> piercedEntityIds = new HashSet<>();

		public WandIceBall(
				Level level,
				LivingEntity owner,
				float directDamage,
				int flameSeconds,
				int punchLevel,
				int piercingLevel,
				boolean freezing
		) {
			super(level, owner);
			this.directDamage = directDamage;
			this.flameSeconds = flameSeconds;
			this.punchLevel = punchLevel;
			this.piercingLevel = piercingLevel;
			this.freezing = freezing;
			setItem(new ItemStack(Items.SNOWBALL));
		}

		@Override
		public void tick() {
			super.tick();
			if (level() instanceof ServerLevel serverLevel && isAlive()) {
				serverLevel.sendParticles(
						ParticleTypes.SOUL_FIRE_FLAME,
						getX(),
						getY(),
						getZ(),
						2,
						0.06D,
						0.06D,
						0.06D,
						0.0D
				);
				serverLevel.sendParticles(
						ParticleTypes.SNOWFLAKE,
						getX(),
						getY(),
						getZ(),
						1,
						0.04D,
						0.04D,
						0.04D,
						0.01D
				);
			}
		}

		@Override
		protected void onHit(HitResult hitResult) {
			if (hitResult instanceof EntityHitResult entityHitResult) {
				if (!piercedEntityIds.add(entityHitResult.getEntity().getId())) {
					return;
				}
				onHitEntity(entityHitResult);
				level().broadcastEntityEvent(this, (byte) 3);
				if (!level().isClientSide && piercedEntityIds.size() > piercingLevel) {
					discard();
				}
			} else if (hitResult instanceof BlockHitResult blockHitResult) {
				onHitBlock(blockHitResult);
				if (!level().isClientSide) {
					level().broadcastEntityEvent(this, (byte) 3);
					discard();
				}
			}
		}

		@Override
		protected void onHitEntity(EntityHitResult hitResult) {
			if (level().isClientSide) {
				return;
			}

			Entity target = hitResult.getEntity();
			Entity owner = getOwner();
			int previousFireTicks = target.getRemainingFireTicks();
			if (flameSeconds > 0) {
				target.setSecondsOnFire(flameSeconds);
			}

			if (!target.hurt(damageSources().thrown(this, owner), directDamage)) {
				target.setRemainingFireTicks(previousFireTicks);
				return;
			}

			if (owner instanceof LivingEntity livingOwner) {
				doEnchantDamageEffects(livingOwner, target);
			}
			if (target instanceof LivingEntity livingTarget) {
				livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
				if (freezing) {
					livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9));
					livingTarget.addEffect(new MobEffectInstance(FROZEN_TINT, 40, 0, false, false, false));
					freezeEntity(livingTarget, 40);
				}
			}
			if (punchLevel > 0) {
				Vec3 movement = getDeltaMovement();
				double horizontalLength = movement.horizontalDistance();
				if (horizontalLength > 0.0D) {
					target.push(
							movement.x / horizontalLength * punchLevel * 0.6D,
							0.1D,
							movement.z / horizontalLength * punchLevel * 0.6D
					);
				}
			}
		}
	}
}
