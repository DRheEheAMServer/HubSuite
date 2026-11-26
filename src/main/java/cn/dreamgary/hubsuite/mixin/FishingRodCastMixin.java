package cn.dreamgary.hubsuite.mixin;

import cn.dreamgary.hubsuite.feature.LavaFishingHook;
import cn.dreamgary.hubsuite.feature.item.HubSuiteItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 抛竿时给钩子打标记：这枚钩子耐不耐岩浆（看竿挂的钩）。 */
@Mixin(FishingRodItem.class)
public abstract class FishingRodCastMixin {

    /** 坏掉的竿不能抛（方案第 5 节：耐久 ≤ 0 → 不能钓）。 */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void hubsuite$blockBrokenRod(Level level, Player player, InteractionHand hand,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        cn.dreamgary.hubsuite.rod.RodData data = cn.dreamgary.hubsuite.rod.RodData.of(stack);
        if (data != null && data.broken()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00A7c这根竿已经坏掉了 —— 去装配台在杆位放一根同种材料修补，或换底。"));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "use", at = @At("TAIL"))
    private void hubsuite$markLavaRod(Level level, Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null) {
            return;
        }
        FishingHook hook = player.fishing;
        if (hook == null) {
            return;
        }
        // 能不能钓岩浆，看的是**这根竿挂的钩**（服务端记录钩子本身耐岩浆属性；同步客户端时再结合当前维度渔获表）
        cn.dreamgary.hubsuite.rod.RodData data =
                cn.dreamgary.hubsuite.rod.RodData.of(player.getItemInHand(hand));
        boolean rodLava = data != null && data.lavaCapable();
        ((LavaFishingHook) hook).hubsuite$setLavaCapable(rodLava);
        boolean clientLava = rodLava
                && cn.dreamgary.hubsuite.feature.FishingLoot.tableFor(
                        cn.dreamgary.hubsuite.HubSuite.fishing(), level.dimension()) != null;
        if (!level.isClientSide() && clientLava) {
            var pkt = new cn.dreamgary.hubsuite.net.LavaHookSyncPayload.LavaHookSync(hook.getId(), true);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp
                    && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(sp, pkt.type())) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, pkt);
            }
            for (var watcher : net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(hook)) {
                if (watcher != player
                        && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(watcher, pkt.type())) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(watcher, pkt);
                }
            }
        }
    }
}
