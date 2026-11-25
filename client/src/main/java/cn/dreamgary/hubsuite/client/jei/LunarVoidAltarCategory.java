package cn.dreamgary.hubsuite.client.jei;

import cn.dreamgary.hubsuite.magic.MagicIds;
import cn.dreamgary.hubsuite.magic.MagicRecipeDefs;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * JEI 自定义配方分类：虚月祭坛仪式（8 基座环形摆法 + 月相条件 + 双能消耗）。
 */
public final class LunarVoidAltarCategory implements IRecipeCategory<MagicRecipeDefs.Def> {

    public static final IRecipeType<MagicRecipeDefs.Def> TYPE =
            IRecipeType.create(Identifier.fromNamespaceAndPath("hubsuite", "lunar_void_ritual"),
                    MagicRecipeDefs.Def.class);

    private static final int[][] RING_SLOTS = {
            {42, 8}, {68, 34}, {42, 60}, {16, 34},
            {24, 16}, {60, 16}, {24, 52}, {60, 52}
    };

    private final IDrawable icon;

    public LunarVoidAltarCategory(IGuiHelper guiHelper) {
        Item altar = BuiltInRegistries.ITEM.getValue(MagicIds.LUNAR_VOID_ALTAR);
        this.icon = guiHelper.createDrawableItemLike(altar);
    }

    @Override
    public IRecipeType<MagicRecipeDefs.Def> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("虚月祭坛仪式");
    }

    @Override
    public int getWidth() {
        return 172;
    }

    @Override
    public int getHeight() {
        return 98;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MagicRecipeDefs.Def recipe, IFocusGroup focuses) {
        Item center = BuiltInRegistries.ITEM.getValue(recipe.centerId());
        builder.addInputSlot(42, 34)
                .setStandardSlotBackground()
                .add(new ItemStack(center));

        List<Identifier> pedestals = recipe.pedestalIds();
        int idx = 0;
        for (Identifier pid : pedestals) {
            if (idx >= RING_SLOTS.length) {
                break;
            }
            int[] xy = RING_SLOTS[idx++];
            builder.addInputSlot(xy[0], xy[1])
                    .setStandardSlotBackground()
                    .add(new ItemStack(BuiltInRegistries.ITEM.getValue(pid)));
        }
        // 在剩余空基座槽展示所需能量载体提示
        if (recipe.moonCost() > 0 && idx < RING_SLOTS.length) {
            int[] xy = RING_SLOTS[idx++];
            builder.addInputSlot(xy[0], xy[1])
                    .setStandardSlotBackground()
                    .add(new ItemStack(BuiltInRegistries.ITEM.getValue(MagicIds.MOON_CRYSTAL)));
        }
        if (recipe.voidCost() > 0 && idx < RING_SLOTS.length) {
            int[] xy = RING_SLOTS[idx++];
            builder.addInputSlot(xy[0], xy[1])
                    .setStandardSlotBackground()
                    .add(new ItemStack(BuiltInRegistries.ITEM.getValue(MagicIds.VOID_CORE)));
        }

        if (recipe.isPhaseShift()) {
            builder.addOutputSlot(134, 34)
                    .setOutputSlotBackground()
                    .add(new ItemStack(Items.CLOCK));
        } else {
            Item out = BuiltInRegistries.ITEM.getValue(recipe.outputId());
            builder.addOutputSlot(134, 34)
                    .setOutputSlotBackground()
                    .add(new ItemStack(out, Math.max(1, recipe.outputCount())));
        }
    }

    @Override
    public void draw(MagicRecipeDefs.Def recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        graphics.text(font, recipe.displayName(), 96, 8, 0xFF2B2B2B, false);
        graphics.text(font, "→", 104, 38, 0xFF555555, false);

        String phase = describePhaseRequirement(recipe);
        graphics.text(font, "月相: " + phase, 94, 60, 0xFF3355AA, false);

        StringBuilder cost = new StringBuilder();
        if (recipe.moonCost() > 0) {
            cost.append("月能 ").append(recipe.moonCost());
        }
        if (recipe.voidCost() > 0) {
            if (!cost.isEmpty()) {
                cost.append("  ");
            }
            cost.append("虚空 ").append(recipe.voidCost());
        }
        graphics.text(font, cost.toString(), 94, 72, 0xFF6B2CA8, false);
        graphics.text(font, "潜行空手右键中心祭坛启动", 8, 86, 0xFF666666, false);
    }

    private static String describePhaseRequirement(MagicRecipeDefs.Def r) {
        if (r.minMoonBright() >= 0.99F) {
            return "满月";
        }
        if (r.maxMoonBright() <= 0.01F) {
            return "新月";
        }
        if (r.minMoonBright() >= 0.48F && r.maxMoonBright() <= 0.52F) {
            return "弦月 (1:1)";
        }
        if (r.minMoonBright() >= 0.70F) {
            return "盈月";
        }
        if (r.maxMoonBright() <= 0.30F) {
            return "残月";
        }
        return "任意夜晚";
    }
}
