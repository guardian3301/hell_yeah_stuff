package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.registry.ModEnchantments;
import com.example.hell_yeah_stuff.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Алебарда — древковое оружие ближнего боя с режущим (slash) уроном.
 *
 * ЛКМ: каждый удар по цели дополнительно рассекает всех врагов рядом с ней
 * (всегда-активный «размах»). Взмах рисуется фиолетовой частицей
 * {@code hell_yeah_stuff:slash}.
 *
 * ПКМ: дальний рубящий удар по области — центр в {@value #SPLASH_REACH}
 * блоках по взгляду, радиус {@value #SPLASH_RANGE}. С откатом, тратит
 * прочность. С зачарованием «Режущий рывок» ({@link ModEnchantments#CUTTING_DASH})
 * добавляется рывок вперёд и второй удар, перпендикулярный первому (крест).
 */
public class HalberdItem extends SwordItem {

    /** Радиус рассечения вокруг основной цели (обычный ЛКМ-размах). */
    private static final double SLASH_RANGE = 1.7D;
    /** Slash-урон по каждой задетой цели рядом. */
    private static final float SLASH_DAMAGE = 4.0F;
    /** Дальше этого от атакующего ЛКМ-размах не достаёт. */
    private static final double MAX_REACH_SQR = 12.25D; // 3.5 блока

    /** ПКМ-удар: дальность центра. */
    private static final double SPLASH_REACH = 4.5D;
    /** ПКМ-удар: радиус области. */
    private static final double SPLASH_RANGE = SLASH_RANGE * 2.0D;
    /** Полувысота области удара. */
    private static final double SPLASH_HEIGHT = 1.5D;
    /** Откат ПКМ-удара (тиков). */
    private static final int SPLASH_COOLDOWN = 30;
    /** Прочность за ПКМ-удар. */
    private static final int SPLASH_DURABILITY_COST = 2;

    /** Сила рывка вперёд у «Режущего рывка» (блоков/тик). */
    private static final double DASH_STRENGTH = 0.9D;
    /** Минимальный подброс рывка вверх. */
    private static final double DASH_UP = 0.15D;

    public HalberdItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    // ------------------------------------------------------------------
    // ЛКМ: рассечение вокруг цели
    // ------------------------------------------------------------------

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel server) {
            DamageSource source = damageSource(server, attacker);
            for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(SLASH_RANGE, 0.5D, SLASH_RANGE))) {
                if (other == attacker || other == target || !canSlash(attacker, other)) {
                    continue;
                }
                if (attacker.distanceToSqr(other) > MAX_REACH_SQR) {
                    continue;
                }
                if (other.hurt(source, SLASH_DAMAGE)) {
                    other.knockback(0.4D,
                            attacker.getX() - other.getX(),
                            attacker.getZ() - other.getZ());
                }
            }
            server.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, attacker.getSoundSource(), 1.0F, 1.3F);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * Фиолетовый «взмах» на КАЖДЫЙ мах алебардой (в том числе по воздуху).
     * Возвращаем false — саму анимацию руки не отменяем.
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && entity.level() instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            spawnSlash(server, entity.getX() + look.x * 1.4D, entity.getY(0.55D),
                    entity.getZ() + look.z * 1.4D, look);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // ПКМ: дальний рубящий удар по области (+ «Режущий рывок»)
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, SPLASH_COOLDOWN);

        boolean cuttingDash = ModEnchantments.level(stack, ModEnchantments.CUTTING_DASH) > 0;

        if (level instanceof ServerLevel server) {
            Vec3 look = player.getLookAngle();
            Vec3 center = player.getEyePosition().add(look.scale(SPLASH_REACH));
            DamageSource source = damageSource(server, player);

            // Первый (горизонтальный) удар.
            int hits = areaSlash(server, player, source, center);
            Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize().scale(SPLASH_RANGE * 0.55D);
            spawnSlash(server, center.x, center.y, center.z, look);
            spawnSlash(server, center.x + side.x, center.y, center.z + side.z, look);
            spawnSlash(server, center.x - side.x, center.y, center.z - side.z, look);

            // «Режущий рывок»: второй удар, перпендикулярный первому (крест) —
            // тот же урон ещё раз + вертикальная полоса взмахов.
            if (cuttingDash) {
                hits += areaSlash(server, player, source, center);
                spawnSlash(server, center.x, center.y + SPLASH_HEIGHT * 0.6D, center.z, look);
                spawnSlash(server, center.x, center.y, center.z, look);
                spawnSlash(server, center.x, center.y - SPLASH_HEIGHT * 0.6D, center.z, look);
            }

            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(),
                    1.0F, hits > 0 ? 0.9F : 1.1F);
            stack.hurtAndBreak(SPLASH_DURABILITY_COST, player, LivingEntity.getSlotForHand(hand));
        } else if (cuttingDash) {
            // Рывок вперёд — на клиенте владельца (движение клиент-авторитативно,
            // как release-dash крюка-кошки), иначе сервер затрёт импульс.
            Vec3 look = player.getLookAngle();
            Vec3 dash = new Vec3(look.x, Math.max(look.y, DASH_UP), look.z)
                    .normalize().scale(DASH_STRENGTH);
            player.setDeltaMovement(player.getDeltaMovement().add(dash));
            player.fallDistance = 0.0F;
        }
        // SUCCESS на клиенте -> обычный мах рукой (и фиолетовый след от onEntitySwing).
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ------------------------------------------------------------------
    // Общее
    // ------------------------------------------------------------------

    /** Один рубящий проход по кругу вокруг {@code center}; возвращает число задетых. */
    private static int areaSlash(ServerLevel server, Player player, DamageSource source, Vec3 center) {
        AABB area = AABB.ofSize(center, SPLASH_RANGE * 2.0D, SPLASH_HEIGHT * 2.0D, SPLASH_RANGE * 2.0D);
        int hits = 0;
        for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class, area)) {
            if (other == player || !canSlash(player, other)) {
                continue;
            }
            // Область — круг, а не квадрат: отсечь углы AABB.
            if (other.position().subtract(center).horizontalDistanceSqr() > SPLASH_RANGE * SPLASH_RANGE) {
                continue;
            }
            if (other.hurt(source, SLASH_DAMAGE)) {
                other.knockback(0.5D, center.x - other.getX(), center.z - other.getZ());
                hits++;
            }
        }
        return hits;
    }

    private static DamageSource damageSource(ServerLevel server, LivingEntity attacker) {
        return attacker instanceof Player player
                ? server.damageSources().playerAttack(player)
                : server.damageSources().mobAttack(attacker);
    }

    /** Можно ли рассечь цель: не союзник и не мирный PvP-игрок. */
    private static boolean canSlash(LivingEntity attacker, LivingEntity target) {
        if (target.isAlliedTo(attacker)) {
            return false;
        }
        return !(target instanceof Player tp && attacker instanceof Player ap && !ap.canHarmPlayer(tp));
    }

    private static void spawnSlash(ServerLevel server, double x, double y, double z, Vec3 look) {
        // count = 0: параметры скорости задают направление единственной частицы.
        server.sendParticles(ModParticles.SLASH.get(), x, y, z, 0, look.x, 0.0D, look.z, 0.0D);
    }
}
