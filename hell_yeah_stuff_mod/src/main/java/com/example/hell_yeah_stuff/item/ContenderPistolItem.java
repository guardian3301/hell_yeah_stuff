package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.client.RecoilSys;
import com.example.hell_yeah_stuff.entity.BulletEntity;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Однозарядный пистолет в духе Thompson/Center Contender .44:
 * большой калибр, сильная отдача, только 1 патрон в стволе.
 *
 * Механика (без анимации — обычная модель предмета):
 *  - ПКМ с пустым стволом и патроном в инвентаре — ЗАРЯЖАЕТ 1 патрон
 *    (хранится в DataComponents.CHARGED_PROJECTILES, как у арбалета).
 *  - ПКМ с заряженным стволом — ВЫСТРЕЛ: пуля летит по взгляду,
 *    игрока отбрасывает назад-вверх (большая отдача), ствол пустеет.
 *  - ПКМ без патронов — сухой щелчок.
 */
public class ContenderPistolItem extends ProjectileWeaponItem {

    /** Начальная скорость пули (блоков/тик). */
    private static final float BULLET_VELOCITY = 4.0F;
    /** Разброс (чем больше — тем менее точно). */
    private static final float BULLET_INACCURACY = 1.5F;
    /** Отдача назад (импульс скорости игрока). */
    private static final double RECOIL_BACK = 0.9D;
    /** Подброс вверх при выстреле. */
    private static final double RECOIL_UP = 0.30D;
    /** Сила кика камеры (тангаж вверх), как recoil-атрибут в bren. */
    private static final float CAMERA_RECOIL = 6.0F;
    /** Задержка между выстрелами (тиков). */
    private static final int FIRE_COOLDOWN = 30;
    /** Задержка после заряжания (тиков). */
    private static final int LOAD_COOLDOWN = 8;
    /** Длительность перезарядки при удержании ПКМ (тиков), как у арбалета. */
    public static final int RELOAD_TICKS = 25;
    /** Тик, на котором показывается кадр reloading_2 (80% прогресса)
     *  и играет звук вставки патрона. */
    private static final int INSERT_SOUND_TICK = (int) (RELOAD_TICKS * 0.8D);
    /** NBT-флаг «в стволе стреляная гильза» (ставится после выстрела). */
    private static final String SPENT_CASING_TAG = "SpentCasing";

    public ContenderPistolItem(Properties properties) {
        super(properties);
    }

    // Наследуемся от ProjectileWeaponItem (НЕ CrossbowItem!):
    // ванильный рендер рук в первом лице применяет «арбалетную» позу
    // ко всем предметам-наследникам CrossbowItem, из-за чего руки
    // поднимались как при зарядке арбалета. Обычный ProjectileWeaponItem
    // таких спец-поз не имеет. Абстрактные методы:
    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> stack.is(ModItems.CARTRIDGE.get());
    }

    @Override
    public int getDefaultProjectileRange() {
        return 12;
    }

    /** Не используется — выстрел реализован своим методом fire(). */
    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index,
                                   float velocity, float inaccuracy, float angle,
                                   @Nullable LivingEntity target) {
        projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle,
                0.0F, velocity, inaccuracy);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);

        // Пока идёт перезарядка/откат — игнорируем ПКМ.
        if (player.getCooldowns().isOnCooldown(gun.getItem())) {
            return InteractionResultHolder.fail(gun);
        }

        ChargedProjectiles loaded = gun.get(DataComponents.CHARGED_PROJECTILES);
        boolean isLoaded = loaded != null && !loaded.isEmpty();

        if (isLoaded) {
            return fire(level, player, gun);
        }

        // Перезарядка как у арбалета: удержание ПКМ.
        // Патронов нет — просто ничего не делаем (без звука сухого щелчка).
        if (findAmmo(player).isEmpty()) {
            return InteractionResultHolder.fail(gun);
        }

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 0.8F, 1.1F);

            // Если до этого был выстрел — при переломе ствола выпадает
            // стреляная гильза (случайный из 3 звуков отскока).
            CustomData data = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (data.copyTag().getBoolean(SPENT_CASING_TAG)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.randomCasing(level.getRandom()), SoundSource.PLAYERS,
                        0.7F, 0.95F + level.getRandom().nextFloat() * 0.1F);
                CustomData.update(DataComponents.CUSTOM_DATA, gun,
                        tag -> tag.remove(SPENT_CASING_TAG));
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(gun);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return RELOAD_TICKS + 3;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // Без специальной анимации использования: руки остаются обычными,
        // а движение при перезарядке даёт только смена кадров модели.
        // Прошлая проблема «пистолет по центру туловища» была вызвана
        // не UseAnim.NONE, а старыми 3D display-трансформами в моделях
        // кадров перезарядки — они уже убраны (чистый item/handheld).
        return UseAnim.NONE;
    }

    /**
     * Тик перезарядки: на кадре thompson_center_44_reloading_2
     * (80% прогресса — момент вставки патрона в ствол)
     * проигрывается звук bullet_insert.
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (level.isClientSide) {
            return;
        }
        int elapsed = this.getUseDuration(stack, entity) - remainingTicks;
        if (elapsed == INSERT_SOUND_TICK) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    ModSounds.randomInsert(level.getRandom()), SoundSource.PLAYERS,
                    1.0F, 0.95F + level.getRandom().nextFloat() * 0.1F);
        }
    }

    /**
     * Мы наследуемся от CrossbowItem, а для «арбалетных» предметов
<<<<<<< HEAD
     * ванилла НЕ вызывает finishUsingItem по ис��ечении времени —
=======
     * ванилла НЕ вызывает finishUsingItem по истечении времени —
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
     * только releaseUsing при отпускании. Возвращаем false, чтобы
     * finishUsingItem снова работал как у обычных предметов.
     */
    @Override
    public boolean useOnRelease(ItemStack stack) {
        return false;
    }

    /**
     * Полное удержание ПКМ до конца: патрон уходит в ствол,
     * боезапас уменьшается на 1.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack gun, Level level, LivingEntity entity) {
        tryLoad(gun, level, entity);
        return gun;
    }

    /**
     * Отпустили ПКМ: если продержали полную перезарядку — заряжаем
     * (страховка на случай, если finishUsingItem не сработает),
     * иначе перезарядка просто отменяется.
     */
    @Override
    public void releaseUsing(ItemStack gun, Level level, LivingEntity entity, int timeLeft) {
        int elapsed = this.getUseDuration(gun, entity) - timeLeft;
        if (elapsed >= RELOAD_TICKS) {
            tryLoad(gun, level, entity);
        }
    }

    /** Заряжание: кладёт 1 патрон в ствол, если он пуст и патрон есть. */
    private static void tryLoad(ItemStack gun, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }

        ChargedProjectiles loaded = gun.get(DataComponents.CHARGED_PROJECTILES);
        if (loaded != null && !loaded.isEmpty()) {
            return; // уже заряжен (защита от двойного вызова)
        }

        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty()) {
            return;
        }

        if (!level.isClientSide) {
            gun.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(ammo.copyWithCount(1)));
            if (!player.getAbilities().instabuild) {
                ammo.shrink(1); // -1 патрон из инвентаря
            }
            // Звук вставки патрона играет раньше — на кадре reloading_2
            // (см. onUseTick), здесь только фиксация заряда.
            player.getCooldowns().addCooldown(gun.getItem(), LOAD_COOLDOWN);
        }
    }

    /** Заряженное состояние: жёлтая заполненная полоска снизу (как у мешочка). */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        ChargedProjectiles loaded = stack.get(DataComponents.CHARGED_PROJECTILES);
        return loaded != null && !loaded.isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13; // полностью заполнена
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFD830; // жёлтый
    }

    /** Выстрел: пуля + отдача + пороховые газы + опустошение ствола. */
    private InteractionResultHolder<ItemStack> fire(Level level, Player player, ItemStack gun) {
        if (level.isClientSide) {
            // Отдача корпуса применяется на КЛИЕНТЕ: движение игрока
            // клиент-авторитативно, поэтому толчок ощущается сразу
            // (тот же приём, что и release-dash у крюка-кошки).
            Vec3 look = player.getLookAngle();
            Vec3 recoil = new Vec3(-look.x * RECOIL_BACK, RECOIL_UP, -look.z * RECOIL_BACK);
            player.setDeltaMovement(player.getDeltaMovement().add(recoil));
            player.hurtMarked = true;
            player.hasImpulse = true;

            // Кик камеры вверх со случайным сносом вбок — система
            // отдачи, портированная из bren (RecoilSys).
            RecoilSys.shotEvent(player, CAMERA_RECOIL);
        } else {
            BulletEntity bullet = new BulletEntity(level, player);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    BULLET_VELOCITY, BULLET_INACCURACY);
            level.addFreshEntity(bullet);

            // Пороховые газы у дульного среза: вспышка + облако дыма,
            // летящее по направлению выстрела и медленно рассеивающееся.
            if (level instanceof ServerLevel serverLevel) {
                Vec3 look = player.getLookAngle();
                Vec3 muzzle = player.getEyePosition()
                        .add(look.scale(0.9D))
                        .add(0.0D, -0.1D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        muzzle.x, muzzle.y, muzzle.z,
                        3, 0.02D, 0.02D, 0.02D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        muzzle.x + look.x * 0.3D, muzzle.y + look.y * 0.3D, muzzle.z + look.z * 0.3D,
                        12, 0.08D, 0.08D, 0.08D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        muzzle.x, muzzle.y, muzzle.z,
                        4, 0.05D, 0.05D, 0.05D, 0.005D);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.randomShot(level.getRandom()), SoundSource.PLAYERS,
                    2.0F, 0.95F + level.getRandom().nextFloat() * 0.1F);

            gun.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            // Помечаем: в стволе осталась стреляная гильза —
            // при следующей перезарядке она выпадет со звуком.
            CustomData.update(DataComponents.CUSTOM_DATA, gun,
                    tag -> tag.putBoolean(SPENT_CASING_TAG, true));
            player.getCooldowns().addCooldown(gun.getItem(), FIRE_COOLDOWN);
        }
        return InteractionResultHolder.consume(gun);
    }

    /** Первый найденный в инвентаре патрон (или EMPTY). */
    private static ItemStack findAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.CARTRIDGE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
