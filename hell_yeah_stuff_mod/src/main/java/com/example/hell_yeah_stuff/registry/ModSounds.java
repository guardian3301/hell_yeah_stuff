package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Кастомные звуки мода (см. assets/hell_yeah_stuff/sounds.json).
 * Варианты зарегистрированы ОТДЕЛЬНЫМИ событиями, а ротация
 * делается в коде через random*() — гарантированно случайный
 * выбор варианта на каждое воспроизведение.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, HellYeahStuffMod.MODID);

    /** Выстрел Contender .44 — 2 варианта. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTENDER_SHOT_0 =
            register("contender_shot_0");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONTENDER_SHOT_1 =
            register("contender_shot_1");

    /** Вставка патрона при перезарядке — 2 варианта. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_INSERT_0 =
            register("bullet_insert_0");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_INSERT_1 =
            register("bullet_insert_1");

    /** Выпадение стреляной гильзы при переломе ствола — 3 варианта. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CASING_BOUNCE_0 =
            register("casing_bounce_0");
    public static final DeferredHolder<SoundEvent, SoundEvent> CASING_BOUNCE_1 =
            register("casing_bounce_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> CASING_BOUNCE_2 =
            register("casing_bounce_2");

    private static final List<DeferredHolder<SoundEvent, SoundEvent>> SHOTS =
            List.of(CONTENDER_SHOT_0, CONTENDER_SHOT_1);
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> INSERTS =
            List.of(BULLET_INSERT_0, BULLET_INSERT_1);
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> CASINGS =
            List.of(CASING_BOUNCE_0, CASING_BOUNCE_1, CASING_BOUNCE_2);

    /** Случайный вариант звука выстрела. */
    public static SoundEvent randomShot(RandomSource random) {
        return SHOTS.get(random.nextInt(SHOTS.size())).get();
    }

    /** Случайный вариант звука вставки патрона. */
    public static SoundEvent randomInsert(RandomSource random) {
        return INSERTS.get(random.nextInt(INSERTS.size())).get();
    }

    /** Случайный вариант звука отскока гильзы. */
    public static SoundEvent randomCasing(RandomSource random) {
        return CASINGS.get(random.nextInt(CASINGS.size())).get();
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, name)));
    }

    private ModSounds() {}
}
