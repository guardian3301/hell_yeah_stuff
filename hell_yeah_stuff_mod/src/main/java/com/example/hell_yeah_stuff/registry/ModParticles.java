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
     * «Взмах» алебарды по ЛКМ — своя текстура (particle/slash, 16x16).
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLASH =
            PARTICLE_TYPES.register("slash", () -> new SimpleParticleType(false));

    /** Дальний рубящий удар по ПКМ — своя текстура (particle/slash_cut). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLASH_CUT =
            PARTICLE_TYPES.register("slash_cut", () -> new SimpleParticleType(false));

    /** Второй (перпендикулярный) удар «Judgment Cut» — своя текстура
     *  (particle/slash_judgment). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLASH_JUDGMENT =
            PARTICLE_TYPES.register("slash_judgment", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
