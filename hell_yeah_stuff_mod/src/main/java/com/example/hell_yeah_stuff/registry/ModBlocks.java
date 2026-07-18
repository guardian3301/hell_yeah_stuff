package com.example.hell_yeah_stuff.registry;

import com.example.hell_yeah_stuff.HellYeahStuffMod;
import com.example.hell_yeah_stuff.block.OilCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(HellYeahStuffMod.MODID);

    /** Котёл с подсолнечным маслом (появляется при заливке масла в котёл). */
    public static final DeferredBlock<Block> OIL_CAULDRON = BLOCKS.register("oil_cauldron",
            () -> new OilCauldronBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON)));

    private ModBlocks() {}
}
