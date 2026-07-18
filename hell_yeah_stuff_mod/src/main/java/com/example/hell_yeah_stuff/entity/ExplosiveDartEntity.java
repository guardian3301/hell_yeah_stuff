package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
<<<<<<< HEAD
=======
import net.minecraft.world.phys.Vec3;
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
import org.jetbrains.annotations.Nullable;

public class ExplosiveDartEntity extends AbstractArrow {

    /** Мощность взрыва (у ТНТ = 4.0) */
    private static final float EXPLOSION_POWER = 2.5F;
    /** Задержка детонации: 3 секунды = 60 тиков */
    private static final int FUSE_TIME = 60;
    /** Урон при прямом попадании (до взрыва) */
    private static final double IMPACT_DAMAGE = 3.0D;

    /** -1 = не активирован; иначе — оставшиеся тики до взрыва */
    private int fuseTicks = -1;

<<<<<<< HEAD
=======
    /** Цель, в которую впился дротик: до взрыва он следует за ней,
     *  как обычная стрела, торчащая из моба. Не сохраняется в NBT —
     *  после перезагрузки мира дротик просто доработает таймер на месте. */
    @Nullable
    private LivingEntity stuckTarget;

>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
    public ExplosiveDartEntity(EntityType<? extends ExplosiveDartEntity> type, Level level) {
        super(type, level);
    }

    public ExplosiveDartEntity(Level level, LivingEntity shooter, ItemStack pickupStack, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.EXPLOSIVE_DART.get(), shooter, level, pickupStack, firedFromWeapon);
        this.setBaseDamage(IMPACT_DAMAGE);
    }

    public ExplosiveDartEntity(Level level, double x, double y, double z, ItemStack pickupStack) {
        super(ModEntities.EXPLOSIVE_DART.get(), x, y, z, level, pickupStack, null);
        this.setBaseDamage(IMPACT_DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();

<<<<<<< HEAD
        if (this.fuseTicks >= 0 && !this.level().isClientSide) {
=======
        if (this.level().isClientSide) {
            return;
        }

        // Впился в цель: едем вместе с ней (взрыв догонит жертву).
        if (this.stuckTarget != null) {
            if (this.stuckTarget.isAlive() && this.stuckTarget.level() == this.level()) {
                this.setPos(this.stuckTarget.getX(),
                        this.stuckTarget.getY(0.5D),
                        this.stuckTarget.getZ());
                this.setDeltaMovement(Vec3.ZERO);
            } else {
                // Цель умерла/пропала — дротик остаётся тикать на месте.
                this.stuckTarget = null;
                this.noPhysics = false;
            }
        }

        if (this.fuseTicks >= 0) {
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
            if (this.fuseTicks == 0) {
                this.explode();
                return;
            }

            // Нарастающий писк: интервал между сигналами сокращается,
            // тон становится выше по мере приближения взрыва
            int elapsed = FUSE_TIME - this.fuseTicks;
            int interval = this.fuseTicks > 40 ? 12 : (this.fuseTicks > 20 ? 6 : 3);
            if (this.fuseTicks % interval == 0) {
                float progress = elapsed / (float) FUSE_TIME; // 0.0 -> 1.0
                float pitch = 0.8F + 1.2F * progress;          // 0.8 -> 2.0
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL,
                        1.5F, pitch);
            }

            this.fuseTicks--;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.arm();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
<<<<<<< HEAD
        // Не вызываем super, чтобы дротик не исчез после попадания —
        // он должен остаться и взорваться по таймеру
=======
        // Не вызываем super: ванильный путь уничтожил бы дротик после
        // попадания, а он должен ВПИТЬСЯ в цель, как обычная стрела,
        // и взорваться по таймеру, следуя за жертвой.
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
        if (!this.level().isClientSide) {
            Entity target = result.getEntity();
            Entity owner = this.getOwner();
            DamageSource source = this.damageSources().arrow(this, owner != null ? owner : this);
<<<<<<< HEAD
            target.hurt(source, (float) this.getBaseDamage());

            // Дротик застревает: гасим скорость и останавливаемся на месте попадания
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.05D));
=======
            boolean hurt = target.hurt(source, (float) this.getBaseDamage());

            if (hurt && target instanceof LivingEntity living && living.isAlive()) {
                // «Впился»: визуально торчит из цели (счётчик стрел ванили)
                // и с этого момента прилип — следует за целью до взрыва.
                living.setArrowCount(living.getArrowCount() + 1);
                this.stuckTarget = living;
                this.noPhysics = true; // не цепляем повторные хиты/блоки по пути
            }
            this.setDeltaMovement(Vec3.ZERO);
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
            this.arm();
        }
    }

    /** Активирует 3-секундный таймер детонации */
    private void arm() {
        if (!this.level().isClientSide && this.fuseTicks < 0) {
            this.fuseTicks = FUSE_TIME;
            this.pickup = Pickup.DISALLOWED; // активированный дротик нельзя подобрать
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.NEUTRAL,
                    1.5F, 0.8F);
        }
    }

    private void explode() {
        this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
        this.discard();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.EXPLOSIVE_DART.get());
    }
}
