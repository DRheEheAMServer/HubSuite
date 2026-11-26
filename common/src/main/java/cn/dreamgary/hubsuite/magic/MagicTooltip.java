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

        if (MagicIds.MOON_CRYSTAL.equals(itemId) || MagicIds.BLAZING_MOON_CRYSTAL.equals(itemId)) {
            boolean blazing = MagicIds.BLAZING_MOON_CRYSTAL.equals(itemId);
            MagicEnergyData d = data != null ? data
                    : (blazing ? MagicEnergyData.emptyBlazingMoonCrystal() : MagicEnergyData.emptyMoonCrystal());
            lines.add(Component.literal("\u00A7b月光能: \u00A7f" + d.moon() + " / " + d.maxMoon()));
            if (!blazing && d.overload() > 0) {
                lines.add(Component.literal("\u00A7c⚠ 日光过载: " + d.overload() + "%（满 100% 将碎裂）"));
            }
            if (shift) {
                lines.add(Component.literal("\u00A77放上月光祭坛在夜晚吸收月光能"
                        + (blazing ? "（充能效率 ×1.5）" : "")));
                lines.add(Component.literal(blazing
                        ? "\u00A78炽月晶已淬炼完成，免疫日光过载"
                        : "\u00A78白天请收入封闭容器，露天暴晒会过载碎裂"));
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

        if (MagicIds.LUNAR_SPYGLASS.equals(itemId)) {
            if (shift) {
                lines.add(Component.literal("\u00A77右键查看当前月相、月光/虚空倍率与蚀空侵蚀状态"));
            } else {
                lines.add(Component.literal("\u00A78按住 Shift 查看说明"));
            }
            return lines;
        }

        boolean moonArtifact = MagicIds.MOONWING_STAFF.equals(itemId)
                || MagicIds.LUNAR_ANKLET.equals(itemId)
                || MagicIds.MOON_SIPHON_RING.equals(itemId);
        boolean voidArtifact = MagicIds.VOID_CHARM.equals(itemId)
                || MagicIds.VOID_BUILDER_STAFF.equals(itemId)
                || MagicIds.VOID_BRACER.equals(itemId)
                || MagicIds.VOID_ANCHOR_CLOAK.equals(itemId);
        if (moonArtifact || voidArtifact) {
            MagicEnergyData d = data != null ? data : MagicEnergyData.emptyArtifact();
            if (!d.hasGem()) {
                lines.add(Component.literal("\u00A77镶嵌槽: \u00A78空（未镶嵌"
                        + (moonArtifact ? "月光水晶 / 炽月晶" : "虚空晶核") + "）"));
            } else if (d.isMoonGem()) {
                lines.add(Component.literal("\u00A77镶嵌: \u00A7b"
                        + (d.isBlazingMoon() ? "炽月晶" : "月光水晶")
                        + " \u00A7f(" + d.moon() + " / " + d.maxMoon() + ")"));
                if (!d.isBlazingMoon() && d.overload() > 0) {
                    lines.add(Component.literal("\u00A7c⚠ 宝石过载: " + d.overload() + "%（满 100% 仅宝石碎裂）"));
                }
            } else {
                lines.add(Component.literal("\u00A77镶嵌: \u00A75虚空晶核 \u00A7f("
                        + d.voidEnergy() + " / " + d.maxVoid() + ")"));
            }
            if (shift) {
                if (MagicIds.MOONWING_STAFF.equals(itemId)) {
                    lines.add(Component.literal("\u00A77右键消耗 40 月光能：轻跃 + 6 秒缓降"));
                } else if (MagicIds.VOID_CHARM.equals(itemId)) {
                    lines.add(Component.literal("\u00A77饰品/副手（护符）：摔落免伤（80 虚空能），右键吸附掉落物（20）"));
                } else if (MagicIds.VOID_BUILDER_STAFF.equals(itemId)) {
                    lines.add(Component.literal("\u00A77右键消耗虚空能：远距辅助建造与方块延伸"));
                } else if (MagicIds.VOID_BRACER.equals(itemId)) {
                    lines.add(Component.literal("\u00A77饰品（护腕）：提升方块交互距离（+2.5 / +3.5 格）"));
                } else if (MagicIds.LUNAR_ANKLET.equals(itemId)) {
                    lines.add(Component.literal("\u00A77饰品（戒指）：提升跨步高度、安全坠落与轻盈重力"));
                } else if (MagicIds.VOID_ANCHOR_CLOAK.equals(itemId)) {
                    lines.add(Component.literal("\u00A77饰品（披风）：Y ≤ -55 坠入虚空时消耗虚空能拉回安全平台"));
                } else if (MagicIds.MOON_SIPHON_RING.equals(itemId)) {
                    lines.add(Component.literal("\u00A77饰品（戒指）：夜晚露天自动虹吸月光为所嵌宝石充能"));
                }
                lines.add(Component.literal("\u00A78背包中可用对应晶体直接点击镶嵌；按 B 或 /trinket 佩戴饰品"));
            } else {
                lines.add(Component.literal("\u00A78按住 Shift 查看说明"));
            }
            return lines;
        }

        return lines;
    }
}
