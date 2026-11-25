package cn.dreamgary.hubsuite.client;

import cn.dreamgary.hubsuite.net.UiActionPayload;
import cn.dreamgary.hubsuite.trinket.TrinketMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Curios 风格饰品栏客户端入口：快捷键 {@code B} 打开虚月饰品栏。
 */
public final class ClientTrinketUi {

    private static KeyMapping openKey;

    private ClientTrinketUi() {
    }

    public static void register() {
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hubsuite.trinkets",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KeyMapping.Category.INVENTORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.screen != null) {
                return;
            }
            while (openKey.consumeClick()) {
                sendOpen();
            }
        });
    }

    public static void sendOpen() {
        if (ClientPlayNetworking.canSend(UiActionPayload.UiAction.TYPE)) {
            ClientPlayNetworking.send(new UiActionPayload.UiAction(TrinketMenu.ID.toString(), "open"));
        }
    }
}
