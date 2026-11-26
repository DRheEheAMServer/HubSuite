package cn.dreamgary.hubsuite.ocean;

import cn.dreamgary.hubsuite.rod.RodScope;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 海岛蟹笼方块实体：9 格缓存，水中每 600 tick（放鱼饵后 300 tick）被动捕获基础海产/沙砾。
 */
public class FishTrapBlockEntity extends BlockEntity implements WorldlyContainer {

    public static final int MAX_BAIT_CHARGES = 16;
    private static final int BASE_INTERVAL = 600;
    private static final int BAITED_INTERVAL = 300;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private int progressTicks;
    private int baitCharges;

    public FishTrapBlockEntity(BlockPos pos, BlockState state) {
        super(OceanBlockIds.FISH_TRAP_BE, pos, state);
    }

    public int baitCharges() {
        return baitCharges;
    }

    public void addBaitCharges(int delta) {
        baitCharges = Math.max(0, Math.min(MAX_BAIT_CHARGES, baitCharges + delta));
        setChanged();
    }

    public static boolean hasWaterAround(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.WATER)
                || level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FishTrapBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || !RodScope.allowed(sl)) {
            return;
        }
        if (sl.getGameTime() % 20L != 0L || !hasWaterAround(sl, pos)) {
            return;
        }
        int interval = be.baitCharges > 0 ? BAITED_INTERVAL : BASE_INTERVAL;
        be.progressTicks += 20;
        if (be.progressTicks < interval) {
            return;
        }
        be.progressTicks = 0;
        ItemStack caught = rollTrapLoot(sl.getRandom(), be.baitCharges > 0);
        if (be.insert(caught) && be.baitCharges > 0) {
            be.baitCharges--;
        }
        be.setChanged();
    }

    private static ItemStack rollTrapLoot(RandomSource rand, boolean baited) {
        int roll = rand.nextInt(100);
        if (baited && roll < 8) {
            Item butterfly = BuiltInRegistries.ITEM.getValue(OceanFishDefs.CORAL_BUTTERFLYFISH.id());
            if (butterfly != Items.AIR) {
                return new ItemStack(butterfly, 1);
            }
        }
        if (roll < 28) {
            return new ItemStack(Items.COD, 1);
        }
        if (roll < 44) {
            return new ItemStack(Items.SALMON, 1);
        }
        if (roll < 62) {
            return new ItemStack(Items.KELP, 1 + rand.nextInt(3));
        }
        if (roll < 76) {
            return new ItemStack(Items.SAND, 1 + rand.nextInt(2));
        }
        if (roll < 88) {
            return new ItemStack(Items.GRAVEL, 1 + rand.nextInt(2));
        }
        if (roll < 95) {
            return new ItemStack(Items.CLAY_BALL, 1 + rand.nextInt(2));
        }
        if (roll < 99) {
            return new ItemStack(Items.PRISMARINE_SHARD, 1);
        }
        Item bait = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("hubsuite", "basic_bait"));
        return new ItemStack(bait != Items.AIR ? bait : Items.NAUTILUS_SHELL, 1);
    }

    private boolean insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack remaining = stack.copy();
        for (int i = 0; i < items.size(); i++) {
            ItemStack cur = items.get(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, remaining)
                    && cur.getCount() < cur.getMaxStackSize()) {
                int add = Math.min(remaining.getCount(), cur.getMaxStackSize() - cur.getCount());
                cur.grow(add);
                remaining.shrink(add);
                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, remaining);
                return true;
            }
        }
        return remaining.getCount() < stack.getCount();
    }

    public int collectTo(ServerPlayer player) {
        int total = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) {
                continue;
            }
            total += s.getCount();
            if (!player.getInventory().add(s.copy())) {
                player.drop(s.copy(), false);
            }
            items.set(i, ItemStack.EMPTY);
        }
        if (total > 0) {
            setChanged();
        }
        return total;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS : new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return dir == Direction.DOWN;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack == null ? ItemStack.EMPTY : stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BaitCharges", baitCharges);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        baitCharges = input.getIntOr("BaitCharges", 0);
    }
}
