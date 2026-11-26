package cn.dreamgary.hubsuite.client.magic;

import cn.dreamgary.hubsuite.client.HubSuiteClient;
import cn.dreamgary.hubsuite.magic.MagicBlockEntities;
import cn.dreamgary.hubsuite.magic.MagicIds;
import cn.dreamgary.hubsuite.magic.MagicSocketing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 客户端侧魔法方块、方块实体类型与物品注册（与服务端 ID 一一对应）。
 */
public final class ClientMagicRegistry {

    private ClientMagicRegistry() {
    }

    public static void register() {
        Block moonAltar = Registry.register(BuiltInRegistries.BLOCK, MagicIds.MOON_ALTAR,
                new ClientInteractiveBlock(MagicIds.altarBlockProps(MagicIds.MOON_ALTAR)));
        Block pedestal = Registry.register(BuiltInRegistries.BLOCK, MagicIds.MAGIC_PEDESTAL,
                new ClientInteractiveBlock(MagicIds.altarBlockProps(MagicIds.MAGIC_PEDESTAL)));
        Block lunarVoidAltar = Registry.register(BuiltInRegistries.BLOCK, MagicIds.LUNAR_VOID_ALTAR,
                new ClientInteractiveBlock(MagicIds.altarBlockProps(MagicIds.LUNAR_VOID_ALTAR)));
        Block voidCrystal = Registry.register(BuiltInRegistries.BLOCK, MagicIds.VOID_CRYSTAL,
                new Block(MagicIds.voidCrystalBlockProps()));
        Block bloomTotem = Registry.register(BuiltInRegistries.BLOCK, MagicIds.LUNAR_BLOOM_TOTEM,
                new ClientInteractiveBlock(MagicIds.altarBlockProps(MagicIds.LUNAR_BLOOM_TOTEM)));

        MagicBlockEntities.registerAll(
                moonAltar, (pos, st) -> new ClientStubBlockEntity(MagicBlockEntities.MOON_ALTAR, pos, st),
                pedestal, (pos, st) -> new ClientStubBlockEntity(MagicBlockEntities.MAGIC_PEDESTAL, pos, st),
                lunarVoidAltar, (pos, st) -> new ClientStubBlockEntity(MagicBlockEntities.LUNAR_VOID_ALTAR, pos, st),
                voidCrystal, (pos, st) -> new ClientStubBlockEntity(MagicBlockEntities.VOID_CRYSTAL, pos, st),
                bloomTotem, (pos, st) -> new ClientStubBlockEntity(MagicBlockEntities.LUNAR_BLOOM_TOTEM, pos, st));

        Registry.register(BuiltInRegistries.ITEM, MagicIds.MOON_ALTAR,
                new BlockItem(moonAltar, MagicIds.blockItemProps(MagicIds.MOON_ALTAR)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.MAGIC_PEDESTAL,
                new BlockItem(pedestal, MagicIds.blockItemProps(MagicIds.MAGIC_PEDESTAL)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_VOID_ALTAR,
                new BlockItem(lunarVoidAltar, MagicIds.blockItemProps(MagicIds.LUNAR_VOID_ALTAR)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_CRYSTAL,
                new BlockItem(voidCrystal, MagicIds.blockItemProps(MagicIds.VOID_CRYSTAL)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_BLOOM_TOTEM,
                new BlockItem(bloomTotem, MagicIds.blockItemProps(MagicIds.LUNAR_BLOOM_TOTEM)));

        Registry.register(BuiltInRegistries.ITEM, MagicIds.MOON_CRYSTAL,
                new Item(MagicIds.carrierItemProps(MagicIds.MOON_CRYSTAL, Rarity.UNCOMMON)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.BLAZING_MOON_CRYSTAL,
                new Item(MagicIds.carrierItemProps(MagicIds.BLAZING_MOON_CRYSTAL, Rarity.RARE)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_CORE,
                new Item(MagicIds.carrierItemProps(MagicIds.VOID_CORE, Rarity.UNCOMMON)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_SPYGLASS,
                new Item(MagicIds.simpleItemProps(MagicIds.LUNAR_SPYGLASS, 1, Rarity.UNCOMMON)));

        Registry.register(BuiltInRegistries.ITEM, MagicIds.MOONWING_STAFF,
                new ClientSocketableItem(MagicIds.carrierItemProps(MagicIds.MOONWING_STAFF, Rarity.RARE), true));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_CHARM,
                new ClientSocketableItem(MagicIds.carrierItemProps(MagicIds.VOID_CHARM, Rarity.RARE), false));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_BUILDER_STAFF,
                new ClientSocketableItem(MagicIds.carrierItemProps(MagicIds.VOID_BUILDER_STAFF, Rarity.RARE), false));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_BRACER,
                new ClientSocketableItem(MagicIds.carrierItemProps(MagicIds.VOID_BRACER, Rarity.RARE), false));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_ANKLET,
                new ClientSocketableItem(MagicIds.carrierItemProps(MagicIds.LUNAR_ANKLET, Rarity.RARE), true));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_ANCHOR_CLOAK,
                new ClientSocketableItem(MagicIds.carrierItemProps(MagicIds.VOID_ANCHOR_CLOAK, Rarity.RARE), false));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.MOON_SIPHON_RING,
                new ClientSocketableItem(MagicIds.carrierItemProps(MagicIds.MOON_SIPHON_RING, Rarity.RARE), true));

        HubSuiteClient.LOGGER.info("已注册客户端魔法方块与物品（5 方块 / 11 物品）");
    }

    private static final class ClientInteractiveBlock extends Block {
        ClientInteractiveBlock(Properties properties) {
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

    private static final class ClientSocketableItem extends Item {
        private final boolean moonFaction;

        ClientSocketableItem(Properties props, boolean moonFaction) {
            super(props);
            this.moonFaction = moonFaction;
        }

        @Override
        public boolean overrideOtherStackedOnMe(ItemStack artifact, ItemStack other, Slot slot,
                                                ClickAction action, Player player, SlotAccess access) {
            if (!slot.allowModification(player)) {
                return false;
            }
            if (MagicSocketing.trySocketFaction(artifact, other, moonFaction, player, false)) {
                slot.setChanged();
                return true;
            }
            return false;
        }

        @Override
        public boolean overrideStackedOnOther(ItemStack artifact, Slot slot, ClickAction action, Player player) {
            if (!slot.allowModification(player)) {
                return false;
            }
            if (MagicSocketing.trySocketFaction(artifact, slot.getItem(), moonFaction, player, false)) {
                slot.setChanged();
                return true;
            }
            return false;
        }
    }
}
