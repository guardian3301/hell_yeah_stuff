package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
<<<<<<< HEAD
import net.minecraft.world.item.CreativeModeTabs;
=======
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

<<<<<<< HEAD
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {

    @SubscribeEvent
    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.MINI_CROSSBOW.get());
            event.accept(ModItems.DART.get());
            event.accept(ModItems.EXPLOSIVE_DART.get());
            event.accept(ModItems.GRAPPLE_DART.get());
            // >>> NEW
            event.accept(ModItems.CONTENDER_PISTOL.get());
            event.accept(ModItems.CARTRIDGE.get());
            // <<< NEW
            event.accept(ModItems.BLOCK_MAGAZINE.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.SUNFLOWER_SEEDS.get());
            event.accept(ModItems.ROASTED_SEEDS.get());
            event.accept(ModItems.CRUSHED_SEEDS.get());
            event.accept(ModItems.SUNFLOWER_OIL.get());
            event.accept(ModItems.HARDENED_IRON.get());
        }
=======
/**
 * Предметы мода встают в креативе РЯДОМ СО СВОИМИ АНАЛОГАМИ (вкладка Combat):
 *  - мини-арбалет и Contender — сразу после ванильного арбалета;
 *  - осколок аметиста, дротики и патрон .44 — перед ракетами фейерверков
 *    (блок боеприпасов арбалетов);
 *  - аметистовая сабля — рядом с мечами (после незеритового);
 *  - блочный магазин — рядом с лошадиной бронёй.
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {

    private static final CreativeModeTab.TabVisibility VISIBLE =
            CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;

    @SubscribeEvent
    static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) {
            return;
        }
        // Арбалеты: ванильный -> мини-арбалет -> Contender .44.
        event.insertAfter(new ItemStack(Items.CROSSBOW),
                new ItemStack(ModItems.MINI_CROSSBOW.get()), VISIBLE);
        event.insertAfter(new ItemStack(ModItems.MINI_CROSSBOW.get()),
                new ItemStack(ModItems.CONTENDER_PISTOL.get()), VISIBLE);

        // Боеприпасы перед фейерверками: аметист, дротики, патрон.
        // Каждый insertBefore ставит предмет вплотную перед ракетой,
        // поэтому порядок вызовов = порядок в списке.
        event.insertBefore(new ItemStack(Items.FIREWORK_ROCKET),
                new ItemStack(Items.AMETHYST_SHARD), VISIBLE);
        event.insertBefore(new ItemStack(Items.FIREWORK_ROCKET),
                new ItemStack(ModItems.DART.get()), VISIBLE);
        event.insertBefore(new ItemStack(Items.FIREWORK_ROCKET),
                new ItemStack(ModItems.EXPLOSIVE_DART.get()), VISIBLE);
        event.insertBefore(new ItemStack(Items.FIREWORK_ROCKET),
                new ItemStack(ModItems.GRAPPLE_DART.get()), VISIBLE);
        event.insertBefore(new ItemStack(Items.FIREWORK_ROCKET),
                new ItemStack(ModItems.CARTRIDGE.get()), VISIBLE);

        // Сабля — в ряду мечей, после незеритового.
        event.insertAfter(new ItemStack(Items.NETHERITE_SWORD),
                new ItemStack(ModItems.AMETHYST_SABER.get()), VISIBLE);

        // Блочный магазин — рядом с лошадиной бронёй.
        event.insertAfter(new ItemStack(Items.DIAMOND_HORSE_ARMOR),
                new ItemStack(ModItems.BLOCK_MAGAZINE.get()), VISIBLE);
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
    }

    private ModCreativeTabs() {}
}
