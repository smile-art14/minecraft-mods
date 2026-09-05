package com.example.client;

import com.example.ExampleMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ExcaliburChargeHud.initialize();
		ExcaliburChargeEffects.initialize();
		EntityRendererRegistry.register(
				ExampleMod.GOLDEN_SWORD_WAVE_ENTITY,
				GoldenSwordWaveRenderer::new
		);
		ParticleFactoryRegistry.getInstance().register(
				ExampleMod.GOLDEN_SWORD_WAVE_PARTICLE,
				GoldenSwordWaveParticle.Provider::new
		);
		ParticleFactoryRegistry.getInstance().register(
				ExampleMod.GOLDEN_SWORD_TRAIL_PARTICLE,
				GoldenSwordTrailParticle.Provider::new
		);
		ParticleFactoryRegistry.getInstance().register(
				ExampleMod.EXCALIBUR_CHARGE_PARTICLE,
				ExcaliburChargeParticle.Provider::new
		);
		ItemProperties.register(
				ExampleMod.EXCALIBUR,
				ExampleMod.id("charge"),
				(stack, level, entity, seed) -> entity != null
						&& entity.isUsingItem()
						&& entity.getUseItem().is(ExampleMod.EXCALIBUR)
						? com.example.item.ExcaliburItem.getChargeProgress(entity.getTicksUsingItem())
						: 0.0F
		);
		registerSwallowSwordModelPredicate();
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
				(entityType, entityRenderer, registrationHelper, context) ->
						registerFrozenTint(registrationHelper, entityRenderer)
		);
	}

	private static void registerSwallowSwordModelPredicate() {
		Item[] swords = {
				Items.WOODEN_SWORD,
				Items.STONE_SWORD,
				Items.IRON_SWORD,
				Items.GOLDEN_SWORD,
				Items.DIAMOND_SWORD,
				Items.NETHERITE_SWORD
		};
		for (Item sword : swords) {
			ItemProperties.register(
					sword,
					ExampleMod.id("swallow_stage"),
					(stack, level, entity, seed) -> ExampleMod.isSwallowSword(stack)
							? (float) ExampleMod.getSwallowStage(stack) / (float) ExampleMod.SWALLOW_STAGE_COUNT
							: 0.0F
			);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerFrozenTint(
			LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper registrationHelper,
			LivingEntityRenderer<?, ?> entityRenderer
	) {
		registrationHelper.register(new FrozenTintRenderLayer((LivingEntityRenderer) entityRenderer));
	}
}
