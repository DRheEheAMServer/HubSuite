package cn.dreamgary.hubsuite.ocean;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * 海岛服专属奇鱼、海味料理与 JEI 垂钓图鉴共用定义（双端同源）。
 */
public final class OceanFishDefs {

    public static final String NAMESPACE = "hubsuite";
    public static final String TAG_ROOT = "hubsuite_fish";

    public record FishDef(String path,
                          String zhName,
                          String enName,
                          boolean lava,
                          String rarity,
                          double minKg,
                          double maxKg,
                          String habitatHint,
                          List<String> biomes) {

        public Identifier id() {
            return Identifier.fromNamespaceAndPath(NAMESPACE, path);
        }

        public Rarity mcRarity() {
            return switch (rarity) {
                case "legendary" -> Rarity.EPIC;
                case "rare" -> Rarity.RARE;
                default -> Rarity.UNCOMMON;
            };
        }
    }

    public record DishDef(String path, String zhName, String enName, Rarity rarity) {
        public Identifier id() {
            return Identifier.fromNamespaceAndPath(NAMESPACE, path);
        }
    }

    public record JeiFishingEntry(Identifier itemId, boolean lava, String rarity, String biomeHint) {
    }

    public record FishData(int stars, double weightKg) {
        public static FishData of(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd == null) {
                return null;
            }
            CompoundTag root = cd.copyTag();
            if (!root.contains(TAG_ROOT)) {
                return null;
            }
            CompoundTag t = root.getCompoundOrEmpty(TAG_ROOT);
            int stars = Math.max(1, Math.min(3, t.getIntOr("stars", 1)));
            double kg = Math.max(0.1, t.getDoubleOr("kg", 1.0));
            return new FishData(stars, kg);
        }

        public void writeTo(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag root = existing.copyTag();
            CompoundTag t = new CompoundTag();
            t.putInt("stars", Math.max(1, Math.min(3, stars)));
            t.putDouble("kg", Math.round(weightKg * 100.0) / 100.0);
            root.put(TAG_ROOT, t);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    // ---- 5 种海域奇鱼 ----
    public static final FishDef CORAL_BUTTERFLYFISH = new FishDef(
            "coral_butterflyfish", "珊瑚蝶鱼", "Coral Butterflyfish", false, "uncommon", 1.0, 4.0,
            "暖海 / 温水洋", List.of("warm_ocean", "lukewarm_ocean", "deep_lukewarm_ocean"));
    public static final FishDef FROSTFIN_COD = new FishDef(
            "frostfin_cod", "霜鳍鳕", "Frostfin Cod", false, "uncommon", 2.0, 8.0,
            "冻洋 / 冰冻深海", List.of("frozen_ocean", "deep_frozen_ocean"));
    public static final FishDef ABYSSAL_ANGLER = new FishDef(
            "abyssal_angler", "深渊灯笼鱼", "Abyssal Angler", false, "rare", 3.0, 12.0,
            "各类深海", List.of("deep_cold_ocean", "deep_lukewarm_ocean", "deep_frozen_ocean"));
    public static final FishDef MOONLIGHT_JELLY = new FishDef(
            "moonlight_jelly", "月光水母", "Moonlight Jelly", false, "rare", 0.5, 3.0,
            "海岛全海域（夜晚 / 荧光夜潮高概率）", List.of());
    public static final FishDef GOLDEN_KOI = new FishDef(
            "golden_koi", "黄金锦鲤", "Golden Koi", false, "legendary", 4.0, 18.0,
            "海岛全海域（传说）", List.of());

    // ---- 4 种岩浆海奇鱼 ----
    public static final FishDef EMBER_ROCKFISH = new FishDef(
            "ember_rockfish", "余烬岩鱼", "Ember Rockfish", true, "uncommon", 2.0, 7.0,
            "岩浆海全群系", List.of());
    public static final FishDef SOULFIRE_EEL = new FishDef(
            "soulfire_eel", "魂火鳗", "Soulfire Eel", true, "rare", 3.0, 10.0,
            "灵魂沙峡谷", List.of("soul_sand_valley"));
    public static final FishDef BASALT_SNAPPER = new FishDef(
            "basalt_snapper", "玄武甲鱼", "Basalt Snapper", true, "rare", 5.0, 16.0,
            "玄武岩三角洲", List.of("basalt_deltas"));
    public static final FishDef MAGMA_DRAGONFISH = new FishDef(
            "magma_dragonfish", "熔核龙鱼", "Magma Dragonfish", true, "legendary", 8.0, 30.0,
            "岩浆海全群系（传说）", List.of());

    public static final List<FishDef> ALL_FISH = List.of(
            CORAL_BUTTERFLYFISH, FROSTFIN_COD, ABYSSAL_ANGLER, MOONLIGHT_JELLY, GOLDEN_KOI,
            EMBER_ROCKFISH, SOULFIRE_EEL, BASALT_SNAPPER, MAGMA_DRAGONFISH);

    // ---- 3 种海味料理 ----
    public static final DishDef OCEAN_SASHIMI = new DishDef(
            "ocean_sashimi", "海味刺身", "Ocean Sashimi", Rarity.UNCOMMON);
    public static final DishDef GOLDEN_FISH_SOUP = new DishDef(
            "golden_fish_soup", "金鳞鱼汤", "Golden Fish Soup", Rarity.RARE);
    public static final DishDef MAGMA_SEARED_STEAK = new DishDef(
            "magma_seared_steak", "熔岩炙鱼排", "Magma-Seared Fish Steak", Rarity.RARE);

    public static final List<DishDef> ALL_DISHES = List.of(
            OCEAN_SASHIMI, GOLDEN_FISH_SOUP, MAGMA_SEARED_STEAK);

    public static final List<Identifier> ALL_ITEM_IDS = buildAllItemIds();

    private static List<Identifier> buildAllItemIds() {
        List<Identifier> ids = new ArrayList<>(ALL_FISH.size() + ALL_DISHES.size());
        for (FishDef f : ALL_FISH) {
            ids.add(f.id());
        }
        for (DishDef d : ALL_DISHES) {
            ids.add(d.id());
        }
        return List.copyOf(ids);
    }

    private OceanFishDefs() {
    }

    public static FishDef byId(Identifier id) {
        if (id == null) {
            return null;
        }
        for (FishDef f : ALL_FISH) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        return null;
    }

    public static FishDef byPath(String path) {
        if (path == null) {
            return null;
        }
        for (FishDef f : ALL_FISH) {
            if (f.path().equals(path)) {
                return f;
            }
        }
        return null;
    }

    public static boolean isSpecialtyFish(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return byId(BuiltInRegistries.ITEM.getKey(stack.getItem())) != null;
    }

    public static Item.Properties itemProps(Identifier id, int maxStack, Rarity rarity) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .stacksTo(maxStack)
                .rarity(rarity)
                .component(DataComponents.ITEM_NAME,
                        Component.translatable("item.hubsuite." + id.getPath()))
                .component(DataComponents.ITEM_MODEL, id);
    }

    public static List<JeiFishingEntry> jeiEntries() {
        List<JeiFishingEntry> list = new ArrayList<>();
        for (FishDef f : ALL_FISH) {
            list.add(new JeiFishingEntry(f.id(), f.lava(), f.rarity(), f.habitatHint()));
        }
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("heart_of_the_sea"), false, "legendary", "海岛宝物 / 深海奇珍"));
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("trident"), false, "legendary", "海岛深海奇珍"));
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("sponge"), false, "rare", "海岛大鱼池"));
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("nautilus_shell"), false, "rare", "海岛大鱼 / 宝物池"));
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("ancient_debris"), true, "legendary", "岩浆海宝物池"));
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("netherite_scrap"), true, "legendary", "岩浆海宝物池"));
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("ghast_tear"), true, "rare", "岩浆海宝物池"));
        list.add(new JeiFishingEntry(Identifier.withDefaultNamespace("crying_obsidian"), true, "rare", "岩浆海宝物池"));
        return list;
    }
}
