package com.example.hell_yeah_stuff.event;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.item.MiniCrossbowItem;
import com.example.hell_yeah_stuff.registry.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Несовместимость зачарований мини-арбалета: Тройной выстрел (Multishot)
 * нельзя сочетать с Быстрой перезарядкой III (Quick Charge 3).
 * Quick Charge I-II с Multishot по-прежнему разрешены.
 *
 * Наковальня — единственный ванильный способ докинуть зачарование на уже
 * зачарованный предмет, поэтому достаточно отменить AnvilUpdateEvent.
 * (Стол зачарований не трогаем: комбинации, выпавшие там разом, чинятся
 * страховкой в {@link MiniCrossbowItem#inventoryTick}.)
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class EnchantIncompatHandler {

    @SubscribeEvent
    static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (!(left.getItem() instanceof MiniCrossbowItem)) {
            return;
        }
        ItemStack right = event.getRight();
        int multishot = Math.max(
                ModEnchantments.level(left, Enchantments.MULTISHOT),
                ModEnchantments.level(right, Enchantments.MULTISHOT));
        int quickCharge = Math.max(
                ModEnchantments.level(left, Enchantments.QUICK_CHARGE),
                ModEnchantments.level(right, Enchantments.QUICK_CHARGE));
        if (multishot > 0 && quickCharge >= 3) {
            // Результата нет — слоты подсвечиваются как несовместимые.
            event.setCanceled(true);
        }
    }

    private EnchantIncompatHandler() {}
}
