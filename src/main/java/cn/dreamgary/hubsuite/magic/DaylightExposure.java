package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 白天日光暴露处理：
 * <ul>
 *   <li>封闭容器（箱子、木桶、背包）完全安全；</li>
 *   <li>开放式台座（月光祭坛、魔法基座、虚月祭坛）与露天掉落物在白天暴晒时：
 *     <ul>
 *       <li>虚空能物品每秒流失能量；</li>
 *       <li>月光能物品每秒累积过载（满 100 时月光水晶直接碎裂消失；法器则仅碎裂镶嵌宝石，保留空槽本体）。</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class DaylightExposure {

    public enum Result {
        NONE,
        UPDATED,
        /** 载体本体碎裂（如月光水晶），调用方应销毁该物品。 */
        SHATTERED_ITEM,
        /** 法器上的镶嵌宝石碎裂，物品本体已更新为空槽。 */
        SHATTERED_GEM
    }

    private static int tickCounter;

    private DaylightExposure() {
    }

    public static boolean isExposedDaylight(ServerLevel level, BlockPos skyCheckPos) {
        return level != null && level.isBrightOutside() && level.canSeeSky(skyCheckPos);
    }

    /** 每秒对一个暴露在日光下的含能物品执行一次惩罚。 */
    public static Result tickExposed(ServerLevel level, BlockPos fxPos, ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) {
            return Result.NONE;
        }
        MagicEnergyData data = MagicEnergyData.of(stack);
        if (data == null) {
            return Result.NONE;
        }

        boolean changed = false;

        // 1. 虚空能：白天暴露迅速流失
        if (data.isVoidGem() && data.voidEnergy() > 0) {
            int drain = Math.max(15, data.maxVoid() / 20);
            data = data.withVoid(data.voidEnergy() - drain);
            changed = true;
            level.sendParticles(ParticleTypes.SMOKE,
                    fxPos.getX() + 0.5, fxPos.getY() + 1.0, fxPos.getZ() + 0.5,
                    3, 0.15, 0.15, 0.15, 0.01);
        }

        // 2. 月光能：白天暴露过载
        if (data.isMoonGem()) {
            int nextOverload = data.overload() + 20;
            if (nextOverload >= MagicEnergyData.OVERLOAD_LIMIT) {
                level.playSound(null, fxPos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.2F);
                level.sendParticles(ParticleTypes.FLAME,
                        fxPos.getX() + 0.5, fxPos.getY() + 1.0, fxPos.getZ() + 0.5,
                        10, 0.2, 0.2, 0.2, 0.03);
                if (stack.is(MagicRegistry.MOON_CRYSTAL)) {
                    return Result.SHATTERED_ITEM;
                }
                // 法器：仅碎裂镶嵌宝石，保留空槽法器本体
                data.shatterGem().writeTo(stack);
                return Result.SHATTERED_GEM;
            }
            data = data.withMoon(data.moon() + 25).withOverload(nextOverload);
            changed = true;
            level.sendParticles(ParticleTypes.FLAME,
                    fxPos.getX() + 0.5, fxPos.getY() + 1.0, fxPos.getZ() + 0.5,
                    2, 0.12, 0.12, 0.12, 0.01);
        }

        if (changed) {
            data.writeTo(stack);
            return Result.UPDATED;
        }
        return Result.NONE;
    }

    /** 每 20 tick 扫描一次经典空岛维度中的露天魔法掉落物（非空岛维度零开销）。 */
    public static void onServerTick(MinecraftServer server) {
        if (++tickCounter % 20 != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (!MagicScope.allowed(level) || !level.isBrightOutside()) {
                continue;
            }
            for (ItemEntity entity : level.getEntities(EntityType.ITEM, e -> e.isAlive())) {
                ItemStack stack = entity.getItem();
                if (MagicEnergyData.of(stack) == null) {
                    continue;
                }
                BlockPos pos = entity.blockPosition();
                if (!level.canSeeSky(pos)) {
                    continue;
                }
                Result res = tickExposed(level, pos, stack);
                if (res == Result.SHATTERED_ITEM) {
                    entity.discard();
                } else if (res == Result.UPDATED || res == Result.SHATTERED_GEM) {
                    entity.setItem(stack);
                }
            }
        }
    }
}
