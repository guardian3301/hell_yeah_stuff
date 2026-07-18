package com.example.hell_yeah_stuff.event;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Дробление семечек падающей наковальней. Проверка идёт каждый тик падения
 * наковальни (EntityTickEvent) — это надёжнее события установки блока,
 * которое для падающих блоков срабатывает не всегда: все брошенные
 * подсолнечные семечки в зоне под падающей наковальней превращаются
 * в раздавленные (1 к 1).
 */
@EventBusSubscriber(modid = HellYeahStuffMod.MODID)
public final class AnvilCrushHandler {

    @SubscribeEvent
    public static void onFallingBlockTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof FallingBlockEntity fallingBlock)
                || fallingBlock.level().isClientSide) {
            return;
        }
        if (!(fallingBlock.getBlockState().getBlock() instanceof AnvilBlock)) {
            return;
        }
        // Дробим только когда наковальня реально падает вниз.
        if (fallingBlock.getDeltaMovement().y >= -0.05) {
            return;
        }
        // Зона удара: хитбокс наковальни + до 1 блока ниже (куда она летит).
        AABB crushZone = fallingBlock.getBoundingBox()
                .expandTowards(0, -1.0, 0)
                .inflate(0.25, 0, 0.25);
        boolean crushed = false;
        for (ItemEntity itemEntity : fallingBlock.level()
                .getEntitiesOfClass(ItemEntity.class, crushZone)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(ModItems.SUNFLOWER_SEEDS.get())) {
                itemEntity.setItem(new ItemStack(ModItems.CRUSHED_SEEDS.get(), stack.getCount()));
                crushed = true;
            }
        }
        if (crushed) {
            BlockPos pos = fallingBlock.blockPosition();
            fallingBlock.level().playSound(null, pos, SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS, 0.5f, 1.4f);
        }
    }

    private AnvilCrushHandler() {}
}
