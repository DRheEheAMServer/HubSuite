package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 观星镜：观测当前经典空岛的精确月相、双能系数、距离满月/新月的天数，以及附近祭坛状态。
 */
public class LunarSpyglassItem extends Item {

    public LunarSpyglassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        long dayTime = sl.getOverworldClockTime();
        int phase = (int) ((dayTime / 24000L) % 8L);
        if (phase < 0) {
            phase += 8;
        }
        float bright = sl.getMoonBrightness(sp.blockPosition());
        double moonFactor = MoonPhaseMath.moonFactor(bright);
        double voidFactor = MoonPhaseMath.voidFactor(bright);
        int toFull = (0 - phase + 8) % 8;
        int toNew = (4 - phase + 8) % 8;

        String timeText = sl.isBrightOutside() ? "\u00A7e白天（注意防晒）" : "\u00A7b夜晚（可进行月光充能/仪式）";
        String balanced = cn.dreamgary.hubsuite.trinket.TrinketEngine.isBalanced(sp)
                ? " \u00A7d| 虚月平衡共鸣：能耗 -25%" : "";

        sp.sendSystemMessage(Component.literal(
                "\u00A7b✦ 观星观测 \u00A77| " + MoonPhaseMath.describeBrightness(bright)
                        + " \u00A77| " + timeText
                        + String.format(" \u00A77| 月能 \u00A7b%.0f%% \u00A77/ 虚空 \u00A75%.0f%%",
                        moonFactor * 100.0, voidFactor * 100.0)
                        + " \u00A77| 距满月 " + toFull + " 天 / 新月 " + toNew + " 天"
                        + balanced), false);

        BlockPos center = findNearbyAltar(sl, sp.blockPosition());
        if (center != null) {
            sp.sendSystemMessage(Component.literal(
                    "\u00A77  附近虚月祭坛：(" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"), false);
        }
        sl.playSound(null, sp.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8F, 1.1F);
        return InteractionResult.SUCCESS;
    }

    private static BlockPos findNearbyAltar(ServerLevel sl, BlockPos origin) {
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (sl.getBlockState(p).is(MagicRegistry.LUNAR_VOID_ALTAR)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
}
