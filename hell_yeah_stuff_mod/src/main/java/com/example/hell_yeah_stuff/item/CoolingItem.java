package com.example.hell_yeah_stuff.item;

import com.example.hell_yeah_stuff.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Предмет с таймером остывания. Таймер запускается при попадании стака в
 * инвентарь (первый inventoryTick) и виден в описании предмета. По истечении
 * таймер просто исчезает — предмет "остывает" без превращения, и состояние
 * таймера не влияет ни на какие крафты.
 */
public class CoolingItem extends Item {

    /** Длительность остывания в тиках (минуты * 60 сек * 20 тиков). */
    private final long coolTicks;

    public CoolingItem(Properties properties, int minutes) {
        super(properties);
        this.coolTicks = minutes * 60L * 20L;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        Long until = stack.get(ModDataComponents.COOL_UNTIL.get());
        if (until == null) {
            // Стак только что попал в инвентарь — запускаем таймер (один раз).
            stack.set(ModDataComponents.COOL_UNTIL.get(), level.getGameTime() + this.coolTicks);
        } else if (until > 0 && level.getGameTime() >= until) {
            // Остыло НАВСЕГДА: ставим маркер 0 вместо удаления компонента,
            // иначе следующий тик запускал бы таймер заново по кругу.
            stack.set(ModDataComponents.COOL_UNTIL.get(), 0L);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Long until = stack.get(ModDataComponents.COOL_UNTIL.get());
        if (until == null || until <= 0) {
            return; // 0 — маркер "уже остыло", надписи нет
        }
        Level level = context.level();
        if (level == null) {
            tooltip.add(Component.translatable("tooltip.hell_yeah_stuff.hot")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        long remaining = until - level.getGameTime();
        if (remaining <= 0) {
            return;
        }
        long totalSeconds = remaining / 20;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        tooltip.add(Component.translatable("tooltip.hell_yeah_stuff.hot_timer",
                        String.format("%d:%02d", minutes, seconds))
                .withStyle(ChatFormatting.GOLD));
    }
}
