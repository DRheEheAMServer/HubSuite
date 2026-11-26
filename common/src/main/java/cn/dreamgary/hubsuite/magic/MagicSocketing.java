package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 虚月律法器/饰品与宝石的通用镶嵌逻辑（双端共用，支持月能结晶、炽月晶与虚空核心）。
 */
public final class MagicSocketing {

    private MagicSocketing() {
    }

    /** 尝试把 {@code gemStack} 镶嵌进只接受特定宝石 ID 的物品（兼容旧签名）。 */
    public static boolean trySocket(ItemStack artifact, ItemStack gemStack,
                                    Identifier expectedGemId, String gemType,
                                    Player player, boolean mutateDropOld) {
        if (MagicIds.VOID_CORE.equals(expectedGemId)) {
            return trySocketFaction(artifact, gemStack, false, player, mutateDropOld);
        }
        return trySocketFaction(artifact, gemStack, true, player, mutateDropOld);
    }

    /**
     * 按派系镶嵌宝石：
     * <ul>
     *   <li>{@code moonFaction = true}：接受 {@code hubsuite:moon_crystal} 与 {@code hubsuite:blazing_moon_crystal}；</li>
     *   <li>{@code moonFaction = false}：接受 {@code hubsuite:void_core}。</li>
     * </ul>
     */
    public static boolean trySocketFaction(ItemStack artifact, ItemStack gemStack,
                                           boolean moonFaction, Player player, boolean mutateDropOld) {
        if (artifact == null || artifact.isEmpty() || gemStack == null || gemStack.isEmpty()) {
            return false;
        }
        Identifier actualGemId = BuiltInRegistries.ITEM.getKey(gemStack.getItem());
        String targetGemType;
        if (moonFaction) {
            if (MagicIds.BLAZING_MOON_CRYSTAL.equals(actualGemId)) {
                targetGemType = MagicEnergyData.GEM_BLAZING_MOON;
            } else if (MagicIds.MOON_CRYSTAL.equals(actualGemId)) {
                targetGemType = MagicEnergyData.GEM_MOON;
            } else {
                return false;
            }
        } else {
            if (!MagicIds.VOID_CORE.equals(actualGemId)) {
                return false;
            }
            targetGemType = MagicEnergyData.GEM_VOID;
        }

        MagicEnergyData gemData = MagicEnergyData.of(gemStack);
        if (gemData == null) {
            gemData = MagicEnergyData.GEM_BLAZING_MOON.equals(targetGemType)
                    ? MagicEnergyData.emptyBlazingMoonCrystal()
                    : (moonFaction ? MagicEnergyData.emptyMoonCrystal() : MagicEnergyData.emptyVoidCore());
        }
        MagicEnergyData cur = MagicEnergyData.of(artifact);
        if (cur == null) {
            cur = MagicEnergyData.emptyArtifact();
        }

        if (mutateDropOld && player != null && !player.level().isClientSide() && cur.hasGem()) {
            if (moonFaction && cur.isMoonGem()) {
                Identifier oldId = cur.isBlazingMoon() ? MagicIds.BLAZING_MOON_CRYSTAL : MagicIds.MOON_CRYSTAL;
                Item oldItem = BuiltInRegistries.ITEM.getValue(oldId);
                ItemStack oldGem = new ItemStack(oldItem);
                MagicEnergyData base = cur.isBlazingMoon()
                        ? MagicEnergyData.emptyBlazingMoonCrystal()
                        : MagicEnergyData.emptyMoonCrystal();
                base.withMoon(cur.moon()).writeTo(oldGem);
                if (!player.getInventory().add(oldGem)) {
                    player.drop(oldGem, false);
                }
            } else if (!moonFaction && cur.isVoidGem()) {
                Item oldItem = BuiltInRegistries.ITEM.getValue(MagicIds.VOID_CORE);
                ItemStack oldGem = new ItemStack(oldItem);
                MagicEnergyData.emptyVoidCore().withVoid(cur.voidEnergy()).writeTo(oldGem);
                if (!player.getInventory().add(oldGem)) {
                    player.drop(oldGem, false);
                }
            }
        }

        int energy = moonFaction ? gemData.moon() : gemData.voidEnergy();
        int defaultCap = MagicEnergyData.GEM_BLAZING_MOON.equals(targetGemType)
                ? MagicEnergyData.BLAZING_CAPACITY
                : MagicEnergyData.DEFAULT_CAPACITY;
        int cap = moonFaction
                ? Math.max(defaultCap, gemData.maxMoon())
                : Math.max(defaultCap, gemData.maxVoid());

        cur.socketGem(targetGemType, energy, cap).writeTo(artifact);
        gemStack.shrink(1);

        if (player != null && !player.level().isClientSide()) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS,
                    0.7F, moonFaction ? 1.4F : 0.85F);
            String gemName = MagicEnergyData.GEM_BLAZING_MOON.equals(targetGemType) ? "炽月晶"
                    : (moonFaction ? "月光水晶" : "虚空核心");
            player.sendOverlayMessage(Component.literal(
                    (moonFaction ? "\u00A7b✦ 已镶嵌 " : "\u00A75✦ 已镶嵌 ")
                            + gemName + "（" + energy + "/" + cap + "）"));
        }
        return true;
    }
}
