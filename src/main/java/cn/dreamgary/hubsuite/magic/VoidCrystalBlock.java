package cn.dreamgary.hubsuite.magic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 蚀空凝晶：低空虚空侵蚀产物。不可破坏，只能使用虚空晶核吸取能量后直接消失。
 */
public class VoidCrystalBlock extends BaseEntityBlock {

    public VoidCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(VoidCrystalBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VoidCrystalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return absorb(level, pos, player, player.getMainHandItem());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return absorb(level, pos, player, stack);
    }

    private static InteractionResult absorb(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof VoidCrystalBlockEntity crystal)) {
            return InteractionResult.PASS;
        }
        int stored = crystal.storedVoid();
        if (!stack.is(MagicRegistry.VOID_CORE)) {
            sp.sendSystemMessage(Component.literal(
                    "\u00A75蚀空凝晶（蕴含 " + stored + " 虚空能）\u00A77—— 请用未满的虚空晶核右键吸取。"), true);
            return InteractionResult.CONSUME;
        }
        MagicEnergyData data = MagicEnergyData.of(stack);
        if (data == null) {
            data = MagicEnergyData.emptyVoidCore();
        }
        int room = data.maxVoid() - data.voidEnergy();
        if (room <= 0) {
            sp.sendSystemMessage(Component.literal("\u00A7e这颗虚空晶核已经满了。"), true);
            return InteractionResult.CONSUME;
        }
        int gained = Math.min(room, stored);
        data.withVoid(data.voidEnergy() + gained).writeTo(stack);

        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                16, 0.25, 0.25, 0.25, 0.04);
        serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.0F, 0.85F);
        // 吸走后方块直接消失
        serverLevel.removeBlock(pos, false);
        sp.sendSystemMessage(Component.literal(
                "\u00A75吸取了 " + gained + " 点虚空能（晶核："
                        + (data.voidEnergy() + gained) + "/" + data.maxVoid() + "）"), true);
        return InteractionResult.CONSUME;
    }
}
