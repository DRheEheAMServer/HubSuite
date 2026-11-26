package cn.dreamgary.hubsuite.client.ocean;

import cn.dreamgary.hubsuite.ocean.OceanFishDefs;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * 客户端海岛奇鱼与海味料理的 Tooltip 渲染。
 */
public final class ClientOceanTooltip {

    private ClientOceanTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            OceanFishDefs.FishDef fish = OceanFishDefs.byId(id);
            if (fish != null) {
                OceanFishDefs.FishData data = OceanFishDefs.FishData.of(stack);
                if (data != null) {
                    String stars = "★".repeat(data.stars()) + "☆".repeat(3 - data.stars());
                    lines.add(Component.literal("§6星级：§e" + stars + " §7| 重量：§f"
                            + String.format(Locale.ROOT, "%.2f", data.weightKg()) + " kg"));
                }
                lines.add(Component.literal("§7出没：§b" + fish.habitatHint()));
                lines.add(Component.literal("§8可在工作台提炼稀有资源或烹制海味料理；输入 /fishdex 查图鉴"));
                return;
            }
            if (OceanFishDefs.OCEAN_SASHIMI.id().equals(id)) {
                lines.add(Component.literal("§b右键食用：§f水下呼吸 + 海豚的恩惠（3 分钟）"));
            } else if (OceanFishDefs.GOLDEN_FISH_SOUP.id().equals(id)) {
                lines.add(Component.literal("§6右键食用：§f幸运 II（4 分钟）+ 生命恢复（15 秒）"));
            } else if (OceanFishDefs.MAGMA_SEARED_STEAK.id().equals(id)) {
                lines.add(Component.literal("§c右键食用：§f抗火（5 分钟），岩浆垂钓必备"));
            }
        });
    }
}
