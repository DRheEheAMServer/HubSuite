package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 蚀空凝晶的方块实体：记录成型时依据原方块硬度算出的虚空能储量。
 */
public class VoidCrystalBlockEntity extends BlockEntity {

    private int storedVoid = 60;

    public VoidCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(MagicBlockEntities.VOID_CRYSTAL, pos, state);
    }

    public int storedVoid() {
        return storedVoid;
    }

    public void setStoredVoid(int storedVoid) {
        this.storedVoid = Math.max(1, storedVoid);
        setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("storedVoid", storedVoid);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        storedVoid = Math.max(1, input.getIntOr("storedVoid", 60));
    }
}
