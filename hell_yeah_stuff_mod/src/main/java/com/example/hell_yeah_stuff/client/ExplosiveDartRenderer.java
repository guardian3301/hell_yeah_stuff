package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.entity.ExplosiveDartEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Рендер взрывного дротика. */
public class ExplosiveDartRenderer extends ArrowRenderer<ExplosiveDartEntity> {

    // ponytail: текстуры explosive_dart.png нет в ресурсах — временно ванильная спектральная стрела,
    // заменить на textures/entity/projectiles/explosive_dart.png когда появится своя текстура
    public static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/spectral_arrow.png");

    public ExplosiveDartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ExplosiveDartEntity entity) {
        return TEXTURE;
    }
}
