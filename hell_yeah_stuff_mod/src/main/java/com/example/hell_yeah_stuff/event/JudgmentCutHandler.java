package com.example.hell_yeah_stuff.event;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.item.HalberdItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Отложенный второй разрез «Judgment Cut»: алебарда планирует его на 1 секунду
 * позже первого удара, а этот обработчик выполняет его по серверному тику в той
 * же мировой точке. Игрок ищется по UUID на момент срабатывания; если вышел из
 * игры — разрез всё равно происходит, но без привязки к игроку.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class JudgmentCutHandler {

    private record Pending(ServerLevel level, UUID attacker, Vec3 center, long dueTick) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    /** Запланировать второй разрез через {@code delayTicks} тиков в точке {@code center}. */
    public static void schedule(ServerLevel level, Player attacker, Vec3 center, int delayTicks) {
        PENDING.add(new Pending(level, attacker.getUUID(), center, level.getGameTime() + delayTicks));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            // Отбросить записи от уже остановленного сервера (например, выход из мира).
            if (p.level().getServer() != event.getServer()) {
                it.remove();
                continue;
            }
            if (p.level().getGameTime() < p.dueTick()) {
                continue;
            }
            it.remove();
            Player attacker = p.level().getPlayerByUUID(p.attacker());
            HalberdItem.performSecondCut(p.level(), attacker, p.center());
        }
    }

    private JudgmentCutHandler() {}
}
