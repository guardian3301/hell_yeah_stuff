package com.example.hell_yeah_stuff.event;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Горячесть ОБЫЧНОГО железного слитка (отдельного предмета больше нет).
 *  - При взятии слитка из печи/плавильной печи он получает таймер температуры
 *    на 20 минут (компонент COOL_UNTIL).
 *  - Пока слиток горячий, раз в 10 секунд он коротко вспыхивает зачарованным
 *    блеском (зацикленная анимация, ни на что не влияет).
 *  - По истечении таймер и анимация пропадают НАВСЕГДА (маркер 0), и остывший
 *    слиток больше нельзя закалить в масле. На все остальные крафты
 *    состояние таймера не влияет — компонент чисто информационный.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class HotIronHandler {

    /** 20 минут в тиках. */
    private static final long HOT_TICKS = 20L * 60L * 20L;

    /** true, если слиток сейчас горячий (таймер есть и не истёк). */
    public static boolean isHot(ItemStack stack, Level level) {
        if (!stack.is(Items.IRON_INGOT)) {
            return false;
        }
        Long until = stack.get(ModDataComponents.COOL_UNTIL.get());
        return until != null && until > 0 && level.getGameTime() < until;
    }

    /** Слиток взят из печи — становится горячим. */
    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        ItemStack stack = event.getSmelting();
        Player player = event.getEntity();
        if (stack.is(Items.IRON_INGOT) && !player.level().isClientSide) {
            stack.set(ModDataComponents.COOL_UNTIL.get(),
                    player.level().getGameTime() + HOT_TICKS);
        }
    }

    /** Тик игрока: истечение таймера температуры (без анимации). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        long gameTime = level.getGameTime();

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(Items.IRON_INGOT)) {
                continue;
            }
            Long until = stack.get(ModDataComponents.COOL_UNTIL.get());
            if (until == null || until <= 0) {
                continue; // не горячий (или уже остыл навсегда)
            }
            if (gameTime >= until) {
                // Остыло: маркер 0 (не по кругу!). Снимаем блеск, если он
                // остался от прошлых версий с анимацией.
                stack.set(ModDataComponents.COOL_UNTIL.get(), 0L);
                stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            }
        }
    }

    /** Таймер температуры в описании горячего слитка. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.IRON_INGOT)) {
            return;
        }
        Long until = stack.get(ModDataComponents.COOL_UNTIL.get());
        if (until == null || until <= 0) {
            return;
        }
        Level level = event.getContext().level();
        if (level == null) {
            event.getToolTip().add(Component.translatable("tooltip.hell_yeah_stuff.hot")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        long remaining = until - level.getGameTime();
        if (remaining <= 0) {
            return;
        }
        long totalSeconds = remaining / 20;
        event.getToolTip().add(Component.translatable("tooltip.hell_yeah_stuff.hot_timer",
                        String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60))
                .withStyle(ChatFormatting.GOLD));
    }

    private HotIronHandler() {}
}
