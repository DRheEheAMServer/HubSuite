package cn.dreamgary.hubsuite.magic;

import cn.dreamgary.hubsuite.trinket.TrinketEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 蚀空塑形杖：镶嵌虚空晶核后，副手持任意方块、主手右键凌空延伸搭桥（1 格 / 3 格模式）。
 */
public class VoidBuilderStaffItem extends Item {

    public static final int COST_PER_BLOCK = 5;
    private static final String TAG_RANGE = "hubsuite_build_range";

    public VoidBuilderStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        MagicEnergyData.emptyArtifact().writeTo(stack);
        return stack;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack staff, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) {
            return false;
        }
        if (MagicSocketing.trySocketFaction(staff, other, false, player, true)) {
            slot.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack staff, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) {
            return false;
        }
        if (MagicSocketing.trySocketFaction(staff, slot.getItem(), false, player, true)) {
            slot.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack staff = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        InteractionHand offHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack offStack = sp.getItemInHand(offHand);

        if (offStack.is(MagicRegistry.VOID_CORE)
                && MagicSocketing.trySocketFaction(staff, offStack, false, sp, true)) {
            return InteractionResult.SUCCESS;
        }

        if (sp.isShiftKeyDown() && offStack.isEmpty()) {
            int cur = rangeOf(staff);
            int next = cur == 3 ? 1 : 3;
            setRange(staff, next);
            sp.sendOverlayMessage(Component.literal("\u00A75✦ 蚀空塑形杖切换为：" + next + " 格延伸模式"));
            return InteractionResult.SUCCESS;
        }

        if (!MagicScope.allowed(sl)) {
            sp.sendSystemMessage(Component.literal("\u00A77蚀空塑形杖只能在经典空岛使用。"), true);
            return InteractionResult.FAIL;
        }

        MagicEnergyData data = MagicEnergyData.of(staff);
        if (data == null || !data.isVoidGem()) {
            sp.sendSystemMessage(Component.literal("\u00A7e塑形杖未镶嵌虚空核心（背包点击或副手拿虚空核心右键）。"), true);
            return InteractionResult.FAIL;
        }

        if (!(offStack.getItem() instanceof BlockItem blockItem)) {
            sp.sendSystemMessage(Component.literal("\u00A77请在另一只手拿着要凌空放置的方块（潜行空手右键可切换 1/3 格模式）。"), true);
            return InteractionResult.FAIL;
        }

        int range = rangeOf(staff);
        int perCost = TrinketEngine.scaleCost(sp, COST_PER_BLOCK);
        if (data.voidEnergy() < perCost) {
            sp.sendSystemMessage(Component.literal("\u00A7c虚空能不足（至少需要 " + perCost + "）。"), true);
            return InteractionResult.FAIL;
        }

        Direction dir = sp.getDirection();
        BlockPos start = sp.blockPosition().below().relative(dir);
        if (!sl.getBlockState(start).canBeReplaced()) {
            start = sp.blockPosition().relative(dir);
        }
        Block block = blockItem.getBlock();
        int placed = 0;
        int energy = data.voidEnergy();
        for (int i = 0; i < range; i++) {
            if (offStack.isEmpty() || energy < perCost) {
                break;
            }
            BlockPos target = start.relative(dir, i);
            if (!sl.isInWorldBounds(target) || !sl.getBlockState(target).canBeReplaced()) {
                continue;
            }
            sl.setBlock(target, block.defaultBlockState(), 3);
            sl.sendParticles(ParticleTypes.PORTAL,
                    target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                    6, 0.2, 0.2, 0.2, 0.02);
            if (!sp.isCreative()) {
                offStack.shrink(1);
            }
            energy -= perCost;
            placed++;
        }

        if (placed == 0) {
            sp.sendSystemMessage(Component.literal("\u00A77前方没有可凌空放置的空位。"), true);
            return InteractionResult.PASS;
        }
        data.withVoid(energy).writeTo(staff);
        sp.getCooldowns().addCooldown(staff, 6);
        sl.playSound(null, start, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
        return InteractionResult.SUCCESS;
    }

    private static int rangeOf(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return 1;
        }
        int v = cd.copyTag().getIntOr(TAG_RANGE, 1);
        return v == 3 ? 3 : 1;
    }

    private static void setRange(ItemStack stack, int range) {
        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = cd.copyTag();
        root.putInt(TAG_RANGE, range);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
