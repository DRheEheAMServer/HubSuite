package cn.dreamgary.hubsuite.ocean;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 海岛蟹笼方块与方块实体 ID 定义（双端同源）。
 */
public final class OceanBlockIds {

    public static final Identifier FISH_TRAP = Identifier.fromNamespaceAndPath("hubsuite", "fish_trap");
    public static BlockEntityType<BlockEntity> FISH_TRAP_BE;

    @FunctionalInterface
    public interface Factory {
        BlockEntity create(BlockPos pos, BlockState state);
    }

    private OceanBlockIds() {
    }

    public static Block.Properties fishTrapBlockProps() {
        return Block.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, FISH_TRAP))
                .strength(2.0F, 3.0F)
                .noOcclusion()
                .sound(SoundType.WOOD);
    }

    public static Item.Properties fishTrapItemProps() {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, FISH_TRAP))
                .useBlockDescriptionPrefix()
                .component(DataComponents.ITEM_MODEL, FISH_TRAP);
    }

    public static BlockEntityType<BlockEntity> registerBlockEntity(Block block, Factory factory) {
        if (FISH_TRAP_BE == null) {
            FISH_TRAP_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, FISH_TRAP,
                    FabricBlockEntityTypeBuilder.create(factory::create, block).build());
        }
        return FISH_TRAP_BE;
    }
}
