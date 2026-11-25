package cn.dreamgary.hubsuite.magic;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 魔法系统方块实体类型（双端必须同时注册同 ID，保证注册表同步对齐）。
 */
public final class MagicBlockEntities {

    public static BlockEntityType<BlockEntity> MOON_ALTAR;
    public static BlockEntityType<BlockEntity> MAGIC_PEDESTAL;
    public static BlockEntityType<BlockEntity> LUNAR_VOID_ALTAR;
    public static BlockEntityType<BlockEntity> VOID_CRYSTAL;
    public static BlockEntityType<BlockEntity> LUNAR_BLOOM_TOTEM;

    @FunctionalInterface
    public interface Factory {
        BlockEntity create(BlockPos pos, BlockState state);
    }

    private MagicBlockEntities() {
    }

    public static void registerAll(Block moonAltar, Factory moonFactory,
                                   Block pedestal, Factory pedestalFactory,
                                   Block lunarVoidAltar, Factory lunarVoidFactory,
                                   Block voidCrystal, Factory crystalFactory,
                                   Block lunarBloomTotem, Factory bloomFactory) {
        if (MOON_ALTAR == null) {
            MOON_ALTAR = reg(MagicIds.MOON_ALTAR, moonAltar, moonFactory);
        }
        if (MAGIC_PEDESTAL == null) {
            MAGIC_PEDESTAL = reg(MagicIds.MAGIC_PEDESTAL, pedestal, pedestalFactory);
        }
        if (LUNAR_VOID_ALTAR == null) {
            LUNAR_VOID_ALTAR = reg(MagicIds.LUNAR_VOID_ALTAR, lunarVoidAltar, lunarVoidFactory);
        }
        if (VOID_CRYSTAL == null) {
            VOID_CRYSTAL = reg(MagicIds.VOID_CRYSTAL, voidCrystal, crystalFactory);
        }
        if (LUNAR_BLOOM_TOTEM == null) {
            LUNAR_BLOOM_TOTEM = reg(MagicIds.LUNAR_BLOOM_TOTEM, lunarBloomTotem, bloomFactory);
        }
    }

    private static BlockEntityType<BlockEntity> reg(Identifier id, Block block, Factory factory) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(factory::create, block).build());
    }
}
