package cn.dreamgary.hubsuite.client;

import cn.dreamgary.hubsuite.net.ClientVersions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 轻量客户端更新检查（docs/分发与更新.md 第 5/6 节）：
 * 启动时异步请求一次 GitHub Releases API（3 秒超时），网络异常静默降级；
 * 若发现更高版本且玩家仍在游戏内，则在聊天栏给出可点击打开下载页的提示（每会话最多一次）。
 */
public final class ClientUpdateChecker {

    private static final String RELEASES_API =
            "https://api.github.com/repos/DRheEheAMServer/HubSuite/releases?per_page=5";
    private static final String RELEASES_PAGE =
            "https://github.com/DRheEheAMServer/HubSuite/releases";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private static volatile String latestVersion = "";
    private static volatile String latestUrl = RELEASES_PAGE;
    private static volatile boolean notified;

    private ClientUpdateChecker() {
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> tryNotify(client)));

        Thread checker = new Thread(ClientUpdateChecker::checkAsync, "HubSuite-UpdateChecker");
        checker.setDaemon(true);
        checker.start();
    }

    private static void checkAsync() {
        try {
            String current = HubSuiteClient.version();
            if (current == null || current.isBlank() || "unknown".equals(current)) {
                return;
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASES_API))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "HubSuite-Client/" + current)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }
            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonArray()) {
                return;
            }
            for (JsonElement el : root.getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("draft") && obj.get("draft").getAsBoolean()) {
                    continue;
                }
                if (!obj.has("tag_name") || obj.get("tag_name").isJsonNull()) {
                    continue;
                }
                String tag = obj.get("tag_name").getAsString().trim();
                String remote = stripLeadingV(tag);
                if (remote.isEmpty()) {
                    continue;
                }
                if (!ClientVersions.atLeast(current, remote)) {
                    latestVersion = remote;
                    if (obj.has("html_url") && !obj.get("html_url").isJsonNull()) {
                        latestUrl = obj.get("html_url").getAsString().trim();
                    }
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null) {
                        mc.execute(() -> tryNotify(mc));
                    }
                }
                break;
            }
        } catch (Throwable ignored) {
            // 静默降级：不卡启动、不弹错误框
        }
    }

    private static void tryNotify(Minecraft mc) {
        if (notified || latestVersion.isEmpty() || mc == null || mc.player == null) {
            return;
        }
        notified = true;
        String current = HubSuiteClient.version();
        URI parsed;
        try {
            parsed = URI.create(latestUrl);
        } catch (IllegalArgumentException e) {
            parsed = URI.create(RELEASES_PAGE);
        }
        final URI target = parsed;
        Component link = Component.literal("[点击打开下载页]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(target))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("前往 " + latestUrl))));
        Component msg = Component.literal("§e[HubSuite] §f客户端有新版本 §av" + latestVersion
                + " §7（当前 " + current + "） ")
                .append(link);
        mc.player.sendSystemMessage(msg);
    }

    private static String stripLeadingV(String tag) {
        if (tag.startsWith("v") || tag.startsWith("V")) {
            return tag.substring(1).trim();
        }
        return tag;
    }
}
