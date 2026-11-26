package cn.dreamgary.hubsuite.rod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 装配台的方块实体（仅用于方块实体锚点与旧存档遗留物品兼容；
 * 实现 {@link WorldlyContainer} 彻底禁止漏斗读写，防止刷竿或塞入非法物品）。
 */
public class AssemblyBlockEntity extends BlockEntity implements WorldlyContainer {

    /** 0-3 = 输入（竿 / 杆 / 线 / 钩）。成品格由菜单私有 {@code ResultContainer} 持有。 */
    public static final int INPUT_SIZE = 4;
    public static final int SIZE = INPUT_SIZE;
    private static final int[] NO_SLOTS = new int[0];

    private final NonNullList<ItemStack> items = NonNullList.withSize(INPUT_SIZE, ItemStack.EMPTY);

    public AssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(AssemblyBlockEntities.TYPE, pos, state);
    }

    // ------------------------------------------------------------------
    // WorldlyContainer（禁止漏斗交互）
    // ------------------------------------------------------------------

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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    // ------------------------------------------------------------------
    // Container
    // ------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return INPUT_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < INPUT_SIZE ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= INPUT_SIZE) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= INPUT_SIZE) {
            return ItemStack.EMPTY;
        }
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= INPUT_SIZE) {
            return;
        }
        items.set(slot, stack);
        setChanged();
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

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    // 26.1 起方块实体的存取走 ValueOutput / ValueInput（不再是 CompoundTag + RegistryAccess）
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        // 兼容旧存档（曾存 5 格）：只读取前 4 个输入格
        NonNullList<ItemStack> loaded = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, loaded);
        for (int i = 0; i < INPUT_SIZE; i++) {
            items.set(i, loaded.get(i));
        }
    }
}
