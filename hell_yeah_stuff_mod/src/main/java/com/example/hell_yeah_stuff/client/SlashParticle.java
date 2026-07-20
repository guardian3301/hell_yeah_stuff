package com.example.hell_yeah_stuff.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Частица «взмаха» алебарды — ОДИН большой анимированный слэш, кадры которого
 * берутся из sprite set (particle/slash_0..slash_3). Один и тот же класс
 * обслуживает три варианта атаки, различающиеся только размером, наклоном и
 * оттенком:
 *   • ЛКМ — обычный диагональный взмах;
 *   • ПКМ — большой горизонтальный разрез;
 *   • «Judgment Cut» — большой вертикальный (перпендикулярный) разрез с лёгким
 *     фиолетовым оттенком.
 * Никаких «трёх маленьких» частиц: на каждую атаку рождается ровно одна,
 * а движение кадров даёт анимацию.
 */
@OnlyIn(Dist.CLIENT)
public class SlashParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected SlashParticle(ClientLevel level, double x, double y, double z,
                            SpriteSet sprites, float size, float rollRad,
                            float red, float green, float blue) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.lifetime = 8;          // 4 кадра примерно по 2 тика — плавная анимация
        this.gravity = 0.0F;
        this.quadSize = size;       // размер «большого» слэша
        this.roll = rollRad;        // наклон плоскости слэша (диагональ/горизонт/вертикаль)
        this.oRoll = rollRad;
        this.rCol = red;
        this.gCol = green;
        this.bCol = blue;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // fullbright, как у ванильного sweep_attack
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            // Проигрываем кадры slash_0..slash_3 по мере старения частицы.
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT; // как у ванили
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;
        private final float rollRad;
        private final float red;
        private final float green;
        private final float blue;

        /**
         * @param size    размер слэша (quadSize)
         * @param rollDeg наклон плоскости слэша в градусах
         * @param red/green/blue оттенок (1,1,1 — без тинта)
         */
        public Provider(SpriteSet sprites, float size, float rollDeg,
                        float red, float green, float blue) {
            this.sprites = sprites;
            this.size = size;
            this.rollRad = rollDeg * ((float) Math.PI / 180.0F);
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SlashParticle(level, x, y, z, this.sprites,
                    this.size, this.rollRad, this.red, this.green, this.blue);
        }
    }
}
