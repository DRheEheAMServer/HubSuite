package cn.dreamgary.hubsuite.trinket;

import cn.dreamgary.hubsuite.magic.MagicIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全部可佩戴饰品的槽位类别与虚月派系定义（双端共用）。
 */
public final class TrinketDefs {

    public enum Faction {
        NONE,
        MOON,
        VOID
    }

    public record Def(Identifier id, TrinketSlotType.Category category, Faction faction) {
    }

    private static final Map<Identifier, Def> BY_ID = new LinkedHashMap<>();

    static {
        reg(MagicIds.VOID_CHARM, TrinketSlotType.Category.AMULET, Faction.VOID);
        reg(MagicIds.LUNAR_ANKLET, TrinketSlotType.Category.RING, Faction.MOON);
        reg(MagicIds.MOON_SIPHON_RING, TrinketSlotType.Category.RING, Faction.MOON);
        reg(MagicIds.VOID_ANCHOR_CLOAK, TrinketSlotType.Category.CLOAK, Faction.VOID);
        reg(MagicIds.VOID_BRACER, TrinketSlotType.Category.WRIST, Faction.VOID);
    }

    private static void reg(Identifier id, TrinketSlotType.Category category, Faction faction) {
        BY_ID.put(id, new Def(id, category, faction));
    }

    private TrinketDefs() {
    }

    public static Def of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return BY_ID.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isTrinket(ItemStack stack) {
        return of(stack) != null;
    }

    public static boolean accepts(TrinketSlotType slot, ItemStack stack) {
        if (slot == null || stack == null || stack.isEmpty()) {
            return false;
        }
        Def def = of(stack);
        return def != null && slot.acceptsCategory(def.category());
    }
}
