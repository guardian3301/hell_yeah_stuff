package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.registry.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Рывок в стиле Content SMP: клавиша {@link ModKeyMappings#DASH_KEY} даёт
 * быстрый горизонтальный рывок, если на игроке поножи с зачарованием
 * «Рывок» ({@link ModEnchantments#DASH}). Направление — по вводу движения
 * (WASD), при его отсутствии — по горизонтали взгляда. С откатом.
 *
 * Импульс задаётся на клиенте владельца: движение локального игрока
 * клиент-авторитативно, поэтому setDeltaMovement здесь и синхронизируется
 * на сервер (тот же приём, что release-dash у крюка-кошки).
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT)
public final class DashHandler {

    /** Откат рывка (тиков). */
    private static final int COOLDOWN_TICKS = 40;
    /** Сила горизонтального рывка (блоков/тик). */
    private static final double DASH_STRENGTH = 1.1D;
    /** Подброс вверх при рывке с земли (чтобы трение сразу не съело импульс). */
    private static final double DASH_UP = 0.35D;

    private static int cooldown = 0;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }

        boolean pressed = false;
        while (ModKeyMappings.DASH_KEY.consumeClick()) {
            pressed = true;
        }
        if (!pressed || mc.screen != null || cooldown > 0) {
            return;
        }

        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (ModEnchantments.level(legs, ModEnchantments.DASH) <= 0) {
            return;
        }

        if (dash(player)) {
            cooldown = COOLDOWN_TICKS;
        }
    }

    private static boolean dash(LocalPlayer player) {
        // Базис по рысканью: forward = (-sin, 0, cos), left = (cos... ) — используем
        // horizontalной проекции взгляда и левый вектор для стрейфа.
        float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 left = new Vec3(forward.z, 0.0D, -forward.x);

        // xxa: стрейф (лево +), zza: вперёд/назад (вперёд +).
        Vec3 dir = forward.scale(player.zza).add(left.scale(player.xxa));
        if (dir.lengthSqr() < 1.0E-4D) {
            dir = forward; // ввода нет — рывок по направлению взгляда
        }
        dir = dir.normalize().scale(DASH_STRENGTH);

        Vec3 vel = player.getDeltaMovement();
        double y = player.onGround() ? DASH_UP : Math.max(vel.y, -0.1D);
        player.setDeltaMovement(dir.x, y, dir.z);
        player.fallDistance = 0.0F;

        player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 0.6F, 1.6F);
        for (int i = 0; i < 8; i++) {
            player.level().addParticle(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.1D, player.getZ(),
                    (player.getRandom().nextDouble() - 0.5D) * 0.4D, 0.02D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.4D);
        }
        return true;
    }

    private DashHandler() {}
}
