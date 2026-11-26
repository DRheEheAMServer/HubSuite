package cn.dreamgary.hubsuite.client.ocean;

import cn.dreamgary.hubsuite.client.HubSuiteClient;
import cn.dreamgary.hubsuite.ocean.OceanBlockIds;
import cn.dreamgary.hubsuite.ocean.OceanFishDefs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 客户端侧海岛扩展方块、方块实体与物品注册（与服务端 ID 一一对应）。
 */
public final class ClientOceanRegistry {

    private ClientOceanRegistry() {
    }

    public static void register() {
        Block fishTrap = Registry.register(BuiltInRegistries.BLOCK, OceanBlockIds.FISH_TRAP,
                new ClientTrapBlock(OceanBlockIds.fishTrapBlockProps()));
        OceanBlockIds.registerBlockEntity(fishTrap,
                (pos, st) -> new ClientStubBlockEntity(OceanBlockIds.FISH_TRAP_BE, pos, st));
        Registry.register(BuiltInRegistries.ITEM, OceanBlockIds.FISH_TRAP,
                new BlockItem(fishTrap, OceanBlockIds.fishTrapItemProps()));

        for (OceanFishDefs.FishDef def : OceanFishDefs.ALL_FISH) {
            Registry.register(BuiltInRegistries.ITEM, def.id(),
                    new Item(OceanFishDefs.itemProps(def.id(), 16, def.mcRarity())));
        }
        for (OceanFishDefs.DishDef dish : OceanFishDefs.ALL_DISHES) {
            Registry.register(BuiltInRegistries.ITEM, dish.id(),
                    new Item(OceanFishDefs.itemProps(dish.id(), 16, dish.rarity())));
        }

        HubSuiteClient.LOGGER.info("已注册客户端海岛扩展（1 蟹笼 + 9 奇鱼 + 3 海味料理）");
    }

    private static final class ClientTrapBlock extends Block {
        ClientTrapBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hit) {
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
            return hand == InteractionHand.MAIN_HAND ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
    }

    private static final class ClientStubBlockEntity extends BlockEntity {
        ClientStubBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
