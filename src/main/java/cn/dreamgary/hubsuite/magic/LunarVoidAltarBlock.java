package cn.dreamgary.hubsuite.magic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 中心虚月祭坛方块：世界内摆/取主材，潜行空手右键或红石信号启动仪式。
 */
public class LunarVoidAltarBlock extends BaseEntityBlock {

    public LunarVoidAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(LunarVoidAltarBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LunarVoidAltarBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != MagicBlockEntities.LUNAR_VOID_ALTAR) {
            return null;
        }
        return (lvl, pos, st, be) -> LunarVoidAltarBlockEntity.serverTick(lvl, pos, st, (LunarVoidAltarBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return interact(level, pos, player, ItemStack.EMPTY);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player, stack);
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack handStack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)
                || !(level.getBlockEntity(pos) instanceof LunarVoidAltarBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!MagicScope.allowed(level)) {
            sp.sendSystemMessage(Component.literal("\u00A77虚月祭坛只能在经典空岛使用。"), true);
            return InteractionResult.CONSUME;
        }
        if (sp.isShiftKeyDown() && handStack.isEmpty()) {
            be.tryStart(sl, sp);
            return InteractionResult.CONSUME;
        }
        ItemStack current = be.held();
        if (!current.isEmpty()) {
            if (!sp.getInventory().add(current.copy())) {
                sp.drop(current.copy(), false);
            }
            be.setHeld(ItemStack.EMPTY);
            be.cancelRitual();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (handStack.isEmpty()) {
            sp.sendSystemMessage(Component.literal("\u00A77放上主材后，潜行空手右键或通入红石信号启动仪式。"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack one = handStack.split(1);
        be.setHeld(one);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.7F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level instanceof ServerLevel sl && sl.hasNeighborSignal(pos)
                && sl.getBlockEntity(pos) instanceof LunarVoidAltarBlockEntity be) {
            be.tryStart(sl, null);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof LunarVoidAltarBlockEntity be) {
            ItemStack held = be.held();
            if (!held.isEmpty()) {
                Containers.dropItemStack(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, held);
            }
            be.clearContent();
            be.clearDisplay(sl);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
