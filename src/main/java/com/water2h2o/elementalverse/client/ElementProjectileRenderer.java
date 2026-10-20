package com.water2h2o.elementalverse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.water2h2o.elementalverse.entity.SpellProjectile;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 弹射物渲染：直接画**元素本体**，不画瓶子。
 *
 * <p>贴图放在 {@code assets/elemental_verse/textures/entity/element/<元素id>.png}，
 * 由 2026-10-21 那批"去掉瓶子的元素"提供。找不到就退回水元素。
 */
public class ElementProjectileRenderer extends EntityRenderer<SpellProjectile> {

    private static final ResourceLocation FALLBACK = texture("water");

    /** 画多大：略小于一格，看起来像"一团元素"而不是方块。 */
    private static final float SIZE = 0.55F;

    public ElementProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    private static ResourceLocation texture(String element) {
        return ResourceLocation.fromNamespaceAndPath("elemental_verse",
                "textures/entity/element/" + element + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(SpellProjectile entity) {
        String id = entity.elementId();
        return id == null || id.isEmpty() ? FALLBACK : texture(id);
    }

    @Override
    public void render(SpellProjectile entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        // 永远面向镜头
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.scale(SIZE, SIZE, SIZE);

        // 飞行时慢慢转，别让它是块死图
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(
                (entity.tickCount + partialTick) * 9.0F));

        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));
        PoseStack.Pose last = pose.last();
        float h = 0.5F;
        vertex(vc, last, -h, -h, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(vc, last, h, -h, 0.0F, 1.0F, 1.0F, packedLight);
        vertex(vc, last, h, h, 0.0F, 1.0F, 0.0F, packedLight);
        vertex(vc, last, -h, h, 0.0F, 0.0F, 0.0F, packedLight);

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y,
                               float z, float u, float v, int light) {
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
