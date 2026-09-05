package com.example.client;

import com.example.ExampleMod;
import com.example.entity.GoldenSwordWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws each sword wave as one emissive textured quad instead of assembling
 * its silhouette from many billboard particles.
 */
public final class GoldenSwordWaveRenderer
		extends EntityRenderer<GoldenSwordWaveEntity> {
	private static final ResourceLocation TEXTURE =
			ExampleMod.id("textures/entity/golden_sword_wave.png");

	public GoldenSwordWaveRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.0F;
	}

	@Override
	public void render(
			GoldenSwordWaveEntity entity,
			float entityYaw,
			float partialTick,
			PoseStack poseStack,
			MultiBufferSource buffers,
			int packedLight
	) {
		float halfWidth = entity.getHalfWidth();
		poseStack.pushPose();
		Vec3 direction = entity.getTravelDirection();
		// The slash plane contains the travel vector. Therefore a player looking
		// along that vector sees only its thin edge, not the complete texture.
		// The second in-plane axis is the horizontal side vector, matching the
		// server-side crescent collision plane.
		Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (side.lengthSqr() < 0.0001D) {
			side = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			side = side.normalize();
		}
		Vec3 normalVector = direction.cross(side).normalize();
		double halfSide = halfWidth * 0.725D;
		double halfThickness = Math.max(
				0.08D,
				Math.min(0.32D, halfWidth * 0.045D)
		);
		Vec3 rear = direction.scale(-halfWidth);
		Vec3 front = direction.scale(halfWidth);
		Vec3 left = side.scale(-halfSide);
		Vec3 right = side.scale(halfSide);

		PoseStack.Pose pose = poseStack.last();
		Matrix4f matrix = pose.pose();
		Matrix3f normal = pose.normal();
		VertexConsumer vertices = buffers.getBuffer(
				RenderType.entityTranslucentEmissive(TEXTURE)
		);
		vertex(vertices, matrix, normal, rear.add(left), normalVector,
				0.0F, 1.0F);
		vertex(vertices, matrix, normal, front.add(left), normalVector,
				1.0F, 1.0F);
		vertex(vertices, matrix, normal, front.add(right), normalVector,
				1.0F, 0.0F);
		vertex(vertices, matrix, normal, rear.add(right), normalVector,
				0.0F, 0.0F);

		renderAttachedCuttingEdge(
				vertices,
				matrix,
				normal,
				direction,
				side,
				normalVector,
				halfWidth,
				halfSide,
				halfThickness
		);
		poseStack.popPose();
		super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
	}

	private static void renderAttachedCuttingEdge(
			VertexConsumer vertices,
			Matrix4f matrix,
			Matrix3f normal,
			Vec3 direction,
			Vec3 side,
			Vec3 normalVector,
			double halfWidth,
			double halfSide,
			double halfThickness
	) {
		// Follow the exact front curve used to author golden_sword_wave.png:
		// x = 126 + 108 * sqrt(1 - t^2), y = 96 + 72 * t.
		// Converting those texture coordinates into the quad's world axes keeps
		// the white-gold thickness strip physically attached to the crescent.
		final int segments = 24;
		Vec3 upperOffset = normalVector.scale(halfThickness);
		Vec3 lowerOffset = normalVector.scale(-halfThickness);
		for (int segment = 0; segment < segments; segment++) {
			double t0 = -1.0D + 2.0D * segment / segments;
			double t1 = -1.0D + 2.0D * (segment + 1) / segments;
			float u0 = cuttingEdgeU(t0);
			float u1 = cuttingEdgeU(t1);
			float v0 = (float) (0.5D + t0 * 0.375D);
			float v1 = (float) (0.5D + t1 * 0.375D);
			Vec3 edge0 = cuttingEdgePosition(
					direction, side, halfWidth, halfSide, t0, u0
			);
			Vec3 edge1 = cuttingEdgePosition(
					direction, side, halfWidth, halfSide, t1, u1
			);
			Vec3 lower0 = edge0.add(lowerOffset);
			Vec3 upper0 = edge0.add(upperOffset);
			Vec3 upper1 = edge1.add(upperOffset);
			Vec3 lower1 = edge1.add(lowerOffset);
			float innerU0 = Math.max(0.0F, u0 - 0.045F);
			float innerU1 = Math.max(0.0F, u1 - 0.045F);

			vertex(vertices, matrix, normal, lower0, direction,
					innerU0, v0);
			vertex(vertices, matrix, normal, upper0, direction,
					u0, v0);
			vertex(vertices, matrix, normal, upper1, direction,
					u1, v1);
			vertex(vertices, matrix, normal, lower1, direction,
					innerU1, v1);

			// Reverse winding keeps the attached edge visible from either end.
			Vec3 reverseNormal = direction.scale(-1.0D);
			vertex(vertices, matrix, normal, lower1, reverseNormal,
					innerU1, v1);
			vertex(vertices, matrix, normal, upper1, reverseNormal,
					u1, v1);
			vertex(vertices, matrix, normal, upper0, reverseNormal,
					u0, v0);
			vertex(vertices, matrix, normal, lower0, reverseNormal,
					innerU0, v0);
		}
	}

	private static float cuttingEdgeU(double t) {
		double textureX = 126.0D
				+ 108.0D * Math.sqrt(Math.max(0.0D, 1.0D - t * t));
		return (float) (textureX / 256.0D);
	}

	private static Vec3 cuttingEdgePosition(
			Vec3 direction,
			Vec3 side,
			double halfWidth,
			double halfSide,
			double t,
			float u
	) {
		double travelOffset = (u * 2.0D - 1.0D) * halfWidth;
		double sideOffset = -t * halfSide * 0.75D;
		return direction.scale(travelOffset).add(side.scale(sideOffset));
	}

	private static void vertex(
			VertexConsumer vertices,
			Matrix4f matrix,
			Matrix3f normal,
			Vec3 position,
			Vec3 normalVector,
			float u,
			float v
	) {
		vertices.vertex(
						matrix,
						(float) position.x,
						(float) position.y,
						(float) position.z
				)
				.color(255, 255, 255, 255)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(0xF000F0)
				.normal(
						normal,
						(float) normalVector.x,
						(float) normalVector.y,
						(float) normalVector.z
				)
				.endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(GoldenSwordWaveEntity entity) {
		return TEXTURE;
	}
}
