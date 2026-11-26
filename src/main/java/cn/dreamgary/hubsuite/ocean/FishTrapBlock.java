package cn.dreamgary.hubsuite.ocean;

import cn.dreamgary.hubsuite.feature.FishingBait;
import cn.dreamgary.hubsuite.rod.RodScope;
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
 * 海岛蟹笼方块（方向 3）：放在水中缓慢被动捕获基础海产/沙砾，可放鱼饵加速。
 */
public class FishTrapBlock extends BaseEntityBlock {

    public FishTrapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(FishTrapBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FishTrapBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != OceanBlockIds.FISH_TRAP_BE) {
            return null;
        }
        return (lvl, pos, st, be) -> FishTrapBlockEntity.serverTick(lvl, pos, st, (FishTrapBlockEntity) be);
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
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof FishTrapBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!RodScope.allowed(level)) {
            sp.sendSystemMessage(Component.literal("§7海岛蟹笼只能在海岛服使用。"), true);
            return InteractionResult.CONSUME;
        }
        if (!handStack.isEmpty() && FishingBait.isBait(handStack) && !FishingBait.isNetherBait(handStack)) {
            if (be.baitCharges() >= FishTrapBlockEntity.MAX_BAIT_CHARGES) {
                sp.sendSystemMessage(Component.literal("§e蟹笼鱼饵充能已满（" + be.baitCharges() + "）。"), true);
                return InteractionResult.CONSUME;
            }
            if (!sp.hasInfiniteMaterials()) {
                handStack.shrink(1);
            }
            be.addBaitCharges(4);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7F, 1.1F);
            sp.sendSystemMessage(Component.literal("§a已放入鱼饵！当前蟹笼加速充能：" + be.baitCharges() + " 次"), true);
            return InteractionResult.CONSUME;
        }
        int collected = be.collectTo(sp);
        if (collected > 0) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7F, 1.1F);
            sp.sendSystemMessage(Component.literal("§b✦ 从蟹笼收取了 " + collected + " 个海产！"), true);
        } else {
            boolean inWater = FishTrapBlockEntity.hasWaterAround(level, pos);
            sp.sendSystemMessage(Component.literal(inWater
                    ? "§7蟹笼正在水下捕捞中…（加速充能：" + be.baitCharges() + "，可手持水域鱼饵右键放入）"
                    : "§e请将蟹笼放在水边或水下以开始自动捕捞！"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof FishTrapBlockEntity be) {
            Containers.dropContents(sl, pos, be);
            be.clearContent();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
