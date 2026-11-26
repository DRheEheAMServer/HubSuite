package cn.dreamgary.hubsuite.trinket;

import cn.dreamgary.hubsuite.ui.UiLayout;
import cn.dreamgary.hubsuite.ui.UiLayouts;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Curios 风格 5 格饰品栏菜单（双端共用，走通用 {@code UiLayouts.TRINKETS} 渲染）。
 */
public class TrinketMenu extends AbstractContainerMenu {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("hubsuite", "trinkets");
    public static final UiLayout LAYOUT = UiLayouts.TRINKETS;

    public static MenuType<TrinketMenu> registerType() {
        return Registry.register(BuiltInRegistries.MENU, ID,
                new MenuType<>(TrinketMenu::new, FeatureFlags.DEFAULT_FLAGS));
    }

    @SuppressWarnings("unchecked")
    private static MenuType<TrinketMenu> menuType() {
        return (MenuType<TrinketMenu>) BuiltInRegistries.MENU.getValue(ID);
    }

    private final Container trinkets;

    /** 客户端构造：内容由服务端同步。 */
    public TrinketMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TrinketSlotType.SLOT_COUNT));
    }

    public TrinketMenu(int containerId, Inventory playerInventory, Container trinkets) {
        super(menuType(), containerId);
        this.trinkets = trinkets;

        for (TrinketSlotType type : TrinketSlotType.values()) {
            UiLayout.SlotFrame f = LAYOUT.frames().get(type.index());
            addSlot(new Slot(trinkets, type.index(), f.x(), f.y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return TrinketDefs.accepts(type, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        // 玩家背包（与 LAYOUT 高度 166 对齐：主背包 y=84，快捷栏 y=142）
        int invTop = LAYOUT.height() - 82;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, invTop + row * 18));
            }
        }
        int hotbarTop = invTop + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarTop));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return trinkets.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int trinketCount = TrinketSlotType.SLOT_COUNT;
        if (index < trinketCount) {
            if (!moveItemStackTo(stack, trinketCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!TrinketDefs.isTrinket(stack) || !moveItemStackTo(stack, 0, trinketCount, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
