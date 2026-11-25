package cn.dreamgary.hubsuite.client.jei;

import cn.dreamgary.hubsuite.magic.MagicIds;
import cn.dreamgary.hubsuite.magic.MagicRecipeDefs;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * HubSuite 客户端 JEI 插件（软依赖，装了 JEI 自动加载）。
 */
@JeiPlugin
public final class HubSuiteJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath("hubsuite", "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new LunarVoidAltarCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(LunarVoidAltarCategory.TYPE, MagicRecipeDefs.all());

        registration.addIngredientInfo(
                item(MagicIds.BLAZING_MOON_CRYSTAL),
                Component.literal("【过载淬炼】将月光水晶放在月光祭坛白天暴晒，过载度达到 75% ~ 95% 时及时取下，即可淬炼升华为炽月晶（容量 1500、充能效率 ×1.5）。超过 100% 会碎裂！"));
        registration.addIngredientInfo(
                item(MagicIds.VOID_CRYSTAL),
                Component.literal("【虚空侵蚀】在经典空岛 Y <= -48 的虚空边缘放置任意方块，随时间侵蚀为不可破坏的蚀空凝晶（原方块越硬耗时越久但含能越高）。手持虚空晶核右键吸取后方块消散。"));
        registration.addIngredientInfo(
                item(MagicIds.VOID_CORE),
                Component.literal("手持虚空晶核右键 Y <= -48 处生成的「蚀空凝晶」即可吸收虚空能；白天露天放置或丢在地上会流失能量。"));
        registration.addIngredientInfo(
                item(MagicIds.MOON_ALTAR),
                Component.literal("夜晚露天放置月光水晶或炽月晶即可充能（满月最快）；白天露天放置会累积过载。"));
        registration.addIngredientInfo(
                item(MagicIds.LUNAR_BLOOM_TOTEM),
                Component.literal("摆在农田或树场旁并放入月光水晶/炽月晶，夜晚自动消耗月能催熟周围 5×5（满月 7×7）作物与树苗。"));
        registration.addIngredientInfo(
                item(MagicIds.VOID_BRACER),
                Component.literal("按 B 键、点击背包左上角「✦」按钮或输入 /trinket 打开 5 格虚月饰品栏；月系与虚空系饰品 1:1 佩戴时触发虚月平衡共鸣（能耗 -25%）。"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(LunarVoidAltarCategory.TYPE, item(MagicIds.LUNAR_VOID_ALTAR));
        registration.addCraftingStation(LunarVoidAltarCategory.TYPE, item(MagicIds.MAGIC_PEDESTAL));
    }

    private static Item item(Identifier id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }
}
