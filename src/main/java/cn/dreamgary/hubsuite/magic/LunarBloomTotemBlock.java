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
 * 月华催生仪方块：摆在农田或树场旁，夜晚消耗月能催熟周围作物与树苗。
 */
public class LunarBloomTotemBlock extends BaseEntityBlock {

    public LunarBloomTotemBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(LunarBloomTotemBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LunarBloomTotemBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != MagicBlockEntities.LUNAR_BLOOM_TOTEM) {
            return null;
        }
        return (lvl, pos, st, be) -> LunarBloomTotemBlockEntity.serverTick(lvl, pos, st, (LunarBloomTotemBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return interact(level, pos, player, ItemStack.EMPTY);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return interact(level, pos, player, stack);
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack handStack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof LunarBloomTotemBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!MagicScope.allowed(level)) {
            sp.sendSystemMessage(Component.literal("\u00A77月华催生仪只能在经典空岛使用。"), true);
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
            sp.sendSystemMessage(Component.literal("\u00A77请放入充好能的月光水晶或炽月晶，夜晚自动催熟周围作物。"), true);
            return InteractionResult.CONSUME;
        }
        MagicEnergyData data = MagicEnergyData.of(handStack);
        if (data == null || !data.isMoonGem()) {
            sp.sendSystemMessage(Component.literal("\u00A7c月华催生仪只接受含月光能的晶体。"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack one = sp.hasInfiniteMaterials() ? handStack.copyWithCount(1) : handStack.split(1);
        be.setHeld(one);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.7F, 1.3F);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof LunarBloomTotemBlockEntity be) {
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
