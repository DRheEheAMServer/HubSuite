package cn.dreamgary.hubsuite.magic;

/**
 * 虚空侵蚀数值规则（纯函数，双端共用/自检直测）。
 *
 * <p>规则：必须在 {@code Y <= -48} 的低空区域；原方块硬度越高，
 * 侵蚀所需随机刻越多，成型后「蚀空凝晶」蕴含的虚空能也越高。
 */
public final class VoidErosionRules {

    /** 触发虚空侵蚀的最大 Y 坐标（含）。 */
    public static final int MAX_EROSION_Y = -48;

    private VoidErosionRules() {
    }

    /** 完全侵蚀所需的累计进度（20..200）。 */
    public static int requiredProgress(float hardness) {
        float h = Math.max(0.0F, hardness);
        int raw = (int) Math.floor(20.0 + 18.0 * Math.sqrt(h));
        return Math.max(20, Math.min(200, raw));
    }

    /** 成型后「蚀空凝晶」蕴含的虚空能总量（40..500）。 */
    public static int storedVoid(float hardness) {
        float h = Math.max(0.0F, hardness);
        int raw = (int) Math.floor(40.0 + 55.0 * Math.sqrt(h));
        return Math.max(40, Math.min(500, raw));
    }

    /** 单次随机刻推进的进度步长（新月 = 10，满月 = 2）。 */
    public static int stepForMoonBrightness(float moonBrightness) {
        return Math.max(1, (int) Math.round(10.0 * MoonPhaseMath.voidFactor(moonBrightness)));
    }
}
