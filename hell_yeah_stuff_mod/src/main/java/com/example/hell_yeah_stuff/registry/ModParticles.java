package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Кастомные частицы мода. */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, HellYeahStuffMod.MODID);

    /**
     * «Взмах» аметистовой сабли — аналог ванильного sweep_attack,
     * но со своей текстурой (фиолетовая дуга, кадры slash_0..slash_3).
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLASH =
            PARTICLE_TYPES.register("slash", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
