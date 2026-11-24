package cn.dreamgary.hubsuite.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：同步抛出的 {@code FishingHook} 实体是否启用岩浆钓鱼。
 *
 * <p>由服务端结合抛竿手上的钓竿材质与当前维度的岩浆渔获表统一裁决后下发，
 * 避免客户端自行反推双手或跨维度时物理状态分叉导致浮标抽动。
 */
public final class LavaHookSyncPayload {

    public record LavaHookSync(int entityId, boolean lavaCapable) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<LavaHookSync> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("hubsuite", "lava_hook_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, LavaHookSync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, LavaHookSync::entityId,
                ByteBufCodecs.BOOL, LavaHookSync::lavaCapable,
                LavaHookSync::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private LavaHookSyncPayload() {
    }
}
