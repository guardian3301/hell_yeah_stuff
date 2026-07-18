package com.example.hell_yeah_stuff.entity;

import com.example.hell_yeah_stuff.compat.SableCompat;
import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Одноразовый цепкий дротик с физикой а-ля Dying Light 2:
 *
 * 1. Выстреливается из мини-арбалета и втыкается в блок (якорь).
 *    Длина троса фиксируется РОВНО по дистанции выстрела:
 *    на каком расстоянии зацепился — на таком и висишь.
 * 2. Маятник: трос не растягивается — радиальная скорость гасится,
 *    касательная сохраняется (импульс не теряется).
 * 3. Раскачивание: клавиши движения (WASD) в висе подталкивают
 *    игрока — можно раскачаться из неподвижного виса.
 * 4. Нажатие ПКМ с арбалетом — отцепление С РЫВКОМ в направлении
 *    взгляда (как release-dash в DL2). Дротик одноразовый.
 * 5. Один крюк за раз: новый выстрел убирает предыдущий дротик.
 *
 * Физика качания считается НА КЛИЕНТЕ владельца (движение игрока
 * в MC клиент-авторитативно — так нет резинового дёрганья), а сервер
 * отвечает за обрыв троса, рывок и защиту от урона падения.
 * Длина троса синхронизируется через SynchedEntityData.
 * Верёвка до арбалета рисуется в {@code GrappleDartRenderer}.
 */
public class GrappleDartEntity extends AbstractArrow {

    private static final EntityDataAccessor<Boolean> ANCHORED =
            SynchedEntityData.defineId(GrappleDartEntity.class, EntityDataSerializers.BOOLEAN);
    /** Длина троса; синхронизируется, т.к. физика считается на клиенте. */
    private static final EntityDataAccessor<Float> ROPE_LENGTH =
            SynchedEntityData.defineId(GrappleDartEntity.class, EntityDataSerializers.FLOAT);

    /** Максимальная длина троса при зацепе. */
    private static final double MAX_ROPE_LENGTH = 48.0D;
    /** Минимальная длина троса при зацепе в упор. */
    private static final double MIN_ROPE_LENGTH = 1.5D;
    /** Жёсткость «пружины», возвращающей игрока на длину троса. */
    private static final double ROPE_SPRING = 0.25D;
    /** Макс. пружинная поправка скорости за тик. */
    private static final double ROPE_SPRING_MAX = 0.35D;
    /** Сила притягивания к якорю (дротику) — направлена строго на дротик. */
    private static final double PULL_FORCE = 0.08D;
    /** Сила раскачивания клавишами движения (блоков/тик²). */
    private static final double SWING_INPUT_FORCE = 0.035D;
    /** Доля длины троса, до которой игрока подтягивает (как в Dying Light 2):
     *  притягивание останавливается на 2/3 исходной длины верёвки. */
    private static final double PULL_STOP_FRACTION = 2.0D / 3.0D;
    /** Подтягивание работает, только если якорь минимум на столько
     *  блоков ВЫШЕ ног игрока (нельзя тянуться к дротику под собой). */
    private static final double PULL_MIN_ANCHOR_ABOVE = 1.0D;
    /** Компенсация воздушного трения при качании на натянутом тросе:
     *  Minecraft каждый тик гасит горизонтальную скорость игрока (~x0.91),
     *  из-за чего маятник быстро затухает. Умножаем касательную скорость
     *  на этот коэффициент — качание сохраняет импульс и ощущается инертным. */
    private static final double SWING_INERTIA = 1.06D;
    /** Макс. скорость (блоков/тик), до которой работает компенсация инерции. */
    private static final double SWING_MAX_SPEED = 1.6D;
    /** Сила рывка при отцеплении (в направлении взгляда). */
    private static final double DETACH_DASH = 0.85D;
    /** Минимальная вертикальная добавка рывка (подброс). */
    private static final double DETACH_DASH_UP = 0.25D;
    /** Запас сверх MAX, после которого трос рвётся. */
    private static final double BREAK_FACTOR = 1.25D;

    /**
     * Клиентская «рабочая» длина троса (лебёдка с храповиком):
     * притягивание укорачивает трос до текущей дистанции, и обратно
     * он уже не отпускает. Без этого игрок на пике маха вылетал
     * к старому радиусу и его дёргало пружиной назад («резинка»).
     * -1 = ещё не инициализирована (берётся из synced ROPE_LENGTH).
     */
    private double clientRopeLength = -1.0D;

    /**
     * Подтягивание завершено: игрок достиг 1/3 длины троса.
     * После этого трос фиксируется на текущем радиусе, а притягивание
     * и лебёдка выключаются НАВСЕГДА — дальше чистый маятник
     * на постоянной длине (дуга как на схеме «динамический радиус»).
     */
    private boolean pullDone = false;

    /** Клиентское: тик, в который дротик заякорился (старт затухающих
     *  колебаний верёвки y = A·sin(2π/l·x)·cos(ωt)·e^{-t/τ} в рендерере).
     *  -1 = ещё не заякорился. */
    private int clientAnchorTick = -1;

    /** Активные (воткнутые) дротики по владельцам — вместо дорогого поиска
     *  по огромному AABB. Отдельные карты на клиент/сервер (integrated server
     *  держит оба мира в одном процессе). Нужен и для Sable: дротик, воткнутый
     *  в плот саб-левела, физически находится за тысячи блоков от игрока,
     *  и поиск по AABB вокруг игрока его бы не нашёл. */
    private static final Map<UUID, GrappleDartEntity> ACTIVE_SERVER = new HashMap<>();
    private static final Map<UUID, GrappleDartEntity> ACTIVE_CLIENT = new HashMap<>();

    private static Map<UUID, GrappleDartEntity> activeMap(Level level) {
        return level.isClientSide ? ACTIVE_CLIENT : ACTIVE_SERVER;
    }

    public GrappleDartEntity(EntityType<? extends GrappleDartEntity> type, Level level) {
        super(type, level);
    }

    public GrappleDartEntity(Level level, LivingEntity shooter, ItemStack ammo, @Nullable ItemStack weapon) {
        super(ModEntities.GRAPPLE_DART.get(), shooter, level, ammo, weapon);
        this.pickup = Pickup.DISALLOWED; // одноразовый: не подбирается
        this.setBaseDamage(1.0D);
    }

    public GrappleDartEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntities.GRAPPLE_DART.get(), x, y, z, level, stack, null);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(1.0D);
    }

    // ------------------------------------------------------------------
    // Состояние
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANCHORED, false);
        builder.define(ROPE_LENGTH, (float) MAX_ROPE_LENGTH);
    }

    public boolean isAnchored() {
        return this.entityData.get(ANCHORED);
    }

    public double getRopeLength() {
        return this.entityData.get(ROPE_LENGTH);
    }

    /** Время (в тиках, с partialTicks) с момента зацепа — параметр t
     *  колебаний верёвки. Отрицательное = дротик ещё не заякорился. */
    public float getRopeWaveAge(float partialTicks) {
        return this.clientAnchorTick < 0
                ? -1.0F
                : (this.tickCount - this.clientAnchorTick) + partialTicks;
    }

    /** Обрыв троса (сервер): звук + уничтожение дротика. Скорость игрока
     *  НЕ трогаем — иначе сервер затрeт реальный импульс качания. */
    public void detach() {
        if (!this.level().isClientSide && this.getOwner() instanceof LivingEntity living) {
            living.fallDistance = 0.0F; // небольшая страховка в момент отцепа
        }
        Vec3 at = this.anchorPos(); // звук — в мировой точке якоря, не в плоте
        this.level().playSound(null, at.x, at.y, at.z,
                SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 1.0F, 1.1F);
        if (this.getOwner() instanceof Player player) {
            activeMap(this.level()).remove(player.getUUID(), this);
        }
        this.discard();
    }

    /**
     * Рывок при отцеплении (release-dash как в Dying Light 2).
     *
     * Важно: вызывается НА КЛИЕНТЕ владельца (из MiniCrossbowItem#use).
     * Движение игрока клиент-авторитативно: только клиент знает
     * настоящую текущую скорость качания. Если делать рывок на сервере
     * через hurtMarked, сервер перезапишет скорость игрока своим
     * устаревшим (гравитационным) вектором — рывок уходит вниз.
     */
    public void applyDetachDash(LivingEntity living) {
        Vec3 look = living.getLookAngle();
        Vec3 dash = new Vec3(look.x, Math.max(look.y, DETACH_DASH_UP), look.z)
                .normalize().scale(DETACH_DASH);
        living.setDeltaMovement(living.getDeltaMovement().add(dash));
        living.fallDistance = 0.0F;
    }

    /**
     * МИРОВАЯ позиция якоря. Обычно это позиция дротика; но если дротик
     * воткнут в движущуюся структуру Sable (саб-левел), его координаты —
     * плотовые, и сюда возвращается проекция в мир через позу структуры:
     * крюк «едет» вместе с саб-левелом.
     */
    public Vec3 anchorPos() {
        return SableCompat.projectToWorld(this.level(), this.blockPosition(), this.position());
    }

    /** Активный (воткнутый) дротик игрока; работает на обеих сторонах. */
    @Nullable
    public static GrappleDartEntity findActive(Level level, Player player) {
        Map<UUID, GrappleDartEntity> map = activeMap(level);
        GrappleDartEntity dart = map.get(player.getUUID());
        if (dart == null) {
            return null;
        }
        if (dart.isRemoved() || dart.level() != level || !dart.isAnchored() || dart.getOwner() != player) {
            map.remove(player.getUUID());
            return null;
        }
        return dart;
    }

    // ------------------------------------------------------------------
    // Поведение
    // ------------------------------------------------------------------

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.entityData.set(ANCHORED, true);
            // Длина троса = дистанция до стрелка в момент зацепа:
            // выстрелил с 20 блоков — висишь в 20 блоках от якоря.
            // Считаем по МИРОВОЙ позиции якоря (учёт Sable саб-левелов).
            Entity owner = this.getOwner();
            if (owner != null) {
                this.entityData.set(ROPE_LENGTH, (float) Mth.clamp(
                        owner.position().add(0.0D, owner.getBbHeight() * 0.5D, 0.0D).distanceTo(this.anchorPos()),
                        MIN_ROPE_LENGTH, MAX_ROPE_LENGTH));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAnchored()) {
            return;
        }

        // Клиент: запоминаем момент зацепа — от него отсчитывается
        // время t затухающих колебаний верёвки в GrappleDartRenderer.
        if (this.level().isClientSide && this.clientAnchorTick < 0) {
            this.clientAnchorTick = this.tickCount;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity living) || !living.isAlive() || living.level() != this.level()) {
            if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }

        // Регистрация активного крюка владельца (обе стороны) — для
        // findActive без поиска по AABB (и для якорей в Sable-плотах).
        if (owner instanceof Player player) {
            activeMap(this.level()).put(player.getUUID(), this);
        }

        // Физика качания — на клиенте того игрока, который висит:
        // движение игрока клиент-авторитативно, так получается плавно.
        if (this.level().isClientSide) {
            if (living instanceof Player p && p.isLocalPlayer()) {
                this.swingPhysics(living);
            }
            return;
        }

        // ------------------------- сервер -------------------------

        // Блок, в который воткнулся дротик, сломали — крюк слетает.
        if (!this.inGround) {
            this.detach();
            return;
        }

        double dist = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D)
                .distanceTo(this.anchorPos());

        // Слишком далеко — трос рвётся.
        if (dist > MAX_ROPE_LENGTH * BREAK_FACTOR) {
            this.detach();
            return;
        }

        // Защита от урона падения считается на сервере:
        // пока трос близок к натяжению — падение не копится.
        if (!living.onGround() && dist > this.getRopeLength() * 0.8D) {
            living.fallDistance = 0.0F;
        }
    }

    /**
     * Физика троса (клиент владельца):
     * притягивание СТРОГО к дротику (не зависит от направления взгляда)
     * + натянутый трос как ограничитель максимальной длины.
     */
    private void swingPhysics(LivingEntity living) {
        if (this.clientRopeLength < 0.0D) {
            this.clientRopeLength = this.getRopeLength();
        }
        // Якорь — в МИРОВЫХ координатах (для Sable-структур позиция дротика
        // проецируется из плота позой саб-левела: маятник следует за судном).
        Vec3 anchor = this.anchorPos();
        Vec3 body = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
        Vec3 toAnchor = anchor.subtract(body);
        double dist = toAnchor.length();

        // Порог притягивания считается от ИСХОДНОЙ длины троса.
        double pullStop = Math.max(MIN_ROPE_LENGTH, this.getRopeLength() * PULL_STOP_FRACTION);

        // Подтягивание разрешено, только если якорь минимум на
        // PULL_MIN_ANCHOR_ABOVE выше ног игрока (дротик над горизонтом).
        boolean anchorAbove = anchor.y >= living.getY() + PULL_MIN_ANCHOR_ABOVE;

        // Игрок дотянут до 1/3 троса — подтяг завершён, радиус
        // фиксируется, дальше только маятник на постоянной длине.
        if (!this.pullDone && dist <= pullStop + 0.1D) {
            this.pullDone = true;
            this.clientRopeLength = pullStop;
        }

        // Храповик: подтянуло ближе — трос укорачивается до текущей
        // дистанции и обратно не отпускает (лебёдка, как в DL2).
        // Работает только ПОКА идёт подтягивание: после его завершения
        // (pullDone) длина троса больше не меняется — иначе взлёт
        // по дуге маятника укорачивал бы радиус и качание затухало.
        if (!this.pullDone && anchorAbove && dist < this.clientRopeLength) {
            this.clientRopeLength = Math.max(pullStop, dist);
        }
        double ropeLength = this.clientRopeLength;

        Vec3 vel = living.getDeltaMovement();
        boolean changed = false;

        // Раскачивание: WASD в воздухе подталкивает игрока
        // (xxa/zza — ввод движения локального игрока, как в Entity#getInputVector).
        // Толчок проецируется на СФЕРУ вокруг якоря: радиальная составляющая
        // (вдоль троса) убирается, остаётся касательная — игрок раскачивается
        // по дуге вокруг точки хвата, а не просто ползёт по горизонтали.
        if (!living.onGround()) {
            Vec3 input = new Vec3(living.xxa, 0.0D, living.zza);
            if (input.lengthSqr() > 1.0E-4D && dist > 1.0E-3D) {
                Vec3 push = input.normalize()
                        .yRot(-living.getYRot() * ((float) Math.PI / 180.0F))
                        .scale(SWING_INPUT_FORCE);

                // Проекция на касательную плоскость к сфере радиуса dist:
                // push_tangent = push - n * (push · n), где n — направление на якорь.
                Vec3 n = toAnchor.scale(1.0D / dist);
                Vec3 tangent = push.subtract(n.scale(push.dot(n)));

                // Сохраняем силу толчка: после проекции длина уменьшается,
                // возвращаем её к исходной, чтобы качание не слабело у «полюсов».
                double tlen = tangent.length();
                if (tlen > 1.0E-4D) {
                    tangent = tangent.scale(SWING_INPUT_FORCE / tlen);
                    vel = vel.add(tangent);
                    changed = true;
                }
            }
        }

        // Притягивание к дротику: сила направлена ВДОЛЬ вектора «игрок → дротик»,
        // то есть строго на якорь, независимо от того, куда смотрит игрок.
        // Тянет до 1/3 исходной длины верёвки (стиль Dying Light 2):
        // длинный трос — долгий полёт к точке, короткий — быстрый подтяг.
        // Работает ТОЛЬКО если якорь минимум на 1 блок выше ног игрока:
        // к дротику ниже горизонта не тянет (можно лишь качаться на тросе),
        // и ТОЛЬКО до завершения подтяга (pullDone): после — чистый маятник,
        // при качании игрока больше не тянет обратно вверх по радиусу.
        if (!this.pullDone && anchorAbove && dist > pullStop) {
            Vec3 pull = toAnchor.scale(1.0D / dist).scale(PULL_FORCE);
            vel = vel.add(pull);
            living.fallDistance = 0.0F;
            changed = true;
        }

        // Натянутый трос: висим ровно на дистанции выстрела.
        // Ограничение считается по ПРЕДСКАЗАННОЙ позиции следующего тика
        // (body + vel): скорость корректируется ДО пересечения сферы,
        // а не после. Без этого игрок каждый тик прыгал через границу
        // «свободный полёт / жёсткая коррекция» — дуга разбивалась
        // на ступенчатые отрезки.
        if (!(living.onGround() && dist < ropeLength + 1.0D)) {
            Vec3 nextToAnchor = anchor.subtract(body.add(vel));
            double nextDist = nextToAnchor.length();

            if (nextDist > ropeLength && nextDist > 1.0E-3D) {
                Vec3 n = nextToAnchor.scale(1.0D / nextDist); // к якорю

                // 1) Скорость ОТ якоря гасится, касательная (качание) остаётся.
                double radial = vel.dot(n);
                if (radial < 0.0D) {
                    vel = vel.subtract(n.scale(radial));
                }

                // 2) Инерция: компенсируем воздушное трение Minecraft —
                // касательная (маятниковая) составляющая скорости усиливается,
                // чтобы размах не затухал за пару качей. Работает только
                // в воздухе на натянутом тросе и до разумного лимита скорости.
                if (!living.onGround()) {
                    Vec3 radialPart = n.scale(vel.dot(n));
                    Vec3 tangentPart = vel.subtract(radialPart);
                    double speed = tangentPart.length();
                    if (speed > 0.05D && speed < SWING_MAX_SPEED) {
                        vel = radialPart.add(tangentPart.scale(SWING_INERTIA));
                    }
                }

                // 3) Пружина плавно возвращает на длину троса (без телепорта).
                double stretch = nextDist - ropeLength;
                vel = vel.add(n.scale(Math.min(stretch * ROPE_SPRING, ROPE_SPRING_MAX)));

                living.fallDistance = 0.0F;
                changed = true;
            }
        }

        if (changed) {
            living.setDeltaMovement(vel);
        }
    }

    // ------------------------------------------------------------------
    // Сохранение / прочее
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Anchored", this.isAnchored());
        tag.putDouble("RopeLength", this.getRopeLength());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ANCHORED, tag.getBoolean("Anchored"));
        if (tag.contains("RopeLength")) {
            this.entityData.set(ROPE_LENGTH, (float) tag.getDouble("RopeLength"));
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.GRAPPLE_DART.get());
    }
}
