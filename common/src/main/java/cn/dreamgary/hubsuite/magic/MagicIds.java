package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;

/**
 * 空岛「虚月律」魔法系统的双端共用 ID 与属性定义。
 */
public final class MagicIds {

    // ---- 方块 ID ----
    public static final Identifier MOON_ALTAR = id("moon_altar");
    public static final Identifier MAGIC_PEDESTAL = id("magic_pedestal");
    public static final Identifier LUNAR_VOID_ALTAR = id("lunar_void_altar");
    public static final Identifier VOID_CRYSTAL = id("void_crystal");
    public static final Identifier LUNAR_BLOOM_TOTEM = id("lunar_bloom_totem");

    // ---- 物品 ID ----
    public static final Identifier MOON_CRYSTAL = id("moon_crystal");
    public static final Identifier BLAZING_MOON_CRYSTAL = id("blazing_moon_crystal");
    public static final Identifier VOID_CORE = id("void_core");
    public static final Identifier MOONWING_STAFF = id("moonwing_staff");
    public static final Identifier VOID_CHARM = id("void_charm");
    public static final Identifier LUNAR_SPYGLASS = id("lunar_spyglass");
    public static final Identifier VOID_BUILDER_STAFF = id("void_builder_staff");
    public static final Identifier VOID_BRACER = id("void_bracer");
    public static final Identifier LUNAR_ANKLET = id("lunar_anklet");
    public static final Identifier VOID_ANCHOR_CLOAK = id("void_anchor_cloak");
    public static final Identifier MOON_SIPHON_RING = id("moon_siphon_ring");

    /** 全部魔法方块 ID（创造模式标签页与资源校验共用）。 */
    public static final List<Identifier> ALL_BLOCKS = List.of(
            MOON_ALTAR, MAGIC_PEDESTAL, LUNAR_VOID_ALTAR, VOID_CRYSTAL, LUNAR_BLOOM_TOTEM);

    /** 全部魔法物品 ID（含方块物品）。 */
    public static final List<Identifier> ALL_ITEMS = List.of(
            MOON_ALTAR, MAGIC_PEDESTAL, LUNAR_VOID_ALTAR, VOID_CRYSTAL, LUNAR_BLOOM_TOTEM,
            MOON_CRYSTAL, BLAZING_MOON_CRYSTAL, VOID_CORE,
            MOONWING_STAFF, VOID_CHARM, LUNAR_SPYGLASS, VOID_BUILDER_STAFF,
            VOID_BRACER, LUNAR_ANKLET, VOID_ANCHOR_CLOAK, MOON_SIPHON_RING);

    private MagicIds() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("hubsuite", path);
    }

    public static Block.Properties altarBlockProps(Identifier id) {
        return Block.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .strength(3.0F, 6.0F)
                .noOcclusion()
                .sound(SoundType.STONE);
    }

    public static Block.Properties voidCrystalBlockProps() {
        return Block.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, VOID_CRYSTAL))
                .strength(-1.0F, 3600000.0F)
                .pushReaction(PushReaction.BLOCK)
                .noLootTable()
                .sound(SoundType.AMETHYST);
    }

    public static Item.Properties blockItemProps(Identifier id) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix()
                .component(DataComponents.ITEM_MODEL, id);
    }

    public static Item.Properties simpleItemProps(Identifier id, int maxStack, Rarity rarity) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(maxStack)
                .rarity(rarity)
                .component(DataComponents.ITEM_NAME,
                        Component.translatable("item.hubsuite." + id.getPath()))
                .component(DataComponents.ITEM_MODEL, id);
    }

    public static Item.Properties carrierItemProps(Identifier id, Rarity rarity) {
        return simpleItemProps(id, 1, rarity).durability(1000);
    }
}
