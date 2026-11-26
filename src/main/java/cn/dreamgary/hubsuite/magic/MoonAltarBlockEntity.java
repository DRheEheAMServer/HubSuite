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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 月光祭坛方块实体：单格开放式台座，夜晚露天吸收月光能，白天露天触发暴露惩罚。
 */
public class MoonAltarBlockEntity extends BlockEntity implements WorldlyContainer {

    private static final int[] NO_SLOTS = new int[0];
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private UUID displayUuid;
    private int tickCount;

    public MoonAltarBlockEntity(BlockPos pos, BlockState state) {
        super(MagicBlockEntities.MOON_ALTAR, pos, state);
    }

    public ItemStack held() {
        return items.get(0);
    }

    public void setHeld(ItemStack stack) {
        items.set(0, stack == null ? ItemStack.EMPTY : stack);
        setChanged();
        if (level instanceof ServerLevel sl) {
            displayUuid = AltarDisplayHelper.sync(sl, worldPosition, displayUuid, held(), 1.2);
        }
    }

    public void clearDisplay(ServerLevel sl) {
        AltarDisplayHelper.remove(sl, worldPosition, displayUuid);
        displayUuid = null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MoonAltarBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || !MagicScope.allowed(sl)) {
            return;
        }
        ItemStack held = be.held();
        if (!held.isEmpty() && sl.getGameTime() % 10L == 0L) {
            AltarDisplayHelper.tickSpin(sl, pos, be.displayUuid, sl.getGameTime());
        }
        if (++be.tickCount % 20 != 0) {
            return;
        }
        ItemStack stack = be.held();
        if (stack.isEmpty()) {
            return;
        }
        // 确保重启后展示实体恢复
        be.displayUuid = AltarDisplayHelper.sync(sl, pos, be.displayUuid, stack, 1.2);

        boolean canSeeSky = sl.canSeeSky(pos.above());
        if (!canSeeSky) {
            return;
        }
        if (sl.isBrightOutside()) {
            DaylightExposure.Result res = DaylightExposure.tickExposed(sl, pos, stack);
            if (res == DaylightExposure.Result.SHATTERED_ITEM) {
                be.setHeld(ItemStack.EMPTY);
            } else if (res == DaylightExposure.Result.UPDATED || res == DaylightExposure.Result.SHATTERED_GEM) {
                be.setHeld(stack);
            }
            return;
        }

        // 夜晚露天：给支持月光能的载体充能（炽月晶充能效率 ×1.5）
        MagicEnergyData data = MagicEnergyData.of(stack);
        if (data != null && data.isMoonGem() && data.moon() < data.maxMoon()) {
            double factor = MoonPhaseMath.moonFactor(sl.getMoonBrightness(pos));
            double mult = data.isBlazingMoon() ? 1.5 : 1.0;
            int gain = Math.max(1, (int) Math.round(20.0 * factor * mult));
            data.withMoon(data.moon() + gain).withOverload(0).writeTo(stack);
            be.setHeld(stack);
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                    2, 0.15, 0.15, 0.15, 0.01);
        }
    }

    // ---- WorldlyContainer（禁止漏斗） ----
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

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }
}
