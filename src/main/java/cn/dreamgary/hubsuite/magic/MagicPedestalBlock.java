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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 魔法基座方块：放在虚月祭坛周围 8 格位置，世界内右键摆/取配材或能量载体。
 */
public class MagicPedestalBlock extends BaseEntityBlock {

    public MagicPedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MagicPedestalBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicPedestalBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != MagicBlockEntities.MAGIC_PEDESTAL) {
            return null;
        }
        return (lvl, pos, st, be) -> MagicPedestalBlockEntity.serverTick(lvl, pos, st, (MagicPedestalBlockEntity) be);
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
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof MagicPedestalBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!MagicScope.allowed(level)) {
            sp.sendSystemMessage(Component.literal("\u00A77魔法基座只能在经典空岛使用。"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack current = be.held();
        if (!current.isEmpty()) {
            if (!sp.getInventory().add(current.copy())) {
                sp.drop(current.copy(), false);
            }
            be.setHeld(ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (handStack.isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemStack one = handStack.split(1);
        be.setHeld(one);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7F, 1.1F);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof MagicPedestalBlockEntity be) {
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
