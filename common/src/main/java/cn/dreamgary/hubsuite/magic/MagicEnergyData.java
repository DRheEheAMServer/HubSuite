package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 魔法载体与法器的能量组件（挂在 {@code CUSTOM_DATA} 根键 {@value #TAG_ROOT}）。
 *
 * <p>遵循「只增不改」客户端契约：旧客户端读到新增字段时不崩溃，缺键按默认值回退。
 */
public record MagicEnergyData(int ver,
                              int moon,
                              int maxMoon,
                              int voidEnergy,
                              int maxVoid,
                              int overload,
                              String gem) {

    public static final int FORMAT = 1;
    public static final String TAG_ROOT = "hubsuite_magic";
    public static final int DEFAULT_CAPACITY = 1000;
    public static final int BLAZING_CAPACITY = 1500;
    public static final int OVERLOAD_LIMIT = 100;

    public static final String GEM_NONE = "";
    public static final String GEM_MOON = "moon_crystal";
    public static final String GEM_BLAZING_MOON = "blazing_moon_crystal";
    public static final String GEM_VOID = "void_core";

    public static MagicEnergyData emptyMoonCrystal() {
        return new MagicEnergyData(FORMAT, 0, DEFAULT_CAPACITY, 0, 0, 0, GEM_MOON);
    }

    public static MagicEnergyData emptyBlazingMoonCrystal() {
        return new MagicEnergyData(FORMAT, 0, BLAZING_CAPACITY, 0, 0, 0, GEM_BLAZING_MOON);
    }

    public static MagicEnergyData emptyVoidCore() {
        return new MagicEnergyData(FORMAT, 0, 0, 0, DEFAULT_CAPACITY, 0, GEM_VOID);
    }

    public static MagicEnergyData emptyArtifact() {
        return new MagicEnergyData(FORMAT, 0, 0, 0, 0, 0, GEM_NONE);
    }

    public boolean hasGem() {
        return gem != null && !gem.isBlank();
    }

    public boolean isMoonGem() {
        return GEM_MOON.equals(gem) || GEM_BLAZING_MOON.equals(gem);
    }

    public boolean isBlazingMoon() {
        return GEM_BLAZING_MOON.equals(gem);
    }

    public boolean isVoidGem() {
        return GEM_VOID.equals(gem);
    }

    public MagicEnergyData withMoon(int nextMoon) {
        int clamped = Math.max(0, Math.min(maxMoon, nextMoon));
        return new MagicEnergyData(ver, clamped, maxMoon, voidEnergy, maxVoid, overload, gem);
    }

    public MagicEnergyData withVoid(int nextVoid) {
        int clamped = Math.max(0, Math.min(maxVoid, nextVoid));
        return new MagicEnergyData(ver, moon, maxMoon, clamped, maxVoid, overload, gem);
    }

    public MagicEnergyData withOverload(int nextOverload) {
        int clamped = Math.max(0, Math.min(OVERLOAD_LIMIT, nextOverload));
        return new MagicEnergyData(ver, moon, maxMoon, voidEnergy, maxVoid, clamped, gem);
    }

    /** 将一颗晶体/晶核的能量镶嵌进法器或饰品。 */
    public MagicEnergyData socketGem(String gemType, int amount, int capacity) {
        if (GEM_MOON.equals(gemType) || GEM_BLAZING_MOON.equals(gemType)) {
            return new MagicEnergyData(FORMAT, Math.max(0, Math.min(capacity, amount)),
                    capacity, 0, 0, 0, gemType);
        }
        if (GEM_VOID.equals(gemType)) {
            return new MagicEnergyData(FORMAT, 0, 0,
                    Math.max(0, Math.min(capacity, amount)), capacity, 0, GEM_VOID);
        }
        return this;
    }

    /** 白天过载碎裂镶嵌宝石：清空宝石与能量，保留法器空槽本体。 */
    public MagicEnergyData shatterGem() {
        return emptyArtifact();
    }

    public static MagicEnergyData of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return defaultForItem(stack);
        }
        CompoundTag root = cd.copyTag();
        if (!root.contains(TAG_ROOT)) {
            return defaultForItem(stack);
        }
        CompoundTag t = root.getCompoundOrEmpty(TAG_ROOT);
        int ver = t.getIntOr("ver", FORMAT);
        int moon = Math.max(0, t.getIntOr("moon", 0));
        int maxMoon = Math.max(0, t.getIntOr("maxMoon", 0));
        int voidEnergy = Math.max(0, t.getIntOr("void", 0));
        int maxVoid = Math.max(0, t.getIntOr("maxVoid", 0));
        int overload = Math.max(0, Math.min(OVERLOAD_LIMIT, t.getIntOr("overload", 0)));
        String gem = t.getStringOr("gem", GEM_NONE);
        return new MagicEnergyData(ver, moon, maxMoon, voidEnergy, maxVoid, overload, gem);
    }

    /** 对尚未写入 CUSTOM_DATA 的裸物品（如 /give 或工作台合成产物）按注册 ID 赋予默认组件视图。 */
    public static MagicEnergyData defaultForItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (MagicIds.MOON_CRYSTAL.equals(id)) {
            return emptyMoonCrystal();
        }
        if (MagicIds.BLAZING_MOON_CRYSTAL.equals(id)) {
            return emptyBlazingMoonCrystal();
        }
        if (MagicIds.VOID_CORE.equals(id)) {
            return emptyVoidCore();
        }
        if (MagicIds.MOONWING_STAFF.equals(id)
                || MagicIds.VOID_CHARM.equals(id)
                || MagicIds.VOID_BUILDER_STAFF.equals(id)
                || MagicIds.VOID_BRACER.equals(id)
                || MagicIds.LUNAR_ANKLET.equals(id)
                || MagicIds.VOID_ANCHOR_CLOAK.equals(id)
                || MagicIds.MOON_SIPHON_RING.equals(id)) {
            return emptyArtifact();
        }
        return null;
    }

    public void writeTo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData existing = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = existing.copyTag();
        CompoundTag t = new CompoundTag();
        t.putInt("ver", ver);
        t.putInt("moon", moon);
        t.putInt("maxMoon", maxMoon);
        t.putInt("void", voidEnergy);
        t.putInt("maxVoid", maxVoid);
        t.putInt("overload", overload);
        t.putString("gem", gem == null ? GEM_NONE : gem);
        root.put(TAG_ROOT, t);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        applyVanillaBar(stack);
    }

    /** 把当前能量比例映射到原版物品条上（便于背包直观看到余量）。 */
    public void applyVanillaBar(ItemStack stack) {
        int max = Math.max(maxMoon, maxVoid);
        if (max <= 0) {
            stack.setDamageValue(0);
            return;
        }
        int cur = isVoidGem() ? voidEnergy : moon;
        int maxDmg = stack.getMaxDamage();
        if (maxDmg <= 0) {
            return;
        }
        int missing = max - Math.max(0, Math.min(max, cur));
        int dmg = Math.round((float) missing * maxDmg / max);
        if (cur <= 0) {
            dmg = maxDmg - 1;   // 留 1 格防原版误判损坏
        }
        stack.setDamageValue(Math.max(0, Math.min(maxDmg - 1, dmg)));
    }
}
