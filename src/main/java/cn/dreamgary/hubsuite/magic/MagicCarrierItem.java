package cn.dreamgary.hubsuite.magic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 魔法能量载体物品（月光水晶 / 虚空晶核）。
 */
public class MagicCarrierItem extends Item {

    private final boolean moon;

    public MagicCarrierItem(Properties properties, boolean moon) {
        super(properties);
        this.moon = moon;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        (moon ? MagicEnergyData.emptyMoonCrystal() : MagicEnergyData.emptyVoidCore()).writeTo(stack);
        return stack;
    }
}
