package cn.dreamgary.hubsuite.client.mixin;

import cn.dreamgary.hubsuite.client.ClientLavaFishing;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 客户端侧：耐岩浆的钩子泡在岩浆里时**不着火**（火焰是客户端渲染的）。
 *
 * <p>与服务端 {@code EntityLavaHurtMixin} 对称 —— 那边配在 {@code environment: server}，
 * 客户端加载不到，于是玩家看到自己的浮标在岩浆里烧着。
 *
 * <p>{@code Entity.lavaHurt()} 第一件事就是 {@code if (fireImmune()) return;}，
 * 所以这里把"我们的岩浆钩子"也算成免疫火焰：不点火就不会每 tick 抖实体数据，也没有额外同步包。
 * {@code FishingHook} 不是 {@code LivingEntity}，本来就不会被岩浆烧死，这里纯粹是火焰外观。
 */
@Mixin(Entity.class)
public abstract class ClientEntityLavaHurtMixin {

    @ModifyExpressionValue(method = "lavaHurt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;fireImmune()Z"))
    private boolean hubsuite$lavaHookFireImmune(boolean original) {
        if (original || !(((Object) this) instanceof FishingHook hook)) {
            return original;
        }
        return ClientLavaFishing.ownerRodLavaCapable(hook)
                && hook.level().getFluidState(hook.blockPosition()).is(FluidTags.LAVA);
    }
}
