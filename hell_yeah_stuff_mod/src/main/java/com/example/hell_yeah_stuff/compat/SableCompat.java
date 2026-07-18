package com.example.hell_yeah_stuff.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Мягкая совместимость с Sable (библиотека «саб-левелов» — движущихся
 * структур, бэкенд Create Aeronautics) через рефлексию: мод собирается
 * и работает без Sable в classpath.
 *
 * Как устроен Sable: блоки саб-левела физически лежат в удалённом
 * «плоте» того же измерения, а в мир структура проецируется трансформом
 * {@code SubLevel.logicalPose()}. Если цепкий дротик воткнулся в блок
 * плота (движущейся структуры), его координаты — плотовые, за тысячи
 * блоков от игрока. Эти хелперы проецируют такие координаты в мировые
 * (и обратно), чтобы крюк «цеплялся за саб-левел»: трос тянет к движущейся
 * структуре, длина/обрыв считаются по мировой позиции якоря.
 */
public final class SableCompat {

    private static boolean checked;
    private static Method getContainer;      // SubLevelContainer.getContainer(Level)
    private static Method inBounds;          // container.inBounds(BlockPos)
    private static Method getPlot;           // container.getPlot(ChunkPos)
    private static Method plotGetSubLevel;   // LevelPlot.getSubLevel()
    private static Method subIsRemoved;      // SubLevel.isRemoved()
    private static Method logicalPose;       // SubLevel.logicalPose() -> Pose3dc
    private static Method transformPosition;         // Pose3dc.transformPosition(Vec3)
    private static Method transformPositionInverse;  // Pose3dc.transformPositionInverse(Vec3)

    private SableCompat() {}

    public static boolean isLoaded() {
        return resolve();
    }

    /**
     * Мировая позиция точки {@code pos}: если она в плоте саб-левела —
     * проекция через logicalPose, иначе — сама точка. Если саб-левел
     * удалён, вернётся исходная (плотовая) точка — дистанция до игрока
     * станет огромной, и трос корректно порвётся штатной проверкой.
     */
    public static Vec3 projectToWorld(Level level, BlockPos plotAnchor, Vec3 pos) {
        Object pose = poseAt(level, plotAnchor);
        if (pose == null) {
            return pos;
        }
        try {
            return (Vec3) transformPosition.invoke(pose, pos);
        } catch (Throwable t) {
            return pos;
        }
    }

    /**
     * Обратная проекция: мировая точка -> система координат плота,
     * в котором сидит якорь {@code plotAnchor}. Для отрисовки троса
     * от руки игрока (мир) до дротика (плот). Identity, если якорь
     * не в саб-левеле.
     */
    public static Vec3 projectFromWorld(Level level, BlockPos plotAnchor, Vec3 worldPos) {
        Object pose = poseAt(level, plotAnchor);
        if (pose == null) {
            return worldPos;
        }
        try {
            return (Vec3) transformPositionInverse.invoke(pose, worldPos);
        } catch (Throwable t) {
            return worldPos;
        }
    }

    /** Поза саб-левела, в чьём плоте лежит точка, или null. */
    private static Object poseAt(Level level, BlockPos pos) {
        if (!resolve()) {
            return null;
        }
        try {
            Object container = getContainer.invoke(null, level);
            if (container == null || !(Boolean) inBounds.invoke(container, pos)) {
                return null;
            }
            Object plot = getPlot.invoke(container, new ChunkPos(pos));
            if (plot == null) {
                return null;
            }
            Object subLevel = plotGetSubLevel.invoke(plot);
            if (subLevel == null || (Boolean) subIsRemoved.invoke(subLevel)) {
                return null;
            }
            return logicalPose.invoke(subLevel);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean resolve() {
        if (!checked) {
            checked = true;
            try {
                Class<?> containerCls = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
                Class<?> plotCls = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
                Class<?> subLevelCls = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
                Class<?> poseCls = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
                getContainer = containerCls.getMethod("getContainer", Level.class);
                inBounds = containerCls.getMethod("inBounds", BlockPos.class);
                getPlot = containerCls.getMethod("getPlot", ChunkPos.class);
                plotGetSubLevel = plotCls.getMethod("getSubLevel");
                subIsRemoved = subLevelCls.getMethod("isRemoved");
                logicalPose = subLevelCls.getMethod("logicalPose");
                transformPosition = poseCls.getMethod("transformPosition", Vec3.class);
                transformPositionInverse = poseCls.getMethod("transformPositionInverse", Vec3.class);
            } catch (Throwable t) {
                getContainer = null;
            }
        }
        return getContainer != null;
    }
}
