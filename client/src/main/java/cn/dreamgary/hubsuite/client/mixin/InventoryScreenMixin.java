package cn.dreamgary.hubsuite.client.mixin;

import cn.dreamgary.hubsuite.client.ClientTrinketUi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Curios 风格背包角标按钮：在玩家背包界面左上角加一个「✦」小按钮，点击展开虚月饰品栏。
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void hubsuite$addTrinketButton(CallbackInfo ci) {
        addRenderableWidget(Button.builder(Component.literal("✦"), btn -> ClientTrinketUi.sendOpen())
                .bounds(leftPos + 26, topPos + 9, 14, 14)
                .build());
    }
}
