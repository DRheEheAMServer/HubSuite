package cn.dreamgary.hubsuite.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法载体与法器的 Tooltip 生成（纯文本拼装，客户端调用）。
 */
public final class MagicTooltip {

    private MagicTooltip() {
    }

    public static List<Component> build(Identifier itemId, ItemStack stack, boolean shift) {
        List<Component> lines = new ArrayList<>();
        MagicEnergyData data = MagicEnergyData.of(stack);

        if (MagicIds.MOON_CRYSTAL.equals(itemId)) {
            MagicEnergyData d = data != null ? data : MagicEnergyData.emptyMoonCrystal();
            lines.add(Component.literal("\u00A7b月光能: \u00A7f" + d.moon() + " / " + d.maxMoon()));
            if (d.overload() > 0) {
                lines.add(Component.literal("\u00A7c⚠ 日光过载: " + d.overload() + "%（满 100% 将碎裂）"));
            }
            if (shift) {
                lines.add(Component.literal("\u00A77放上月光祭坛在夜晚吸收月光能"));
                lines.add(Component.literal("\u00A78白天请收入封闭容器，露天暴晒会过载碎裂"));
            } else {
                lines.add(Component.literal("\u00A78按住 Shift 查看说明"));
            }
            return lines;
        }

        if (MagicIds.VOID_CORE.equals(itemId)) {
            MagicEnergyData d = data != null ? data : MagicEnergyData.emptyVoidCore();
            lines.add(Component.literal("\u00A75虚空能: \u00A7f" + d.voidEnergy() + " / " + d.maxVoid()));
            if (shift) {
                lines.add(Component.literal("\u00A77右键 Y<=-48 处成型的蚀空凝晶直接吸取能量"));
                lines.add(Component.literal("\u00A78白天露天暴露会导致虚空能迅速流失"));
            } else {
                lines.add(Component.literal("\u00A78按住 Shift 查看说明"));
            }
            return lines;
        }

        if (MagicIds.MOONWING_STAFF.equals(itemId) || MagicIds.VOID_CHARM.equals(itemId)) {
            boolean isStaff = MagicIds.MOONWING_STAFF.equals(itemId);
            MagicEnergyData d = data != null ? data : MagicEnergyData.emptyArtifact();
            if (!d.hasGem()) {
                lines.add(Component.literal("\u00A77镶嵌槽: \u00A78空（未镶嵌"
                        + (isStaff ? "月光水晶" : "虚空晶核") + "）"));
            } else if (d.isMoonGem()) {
                lines.add(Component.literal("\u00A77镶嵌: \u00A7b月光水晶 \u00A7f("
                        + d.moon() + " / " + d.maxMoon() + ")"));
                if (d.overload() > 0) {
                    lines.add(Component.literal("\u00A7c⚠ 宝石过载: " + d.overload() + "%（满 100% 仅宝石碎裂）"));
                }
            } else {
                lines.add(Component.literal("\u00A77镶嵌: \u00A75虚空晶核 \u00A7f("
                        + d.voidEnergy() + " / " + d.maxVoid() + ")"));
            }
            if (shift) {
                if (isStaff) {
                    lines.add(Component.literal("\u00A77右键消耗 40 月光能：轻跃 + 6 秒缓降"));
                    lines.add(Component.literal("\u00A78背包中可用月光水晶直接点击镶嵌；白天暴晒仅碎宝石"));
                } else {
                    lines.add(Component.literal("\u00A77被动：摔落前消耗 80 虚空能免伤；右键消耗 20 吸附掉落物"));
                    lines.add(Component.literal("\u00A78背包中可用虚空晶核直接点击镶嵌；白天露天将流失能量"));
                }
            } else {
                lines.add(Component.literal("\u00A78按住 Shift 查看说明"));
            }
            return lines;
        }

        return lines;
    }
}
