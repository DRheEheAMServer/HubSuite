package cn.dreamgary.hubsuite.magic;

import cn.dreamgary.hubsuite.HubSuite;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚空侵蚀进度管理器：按维度记录低空方块的随机刻侵蚀累计值。
 */
public final class VoidErosionManager {

    private static final Gson GSON = new GsonBuilder().create();
    private static final TypeToken<Map<String, Map<Long, Integer>>> MAP_TYPE = new TypeToken<>() {};

    private static final Map<String, Map<Long, Integer>> PROGRESS = new ConcurrentHashMap<>();
    private static Path storeFile;
    private static volatile boolean dirty;
    private static volatile long lastSavedAt;

    private VoidErosionManager() {
    }

    public static void init(Path configDir) {
        storeFile = configDir.resolve("magic").resolve("erosion.json");
        load();
    }

    public static void onRandomTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return;
        }
        if (!MagicScope.allowed(level) || pos.getY() > VoidErosionRules.MAX_EROSION_Y) {
            return;
        }
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) {
            return;
        }
        if (state.is(MagicRegistry.MOON_ALTAR) || state.is(MagicRegistry.MAGIC_PEDESTAL)
                || state.is(MagicRegistry.LUNAR_VOID_ALTAR) || state.is(MagicRegistry.VOID_CRYSTAL)) {
            return;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) {
            return;   // 不可破坏方块（如基岩）不侵蚀
        }
        // 白天露天直射时虚空侵蚀停滞
        if (DaylightExposure.isExposedDaylight(level, pos.above())) {
            return;
        }

        String dimKey = level.dimension().identifier().toString();
        long posKey = pos.asLong();
        Map<Long, Integer> perDim = PROGRESS.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());

        float moonBright = level.getMoonBrightness(pos);
        int step = VoidErosionRules.stepForMoonBrightness(moonBright);
        int next = perDim.getOrDefault(posKey, 0) + step;
        int required = VoidErosionRules.requiredProgress(hardness);

        if (next >= required) {
            perDim.remove(posKey);
            markDirty();
            int storedVoid = VoidErosionRules.storedVoid(hardness);
            level.setBlockAndUpdate(pos, MagicRegistry.VOID_CRYSTAL.defaultBlockState());
            if (level.getBlockEntity(pos) instanceof VoidCrystalBlockEntity crystal) {
                crystal.setStoredVoid(storedVoid);
            }
            level.sendParticles(ParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    12, 0.25, 0.25, 0.25, 0.05);
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.BLOCKS, 0.5F, 0.7F);
        } else {
            perDim.put(posKey, next);
            markDirty();
        }
    }

    public static void clearAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        Map<Long, Integer> perDim = PROGRESS.get(level.dimension().identifier().toString());
        if (perDim != null && perDim.remove(pos.asLong()) != null) {
            markDirty();
        }
    }

    private static void markDirty() {
        dirty = true;
        long now = System.currentTimeMillis();
        if (now - lastSavedAt >= 15_000L) {
            save();
        }
    }

    public static void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    private static void load() {
        if (storeFile == null || !Files.exists(storeFile)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
            Map<String, Map<Long, Integer>> data = GSON.fromJson(r, MAP_TYPE.getType());
            if (data != null) {
                data.forEach((k, v) -> {
                    if (k != null && v != null) {
                        PROGRESS.put(k, new ConcurrentHashMap<>(v));
                    }
                });
            }
        } catch (Throwable t) {
            HubSuite.logger().warn("载入虚空侵蚀进度失败，已重置：{}", t.toString());
        }
    }

    public static synchronized void save() {
        if (storeFile == null) {
            return;
        }
        try {
            Files.createDirectories(storeFile.getParent());
            Path tmp = storeFile.resolveSibling(storeFile.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(PROGRESS, w);
            }
            try {
                Files.move(tmp, storeFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception unsupported) {
                Files.move(tmp, storeFile, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            lastSavedAt = System.currentTimeMillis();
        } catch (Throwable t) {
            HubSuite.logger().warn("保存虚空侵蚀进度失败：{}", t.toString());
        }
    }
}
