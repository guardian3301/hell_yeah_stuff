package com.example.hell_yeah_stuff;

<<<<<<< HEAD
import com.example.hell_yeah_stuff.registry.ModBlocks;
import com.example.hell_yeah_stuff.registry.ModDataComponents;
import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModFeatures;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModSounds;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
=======
import com.example.hell_yeah_stuff.registry.ModEntities;
import com.example.hell_yeah_stuff.registry.ModFeatures;
import com.example.hell_yeah_stuff.registry.ModItems;
import com.example.hell_yeah_stuff.registry.ModParticles;
import com.example.hell_yeah_stuff.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)

@Mod(HellYeahStuffMod.MODID)
public class HellYeahStuffMod {
    public static final String MODID = "hell_yeah_stuff";

    public HellYeahStuffMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        // ModCreativeTabs регистрируется сам через @EventBusSubscriber —
        // он добавляет предметы в ванильную вкладку Combat, отдельного
        // DeferredRegister у него нет.
        ModSounds.SOUND_EVENTS.register(modEventBus); // >>> NEW: кастомные звуки
        ModFeatures.FEATURES.register(modEventBus); // ель с платформой (worldgen)
<<<<<<< HEAD
        ModBlocks.BLOCKS.register(modEventBus); // котёл с маслом
        ModDataComponents.DATA_COMPONENTS.register(modEventBus); // таймер остывания
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Наливание подсолнечного масла в пустой ванильный котёл —
            // так же, как наливается вода из бутылки.
            CauldronInteraction.EMPTY.map().put(ModItems.SUNFLOWER_OIL.get(),
                    (state, level, pos, player, hand, stack) -> {
                        if (!level.isClientSide) {
                            level.setBlockAndUpdate(pos, ModBlocks.OIL_CAULDRON.get().defaultBlockState());
                            player.setItemInHand(hand, ItemUtils.createFilledResult(
                                    stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY,
                                    SoundSource.BLOCKS, 1.0f, 1.0f);
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    });
        });
=======
        ModParticles.PARTICLE_TYPES.register(modEventBus); // взмах аметистовой сабли
>>>>>>> 6220b5c (аметистовое обновление смотрите updatelog)
    }
}
