package cn.dreamgary.hubsuite.ocean;

import cn.dreamgary.hubsuite.ui.ChestMenuScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 渔获图鉴界面（/fishdex，基于 {@link ChestMenuScreen} 4 行箱子界面）。
 */
public final class FishdexMenu {

    private FishdexMenu() {
    }

    public static void open(ServerPlayer player) {
        if (player == null) {
            return;
        }
        FishdexStorage.PlayerDex dex = FishdexStorage.of(player.getUUID());
        List<ChestMenuScreen.Entry> entries = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            entries.add(filler());
        }

        // 第 0 行 (slots 2..6): 5 种海域奇鱼
        int waterIdx = 2;
        int lavaIdx = 11;
        for (OceanFishDefs.FishDef def : OceanFishDefs.ALL_FISH) {
            int slot = def.lava() ? lavaIdx++ : waterIdx++;
            entries.set(slot, fishEntry(def, dex));
        }

        // 第 2 行 (slots 20, 21, 23, 24): 4 档图鉴收集里程碑奖励
        int[] mSlots = {20, 21, 23, 24};
        for (int i = 0; i < FishdexStorage.MILESTONES.length; i++) {
            int threshold = FishdexStorage.MILESTONES[i];
            entries.set(mSlots[i], milestoneEntry(player, dex, threshold));
        }

        // 第 3 行 (slot 31): 图鉴总览与当前海域事件
        entries.set(31, summaryEntry(dex));

        ChestMenuScreen.open(player, "§3✦ 海岛渔获图鉴 (" + dex.unlockedCount()
                + "/" + OceanFishDefs.ALL_FISH.size() + ")", entries);
    }

    private static ChestMenuScreen.Entry fishEntry(OceanFishDefs.FishDef def, FishdexStorage.PlayerDex dex) {
        FishdexStorage.FishRecord rec = dex.unlocked.get(def.path());
        if (rec == null) {
            List<String> lore = List.of(
                    "§7类型：" + (def.lava() ? "§c岩浆海奇鱼" : "§b海域奇鱼"),
                    "§7品质：" + rarityLabel(def.rarity()),
                    "§7出没：" + def.habitatHint(),
                    "§8尚未钓获");
            return ChestMenuScreen.Entry.of(new ItemStack(Items.GRAY_DYE), "§8???（未解锁）", lore, null);
        }
        Item item = BuiltInRegistries.ITEM.getValue(def.id());
        ItemStack icon = item == Items.AIR ? new ItemStack(Items.COD) : new ItemStack(item);
        List<String> lore = List.of(
                "§7类型：" + (def.lava() ? "§c岩浆海奇鱼" : "§b海域奇鱼"),
                "§7品质：" + rarityLabel(def.rarity()),
                "§7出没：" + def.habitatHint(),
                "§a已钓获：§f" + rec.count + " 次",
                "§6最高星级：§e" + "★".repeat(Math.max(1, rec.maxStars))
                        + " §7| 最大重量：§f" + String.format(java.util.Locale.ROOT, "%.2f", rec.maxWeightKg) + " kg");
        return ChestMenuScreen.Entry.of(icon, "§f" + def.zhName(), lore, null);
    }

    private static ChestMenuScreen.Entry milestoneEntry(ServerPlayer player, FishdexStorage.PlayerDex dex, int threshold) {
        boolean claimed = dex.claimedMilestones.contains(threshold);
        boolean unlocked = dex.unlockedCount() >= threshold;
        ItemStack icon = new ItemStack(claimed ? Items.MINECART : (unlocked ? Items.CHEST_MINECART : Items.IRON_BARS));
        String title = (claimed ? "§7" : (unlocked ? "§a" : "§c")) + "图鉴里程碑 " + threshold + "/9";
        List<String> lore = new ArrayList<>();
        lore.add("§7当前进度：§f" + dex.unlockedCount() + " / " + threshold);
        for (ItemStack r : FishdexStorage.rewardsFor(threshold)) {
            lore.add("§7奖励：§f" + r.getHoverName().getString() + " ×" + r.getCount());
        }
        if (claimed) {
            lore.add("§8✔ 已领取");
            return ChestMenuScreen.Entry.of(icon, title, lore, null);
        }
        if (unlocked) {
            lore.add("§e▶ 点击领取奖励");
            return ChestMenuScreen.Entry.of(icon, title, lore, p -> {
                if (FishdexStorage.claimMilestone(p, threshold)) {
                    open(p);
                }
            });
        }
        lore.add("§8继续收集更多奇鱼解锁");
        return ChestMenuScreen.Entry.of(icon, title, lore, null);
    }

    private static ChestMenuScreen.Entry summaryEntry(FishdexStorage.PlayerDex dex) {
        var ev = OceanEventManager.activeEvent();
        String evText = ev == null ? "§7当前海域事件：§8暂无（定期自动触发）"
                : "§b当前海域事件：" + ev.displayName + " §7(" + OceanEventManager.remainingSeconds() + "s)";
        List<String> lore = List.of(
                "§7已解锁物种：§f" + dex.unlockedCount() + " / " + OceanFishDefs.ALL_FISH.size(),
                "§7累计钓获奇鱼：§f" + dex.totalCatches() + " 条",
                evText,
                "§8提示：可用 JEI 查看全部奇鱼与稀有宝物的出没群系");
        return ChestMenuScreen.Entry.of(new ItemStack(Items.KNOWLEDGE_BOOK), "§b图鉴总览", lore, null);
    }

    private static ChestMenuScreen.Entry filler() {
        return ChestMenuScreen.Entry.of(new ItemStack(Items.BLACK_STAINED_GLASS_PANE), " ", List.of(), null);
    }

    private static String rarityLabel(String rarity) {
        return switch (rarity) {
            case "legendary" -> "§d传说";
            case "rare" -> "§b稀有";
            default -> "§a精良";
        };
    }
}
