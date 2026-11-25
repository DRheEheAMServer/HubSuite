package cn.dreamgary.hubsuite.magic;

import cn.dreamgary.hubsuite.HubSuite;
import cn.dreamgary.hubsuite.trinket.TrinketEngine;
import cn.dreamgary.hubsuite.trinket.TrinketMenu;
import cn.dreamgary.hubsuite.trinket.TrinketStorage;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.nio.file.Path;

/**
 * 服务端「虚月律」魔法系统与 Curios 风格饰品栏总注册入口。
 */
public final class MagicRegistry {

    public static MoonAltarBlock MOON_ALTAR;
    public static MagicPedestalBlock MAGIC_PEDESTAL;
    public static LunarVoidAltarBlock LUNAR_VOID_ALTAR;
    public static VoidCrystalBlock VOID_CRYSTAL;
    public static LunarBloomTotemBlock LUNAR_BLOOM_TOTEM;

    public static Item MOON_CRYSTAL;
    public static Item BLAZING_MOON_CRYSTAL;
    public static Item VOID_CORE;
    public static Item MOONWING_STAFF;
    public static Item VOID_CHARM;
    public static Item LUNAR_SPYGLASS;
    public static Item VOID_BUILDER_STAFF;
    public static Item VOID_BRACER;
    public static Item LUNAR_ANKLET;
    public static Item VOID_ANCHOR_CLOAK;
    public static Item MOON_SIPHON_RING;

    private MagicRegistry() {
    }

    public static void register(Path configDir) {
        if (MOON_ALTAR != null) {
            return;
        }
        TrinketMenu.registerType();
        TrinketStorage.init(configDir);

        MOON_ALTAR = Registry.register(BuiltInRegistries.BLOCK, MagicIds.MOON_ALTAR,
                new MoonAltarBlock(MagicIds.altarBlockProps(MagicIds.MOON_ALTAR)));
        MAGIC_PEDESTAL = Registry.register(BuiltInRegistries.BLOCK, MagicIds.MAGIC_PEDESTAL,
                new MagicPedestalBlock(MagicIds.altarBlockProps(MagicIds.MAGIC_PEDESTAL)));
        LUNAR_VOID_ALTAR = Registry.register(BuiltInRegistries.BLOCK, MagicIds.LUNAR_VOID_ALTAR,
                new LunarVoidAltarBlock(MagicIds.altarBlockProps(MagicIds.LUNAR_VOID_ALTAR)));
        VOID_CRYSTAL = Registry.register(BuiltInRegistries.BLOCK, MagicIds.VOID_CRYSTAL,
                new VoidCrystalBlock(MagicIds.voidCrystalBlockProps()));
        LUNAR_BLOOM_TOTEM = Registry.register(BuiltInRegistries.BLOCK, MagicIds.LUNAR_BLOOM_TOTEM,
                new LunarBloomTotemBlock(MagicIds.altarBlockProps(MagicIds.LUNAR_BLOOM_TOTEM)));

        MagicBlockEntities.registerAll(
                MOON_ALTAR, MoonAltarBlockEntity::new,
                MAGIC_PEDESTAL, MagicPedestalBlockEntity::new,
                LUNAR_VOID_ALTAR, LunarVoidAltarBlockEntity::new,
                VOID_CRYSTAL, VoidCrystalBlockEntity::new,
                LUNAR_BLOOM_TOTEM, LunarBloomTotemBlockEntity::new);

        Registry.register(BuiltInRegistries.ITEM, MagicIds.MOON_ALTAR,
                new BlockItem(MOON_ALTAR, MagicIds.blockItemProps(MagicIds.MOON_ALTAR)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.MAGIC_PEDESTAL,
                new BlockItem(MAGIC_PEDESTAL, MagicIds.blockItemProps(MagicIds.MAGIC_PEDESTAL)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_VOID_ALTAR,
                new BlockItem(LUNAR_VOID_ALTAR, MagicIds.blockItemProps(MagicIds.LUNAR_VOID_ALTAR)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_CRYSTAL,
                new BlockItem(VOID_CRYSTAL, MagicIds.blockItemProps(MagicIds.VOID_CRYSTAL)));
        Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_BLOOM_TOTEM,
                new BlockItem(LUNAR_BLOOM_TOTEM, MagicIds.blockItemProps(MagicIds.LUNAR_BLOOM_TOTEM)));

        MOON_CRYSTAL = Registry.register(BuiltInRegistries.ITEM, MagicIds.MOON_CRYSTAL,
                new MagicCarrierItem(MagicIds.carrierItemProps(MagicIds.MOON_CRYSTAL, Rarity.UNCOMMON), true));
        BLAZING_MOON_CRYSTAL = Registry.register(BuiltInRegistries.ITEM, MagicIds.BLAZING_MOON_CRYSTAL,
                new MagicCarrierItem(MagicIds.carrierItemProps(MagicIds.BLAZING_MOON_CRYSTAL, Rarity.RARE), true));
        VOID_CORE = Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_CORE,
                new MagicCarrierItem(MagicIds.carrierItemProps(MagicIds.VOID_CORE, Rarity.UNCOMMON), false));
        MOONWING_STAFF = Registry.register(BuiltInRegistries.ITEM, MagicIds.MOONWING_STAFF,
                new MoonwingStaffItem(MagicIds.carrierItemProps(MagicIds.MOONWING_STAFF, Rarity.RARE)));
        VOID_CHARM = Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_CHARM,
                new VoidCharmItem(MagicIds.carrierItemProps(MagicIds.VOID_CHARM, Rarity.RARE)));
        LUNAR_SPYGLASS = Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_SPYGLASS,
                new LunarSpyglassItem(MagicIds.simpleItemProps(MagicIds.LUNAR_SPYGLASS, 1, Rarity.UNCOMMON)));
        VOID_BUILDER_STAFF = Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_BUILDER_STAFF,
                new VoidBuilderStaffItem(MagicIds.carrierItemProps(MagicIds.VOID_BUILDER_STAFF, Rarity.RARE)));
        VOID_BRACER = Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_BRACER,
                new TrinketMagicItem(MagicIds.carrierItemProps(MagicIds.VOID_BRACER, Rarity.RARE), false));
        LUNAR_ANKLET = Registry.register(BuiltInRegistries.ITEM, MagicIds.LUNAR_ANKLET,
                new TrinketMagicItem(MagicIds.carrierItemProps(MagicIds.LUNAR_ANKLET, Rarity.RARE), true));
        VOID_ANCHOR_CLOAK = Registry.register(BuiltInRegistries.ITEM, MagicIds.VOID_ANCHOR_CLOAK,
                new TrinketMagicItem(MagicIds.carrierItemProps(MagicIds.VOID_ANCHOR_CLOAK, Rarity.RARE), false));
        MOON_SIPHON_RING = Registry.register(BuiltInRegistries.ITEM, MagicIds.MOON_SIPHON_RING,
                new TrinketMagicItem(MagicIds.carrierItemProps(MagicIds.MOON_SIPHON_RING, Rarity.RARE), true));

        VoidErosionManager.init(configDir);
        ServerTickEvents.END_SERVER_TICK.register(DaylightExposure::onServerTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(VoidCharmItem::allowDamage);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world instanceof ServerLevel sl) {
                VoidErosionManager.clearAt(sl, pos);
            }
        });
        TrinketEngine.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
                dispatcher.register(Commands.literal("trinket").executes(ctx -> {
                    if (ctx.getSource().getEntity() instanceof ServerPlayer sp) {
                        TrinketStorage.openMenu(sp);
                    }
                    return Command.SINGLE_SUCCESS;
                })));

        HubSuite.logger().info("虚月律魔法系统已就绪（经典空岛专属：5 方块 / 11 物品 + 5 格饰品栏）");
    }
}
