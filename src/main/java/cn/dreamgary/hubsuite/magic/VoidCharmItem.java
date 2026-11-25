package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 虚空护符：镶嵌虚空晶核后，主动右键吸附周围掉落物（20 点），被动消耗 80 点免疫摔落伤害。
 */
public class VoidCharmItem extends Item {

    public static final int MAGNET_COST = 20;
    public static final int FALL_SAVE_COST = 80;

    public VoidCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        MagicEnergyData.emptyArtifact().writeTo(stack);
        return stack;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack charm, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) {
            return false;
        }
        if (MagicSocketing.trySocket(charm, other, MagicIds.VOID_CORE, MagicEnergyData.GEM_VOID, player, true)) {
            slot.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack charm, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) {
            return false;
        }
        ItemStack other = slot.getItem();
        if (MagicSocketing.trySocket(charm, other, MagicIds.VOID_CORE, MagicEnergyData.GEM_VOID, player, true)) {
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
        if (otherHand.is(MagicRegistry.VOID_CORE)
                && MagicSocketing.trySocket(stack, otherHand, MagicIds.VOID_CORE, MagicEnergyData.GEM_VOID, sp, true)) {
            return InteractionResult.SUCCESS;
        }
        if (sp.isShiftKeyDown() && cn.dreamgary.hubsuite.trinket.TrinketStorage.quickEquip(sp, stack)) {
            return InteractionResult.SUCCESS;
        }
        if (!MagicScope.allowed(sl)) {
            sp.sendSystemMessage(Component.literal("\u00A77虚空护符只能在经典空岛使用。"), true);
            return InteractionResult.FAIL;
        }
        MagicEnergyData data = MagicEnergyData.of(stack);
        if (data == null || !data.isVoidGem()) {
            sp.sendSystemMessage(Component.literal("\u00A7e护符未镶嵌虚空晶核（背包点击互相镶嵌，或副手拿虚空核心右键）。"), true);
            return InteractionResult.FAIL;
        }
        if (data.voidEnergy() < MAGNET_COST) {
            sp.sendSystemMessage(Component.literal("\u00A7c虚空能不足（需要 " + MAGNET_COST + "）。"), true);
            return InteractionResult.FAIL;
        }
        AABB box = sp.getBoundingBox().inflate(10.0);
        int pulled = 0;
        for (ItemEntity item : sl.getEntities(EntityType.ITEM, box, ItemEntity::isAlive)) {
            item.teleportTo(sp.getX(), sp.getY() + 0.25, sp.getZ());
            item.setNoPickUpDelay();
            pulled++;
        }
        if (pulled == 0) {
            sp.sendSystemMessage(Component.literal("\u00A77周围 10 格内没有可吸附的掉落物。"), true);
            return InteractionResult.PASS;
        }
        data.withVoid(data.voidEnergy() - MAGNET_COST).writeTo(stack);
        sp.getCooldowns().addCooldown(stack, 20);
        sl.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 0.5, sp.getZ(),
                12, 0.3, 0.3, 0.3, 0.05);
        sl.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    /** 被动免疫摔落伤害。返回 false 表示取消伤害。 */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer sp) || !source.is(DamageTypes.FALL) || amount <= 0.0F) {
            return true;
        }
        if (!MagicScope.allowed(sp.level())) {
            return true;
        }
        int cost = cn.dreamgary.hubsuite.trinket.TrinketEngine.scaleCost(sp, FALL_SAVE_COST);
        var trinkets = cn.dreamgary.hubsuite.trinket.TrinketStorage.container(sp);
        ItemStack equipped = trinkets.getItem(cn.dreamgary.hubsuite.trinket.TrinketSlotType.AMULET.index());
        if (!equipped.isEmpty() && equipped.is(MagicRegistry.VOID_CHARM)) {
            MagicEnergyData d = MagicEnergyData.of(equipped);
            if (d != null && d.isVoidGem() && d.voidEnergy() >= cost) {
                d.withVoid(d.voidEnergy() - cost).writeTo(equipped);
                sp.resetFallDistance();
                sp.level().playSound(null, sp.blockPosition(),
                        SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8F, 1.3F);
                sp.sendSystemMessage(Component.literal("\u00A75✦ 虚空护符消耗 " + cost + " 虚空能为你抵消了摔落伤害。"), true);
                return false;
            }
        }
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(MagicRegistry.VOID_CHARM)) {
                continue;
            }
            MagicEnergyData d = MagicEnergyData.of(s);
            if (d != null && d.isVoidGem() && d.voidEnergy() >= cost) {
                d.withVoid(d.voidEnergy() - cost).writeTo(s);
                sp.resetFallDistance();
                sp.level().playSound(null, sp.blockPosition(),
                        SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8F, 1.3F);
                sp.sendSystemMessage(Component.literal("\u00A75✦ 虚空护符消耗 " + cost + " 虚空能为你抵消了摔落伤害。"), true);
                return false;
            }
        }
        return true;
    }
}
