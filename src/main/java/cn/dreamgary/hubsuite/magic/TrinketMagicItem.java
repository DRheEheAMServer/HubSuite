package cn.dreamgary.hubsuite.magic;

import cn.dreamgary.hubsuite.trinket.TrinketStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 虚月饰品通用物品类：支持双面宝石镶嵌、副手镶嵌与潜行右键快捷穿戴。
 */
public class TrinketMagicItem extends Item {

    private final boolean moonFaction;

    public TrinketMagicItem(Properties properties, boolean moonFaction) {
        super(properties);
        this.moonFaction = moonFaction;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        MagicEnergyData.emptyArtifact().writeTo(stack);
        return stack;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack artifact, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) {
            return false;
        }
        if (MagicSocketing.trySocketFaction(artifact, other, moonFaction, player, true)) {
            slot.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack artifact, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) {
            return false;
        }
        if (MagicSocketing.trySocketFaction(artifact, slot.getItem(), moonFaction, player, true)) {
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
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        InteractionHand offHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = sp.getItemInHand(offHand);
        if (MagicSocketing.trySocketFaction(stack, other, moonFaction, sp, true)) {
            return InteractionResult.SUCCESS;
        }
        if (sp.isShiftKeyDown() && TrinketStorage.quickEquip(sp, stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
