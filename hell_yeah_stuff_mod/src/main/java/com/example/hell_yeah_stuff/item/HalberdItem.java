package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.event.JudgmentCutHandler;
import com.example.hell_yeah_stuff.registry.ModEnchantments;
import com.example.hell_yeah_stuff.registry.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.jetbrains.annotations.Nullable;

/**
 * Алебарда — древковое оружие ближнего боя с режущим (slash) уроном.
 *
 * ЛКМ: удар по цели рассекает и всех врагов рядом; взмах рисуется частицей
 * {@code hell_yeah_stuff:slash} (текстура 16x16).
 *
 * ПКМ: дальний рубящий удар по области — центр в {@value #SPLASH_REACH}
 * блоках по взгляду. Первый разрез рисуется частицей
 * {@code hell_yeah_stuff:slash_cut}. С зачарованием «Judgment Cut»
 * ({@link ModEnchantments#JUDGMENT_CUT}) через секунду в той же точке
 * срабатывает ВТОРОЙ, перпендикулярный разрез (частица
 * {@code hell_yeah_stuff:slash_judgment}). Перемещения (рывка) нет.
 */
public class HalberdItem extends SwordItem {

    /** Радиус рассечения вокруг основной цели (ЛКМ). */
    private static final double SLASH_RANGE = 1.7D;
    /** Slash-урон по каждой задетой цели. */
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
    /** Задержка второго разреза Judgment Cut (тиков) — 1 секунда. */
    private static final int SECOND_CUT_DELAY = 20;

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

    /** Фиолетовый «взмах» ЛКМ (в т.ч. по воздуху). Анимацию руки не отменяем. */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && entity.level() instanceof ServerLevel server) {
            Vec3 look = entity.getLookAngle();
            spawnSlash(server, ModParticles.SLASH.get(),
                    entity.getX() + look.x * 1.4D, entity.getY(0.55D), entity.getZ() + look.z * 1.4D);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // ПКМ: дальний рубящий удар (+ отложенный второй разрез Judgment Cut)
    // ------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, SPLASH_COOLDOWN);

        if (level instanceof ServerLevel server) {
            Vec3 look = player.getLookAngle();
            Vec3 center = player.getEyePosition().add(look.scale(SPLASH_REACH));
            DamageSource source = damageSource(server, player);

            int hits = areaSlash(server, player, source, center);
            // Первый разрез — горизонтальные взмахи (текстура ПКМ slash_cut).
            Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize().scale(SPLASH_RANGE * 0.55D);
            spawnSlash(server, ModParticles.SLASH_CUT.get(), center.x, center.y, center.z);
            spawnSlash(server, ModParticles.SLASH_CUT.get(), center.x + side.x, center.y, center.z + side.z);
            spawnSlash(server, ModParticles.SLASH_CUT.get(), center.x - side.x, center.y, center.z - side.z);

            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(),
                    1.0F, hits > 0 ? 0.9F : 1.1F);
            stack.hurtAndBreak(SPLASH_DURABILITY_COST, player, LivingEntity.getSlotForHand(hand));

            // Judgment Cut: через секунду — второй, перпендикулярный разрез в той же точке.
            if (ModEnchantments.level(stack, ModEnchantments.JUDGMENT_CUT) > 0) {
                JudgmentCutHandler.schedule(server, player, center, SECOND_CUT_DELAY);
            }
        }
        // SUCCESS на клиенте -> обычный мах рукой.
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Второй разрез Judgment Cut — вызывается {@link JudgmentCutHandler} через
     * {@value #SECOND_CUT_DELAY} тиков после первого. Тот же урон по кругу и
     * вертикальная (перпендикулярная) полоса взмахов текстурой slash_judgment.
     */
    public static void performSecondCut(ServerLevel server, @Nullable Player attacker, Vec3 center) {
        DamageSource source = attacker != null
                ? server.damageSources().playerAttack(attacker)
                : server.damageSources().magic();
        areaSlash(server, attacker, source, center);
        spawnSlash(server, ModParticles.SLASH_JUDGMENT.get(), center.x, center.y + SPLASH_HEIGHT * 0.6D, center.z);
        spawnSlash(server, ModParticles.SLASH_JUDGMENT.get(), center.x, center.y, center.z);
        spawnSlash(server, ModParticles.SLASH_JUDGMENT.get(), center.x, center.y - SPLASH_HEIGHT * 0.6D, center.z);
        server.playSound(null, center.x, center.y, center.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    // ------------------------------------------------------------------
    // Общее
    // ------------------------------------------------------------------

    /** Один рубящий проход по кругу вокруг {@code center}; возвращает число задетых. */
    private static int areaSlash(ServerLevel server, @Nullable Player player, DamageSource source, Vec3 center) {
        AABB area = AABB.ofSize(center, SPLASH_RANGE * 2.0D, SPLASH_HEIGHT * 2.0D, SPLASH_RANGE * 2.0D);
        int hits = 0;
        for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class, area)) {
            if (player != null && (other == player || !canSlash(player, other))) {
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

    private static void spawnSlash(ServerLevel server, SimpleParticleType type, double x, double y, double z) {
        server.sendParticles(type, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
