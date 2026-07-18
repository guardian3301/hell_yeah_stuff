package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.entity.BulletEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Рендер пули: 3D-кубик 1x1 пиксель (1/16 блока) сплошного серого цвета.
 * Текстура — 1x1 однотонный серый PNG, поэтому UV везде (0,0)-(1,1).
 */
public class BulletRenderer extends EntityRenderer<BulletEntity> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "textures/entity/projectiles/bullet.png");

    /** Полуразмер кубика: 0.5 пикселя = 1/32 блока. */
    private static final float HALF = 1.0F / 32.0F;

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BulletEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Центр кубика на центре хитбокса пули.
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        // 6 граней куба [-HALF..HALF]^3.
        // низ (y-)
        quad(vc, pose, packedLight,
                -HALF, -HALF, -HALF,   HALF, -HALF, -HALF,   HALF, -HALF,  HALF,  -HALF, -HALF,  HALF,  0, -1, 0);
        // верх (y+)
        quad(vc, pose, packedLight,
                -HALF,  HALF,  HALF,   HALF,  HALF,  HALF,   HALF,  HALF, -HALF,  -HALF,  HALF, -HALF,  0, 1, 0);
        // север (z-)
        quad(vc, pose, packedLight,
                 HALF, -HALF, -HALF,  -HALF, -HALF, -HALF,  -HALF,  HALF, -HALF,   HALF,  HALF, -HALF,  0, 0, -1);
        // юг (z+)
        quad(vc, pose, packedLight,
                -HALF, -HALF,  HALF,   HALF, -HALF,  HALF,   HALF,  HALF,  HALF,  -HALF,  HALF,  HALF,  0, 0, 1);
        // запад (x-)
        quad(vc, pose, packedLight,
                -HALF, -HALF, -HALF,  -HALF, -HALF,  HALF,  -HALF,  HALF,  HALF,  -HALF,  HALF, -HALF,  -1, 0, 0);
        // восток (x+)
        quad(vc, pose, packedLight,
                 HALF, -HALF,  HALF,   HALF, -HALF, -HALF,   HALF,  HALF, -HALF,   HALF,  HALF,  HALF,  1, 0, 0);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float nx, float ny, float nz) {
        vertex(vc, pose, x1, y1, z1, 0.0F, 1.0F, light, nx, ny, nz);
        vertex(vc, pose, x2, y2, z2, 1.0F, 1.0F, light, nx, ny, nz);
        vertex(vc, pose, x3, y3, z3, 1.0F, 0.0F, light, nx, ny, nz);
        vertex(vc, pose, x4, y4, z4, 0.0F, 0.0F, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
