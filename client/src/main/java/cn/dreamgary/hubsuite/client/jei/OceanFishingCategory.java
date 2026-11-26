package cn.dreamgary.hubsuite.client.jei;

import cn.dreamgary.hubsuite.ocean.OceanFishDefs;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI 自定义分类：海岛 / 岩浆海垂钓图鉴查询。
 */
public final class OceanFishingCategory implements IRecipeCategory<OceanFishDefs.JeiFishingEntry> {

    public static final IRecipeType<OceanFishDefs.JeiFishingEntry> TYPE =
            IRecipeType.create(Identifier.fromNamespaceAndPath("hubsuite", "ocean_fishing"),
                    OceanFishDefs.JeiFishingEntry.class);

    private final IDrawable icon;

    public OceanFishingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(Items.FISHING_ROD);
    }

    @Override
    public IRecipeType<OceanFishDefs.JeiFishingEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("海岛 / 岩浆海垂钓");
    }

    @Override
    public int getWidth() {
        return 168;
    }

    @Override
    public int getHeight() {
        return 44;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OceanFishDefs.JeiFishingEntry entry, IFocusGroup focuses) {
        Item item = BuiltInRegistries.ITEM.getValue(entry.itemId());
        if (item != Items.AIR) {
            builder.addOutputSlot(8, 14).add(new ItemStack(item));
        }
        builder.addInputSlot(34, 14).add(new ItemStack(entry.lava() ? Items.LAVA_BUCKET : Items.WATER_BUCKET));
    }

    @Override
    public void draw(OceanFishDefs.JeiFishingEntry entry, IRecipeSlotsView slotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        String dimLabel = entry.lava() ? "§c岩浆海（下界）" : "§b海岛海域";
        String rarity = switch (entry.rarity()) {
            case "legendary" -> "§d传说";
            case "rare" -> "§b稀有";
            default -> "§a精良";
        };
        graphics.text(font, dimLabel + " §7| " + rarity, 58, 8, 0xFFEEEEEE, false);
        graphics.text(font, "§7出没：" + entry.biomeHint(), 58, 22, 0xFFCCCCCC, false);
    }
}
