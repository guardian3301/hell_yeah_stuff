package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.entity.GrappleDartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Рендер дротика + верёвка от арбалета до дротика.
 *
 * Верёвка — две скрещенные текстурированные ленты (видны с любого
 * ракурса), текстура — rope_particle из Create Aeronautics (колонки 6–9,
 * как в оригинальной модели rope.json), тайлится каждый блок длины.
 */
public class GrappleDartRenderer extends ArrowRenderer<GrappleDartEntity> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "textures/entity/projectiles/grapple_dart.png");
    private static final ResourceLocation ROPE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "textures/entity/rope.png");

    /** Полуширина ленты верёвки в блоках (~1.5 пикселя). */
    private static final float ROPE_RADIUS = 0.045F;
    /** UV-полоса верёвки: колонки 6..9 текстуры 16x16 (как в rope.json). */
    private static final float U0 = 6.0F / 16.0F;
    private static final float U1 = 9.0F / 16.0F;

    // ---- Колебания струны: y = A·sin(2π/l·x)·cos(ωt)·e^{-t/τ} ----
    /** A = WAVE_AMPLITUDE_REF / l — чем длиннее верёвка, тем меньше амплитуда. */
    private static final float WAVE_AMPLITUDE_REF = 1.5F;
    /** Потолок амплитуды на коротком тросе (блоков). */
    private static final float WAVE_AMPLITUDE_MAX = 0.2F;
    /** Полуразмер узла на концах: куб со стороной в 1.5 раза больше
     *  толщины верёвки (2·ROPE_RADIUS·1.5 / 2). */
    private static final float KNOT_HALF = ROPE_RADIUS * 1.5F;
    /** UV узла: квадрат 3x3 пикселя (колонки 6..9, строки 0..3) —
     *  пиксельно-выровненный, 1:1 на квадратную грань куба, без растяжения. */
    private static final float KNOT_V0 = 0.0F / 16.0F;
    private static final float KNOT_V1 = 3.0F / 16.0F;
    /** ω = WAVE_OMEGA_REF / l (рад/тик) — длинный трос колеблется медленнее. */
    private static final float WAVE_OMEGA_REF = 8.0F;
    /** τ — время затухания в e раз (тиков): колебания гаснут за ~1.5-2 с. */
    private static final float WAVE_DECAY_TICKS = 12.0F;

    public GrappleDartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GrappleDartEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GrappleDartEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        Entity owner = entity.getOwner();
        if (owner instanceof Player player && player.isAlive() && player.level() == entity.level()) {
            renderRope(entity, player, partialTicks, poseStack, buffer, packedLight);
        }
    }

    private void renderRope(GrappleDartEntity dart, Player player, float partialTicks,
                            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Точка у руки с арбалетом (приближённо, как у лески удочки).
        int armSign = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (player.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND) {
            armSign = -armSign;
        }
        float bodyYaw = Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot) * ((float) Math.PI / 180.0F);
        double sin = Mth.sin(bodyYaw);
        double cos = Mth.cos(bodyYaw);

        double px = Mth.lerp(partialTicks, player.xo, player.getX());
        double py = Mth.lerp(partialTicks, player.yo, player.getY());
        double pz = Mth.lerp(partialTicks, player.zo, player.getZ());

        // right = (cos, 0, sin), forward = (-sin, 0, cos)
        Vec3 hand = new Vec3(
                px + cos * 0.36D * armSign - sin * 0.25D,
                py + player.getBbHeight() * 0.62D,
                pz + sin * 0.36D * armSign + cos * 0.25D);

        // Обе точки троса держим в ОДНОЙ (мировой) системе координат.
        // Дротик, воткнутый в плот Sable-структуры, физически лежит в
        // плотовых координатах — проецируем его в мир той же позой, что
        // задаёт рендер сущности (для обычного якоря projectToWorld —
        // тождество). Раньше в плот проецировалась только РУКА, а конец у
        // дротика оставался плотовым: из-за рассогласования систем при
        // зацепе за саб-левел трос менял длину и «гулял» из стороны в
        // сторону. Теперь длина и ориентация совпадают с рендером структуры.
        Vec3 dartRaw = new Vec3(
                Mth.lerp(partialTicks, dart.xo, dart.getX()),
                Mth.lerp(partialTicks, dart.yo, dart.getY()) + 0.05D,
                Mth.lerp(partialTicks, dart.zo, dart.getZ()));
        Vec3 dartPos = com.example.hell_yeah_stuff.compat.SableCompat.projectToWorld(
                dart.level(), dart.blockPosition(), dartRaw);

        Vec3 delta = hand.subtract(dartPos);
        float length = (float) delta.length();
        if (length < 0.1F) {
            return;
        }

        // Ортонормальный базис вокруг оси верёвки — квадратное сечение 1:1.
        Vec3 dir = delta.scale(1.0D / length);
        Vec3 ref = Math.abs(dir.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side1 = dir.cross(ref).normalize().scale(ROPE_RADIUS);
        Vec3 side2 = dir.cross(side1).normalize().scale(ROPE_RADIUS);

        // Четыре угла квадратного сечения (полуширина ROPE_RADIUS по обеим осям).
        Vec3 c1 = side1.add(side2);
        Vec3 c2 = side1.subtract(side2);
        Vec3 c3 = side1.scale(-1.0D).subtract(side2);
        Vec3 c4 = side2.subtract(side1);

        // Затухающая стоячая волна (см. формулу «Колебания струны»):
        // y(x,t) = A·sin(2π/l·x)·cos(ωt)·e^{-t/τ}, где x — точка верёвки.
        // A = REF/l и ω = REF/l: длиннее трос — меньше амплитуда и темп.
        // Концы (рука и дротик) закреплены: sin = 0 на краях.
        Vec3 waveDir = side2.normalize(); // перпендикуляр к оси верёвки
        float waveScale = 0.0F;
        float waveAge = dart.getRopeWaveAge(partialTicks);
        if (waveAge >= 0.0F) {
            float amp = Math.min(WAVE_AMPLITUDE_MAX, WAVE_AMPLITUDE_REF / length);
            float omega = WAVE_OMEGA_REF / length;
            float decay = (float) Math.exp(-waveAge / WAVE_DECAY_TICKS);
            waveScale = amp * Mth.cos(omega * waveAge) * decay;
            if (Math.abs(waveScale) < 1.0E-3F) {
                waveScale = 0.0F; // колебания сошли на нет
            }
        }

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(ROPE_TEXTURE));
        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        // Четыре грани прямоугольной «трубы» верёвки.
        ribbon(vc, pose, delta, c1, c2, length, waveDir, waveScale, packedLight);
        ribbon(vc, pose, delta, c2, c3, length, waveDir, waveScale, packedLight);
        ribbon(vc, pose, delta, c3, c4, length, waveDir, waveScale, packedLight);
        ribbon(vc, pose, delta, c4, c1, length, waveDir, waveScale, packedLight);
        // Узлы на закреплённых концах: куб со стороной 1.5x толщины верёвки,
        // текстура — та же полоса верёвки.
        knot(vc, pose, Vec3.ZERO, dir, side1, side2, packedLight); // у дротика
        knot(vc, pose, delta, dir, side1, side2, packedLight);     // у руки
        poseStack.popPose();
    }

    /**
     * Узел — куб с центром в точке end, ориентированный по осям верёвки
     * (dir вдоль, side1/side2 поперёк). Сторона куба = 2·KNOT_HALF
     * (в 1.5 раза больше толщины верёвки). 6 граней той же UV-полосой.
     */
    private static void knot(VertexConsumer vc, PoseStack.Pose pose, Vec3 end,
                             Vec3 dir, Vec3 side1, Vec3 side2, int light) {
        Vec3 ax = dir.scale(KNOT_HALF);
        Vec3 s1 = side1.normalize().scale(KNOT_HALF);
        Vec3 s2 = side2.normalize().scale(KNOT_HALF);

        // Две грани, перпендикулярные side2 (верх/низ)
        quad(vc, pose, end.add(s2), ax, s1, light);
        quad(vc, pose, end.subtract(s2), ax, s1, light);
        // Две грани, перпендикулярные side1 (бока)
        quad(vc, pose, end.add(s1), ax, s2, light);
        quad(vc, pose, end.subtract(s1), ax, s2, light);
        // Две торцевые грани, перпендикулярные оси верёвки
        quad(vc, pose, end.add(ax), s1, s2, light);
        quad(vc, pose, end.subtract(ax), s1, s2, light);
    }

    /** Квадратная грань куба: центр center, полуоси ua и va.
     *  UV — пиксельный квадрат 3x3 (U0..U1 x KNOT_V0..KNOT_V1), 1:1 на грань. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 center,
                             Vec3 ua, Vec3 va, int light) {
        vertex(vc, pose, center.subtract(ua).subtract(va), U0, KNOT_V0, light);
        vertex(vc, pose, center.add(ua).subtract(va), U1, KNOT_V0, light);
        vertex(vc, pose, center.add(ua).add(va), U1, KNOT_V1, light);
        vertex(vc, pose, center.subtract(ua).add(va), U0, KNOT_V1, light);
    }

    /**
     * Одна грань между двумя углами сечения: сегменты по ~1 блоку
     * (при активной волне — мельче, для плавного изгиба), текстура
     * тайлится по длине. Волновое смещение: waveDir · waveScale · sin(2π·t),
     * t ∈ [0..1] вдоль верёвки — узлы на обоих закреплённых концах.
     */
    private static void ribbon(VertexConsumer vc, PoseStack.Pose pose,
                               Vec3 delta, Vec3 edgeA, Vec3 edgeB, float length,
                               Vec3 waveDir, float waveScale, int light) {
        int segments = waveScale != 0.0F
                ? Math.max(12, Mth.ceil(length * 2.0F))
                : Math.max(1, Mth.ceil(length));
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            Vec3 a = delta.scale(t0).add(waveOffset(waveDir, waveScale, t0));
            Vec3 b = delta.scale(t1).add(waveOffset(waveDir, waveScale, t1));
            float v0 = length * t0;
            float v1 = length * t1;
            vertex(vc, pose, a.add(edgeA), U0, v0, light);
            vertex(vc, pose, a.add(edgeB), U1, v0, light);
            vertex(vc, pose, b.add(edgeB), U1, v1, light);
            vertex(vc, pose, b.add(edgeA), U0, v1, light);
        }
    }

    /** Смещение точки верёвки стоячей волной: sin(2π·t) — полный период
     *  по длине (форма «S», как у струны на схеме), узлы на концах. */
    private static Vec3 waveOffset(Vec3 waveDir, float waveScale, float t) {
        if (waveScale == 0.0F) {
            return Vec3.ZERO;
        }
        return waveDir.scale(waveScale * Mth.sin((float) (Math.PI * 2.0D) * t));
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose,
                               Vec3 p, float u, float v, int light) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
