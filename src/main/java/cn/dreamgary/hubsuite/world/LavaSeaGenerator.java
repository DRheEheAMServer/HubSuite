package cn.dreamgary.hubsuite.world;

import cn.dreamgary.hubsuite.HubSuite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/**
 * 海岛服下界的**岩浆海**生成器。
 *
 * <p>自定义开阔岩浆海 {@link NoiseRouter} 与无顶层基岩 {@link SurfaceRules}：
 * <ul>
 *   <li>液面定在 {@code Y=63}，上方 {@code Y=64..127} 为开阔下界空域（无顶层基岩天花板）；</li>
 *   <li>岩浆海底在 {@code Y≈18..58} 起伏，少数高噪声处露出 {@code Y=64..76} 的火山礁石岛；</li>
 *   <li>保留原版下界 5 大群系与要塞/猪灵堡垒结构，且关闭含水层避免流体高度被覆盖。</li>
 * </ul>
 */
public final class LavaSeaGenerator {

    /** 岩浆海的液面高度（上方 Y=64..127 开阔无顶层基岩）。 */
    public static final int LAVA_SEA_LEVEL = 63;

    private static final int MIN_Y = 0;
    private static final int MAX_Y = 128;

    private LavaSeaGenerator() {
    }

    public static net.minecraft.world.level.chunk.ChunkGenerator create(MinecraftServer server) {
        try {
            var access = server.registryAccess();
            var noises = access.lookupOrThrow(Registries.NOISE);
            NoiseGeneratorSettings vanilla = access.lookupOrThrow(Registries.NOISE_SETTINGS)
                    .getOrThrow(NoiseGeneratorSettings.NETHER).value();

            NoiseRouter router = lavaSeaRouter(vanilla.noiseRouter(), noises);
            SurfaceRules.RuleSource surfaceRule = lavaSeaSurfaceRules();

            NoiseGeneratorSettings settings = new NoiseGeneratorSettings(
                    vanilla.noiseSettings(),
                    vanilla.defaultBlock(),
                    vanilla.defaultFluid(),
                    router,
                    surfaceRule,
                    vanilla.spawnTarget(),
                    LAVA_SEA_LEVEL,
                    vanilla.disableMobGeneration(),
                    false, // 关闭含水层，避免原版流体高度覆盖岩浆海
                    false, // 关闭矿脉
                    vanilla.useLegacyRandomSource());

            var biomeSource = MultiNoiseBiomeSource.createFromPreset(
                    access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                            .getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER));

            HubSuite.logger().info("岩浆海生成器已就绪：液面 Y={}，开阔空域无顶层基岩", LAVA_SEA_LEVEL);
            return new NoiseBasedChunkGenerator(biomeSource, Holder.direct(settings));
        } catch (Throwable t) {
            HubSuite.logger().error("构造岩浆海生成器失败，该下界将回落到虚空", t);
            return null;
        }
    }

    private static NoiseRouter lavaSeaRouter(NoiseRouter vanilla,
                                             net.minecraft.core.HolderGetter<net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters> noises) {
        // yClampedGradient(0, 128, 1.0, -1.0)：Y=64 处为 0，每升 1 格密度 -1/64
        DensityFunction yRamp = DensityFunctions.yClampedGradient(MIN_Y, MAX_Y, 1.0, -1.0);
        // 基准海底 Y=44（相对 Y=64 偏移 -20 格 → -20 / 64）
        DensityFunction baseOffset = DensityFunctions.constant(-20.0 / 64.0);

        DensityFunction medium = DensityFunctions.mul(
                DensityFunctions.constant(0.68),
                DensityFunctions.noise(noises.getOrThrow(Noises.EROSION), 1.5, 1.0));
        DensityFunction fine = DensityFunctions.mul(
                DensityFunctions.constant(0.32),
                DensityFunctions.noise(noises.getOrThrow(Noises.SURFACE), 3.0, 2.0));
        DensityFunction relief = DensityFunctions.mul(
                DensityFunctions.constant(26.0 / 64.0),
                DensityFunctions.add(medium, fine));

        DensityFunction finalDensity = DensityFunctions.add(
                yRamp, DensityFunctions.add(baseOffset, relief));
        DensityFunction preliminarySurface = DensityFunctions.findTopSurface(
                finalDensity, DensityFunctions.constant(MAX_Y), MIN_Y, 8);
        DensityFunction depth = DensityFunctions.yClampedGradient(MIN_Y, LAVA_SEA_LEVEL, -1.0, 1.0);

        return new NoiseRouter(
                vanilla.barrierNoise(),
                vanilla.fluidLevelFloodednessNoise(),
                vanilla.fluidLevelSpreadNoise(),
                vanilla.lavaNoise(),
                vanilla.temperature(),
                vanilla.vegetation(),
                vanilla.continents(),
                vanilla.erosion(),
                depth,
                vanilla.ridges(),
                preliminarySurface,
                finalDensity,
                vanilla.veinToggle(),
                vanilla.veinRidged(),
                vanilla.veinGap());
    }

    /** 仅保留底层 Y=0..4 基岩，彻底移除顶层基岩天花板，并按 5 大下界群系铺设礁石表层。 */
    private static SurfaceRules.RuleSource lavaSeaSurfaceRules() {
        SurfaceRules.RuleSource bedrockFloor = SurfaceRules.ifTrue(
                SurfaceRules.verticalGradient("bedrock_floor",
                        VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
                SurfaceRules.state(Blocks.BEDROCK.defaultBlockState()));

        SurfaceRules.RuleSource soulValley = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(Biomes.SOUL_SAND_VALLEY),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.SOUL_SAND.defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.SOUL_SOIL.defaultBlockState()))));

        SurfaceRules.RuleSource basaltDeltas = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(Biomes.BASALT_DELTAS),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.BASALT.defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.BLACKSTONE.defaultBlockState()))));

        SurfaceRules.RuleSource crimson = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(Biomes.CRIMSON_FOREST),
                SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(LAVA_SEA_LEVEL), 0),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.state(Blocks.CRIMSON_NYLIUM.defaultBlockState()))));

        SurfaceRules.RuleSource warped = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(Biomes.WARPED_FOREST),
                SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(LAVA_SEA_LEVEL), 0),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                SurfaceRules.state(Blocks.WARPED_NYLIUM.defaultBlockState()))));

        return SurfaceRules.sequence(bedrockFloor, soulValley, basaltDeltas, crimson, warped);
    }
}
