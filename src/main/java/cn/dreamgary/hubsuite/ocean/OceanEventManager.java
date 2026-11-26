package cn.dreamgary.hubsuite.ocean;

import cn.dreamgary.hubsuite.HubSuite;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

/**
 * 海岛服动态海域事件（方向 2）：
 * <ul>
 *   <li>{@link EventType#SPRING_TIDE}：丰饶大潮（咬钩速度 +25%，双倍渔获 +15%）</li>
 *   <li>{@link EventType#BIOLUM_NIGHT}：荧光夜潮（幸运 +4，三星鱼王权重 ×2）</li>
 *   <li>{@link EventType#MAGMA_SURGE}：下界熔潮（岩浆海幸运 +4，三星鱼王权重 ×2）</li>
 * </ul>
 */
public final class OceanEventManager {

    public enum EventType {
        SPRING_TIDE("丰饶大潮", "hubsuite:skyblock_ocean", BossEvent.BossBarColor.BLUE,
                "§b✦ 丰饶大潮来临！海岛全海域咬钩速度 +25%，双倍渔获 +15%！"),
        BIOLUM_NIGHT("荧光夜潮", "hubsuite:skyblock_ocean", BossEvent.BossBarColor.PURPLE,
                "§d✦ 荧光夜潮降临！海岛全海域幸运 +4，三星奇鱼概率翻倍！"),
        MAGMA_SURGE("下界熔潮", "hubsuite:skyblock_ocean_nether", BossEvent.BossBarColor.RED,
                "§6✦ 下界熔潮涌动！海岛岩浆海幸运 +4，传说与三星熔岩鱼概率翻倍！");

        public final String displayName;
        public final String targetDim;
        public final BossEvent.BossBarColor color;
        public final String startBroadcast;

        EventType(String displayName, String targetDim, BossEvent.BossBarColor color, String startBroadcast) {
            this.displayName = displayName;
            this.targetDim = targetDim;
            this.color = color;
            this.startBroadcast = startBroadcast;
        }

        public static EventType parse(String raw) {
            if (raw == null) {
                return null;
            }
            return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "spring_tide", "tide", "spring" -> SPRING_TIDE;
                case "biolum_night", "night", "biolum" -> BIOLUM_NIGHT;
                case "magma_surge", "magma", "lava" -> MAGMA_SURGE;
                default -> null;
            };
        }
    }

    private static final int EVENT_DURATION_TICKS = 4800; // 4 分钟
    private static final int COOLDOWN_TICKS = 18000;      // 15 分钟周期

    private static volatile EventType active;
    private static int remainingTicks;
    private static int cooldownTicks = 6000; // 开服 5 分钟后首次检查
    private static ServerBossEvent bossBar;

    private OceanEventManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(OceanEventManager::onServerTick);
    }

    public static EventType activeEvent() {
        return active;
    }

    public static int remainingSeconds() {
        return Math.max(0, remainingTicks / 20);
    }

    public static int bonusLuck(ResourceKey<Level> dim) {
        EventType ev = active;
        if (ev == null || !matchesDim(ev, dim)) {
            return 0;
        }
        return (ev == EventType.BIOLUM_NIGHT || ev == EventType.MAGMA_SURGE) ? 4 : 1;
    }

    public static double bonusDoubleCatch(ResourceKey<Level> dim) {
        EventType ev = active;
        if (ev == null || !matchesDim(ev, dim)) {
            return 0.0;
        }
        return ev == EventType.SPRING_TIDE ? 0.15 : 0.0;
    }

    public static double biteSpeedBonus(ResourceKey<Level> dim) {
        EventType ev = active;
        if (ev == null || !matchesDim(ev, dim)) {
            return 0.0;
        }
        return ev == EventType.SPRING_TIDE ? 0.25 : 0.10;
    }

    public static double starMultiplier(ResourceKey<Level> dim) {
        EventType ev = active;
        if (ev == null || !matchesDim(ev, dim)) {
            return 1.0;
        }
        return (ev == EventType.BIOLUM_NIGHT || ev == EventType.MAGMA_SURGE) ? 2.0 : 1.25;
    }

    public static void startEvent(EventType type, MinecraftServer server) {
        if (type == null) {
            return;
        }
        stopEvent();
        active = type;
        remainingTicks = EVENT_DURATION_TICKS;
        bossBar = new ServerBossEvent(
                java.util.UUID.randomUUID(),
                Component.literal("§b✦ " + type.displayName + " §7(" + remainingSeconds() + "s)"),
                type.color,
                BossEvent.BossBarOverlay.PROGRESS);
        bossBar.setProgress(1.0F);
        if (server != null) {
            ServerLevel target = server.getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(type.targetDim)));
            if (target != null) {
                for (ServerPlayer sp : target.players()) {
                    bossBar.addPlayer(sp);
                    sp.sendSystemMessage(Component.literal(type.startBroadcast));
                }
            }
            syncBossBarPlayers(server);
        }
        HubSuite.logger().info("海岛事件已启动：{}", type.displayName);
    }

    public static void stopEvent() {
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }
        active = null;
        remainingTicks = 0;
    }

    private static void onServerTick(MinecraftServer server) {
        if (active != null) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                EventType ended = active;
                stopEvent();
                cooldownTicks = COOLDOWN_TICKS;
                HubSuite.logger().info("海岛事件已结束：{}", ended.displayName);
                return;
            }
            if (remainingTicks % 20 == 0 && bossBar != null) {
                bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, (float) remainingTicks / EVENT_DURATION_TICKS)));
                bossBar.setName(Component.literal("§b✦ " + active.displayName + " §7(" + remainingSeconds() + "s)"));
                syncBossBarPlayers(server);
            }
            return;
        }
        cooldownTicks--;
        if (cooldownTicks <= 0) {
            cooldownTicks = COOLDOWN_TICKS;
            tryAutoStart(server);
        }
    }

    private static void tryAutoStart(MinecraftServer server) {
        ServerLevel ocean = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse("hubsuite:skyblock_ocean")));
        ServerLevel lava = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse("hubsuite:skyblock_ocean_nether")));
        boolean hasOceanPlayers = ocean != null && !ocean.players().isEmpty();
        boolean hasLavaPlayers = lava != null && !lava.players().isEmpty();
        if (!hasOceanPlayers && !hasLavaPlayers) {
            return;
        }
        if (hasLavaPlayers && (!hasOceanPlayers || server.overworld().getRandom().nextBoolean())) {
            startEvent(EventType.MAGMA_SURGE, server);
        } else if (ocean != null) {
            long tod = ocean.getOverworldClockTime() % 24000L;
            boolean isNight = tod >= 13000L && tod <= 23000L;
            startEvent(isNight ? EventType.BIOLUM_NIGHT : EventType.SPRING_TIDE, server);
        }
    }

    private static void syncBossBarPlayers(MinecraftServer server) {
        if (bossBar == null || active == null || server == null) {
            return;
        }
        Set<ServerPlayer> desired = new HashSet<>();
        ServerLevel target = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(active.targetDim)));
        if (target != null) {
            desired.addAll(target.players());
        }
        for (ServerPlayer current : new java.util.ArrayList<>(bossBar.getPlayers())) {
            if (!desired.contains(current)) {
                bossBar.removePlayer(current);
            }
        }
        for (ServerPlayer sp : desired) {
            if (!bossBar.getPlayers().contains(sp)) {
                bossBar.addPlayer(sp);
            }
        }
    }

    private static boolean matchesDim(EventType ev, ResourceKey<Level> dim) {
        return dim != null && ev.targetDim.equals(dim.identifier().toString());
    }
}
