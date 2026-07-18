package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
<<<<<<< HEAD
=======
import com.example.hell_yeah_stuff.item.AmethystSaberItem;
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
import com.example.hell_yeah_stuff.item.ContenderPistolItem;
import com.example.hell_yeah_stuff.item.DartItem;
import com.example.hell_yeah_stuff.item.ExplosiveDartItem;
import com.example.hell_yeah_stuff.item.GrappleDartItem;
import com.example.hell_yeah_stuff.item.MiniCrossbowItem;
<<<<<<< HEAD
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
=======
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HellYeahStuffMod.MODID);

    public static final DeferredItem<Item> MINI_CROSSBOW = ITEMS.register("mini_crossbow",
            () -> new MiniCrossbowItem(new Item.Properties().stacksTo(1).durability(326)));

    public static final DeferredItem<Item> DART = ITEMS.register("dart",
            () -> new DartItem(new Item.Properties()));

    public static final DeferredItem<Item> EXPLOSIVE_DART = ITEMS.register("explosive_dart",
            () -> new ExplosiveDartItem(new Item.Properties()));

    public static final DeferredItem<Item> GRAPPLE_DART = ITEMS.register("grapple_dart",
            () -> new GrappleDartItem(new Item.Properties()));

<<<<<<< HEAD
=======
    // >>> NEW: аметистовая сабля — ближний бой с режущим (slash) уроном
    // по площади и собственной текстурой взмаха (частица hell_yeah_stuff:slash).
    // Аметистовый боезапас арбалета — ВАНИЛЬНЫЙ осколок аметиста (веер),
    // с зачарованием «Аметистовые гранаты» — осколок + обычная стрела.
    public static final DeferredItem<Item> AMETHYST_SABER = ITEMS.register("amethyst_saber",
            () -> new AmethystSaberItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.2F))
                    .rarity(Rarity.UNCOMMON)));
    // <<< NEW

>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
    // >>> NEW: однозарядный пистолет Contender .44 + патрон к нему
    // Редкость RARE — как у золотого яблока (имя предмета отображается аквой).
    public static final DeferredItem<Item> CONTENDER_PISTOL = ITEMS.register("contender_pistol",
            () -> new ContenderPistolItem(new Item.Properties().stacksTo(1).durability(384).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> CARTRIDGE = ITEMS.register("pistol_cartridge",
            () -> new Item(new Item.Properties()));
    // <<< NEW

    // Блочный магазин — пока просто предмет без функционала.
    public static final DeferredItem<Item> BLOCK_MAGAZINE = ITEMS.register("block_magazine",
            () -> new Item(new Item.Properties().stacksTo(16)));

<<<<<<< HEAD
    // >>> Цепочка подсолнечного масла
    // Семечки — крафтятся из ванильного подсолнуха (1 подсолнух -> 3 семечки).
    public static final DeferredItem<Item> SUNFLOWER_SEEDS = ITEMS.register("sunflower_seeds",
            () -> new Item(new Item.Properties()));

    // Жареные семечки — из печи; просто еда, без таймера.
    public static final DeferredItem<Item> ROASTED_SEEDS = ITEMS.register("roasted_seeds",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).fast().build())));

    // Раздавленные семечки — получаются падающей наковальней.
    public static final DeferredItem<Item> CRUSHED_SEEDS = ITEMS.register("crushed_seeds",
            () -> new Item(new Item.Properties()));

    // Подсолнечное масло — 3 раздавленных семечки + бутылка.
    public static final DeferredItem<Item> SUNFLOWER_OIL = ITEMS.register("sunflower_oil",
            () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));

    // Закалённое железо — горячее железо, закалённое в масле; для крафта дротиков.
    // (горячесть — теперь свойство ОБЫЧНОГО железного слитка, см. HotIronHandler)
    public static final DeferredItem<Item> HARDENED_IRON = ITEMS.register("hardened_iron",
            () -> new Item(new Item.Properties()));
    // <<<

=======
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
    private ModItems() {}
}
