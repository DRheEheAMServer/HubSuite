package cn.dreamgary.hubsuite.item;

import cn.dreamgary.hubsuite.rod.AssemblyTableIds;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本模组的**创造模式物品栏标签页**（0.5.0 起）。
 *
 * <p><b>为什么必须有它：</b>物品只注册进 {@link BuiltInRegistries#ITEM}、却不属于任何
 * {@link CreativeModeTab} 时，会出两个问题 ——
 * <ul>
 *   <li>创造模式物品栏里翻不到它们（创造模式是按标签页分组的）；</li>
 *   <li>JEI 虽然会兜底收录注册表里的全部物品，但未分组的条目排在**所有原版物品之后**，
 *       实际上等同于找不到（JEI 先按标签页构建列表，再补注册表里剩下的）。</li>
 * </ul>
 *
 * <p><b>两端都要注册，且 id 必须一致</b>：服务端在 {@code HubSuite.onInitialize}、
 * 客户端在 {@code HubSuiteClient.onInitializeClient}。物品本身两端都是各自注册的，
 * 所以这里一律按 **id 从注册表里取**，不持有任何静态物品引用 —— 否则两端会拿到对方那份对象。
 *
 * <p>不放进去的：{@code lava_rod}（遗留别名，只能被进服换算掉，不应该再发出去）。
 */
public final class HubSuiteCreativeTabs {

    private static final Logger LOGGER = LoggerFactory.getLogger("HubSuite");

    /** 标签页 id：{@code hubsuite:main}。 */
    public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(HubSuiteItemDefs.NAMESPACE, "main"));

    /** 标签页标题的翻译键。 */
    public static final String TITLE_KEY = "itemGroup.hubsuite.main";

    /** 标签页里放什么（按 id，两端各自解析成自己那份物品）。 */
    private static final List<Identifier> CONTENT_IDS = buildContentIds();

    private static List<Identifier> buildContentIds() {
        List<Identifier> ids = new ArrayList<>();
        ids.add(HubSuiteItemDefs.NETHER_STRING.id());
        ids.add(HubSuiteItemDefs.WOODEN_FISHING_BUCKET.id());
        ids.add(HubSuiteItemDefs.IRON_FISHING_BUCKET.id());
        ids.add(HubSuiteItemDefs.DIAMOND_FISHING_BUCKET.id());
        ids.add(AssemblyTableIds.ID);
        ids.add(HubSuiteItemDefs.BASIC_BAIT.id());
        ids.add(HubSuiteItemDefs.REFINED_BAIT.id());
        ids.add(HubSuiteItemDefs.ABYSSAL_BAIT.id());
        ids.add(HubSuiteItemDefs.EMBER_BAIT.id());
        ids.add(HubSuiteItemDefs.MAGMA_BAIT.id());
        ids.add(HubSuiteItemDefs.INFERNAL_BAIT.id());
        ids.add(HubSuiteItemDefs.TREASURE_BAIT.id());
        ids.add(HubSuiteItemDefs.TWIN_BAIT.id());
        ids.add(cn.dreamgary.hubsuite.ocean.OceanBlockIds.FISH_TRAP);
        ids.addAll(cn.dreamgary.hubsuite.ocean.OceanFishDefs.ALL_ITEM_IDS);
        ids.addAll(cn.dreamgary.hubsuite.magic.MagicIds.ALL_ITEMS);
        return List.copyOf(ids);
    }

    private HubSuiteCreativeTabs() {
    }

    /** 注册标签页。两端各调一次，重复调用直接返回（幂等）。 */
    public static void register() {
        if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(MAIN.identifier())) {
            return;
        }
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN,
                FabricCreativeModeTab.builder()
                        .title(Component.translatable(TITLE_KEY))
                        .icon(HubSuiteCreativeTabs::icon)
                        .displayItems((parameters, output) -> contents().forEach(output::accept))
                        .build());
        LOGGER.info("已注册创造模式标签页 {}（{} 个物品）", MAIN.identifier(), contents().size());
    }

    /** 标签页内容：按 id 从物品注册表取，取不到（未注册）就跳过。 */
    private static List<Item> contents() {
        List<Item> items = new ArrayList<>(CONTENT_IDS.size());
        for (Identifier id : CONTENT_IDS) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        return items;
    }

    /** 标签页图标：优先用装配台，没有就退回地狱线，再退回原版钓竿。 */
    private static ItemStack icon() {
        Item table = BuiltInRegistries.ITEM.getValue(AssemblyTableIds.ID);
        if (table != Items.AIR) {
            return new ItemStack(table);
        }
        Item netherString = BuiltInRegistries.ITEM.getValue(HubSuiteItemDefs.NETHER_STRING.id());
        if (netherString != Items.AIR) {
            return new ItemStack(netherString);
        }
        return new ItemStack(Items.FISHING_ROD);
    }
}
