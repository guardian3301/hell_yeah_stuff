package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.item.AmethystSaberItem;
import com.example.hell_yeah_stuff.item.ContenderPistolItem;
import com.example.hell_yeah_stuff.item.DartItem;
import com.example.hell_yeah_stuff.item.ExplosiveDartItem;
import com.example.hell_yeah_stuff.item.GrappleDartItem;
import com.example.hell_yeah_stuff.item.MiniCrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
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

    // >>> NEW: аметистовая сабля — ближний бой с режущим (slash) уроном
    // по площади и собственной текстурой взмаха (частица hell_yeah_stuff:slash).
    // Аметистовый боезапас арбалета — ВАНИЛЬНЫЙ осколок аметиста (веер),
    // с зачарованием «Аметистовые гранаты» — осколок + обычная стрела.
    public static final DeferredItem<Item> AMETHYST_SABER = ITEMS.register("amethyst_saber",
            () -> new AmethystSaberItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.2F))
                    .rarity(Rarity.UNCOMMON)));
    // <<< NEW

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

    private ModItems() {}
}
