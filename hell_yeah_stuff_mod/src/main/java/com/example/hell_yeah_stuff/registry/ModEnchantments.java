package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Ключи data-driven зачарований мода (сами определения — в JSON:
 * data/hell_yeah_stuff/enchantment/*.json) и утилиты для их чтения со стека.
 */
public final class ModEnchantments {

    /**
     * «Аметистовые гранаты» — мини-арбалет стреляет аметистовым боезапасом
     * как контактной гранатой (взрыв при ударе) вместо дробового веера.
     */
    public static final ResourceKey<Enchantment> AMETHYST_GRENADES = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "amethyst_grenades"));

    /**
     * «Режущий рывок» — для алебарды: ПКМ наносит обычный дальний рубящий
     * удар и вдобавок второй удар, перпендикулярный первому (крест).
     */
    public static final ResourceKey<Enchantment> CUTTING_DASH = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "cutting_dash"));

    /**
     * «Рывок» — для поножей: быстрый горизонтальный рывок по нажатию клавиши
     * (в стиле Content SMP), с откатом. Логика — {@code DashHandler}.
     */
    public static final ResourceKey<Enchantment> DASH = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "dash"));

    /**
     * Уровень зачарования на стеке (для книг — из хранимых зачарований).
     * Работает по ключу, без обращения к реестру — удобно в местах,
     * где под рукой нет RegistryAccess.
     */
    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private ModEnchantments() {}
}
