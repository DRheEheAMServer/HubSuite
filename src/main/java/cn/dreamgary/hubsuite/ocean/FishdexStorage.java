package cn.dreamgary.hubsuite.ocean;

import cn.dreamgary.hubsuite.HubSuite;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家渔获图鉴存储与里程碑奖励（config/hubsuite/fishdex/<uuid>.json）。
 */
public final class FishdexStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerDex> CACHE = new ConcurrentHashMap<>();
    private static Path rootDir;

    public static final int[] MILESTONES = {3, 5, 7, 9};

    public static final class FishRecord {
        public int count;
        public int maxStars;
        public double maxWeightKg;
        public long firstCaughtMillis;
    }

    public static final class PlayerDex {
        public Map<String, FishRecord> unlocked = new LinkedHashMap<>();
        public Set<Integer> claimedMilestones = new LinkedHashSet<>();

        public int unlockedCount() {
            return unlocked == null ? 0 : unlocked.size();
        }

        public int totalCatches() {
            if (unlocked == null) {
                return 0;
            }
            int sum = 0;
            for (FishRecord r : unlocked.values()) {
                if (r != null) {
                    sum += r.count;
                }
            }
            return sum;
        }
    }

    private FishdexStorage() {
    }

    public static void init(Path configDir) {
        rootDir = configDir.resolve("hubsuite").resolve("fishdex");
        try {
            Files.createDirectories(rootDir);
        } catch (Throwable t) {
            HubSuite.logger().warn("创建 fishdex 目录失败：{}", t.toString());
        }
        CACHE.clear();
    }

    public static PlayerDex of(UUID uuid) {
        if (uuid == null) {
            return new PlayerDex();
        }
        return CACHE.computeIfAbsent(uuid, FishdexStorage::load);
    }

    public static void recordCatch(ServerPlayer player, OceanFishDefs.FishDef def, int stars, double weightKg) {
        if (player == null || def == null) {
            return;
        }
        PlayerDex dex = of(player.getUUID());
        boolean firstTime = !dex.unlocked.containsKey(def.path());
        FishRecord rec = dex.unlocked.computeIfAbsent(def.path(), k -> {
            FishRecord r = new FishRecord();
            r.firstCaughtMillis = System.currentTimeMillis();
            return r;
        });
        rec.count++;
        rec.maxStars = Math.max(rec.maxStars, stars);
        rec.maxWeightKg = Math.max(rec.maxWeightKg, Math.round(weightKg * 100.0) / 100.0);
        save(player.getUUID(), dex);

        if (firstTime) {
            player.sendSystemMessage(Component.literal(
                    "§b✦ 渔获图鉴新解锁：§f" + def.zhName()
                            + " §7(" + dex.unlockedCount() + "/" + OceanFishDefs.ALL_FISH.size()
                            + "，输入 /fishdex 查看)"));
        }
        if (stars >= 3) {
            String msg = "§6★ 三星鱼王！§f" + player.getName().getString()
                    + " §7钓获了 §e" + String.format(java.util.Locale.ROOT, "%.2f", weightKg)
                    + " kg §7的 §6" + def.zhName() + "§7！";
            for (ServerPlayer peer : player.level().players()) {
                peer.sendSystemMessage(Component.literal(msg));
            }
        }
    }

    public static boolean claimMilestone(ServerPlayer player, int threshold) {
        if (player == null) {
            return false;
        }
        PlayerDex dex = of(player.getUUID());
        if (dex.unlockedCount() < threshold || dex.claimedMilestones.contains(threshold)) {
            return false;
        }
        List<ItemStack> rewards = rewardsFor(threshold);
        if (rewards.isEmpty()) {
            return false;
        }
        dex.claimedMilestones.add(threshold);
        save(player.getUUID(), dex);
        for (ItemStack r : rewards) {
            if (!player.getInventory().add(r.copy())) {
                player.drop(r.copy(), false);
            }
        }
        player.sendSystemMessage(Component.literal("§a✔ 已领取图鉴里程碑（" + threshold + "/9）奖励！"));
        return true;
    }

    public static List<ItemStack> rewardsFor(int threshold) {
        return switch (threshold) {
            case 3 -> List.of(
                    itemStack("hubsuite:refined_bait", 8),
                    new ItemStack(Items.EMERALD, 4));
            case 5 -> List.of(
                    itemStack("hubsuite:abyssal_bait", 8),
                    itemStack("hubsuite:treasure_bait", 4));
            case 7 -> List.of(
                    itemStack("hubsuite:infernal_bait", 8),
                    itemStack("hubsuite:twin_bait", 4),
                    new ItemStack(Items.DIAMOND, 4));
            case 9 -> List.of(
                    new ItemStack(Items.TRIDENT, 1),
                    new ItemStack(Items.HEART_OF_THE_SEA, 1),
                    new ItemStack(Items.NETHERITE_INGOT, 1));
            default -> List.of();
        };
    }

    private static ItemStack itemStack(String id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if (item == Items.AIR) {
            return new ItemStack(Items.COD, count);
        }
        return new ItemStack(item, count);
    }

    private static PlayerDex load(UUID uuid) {
        if (rootDir == null) {
            return new PlayerDex();
        }
        Path file = rootDir.resolve(uuid.toString() + ".json");
        if (!Files.exists(file)) {
            return new PlayerDex();
        }
        try {
            PlayerDex parsed = GSON.fromJson(Files.readString(file), PlayerDex.class);
            if (parsed == null) {
                return new PlayerDex();
            }
            if (parsed.unlocked == null) {
                parsed.unlocked = new LinkedHashMap<>();
            }
            if (parsed.claimedMilestones == null) {
                parsed.claimedMilestones = new LinkedHashSet<>();
            }
            return parsed;
        } catch (Throwable t) {
            return new PlayerDex();
        }
    }

    public static void save(UUID uuid, PlayerDex dex) {
        if (rootDir == null || uuid == null || dex == null) {
            return;
        }
        try {
            Files.writeString(rootDir.resolve(uuid.toString() + ".json"), GSON.toJson(dex));
        } catch (Throwable t) {
            HubSuite.logger().warn("保存 fishdex 失败（{}）：{}", uuid, t.toString());
        }
    }
}
