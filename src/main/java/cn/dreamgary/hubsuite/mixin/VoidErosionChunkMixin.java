package cn.dreamgary.hubsuite.mixin;

import cn.dreamgary.hubsuite.magic.MagicScope;
import cn.dreamgary.hubsuite.magic.VoidErosionManager;
import cn.dreamgary.hubsuite.magic.VoidErosionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 区块随机刻驱动低空（{@code Y <= -48}）方块的虚空侵蚀，仅在 {@link MagicScope} 维度生效。
 */
@Mixin(ServerLevel.class)
public abstract class VoidErosionChunkMixin {

    @Inject(method = "tickChunk", at = @At("TAIL"))
    private void hubsuite$tickVoidErosion(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (randomTickSpeed <= 0 || chunk == null) {
            return;
        }
        ServerLevel self = (ServerLevel) (Object) this;
        if (!MagicScope.allowed(self)) {
            return;
        }
        int minY = self.getMinY();
        int maxY = Math.min(self.getMaxY(), VoidErosionRules.MAX_EROSION_Y);
        if (maxY < minY) {
            return;
        }
        int span = maxY - minY + 1;
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        var rand = self.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < randomTickSpeed; i++) {
            int x = baseX + rand.nextInt(16);
            int z = baseZ + rand.nextInt(16);
            int y = minY + rand.nextInt(span);
            pos.set(x, y, z);
            var state = chunk.getBlockState(pos);
            if (!state.isAir()) {
                VoidErosionManager.onRandomTick(self, pos.immutable(), state);
            }
        }
    }
}
