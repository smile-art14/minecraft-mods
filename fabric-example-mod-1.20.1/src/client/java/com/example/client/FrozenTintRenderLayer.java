package com.example.client;

import com.example.ExampleMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class FrozenTintRenderLayer<T extends LivingEntity, M extends EntityModel<T>>
		extends RenderLayer<T, M> {
	private final LivingEntityRenderer<T, M> renderer;

	public FrozenTintRenderLayer(LivingEntityRenderer<T, M> renderer) {
		super(renderer);
		this.renderer = renderer;
	}

	@Override
	public void render(
			PoseStack poseStack,
			MultiBufferSource buffers,
			int packedLight,
			T entity,
			float limbSwing,
			float limbSwingAmount,
			float partialTick,
			float ageInTicks,
			float netHeadYaw,
			float headPitch
	) {
		// Mob effects and the vanilla frozen tick counter are both synchronized to
		// clients. Accepting either signal keeps the visual reliable even when a
		// client receives one of the two updates a tick later than the other.
		boolean frozen = entity.hasEffect(ExampleMod.FROZEN_TINT)
				|| entity.getTicksFrozen() >= entity.getTicksRequiredToFreeze();
		if (!frozen || entity.isInvisible()) {
			return;
		}

		ResourceLocation texture = renderer.getTextureLocation(entity);
		VertexConsumer vertexConsumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
		poseStack.pushPose();
		// Rendering on exactly the same surface as the base model can lose the
		// second pass to the depth buffer. A tiny expansion makes this a visible
		// shell without noticeably changing the mob's size.
		poseStack.scale(1.015F, 1.015F, 1.015F);
		getParentModel().renderToBuffer(
				poseStack,
				vertexConsumer,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				0.28F,
				0.82F,
				1.00F,
				0.62F
		);
		poseStack.popPose();
	}
}
