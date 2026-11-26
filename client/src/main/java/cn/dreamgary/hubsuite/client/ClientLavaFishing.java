package cn.dreamgary.hubsuite.client;

import cn.dreamgary.hubsuite.net.LavaHookSyncPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.world.entity.projectile.FishingHook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端侧的「这枚浮标是否启用岩浆钓鱼」同步状态表。
 *
 * <p>由服务端抛竿时通过 {@link LavaHookSyncPayload.LavaHookSync} 统一下发
 * （综合了抛竿手的钩子材质与当前维度的渔获表配置），客户端不再自行反推双手。
 */
public final class ClientLavaFishing {

    private static final Map<Integer, Boolean> SYNCED_HOOKS = new ConcurrentHashMap<>();

    private ClientLavaFishing() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(
                LavaHookSyncPayload.LavaHookSync.TYPE, LavaHookSyncPayload.LavaHookSync.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(LavaHookSyncPayload.LavaHookSync.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.lavaCapable()) {
                        SYNCED_HOOKS.put(payload.entityId(), Boolean.TRUE);
                    } else {
                        SYNCED_HOOKS.remove(payload.entityId());
                    }
                }));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof FishingHook) {
                SYNCED_HOOKS.remove(entity.getId());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SYNCED_HOOKS.clear());
    }

    /** 该浮标是否已由服务端同步为耐岩浆钓鱼状态。 */
    public static boolean ownerRodLavaCapable(FishingHook hook) {
        return hook != null && Boolean.TRUE.equals(SYNCED_HOOKS.get(hook.getId()));
    }
}
