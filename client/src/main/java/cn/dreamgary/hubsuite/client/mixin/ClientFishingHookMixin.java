package cn.dreamgary.hubsuite.client.mixin;

import cn.dreamgary.hubsuite.client.ClientLavaFishing;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端侧：耐岩浆的钩子泡在岩浆里时，"岩浆也算水"。
 *
 * <p><b>为什么必须补这一份：</b>服务端的 {@code FishingHookMixin} 做了同样的替换，但服务端
 * mixin 配在 {@code environment: server} 里，客户端加载不到。于是两端对同一个浮标跑出两套物理：
 * <ul>
 *   <li>服务端：进 {@code BOBBING} 状态，贴着岩浆面（原版"水面高度"那段）；</li>
 *   <li>客户端：仍在 {@code FLYING}，每 tick 走
 *       {@code if (!is(WATER) && !onGround && hookedIn == null) delta += (0, -0.03, 0)}；</li>
 * </ul>
 * 服务端每 tick 把浮标位置同步下来、客户端又自己往下拽 —— 画面上就是**上下抽动**。
 *
 * <p>注入点与服务端一致：{@code tick} 里有两处 {@code FluidState.is(FluidTags.WATER)}，
 * 两处都要改（一处算液面高度、一处加不加那 -0.03 的重力），所以不写 ordinal。
 *
 * <p>顺手在 TAIL 清一次火焰：{@code super.tick()} → {@code baseTick()} → {@code lavaHurt()}
 * 会点火，{@link ClientEntityLavaHurtMixin} 已经在源头挡住了，这里只是双保险
 * （与服务端 {@code FishingHookMixin#hubsuite$noFireInLava} 对称）。
 */
@Mixin(FishingHook.class)
public abstract class ClientFishingHookMixin {

    /**
     * 主人手里的竿钩耐不耐岩浆。每 tick 在 HEAD 算一次并缓存：
     * {@code RodData.of} 要 copyTag，放在两处 {@code is(...)} 里各调一次太浪费。
     * 只缓存"竿"这一半（不随浮标移动变化），"是否真在岩浆里"仍然实时判。
     */
    @Unique
    private boolean hubsuite$rodLavaCapable;

    @Inject(method = "tick", at = @At("HEAD"))
    private void hubsuite$cacheRodLavaCapable(CallbackInfo ci) {
        hubsuite$rodLavaCapable = ClientLavaFishing.ownerRodLavaCapable((FishingHook) (Object) this);
    }

    @ModifyExpressionValue(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean hubsuite$lavaCountsAsWater(boolean original) {
        if (original || !hubsuite$rodLavaCapable) {
            return original;
        }
        FishingHook self = (FishingHook) (Object) this;
        return self.level().getFluidState(self.blockPosition()).is(FluidTags.LAVA);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hubsuite$noFireInLava(CallbackInfo ci) {
        if (!hubsuite$rodLavaCapable) {
            return;
        }
        FishingHook self = (FishingHook) (Object) this;
        if (self.getRemainingFireTicks() > 0) {
            self.clearFire();
        }
    }
}
