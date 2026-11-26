package cn.dreamgary.hubsuite.magic;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 虚月祭坛仪式配方定义（双端共用单一数据源：服务端 {@code MagicRecipes} 与客户端 JEI 插件同源）。
 */
public final class MagicRecipeDefs {

    public record Def(String id,
                      String displayName,
                      Identifier centerId,
                      List<Identifier> pedestalIds,
                      Identifier outputId,
                      int outputCount,
                      int moonCost,
                      int voidCost,
                      float minMoonBright,
                      float maxMoonBright,
                      int baseTicks,
                      int targetMoonPhase) {

        public Def(String id, String displayName, Identifier centerId, List<Identifier> pedestalIds,
                   Identifier outputId, int outputCount, int moonCost, int voidCost,
                   float minMoonBright, float maxMoonBright, int baseTicks) {
            this(id, displayName, centerId, pedestalIds, outputId, outputCount,
                    moonCost, voidCost, minMoonBright, maxMoonBright, baseTicks, -1);
        }

        public boolean isRatioBased() {
            return minMoonBright > 0.01F || maxMoonBright < 0.99F;
        }

        public boolean isPhaseShift() {
            return targetMoonPhase >= 0;
        }
    }

    private MagicRecipeDefs() {
    }

    private static Identifier mc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    public static List<Def> all() {
        return List.of(
                // 1. 第一期法器
                new Def("moonwing_staff", "月翼法杖",
                        mc("stick"),
                        List.of(mc("feather"), mc("feather"), mc("phantom_membrane")),
                        MagicIds.MOONWING_STAFF, 1,
                        400, 0,
                        0.99F, 1.00F, 80),
                new Def("void_charm", "虚空护符",
                        mc("obsidian"),
                        List.of(mc("ender_pearl"), mc("ender_pearl"), mc("amethyst_shard")),
                        MagicIds.VOID_CHARM, 1,
                        0, 400,
                        0.00F, 0.01F, 80),

                // 2. 第二期：非原版饰品与蚀空塑形杖
                new Def("void_bracer", "织空护腕",
                        mc("iron_ingot"),
                        List.of(mc("ender_pearl"), mc("amethyst_shard"), mc("string")),
                        MagicIds.VOID_BRACER, 1,
                        0, 350,
                        0.00F, 0.25F, 80),
                new Def("lunar_anklet", "踏月脚环",
                        mc("copper_ingot"),
                        List.of(mc("feather"), mc("rabbit_foot"), mc("lapis_lazuli")),
                        MagicIds.LUNAR_ANKLET, 1,
                        350, 0,
                        0.75F, 1.00F, 80),
                new Def("void_anchor_cloak", "虚空锚点披风",
                        mc("leather"),
                        List.of(mc("ender_pearl"), mc("crying_obsidian"), mc("chorus_fruit")),
                        MagicIds.VOID_ANCHOR_CLOAK, 1,
                        0, 500,
                        0.00F, 0.01F, 100),
                new Def("moon_siphon_ring", "汲月指环",
                        mc("gold_ingot"),
                        List.of(mc("glowstone_dust"), mc("quartz"), mc("lapis_lazuli")),
                        MagicIds.MOON_SIPHON_RING, 1,
                        450, 0,
                        0.99F, 1.00F, 80),
                new Def("void_builder_staff", "蚀空塑形杖",
                        mc("blaze_rod"),
                        List.of(mc("obsidian"), mc("ender_pearl"), mc("redstone")),
                        MagicIds.VOID_BUILDER_STAFF, 1,
                        0, 450,
                        0.00F, 0.25F, 80),

                // 3. 基础与生态转化
                new Def("cobble_to_deepslate", "深层石材转化",
                        mc("cobblestone"),
                        List.of(mc("coal")),
                        mc("cobbled_deepslate"), 1,
                        0, 100,
                        0.00F, 1.00F, 60),
                new Def("iron_to_diamond", "虚月炼金·钻石",
                        mc("iron_ingot"),
                        List.of(mc("lapis_lazuli"), mc("lapis_lazuli"), mc("redstone"), mc("redstone")),
                        mc("diamond"), 1,
                        300, 300,
                        0.49F, 0.51F, 100),
                new Def("cobble_to_mossy", "苔石转化",
                        mc("cobblestone"),
                        List.of(mc("vine"), mc("vine")),
                        mc("mossy_cobblestone"), 2,
                        0, 80,
                        0.00F, 1.00F, 60),
                new Def("sand_to_soul_sand", "灵魂沙凝炼",
                        mc("sand"),
                        List.of(mc("bone_meal"), mc("coal")),
                        mc("soul_sand"), 2,
                        0, 160,
                        0.00F, 0.01F, 60),
                new Def("dirt_to_mycelium", "菌丝繁育",
                        mc("dirt"),
                        List.of(mc("oak_sapling"), mc("brown_mushroom")),
                        mc("mycelium"), 2,
                        180, 0,
                        0.99F, 1.00F, 60),
                new Def("budding_amethyst", "紫水晶母岩凝成",
                        mc("amethyst_block"),
                        List.of(mc("obsidian"), mc("quartz"), mc("lapis_lazuli")),
                        mc("budding_amethyst"), 1,
                        350, 350,
                        0.49F, 0.51F, 100),

                // 4. 拨月仪式（调节当前经典空岛月相）
                new Def("phase_full_moon", "拨月仪式·满月",
                        mc("glowstone"),
                        List.of(mc("gold_ingot"), mc("lapis_lazuli")),
                        mc("air"), 0,
                        600, 600,
                        0.00F, 1.00F, 80, 0),
                new Def("phase_new_moon", "拨月仪式·新月",
                        mc("obsidian"),
                        List.of(mc("ender_pearl"), mc("coal")),
                        mc("air"), 0,
                        600, 600,
                        0.00F, 1.00F, 80, 4),
                new Def("phase_quarter_moon", "拨月仪式·弦月",
                        mc("amethyst_block"),
                        List.of(mc("quartz"), mc("redstone")),
                        mc("air"), 0,
                        600, 600,
                        0.00F, 1.00F, 80, 2)
        );
    }
}
