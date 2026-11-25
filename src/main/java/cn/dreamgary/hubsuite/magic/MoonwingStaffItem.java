package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 月翼法杖：镶嵌月光水晶后，消耗 40 月光能获得轻跃与 6 秒缓降。
 */
public class MoonwingStaffItem extends Item {

    public static final int USE_COST = 40;

    public MoonwingStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        MagicEnergyData.emptyArtifact().writeTo(stack);
        return stack;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack staff, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) {
            return false;
        }
        if (MagicSocketing.trySocketFaction(staff, other, true, player, true)) {
            slot.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack staff, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) {
            return false;
        }
        ItemStack other = slot.getItem();
        if (MagicSocketing.trySocketFaction(staff, other, true, player, true)) {
            slot.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        InteractionHand offHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherHand = sp.getItemInHand(offHand);
        if ((otherHand.is(MagicRegistry.MOON_CRYSTAL) || otherHand.is(MagicRegistry.BLAZING_MOON_CRYSTAL))
                && MagicSocketing.trySocketFaction(stack, otherHand, true, sp, true)) {
            return InteractionResult.SUCCESS;
        }
        if (!MagicScope.allowed(sl)) {
            sp.sendSystemMessage(Component.literal("\u00A77月翼法杖只能在经典空岛使用。"), true);
            return InteractionResult.FAIL;
        }
        MagicEnergyData data = MagicEnergyData.of(stack);
        if (data == null || !data.isMoonGem()) {
            sp.sendSystemMessage(Component.literal("\u00A7e法杖未镶嵌月光水晶（背包点击互相镶嵌，或副手拿月能结晶右键）。"), true);
            return InteractionResult.FAIL;
        }
        int cost = cn.dreamgary.hubsuite.trinket.TrinketEngine.scaleCost(sp, USE_COST);
        if (data.moon() < cost) {
            sp.sendSystemMessage(Component.literal("\u00A7c月光能不足（需要 " + cost + "）。"), true);
            return InteractionResult.FAIL;
        }
        data.withMoon(data.moon() - cost).writeTo(stack);
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, true));
        var delta = sp.getDeltaMovement();
        sp.setDeltaMovement(delta.x, Math.max(delta.y, 0.55), delta.z);
        sp.hurtMarked = true;
        sp.getCooldowns().addCooldown(stack, 60);
        sl.sendParticles(ParticleTypes.END_ROD,
                sp.getX(), sp.getY() + 0.5, sp.getZ(),
                12, 0.3, 0.2, 0.3, 0.05);
        sl.playSound(null, sp.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.9F, 1.3F);
        return InteractionResult.SUCCESS;
    }
}
