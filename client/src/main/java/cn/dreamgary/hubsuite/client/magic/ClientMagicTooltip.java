package cn.dreamgary.hubsuite.client.magic;

import cn.dreamgary.hubsuite.magic.MagicIds;
import cn.dreamgary.hubsuite.magic.MagicTooltip;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * 客户端魔法载体与法器的 Tooltip 渲染。
 */
public final class ClientMagicTooltip {

    private ClientMagicTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!MagicIds.ALL_ITEMS.contains(id)) {
                return;
            }
            var mc = net.minecraft.client.Minecraft.getInstance();
            var window = mc.getWindow();
            boolean shift = mc.options.keyShift.isDown()
                    || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,
                    org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                    || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,
                    org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
            lines.addAll(MagicTooltip.build(id, stack, shift));
        });
    }
}
