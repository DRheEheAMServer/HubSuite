package cn.dreamgary.hubsuite.integration;

import cn.dreamgary.hubsuite.HubSuite;
import cn.dreamgary.hubsuite.world.SubServer;
import cn.dreamgary.hubsuite.world.WorldsManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * HuskHomes 适配器。
 *
 * <p>HuskHomes 有官方 Fabric 版（本项目按 {@code 4.11+26.1.2} 编译），
 * 它自带 {@code /sethome /home /spawn /tpa /warp} 等完整指令，
 * 所以本模组<strong>不重复实现</strong>这些功能，而是：
 *
 * <ol>
 *   <li><b>探测并接管</b>：装了 HuskHomes 就把"家/传送点"能力标记为可用；</li>
 *   <li><b>补齐它没有的</b>：跨子服传送（{@code /hub <id>}）、大厅、子服规则；</li>
 *   <li><b>兜底</b>：没装 HuskHomes 时给出明确提示，并说明哪些指令会缺失；</li>
 *   <li><b>占位符</b>：把 {@code %huskhomes_...%} 交给 HuskHomes 自己的 Hook
 *       （它依赖 Text Placeholder API），本模组的占位符见
 *       {@link PlaceholderService}。</li>
 * </ol>
 *
 * <p>全部通过反射调用 HuskHomes API，因此它是纯粹的软依赖：
 * 没装不会导致 {@code NoClassDefFoundError}。
 */
public final class HuskHomesIntegration {

    private static final String MOD_ID = "huskhomes";

    private final boolean present;
    private final String version;
    private final Method getInstance;
    private final WorldsManager worlds;

    public HuskHomesIntegration(WorldsManager worlds) {
        this.worlds = worlds;
        boolean loaded = FabricLoader.getInstance().isModLoaded(MOD_ID);
        String detectedVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
        Method instance = null;
        if (loaded) {
            try {
                Class<?> api = Class.forName("net.william278.huskhomes.api.FabricHuskHomesAPI");
                instance = api.getMethod("getInstance");
            } catch (Throwable t) {
                HubSuite.logger().warn("检测到 HuskHomes，但无法取得其 API（{}）。", t.toString());
            }
        }
        this.present = loaded;
        this.version = detectedVersion;
        this.getInstance = instance;
    }

    public boolean present() {
        return present;
    }

    public String version() {
        return version;
    }

    /** 是否真的能调用到 API（而不只是检测到 mod 存在）。 */
    public boolean apiReady() {
        return present && getInstance != null;
    }

    /** 家 / 传送点功能是否可用。 */
    public boolean homesAvailable() {
        return apiReady();
    }

    private record CachedCount(int count, long fetchedAt) {
    }

    private final java.util.Map<java.util.UUID, CachedCount> homeCountCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> homeCountFetching =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 取玩家的家数量（非阻塞读缓存，过期时后台异步刷新，绝不阻塞服务端主线程）。
     */
    public Optional<Integer> homeCount(ServerPlayer player) {
        if (!apiReady() || player == null) {
            return Optional.empty();
        }
        java.util.UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        CachedCount cached = homeCountCache.get(uuid);
        if (cached == null || now - cached.fetchedAt() > 5_000L) {
            if (homeCountFetching.add(uuid)) {
                try {
                    Object api = getInstance.invoke(null);
                    Object user = adaptUser(api, player);
                    if (user == null) {
                        homeCountFetching.remove(uuid);
                    } else {
                        Method getUserHomes = Class
                                .forName("net.william278.huskhomes.api.BaseHuskHomesAPI")
                                .getMethod("getUserHomes",
                                        Class.forName("net.william278.huskhomes.user.User"));
                        Object future = getUserHomes.invoke(api, user);
                        if (future instanceof CompletableFuture<?> cf) {
                            cf.whenComplete((homes, err) -> {
                                try {
                                    if (err == null && homes instanceof List<?> list) {
                                        homeCountCache.put(uuid, new CachedCount(list.size(), System.currentTimeMillis()));
                                    }
                                } finally {
                                    homeCountFetching.remove(uuid);
                                }
                            });
                        } else {
                            homeCountFetching.remove(uuid);
                        }
                    }
                } catch (Throwable t) {
                    homeCountFetching.remove(uuid);
                    HubSuite.logger().debug("读取 HuskHomes 家数量失败：{}", t.toString());
                }
            }
        }
        return cached == null ? Optional.empty() : Optional.of(cached.count());
    }

    private Object adaptUser(Object api, ServerPlayer player) {
        try {
            Class<?> playerIface = Class.forName("net.minecraft.world.entity.player.Player");
            Method adapt = Class.forName("net.william278.huskhomes.api.FabricHuskHomesAPI")
                    .getMethod("adaptUser", playerIface);
            return adapt.invoke(api, player);
        } catch (Throwable t) {
            HubSuite.logger().debug("HuskHomes adaptUser 失败：{}", t.toString());
            return null;
        }
    }

    /** 该子服是否允许使用 HuskHomes 的传送指令（避免把玩家传出空岛/创造服）。 */
    public boolean teleportCommandsAllowed(SubServer sub) {
        return sub != null && sub.config().huskhomesTeleport;
    }

    /**
     * 注册「家按子服隔离」。
     *
     * <p>用 {@code Class.forName} 反射加载：那个类直接引用 HuskHomes 的类型，
     * 没装 HuskHomes 时绝不能触碰它。
     */
    public void registerHomeIsolation() {
        if (!apiReady()) {
            return;
        }
        try {
            Class.forName("cn.dreamgary.hubsuite.integration.HuskHomesHomeIsolation")
                    .getMethod("register", WorldsManager.class)
                    .invoke(null, worlds);
        } catch (Throwable t) {
            HubSuite.logger().warn("注册 HuskHomes 家隔离失败：{}", t.toString());
        }
    }

    /** 启动时打印适配报告。 */
    public void logReport() {
        if (!present) {
            HubSuite.logger().warn("未检测到 HuskHomes："
                    + "/sethome /home /spawn /tpa 等指令将不可用。"
                    + "如需这些功能，请把 HuskHomes 的 Fabric 版放进 mods/ 目录。");
            return;
        }
        if (!apiReady()) {
            HubSuite.logger().warn("HuskHomes {} 已加载，但 API 不可用（版本可能与本模组不匹配）。", version);
            return;
        }
        HubSuite.logger().info("HuskHomes 适配已生效：版本 {}，家/传送点指令由它提供。", version);
    }

    /** 便于 /hub info 展示。 */
    public String statusText() {
        if (!present) {
            return "未安装（家/传送指令不可用）";
        }
        return apiReady() ? ("已适配 v" + version) : ("已加载 v" + version + "，但 API 不可用");
    }
}
