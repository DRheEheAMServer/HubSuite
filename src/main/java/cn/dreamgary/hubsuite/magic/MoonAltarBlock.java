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
 * 月光祭坛方块：世界内右键摆/取月光能载体。
 */
public class MoonAltarBlock extends BaseEntityBlock {

    public MoonAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MoonAltarBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MoonAltarBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != MagicBlockEntities.MOON_ALTAR) {
            return null;
        }
        return (lvl, pos, st, be) -> MoonAltarBlockEntity.serverTick(lvl, pos, st, (MoonAltarBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return interact(level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player, hand, stack);
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player,
                                              InteractionHand hand, ItemStack handStack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof MoonAltarBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!MagicScope.allowed(level)) {
            sp.sendSystemMessage(Component.literal("\u00A77月光祭坛只能在经典空岛使用。"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack current = be.held();
        if (!current.isEmpty()) {
            ItemStack out = current.copy();
            MagicEnergyData curData = MagicEnergyData.of(out);
            if (out.is(MagicRegistry.MOON_CRYSTAL) && curData != null
                    && curData.overload() >= 75 && curData.overload() < MagicEnergyData.OVERLOAD_LIMIT) {
                ItemStack blazing = new ItemStack(MagicRegistry.BLAZING_MOON_CRYSTAL);
                MagicEnergyData.emptyBlazingMoonCrystal().withMoon(curData.moon()).writeTo(blazing);
                out = blazing;
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.3F);
                sp.sendSystemMessage(Component.literal("\u00A76✦ 过载淬炼成功！月光水晶升华为「炽月晶」！"), true);
            } else {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            if (!sp.getInventory().add(out)) {
                sp.drop(out, false);
            }
            be.setHeld(ItemStack.EMPTY);
            return InteractionResult.CONSUME;
        }
        if (handStack.isEmpty()) {
            sp.sendSystemMessage(Component.literal("\u00A77手持月光水晶或已镶嵌月光宝石的法器右键放上祭坛。"), true);
            return InteractionResult.CONSUME;
        }
        MagicEnergyData data = MagicEnergyData.of(handStack);
        if (data == null || !data.isMoonGem()) {
            sp.sendSystemMessage(Component.literal("\u00A7c月光祭坛只能承托含月光能的载体或法器。"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack one = handStack.split(1);
        be.setHeld(one);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.7F, 1.2F);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof MoonAltarBlockEntity be) {
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
