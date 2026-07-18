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
 * Частица взмаха алебарды: собственные кадры анимации в отдельных файлах
 * (hell_yeah_stuff:slash_0..3, текстуры 32x32, см. particles/slash.json и
 * textures/particle/slash_*.png), с фиолетовым (аметистовым) тинтом.
 * Поведение повторяет ванильный AttackSweepParticle.
 */
@OnlyIn(Dist.CLIENT)
public class SlashParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected SlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.lifetime = 4; // как у ванильного sweep_attack
        this.gravity = 0.0F;
        this.quadSize = 1.0F;
        // Фиолетовый тинт поверх белых ванильных кадров:
        // лёгкая случайная вариация яркости, как у ванили (там серый f=0.4..1.0).
        float f = this.random.nextFloat() * 0.2F + 0.8F;
        this.rCol = 0.72F * f;
        this.gCol = 0.42F * f;
        this.bCol = 0.96F * f;
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

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SlashParticle(level, x, y, z, this.sprites);
        }
    }
}
