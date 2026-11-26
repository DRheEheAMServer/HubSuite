package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 月华催生仪方块实体：夜晚消耗月能催熟周围 5×5（满月 7×7）的作物与树苗。
 */
public class LunarBloomTotemBlockEntity extends BlockEntity implements WorldlyContainer {

    public static final int GROW_COST = 15;
    private static final int[] NO_SLOTS = new int[0];
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private UUID displayUuid;
    private int tickCount;

    public LunarBloomTotemBlockEntity(BlockPos pos, BlockState state) {
        super(MagicBlockEntities.LUNAR_BLOOM_TOTEM, pos, state);
    }

    public ItemStack held() {
        return items.get(0);
    }

    public void setHeld(ItemStack stack) {
        items.set(0, stack == null ? ItemStack.EMPTY : stack);
        setChanged();
        if (level instanceof ServerLevel sl) {
            displayUuid = AltarDisplayHelper.sync(sl, worldPosition, displayUuid, held(), 1.25);
        }
    }

    public void clearDisplay(ServerLevel sl) {
        AltarDisplayHelper.remove(sl, worldPosition, displayUuid);
        displayUuid = null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LunarBloomTotemBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || !MagicScope.allowed(sl)) {
            return;
        }
        ItemStack stack = be.held();
        if (!stack.isEmpty() && sl.getGameTime() % 10L == 0L) {
            AltarDisplayHelper.tickSpin(sl, pos, be.displayUuid, sl.getGameTime());
        }
        if (++be.tickCount % 20 != 0 || stack.isEmpty()) {
            return;
        }
        be.displayUuid = AltarDisplayHelper.sync(sl, pos, be.displayUuid, stack, 1.25);

        if (DaylightExposure.isExposedDaylight(sl, pos.above())) {
            DaylightExposure.Result res = DaylightExposure.tickExposed(sl, pos, stack);
            if (res == DaylightExposure.Result.SHATTERED_ITEM) {
                be.setHeld(ItemStack.EMPTY);
            } else if (res == DaylightExposure.Result.UPDATED || res == DaylightExposure.Result.SHATTERED_GEM) {
                be.setHeld(stack);
            }
            return;
        }

        if (sl.isBrightOutside()) {
            return;
        }
        MagicEnergyData data = MagicEnergyData.of(stack);
        if (data == null || !data.isMoonGem() || data.moon() < GROW_COST) {
            return;
        }

        float bright = sl.getMoonBrightness(pos);
        boolean fullMoon = bright >= 0.99F;
        if (!fullMoon && be.tickCount % 40 != 0) {
            return;
        }

        int radius = fullMoon ? 3 : 2;
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(target);
                    if (st.getBlock() instanceof BonemealableBlock b
                            && b.isValidBonemealTarget(sl, target, st)
                            && b.isBonemealSuccess(sl, sl.getRandom(), target, st)) {
                        candidates.add(target);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        BlockPos chosen = candidates.get(sl.getRandom().nextInt(candidates.size()));
        BlockState chosenState = sl.getBlockState(chosen);
        if (chosenState.getBlock() instanceof BonemealableBlock b) {
            b.performBonemeal(sl, sl.getRandom(), chosen, chosenState);
            data.withMoon(data.moon() - GROW_COST).writeTo(stack);
            be.setHeld(stack);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    chosen.getX() + 0.5, chosen.getY() + 0.5, chosen.getZ() + 0.5,
                    6, 0.25, 0.25, 0.25, 0.02);
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5,
                    2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? items.get(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = slot == 0 ? ContainerHelper.removeItem(items, 0, amount) : ItemStack.EMPTY;
        if (!taken.isEmpty()) {
            setHeld(items.get(0));
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == 0 ? ContainerHelper.takeItem(items, 0) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            setHeld(stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.set(0, ItemStack.EMPTY);
        setChanged();
    }
}
