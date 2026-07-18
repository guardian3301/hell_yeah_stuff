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
 * Частица взмаха алебарды. Одна и та же реализация обслуживает три типа
 * частиц с РАЗНЫМИ текстурами: ЛКМ (particle/slash, 16x16), первый разрез
 * ПКМ (particle/slash_cut) и второй разрез «Judgment Cut» (particle/slash_judgment).
 * Текстура берётся из sprite set соответствующего типа; тинт не применяется.
 */
@OnlyIn(Dist.CLIENT)
public class SlashParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected SlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.lifetime = 6;
        this.gravity = 0.0F;
        this.quadSize = 1.0F;
        // Тинта нет — частица показывает цвета своей текстуры как есть
        // (у ЛКМ / первого и второго разрезов ПКМ разные текстуры).
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
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
