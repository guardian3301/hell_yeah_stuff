package com.example.hell_yeah_stuff.block;

import com.example.hell_yeah_stuff.event.HotIronHandler;
import com.example.hell_yeah_stuff.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Котёл с подсолнечным маслом.
 * LEVEL 1-3 — уровень масла (наливается бутылками, как вода вёдрами/бутылками).
 * PHASE 0-4 — фаза потемнения. Каждая закалка горячего железа в полном котле
 * затемняет масло; после пятой закалки масло портится и котёл пустеет.
 */
public class OilCauldronBlock extends Block {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 4);

    /** Форма ванильного котла (стенки без внутренности). */
    private static final VoxelShape INSIDE = box(2, 4, 2, 14, 16, 14);
    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.block(),
            Shapes.or(
                    box(0, 0, 3, 16, 3, 13),
                    box(3, 0, 0, 13, 3, 16),
                    box(2, 0, 2, 14, 3, 14),
                    INSIDE),
            BooleanOp.ONLY_FIRST);

    public OilCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LEVEL, 1)
                .setValue(PHASE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, PHASE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                             BlockPos pos) {
        return INSIDE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Доливаем масло (только пока оно свежее — фаза 0).
        if (stack.is(ModItems.SUNFLOWER_OIL.get())
                && state.getValue(LEVEL) < 3 && state.getValue(PHASE) == 0) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(LEVEL, state.getValue(LEVEL) + 1));
                player.setItemInHand(hand,
                        ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // Забираем свежее масло обратно в бутылку.
        if (stack.is(Items.GLASS_BOTTLE) && state.getValue(PHASE) == 0) {
            if (!level.isClientSide) {
                int newLevel = state.getValue(LEVEL) - 1;
                if (newLevel <= 0) {
                    level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(pos, state.setValue(LEVEL, newLevel));
                }
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player,
                        new ItemStack(ModItems.SUNFLOWER_OIL.get())));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Закалка: ГОРЯЧИЙ железный слиток (обычный, с неистёкшим таймером
        // температуры), брошенный в ПОЛНЫЙ котёл с маслом. Остывший слиток
        // закалить нельзя.
        if (level.isClientSide
                || !(entity instanceof ItemEntity itemEntity)
                || !HotIronHandler.isHot(itemEntity.getItem(), level)
                || state.getValue(LEVEL) < 3) {
            return;
        }
        ItemStack contents = itemEntity.getItem();

        // Закаливаем по одному слитку за раз.
        contents.shrink(1);
        if (contents.isEmpty()) {
            itemEntity.discard();
        }
        ItemEntity hardened = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                new ItemStack(ModItems.HARDENED_IRON.get()));
        hardened.setDefaultPickUpDelay();
        level.addFreshEntity(hardened);

        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.2f);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    8, 0.2, 0.1, 0.2, 0.01);
        }

        int phase = state.getValue(PHASE);
        if (phase >= 4) {
            // Пятая закалка: масло окончательно испортилось, котёл пустеет.
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 0.6f);
        } else {
            level.setBlockAndUpdate(pos, state.setValue(PHASE, phase + 1));
        }
    }
}
