package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 虚月祭坛仪式配方表（基于共用 {@link MagicRecipeDefs} 解析为服务端 {@link Item}）。
 */
public final class MagicRecipes {

    public record Recipe(String id,
                         String displayName,
                         Item centerInput,
                         List<Item> pedestalInputs,
                         Item output,
                         int outputCount,
                         int moonCost,
                         int voidCost,
                         float minMoonBright,
                         float maxMoonBright,
                         int baseTicks,
                         int targetMoonPhase) {

        public boolean isRatioBased() {
            return minMoonBright > 0.01F || maxMoonBright < 0.99F;
        }

        public boolean isPhaseShift() {
            return targetMoonPhase >= 0;
        }
    }

    private MagicRecipes() {
    }

    public static List<Recipe> all() {
        List<MagicRecipeDefs.Def> defs = MagicRecipeDefs.all();
        List<Recipe> out = new ArrayList<>(defs.size());
        for (MagicRecipeDefs.Def d : defs) {
            Item center = BuiltInRegistries.ITEM.getValue(d.centerId());
            List<Item> inputs = new ArrayList<>(d.pedestalIds().size());
            for (var pid : d.pedestalIds()) {
                inputs.add(BuiltInRegistries.ITEM.getValue(pid));
            }
            Item result = BuiltInRegistries.ITEM.getValue(d.outputId());
            out.add(new Recipe(d.id(), d.displayName(), center, inputs, result, d.outputCount(),
                    d.moonCost(), d.voidCost(), d.minMoonBright(), d.maxMoonBright(),
                    d.baseTicks(), d.targetMoonPhase()));
        }
        return out;
    }

    /** 匹配中心主材与基座材料（基座上同时也可放用于供能的水晶/晶核）。 */
    public static Recipe match(ItemStack center, List<ItemStack> pedestalStacks) {
        if (center == null || center.isEmpty()) {
            return null;
        }
        Map<Item, Integer> available = new LinkedHashMap<>();
        for (ItemStack s : pedestalStacks) {
            if (s != null && !s.isEmpty() && !isEnergyCarrier(s)) {
                available.merge(s.getItem(), 1, Integer::sum);
            }
        }
        for (Recipe r : all()) {
            if (!center.is(r.centerInput())) {
                continue;
            }
            Map<Item, Integer> need = new LinkedHashMap<>();
            for (Item item : r.pedestalInputs()) {
                need.merge(item, 1, Integer::sum);
            }
            if (available.equals(need)) {
                return r;
            }
        }
        return null;
    }

    public static boolean isEnergyCarrier(ItemStack stack) {
        return stack != null && (stack.is(MagicRegistry.MOON_CRYSTAL)
                || stack.is(MagicRegistry.BLAZING_MOON_CRYSTAL)
                || stack.is(MagicRegistry.VOID_CORE));
    }

    public static int totalMoonEnergy(List<ItemStack> pedestalStacks) {
        int sum = 0;
        for (ItemStack s : pedestalStacks) {
            MagicEnergyData d = MagicEnergyData.of(s);
            if (d != null && d.isMoonGem()) {
                sum += d.moon();
            }
        }
        return sum;
    }

    public static int totalVoidEnergy(List<ItemStack> pedestalStacks) {
        int sum = 0;
        for (ItemStack s : pedestalStacks) {
            MagicEnergyData d = MagicEnergyData.of(s);
            if (d != null && d.isVoidGem()) {
                sum += d.voidEnergy();
            }
        }
        return sum;
    }
}
