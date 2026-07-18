package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Пуля однозарядного пистолета (Contender .44).
 * Быстрая, урон выше дротика, НЕ подбирается.
 *
 * В отличие от стрелы НЕ застревает в блоках: при ударе о блок
 * ломается (осколки + рикошет-звук) и исчезает без дропа.
 * По мобам наносит урон и тоже исчезает.
 */
public class BulletEntity extends AbstractArrow {

    /** Максимальное время жизни пули (тиков) — дальше просто исчезает. */
    private static final int MAX_LIFETIME = 60;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.BULLET.get(), shooter, level,
                new ItemStack(ModItems.CARTRIDGE.get()), null);
        this.setBaseDamage(10.0D);
        this.pickup = Pickup.DISALLOWED;
        // Убираем «стрельный» звук попадания (arrow hit) — пуля не стрела.
        this.setSoundEvent(SoundEvents.EMPTY);
    }

    /** Звук попадания по умолчанию — тишина (вместо звука стрелы). */
    @Override
    protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.EMPTY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_LIFETIME) {
            this.discard();
        }
    }

    /** Удар о блок: пуля ломается — осколки, звук, никакого дропа. */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = result.getLocation();

            // Осколки патрона + дымок в точке попадания.
            serverLevel.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.CARTRIDGE.get())),
                    pos.x, pos.y, pos.z,
                    8, 0.08D, 0.08D, 0.08D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.x, pos.y, pos.z,
                    4, 0.05D, 0.05D, 0.05D, 0.01D);

            serverLevel.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.NEUTRAL, 0.8F, 1.6F);

            this.discard();
        }
    }

    /** Попадание по сущности: урон наносится (super), пуля исчезает. */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.CARTRIDGE.get());
    }
}
