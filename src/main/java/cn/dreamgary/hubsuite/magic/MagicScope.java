package cn.dreamgary.hubsuite.magic;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * 空岛「虚月律」魔法系统的作用域：默认仅限经典空岛（{@code hubsuite:skyblock_classic*}）。
 */
public final class MagicScope {

    private MagicScope() {
    }

    public static boolean allowed(Level level) {
        if (level == null) {
            return false;
        }
        Identifier id = level.dimension().identifier();
        return id.getPath().contains("skyblock_classic");
    }
}
