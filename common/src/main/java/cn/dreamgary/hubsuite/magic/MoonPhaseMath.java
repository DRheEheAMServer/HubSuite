package cn.dreamgary.hubsuite.magic;

/**
 * 「虚月律」月相与双能比例数学工具（纯函数，无世界状态依赖）。
 */
public final class MoonPhaseMath {

    private MoonPhaseMath() {
    }

    /** 月光能收集/速率系数（满月 1.00，新月保底 0.10）。 */
    public static double moonFactor(float moonBrightness) {
        if (moonBrightness >= 0.99F) {
            return 1.00;
        }
        if (moonBrightness >= 0.74F) {
            return 0.75;
        }
        if (moonBrightness >= 0.49F) {
            return 0.50;
        }
        if (moonBrightness >= 0.24F) {
            return 0.25;
        }
        return 0.10;
    }

    /** 虚空能收集/侵蚀系数（新月 1.00，满月保底 0.20）。 */
    public static double voidFactor(float moonBrightness) {
        if (moonBrightness >= 0.99F) {
            return 0.20;
        }
        if (moonBrightness >= 0.74F) {
            return 0.40;
        }
        if (moonBrightness >= 0.49F) {
            return 0.50;
        }
        if (moonBrightness >= 0.24F) {
            return 0.75;
        }
        return 1.00;
    }

    /** 比例型操作校验：当前月光亮度是否落在配方要求区间内。 */
    public static boolean matchesRange(float brightness, float min, float max) {
        return brightness >= min - 0.02F && brightness <= max + 0.02F;
    }

    public static String describeBrightness(float brightness) {
        if (brightness >= 0.99F) {
            return "满月（月光极盛）";
        }
        if (brightness >= 0.74F) {
            return "凸月（月光偏盛）";
        }
        if (brightness >= 0.49F) {
            return "弦月（虚月 1:1 平衡）";
        }
        if (brightness >= 0.24F) {
            return "眉月/残月（虚空偏盛）";
        }
        return "新月（虚空极盛）";
    }
}
