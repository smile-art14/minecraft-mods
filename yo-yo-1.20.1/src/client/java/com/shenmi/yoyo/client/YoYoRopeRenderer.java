package com.shenmi.yoyo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

final class YoYoRopeRenderer {
    private static final int SEGMENTS = 24;

    private YoYoRopeRenderer() {
    }

    static void renderRope(
            PoseStack poseStack,
            MultiBufferSource buffers,
            Vec3 from,
            Vec3 to,
            float sagScale
    ) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.lineStrip());
        PoseStack.Pose pose = poseStack.last();
        double distance = from.distanceTo(to);
        double sag = Math.min(0.30D, 0.04D + distance * 0.014D) * sagScale;

        Vec3 previous = point(from, to, 0.0D, sag);
        for (int segment = 0; segment <= SEGMENTS; segment++) {
            double t = segment / (double) SEGMENTS;
            Vec3 current = point(from, to, t, sag);
            Vec3 next = point(from, to, Math.min(1.0D, t + 1.0D / SEGMENTS), sag);
            Vec3 tangent = next.subtract(previous);
            if (tangent.lengthSqr() < 0.000001D) {
                tangent = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                tangent = tangent.normalize();
            }

            vertices.vertex(
                            pose.pose(),
                            (float) current.x,
                            (float) current.y,
                            (float) current.z
                    )
                    .color(76, 49, 28, 255)
                    .normal(
                            pose.normal(),
                            (float) tangent.x,
                            (float) tangent.y,
                            (float) tangent.z
                    )
                    .endVertex();
            previous = current;
        }
    }

    static void renderLoop(
            PoseStack poseStack,
            MultiBufferSource buffers,
            double radiusX,
            double radiusZ
    ) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.lineStrip());
        PoseStack.Pose pose = poseStack.last();
        final int loopSegments = 28;

        for (int segment = 0; segment <= loopSegments; segment++) {
            double angle = Math.PI * 2.0D * segment / loopSegments;
            float x = (float) (Math.cos(angle) * radiusX);
            float z = (float) (Math.sin(angle) * radiusZ);
            float nx = (float) Math.cos(angle);
            float nz = (float) Math.sin(angle);

            vertices.vertex(pose.pose(), x, 0.0F, z)
                    .color(76, 49, 28, 255)
                    .normal(pose.normal(), nx, 0.0F, nz)
                    .endVertex();
        }
    }

    private static Vec3 point(Vec3 from, Vec3 to, double t, double sag) {
        Vec3 linear = from.lerp(to, t);
        double curve = Math.sin(Math.PI * t) * sag;
        return linear.add(0.0D, -curve, 0.0D);
    }
}
