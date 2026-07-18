package com.example.hell_yeah_stuff.client;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.item.ContenderPistolItem;
import com.example.hell_yeah_stuff.registry.ModEnchantments;
import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModParticles;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = HellYeahStuffMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Same model predicates as the vanilla crossbow so the item
            // animates identically in hand (pulling frames + charged states).
            ItemProperties.register(ModItems.MINI_CROSSBOW.get(),
                    ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null) {
                            return 0.0F;
                        }
                        return CrossbowItem.isCharged(stack)
                                ? 0.0F
                                : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks())
                                        / (float) CrossbowItem.getChargeDuration(stack, entity);
                    });

            ItemProperties.register(ModItems.MINI_CROSSBOW.get(),
                    ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) ->
                            entity != null
                                    && entity.isUsingItem()
                                    && entity.getUseItem() == stack
                                    && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);

            ItemProperties.register(ModItems.MINI_CROSSBOW.get(),
                    ResourceLocation.withDefaultNamespace("charged"),
                    (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);

            // Custom predicate: 1.0 when an explosive dart is loaded.
            // Reads the modern CHARGED_PROJECTILES data component (new NBT API).
            ItemProperties.register(ModItems.MINI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "explosive"),
                    (stack, level, entity, seed) -> {
                        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
                        return charged != null && charged.contains(ModItems.EXPLOSIVE_DART.get()) ? 1.0F : 0.0F;
                    });

            // 1.0 when a grapple dart is loaded.
            ItemProperties.register(ModItems.MINI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "grapple"),
                    (stack, level, entity, seed) -> {
                        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
                        return charged != null && charged.contains(ModItems.GRAPPLE_DART.get()) ? 1.0F : 0.0F;
                    });

            // 1.0 when an amethyst shard is loaded.
            ItemProperties.register(ModItems.MINI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "amethyst"),
                    (stack, level, entity, seed) -> {
                        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
                        return charged != null && charged.contains(Items.AMETHYST_SHARD) ? 1.0F : 0.0F;
                    });

            // 1.0 когда на арбалете стоит зачарование «Аметистовые гранаты» —
            // вместе с "amethyst" даёт отдельную текстуру гранатного заряда.
            ItemProperties.register(ModItems.MINI_CROSSBOW.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "grenades"),
                    (stack, level, entity, seed) ->
                            ModEnchantments.level(stack, ModEnchantments.AMETHYST_GRENADES) > 0 ? 1.0F : 0.0F);

            // 1.0 когда пистолет заряжен патроном (для будущей модели/анимации).
            ItemProperties.register(ModItems.CONTENDER_PISTOL.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "loaded"),
                    (stack, level, entity, seed) -> {
                        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
                        return charged != null && !charged.isEmpty() ? 1.0F : 0.0F;
                    });

            // >>> NEW: перезарядка пистолета как у арбалета.
            // 1.0 пока игрок удерживает ПКМ и идёт перезарядка.
            ItemProperties.register(ModItems.CONTENDER_PISTOL.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "reloading"),
                    (stack, level, entity, seed) ->
                            entity != null
                                    && entity.isUsingItem()
                                    && entity.getUseItem() == stack ? 1.0F : 0.0F);

            // Прогресс перезарядки 0..1 (аналог "pull" у арбалета) —
            // по нему сменяются кадры перелома ствола.
            ItemProperties.register(ModItems.CONTENDER_PISTOL.get(),
                    ResourceLocation.fromNamespaceAndPath(HellYeahStuffMod.MODID, "reload"),
                    (stack, level, entity, seed) -> {
                        if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) {
                            return 0.0F;
                        }
                        float elapsed = stack.getUseDuration(entity) - entity.getUseItemRemainingTicks();
                        return Math.min(1.0F, elapsed / (float) ContenderPistolItem.RELOAD_TICKS);
                    });
            // <<< NEW
        });
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DART.get(), DartRenderer::new);
        event.registerEntityRenderer(ModEntities.EXPLOSIVE_DART.get(), ExplosiveDartRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAPPLE_DART.get(), GrappleDartRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new); // >>> NEW
        event.registerEntityRenderer(ModEntities.AMETHYST_SHARD.get(), AmethystShardRenderer::new);
        event.registerEntityRenderer(ModEntities.AMETHYST_GRENADE.get(), AmethystGrenadeRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        // Взмахи алебарды: ЛКМ, первый разрез ПКМ и второй разрез Judgment Cut —
        // разные текстуры, общий провайдер.
        event.registerSpriteSet(ModParticles.SLASH.get(), SlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SLASH_CUT.get(), SlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SLASH_JUDGMENT.get(), SlashParticle.Provider::new);
    }

    @SubscribeEvent
    static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        // Жёлтая плашка зарядов магазина над шкалой прочности мини-арбалета.
        event.register(ModItems.MINI_CROSSBOW.get(), new MagazineBarDecorator());
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        // Gives the mini crossbow the exact same first/third person hold and
        // charge animations as the vanilla crossbow.
        event.registerItem(new MiniCrossbowClientExtensions(), ModItems.MINI_CROSSBOW.get());
    }

    private ClientSetup() {}
}
