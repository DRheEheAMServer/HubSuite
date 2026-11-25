package cn.dreamgary.hubsuite.trinket;

import cn.dreamgary.hubsuite.magic.MagicEnergyData;
import cn.dreamgary.hubsuite.magic.MagicRegistry;
import cn.dreamgary.hubsuite.magic.MagicScope;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚月饰品栏被动效果引擎：属性修饰符、虚月平衡共鸣、虚空保命、汲月充能。
 */
public final class TrinketEngine {

    private static final Identifier MOD_RANGE = Identifier.fromNamespaceAndPath("hubsuite", "trinket_range");
    private static final Identifier MOD_STEP = Identifier.fromNamespaceAndPath("hubsuite", "trinket_step");
    private static final Identifier MOD_FALL = Identifier.fromNamespaceAndPath("hubsuite", "trinket_safe_fall");
    private static final Identifier MOD_GRAVITY = Identifier.fromNamespaceAndPath("hubsuite", "trinket_gravity");

    private static final Map<UUID, SafePoint> LAST_SAFE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ANCHOR_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BRACER_USES = new ConcurrentHashMap<>();
    private static int tickCounter;

    private record SafePoint(String dimId, double x, double y, double z) {
    }

    private TrinketEngine() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TrinketEngine::onServerTick);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (player instanceof ServerPlayer sp && MagicScope.allowed(world)) {
                consumeBracerUse(sp);
            }
        });
    }

    /** 计算玩家是否触发「虚月平衡共鸣」（佩戴月系与虚空系饰品数量相等且 >0 时能耗 -25%）。 */
    public static boolean isBalanced(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        SimpleContainer box = TrinketStorage.container(player);
        int moon = 0;
        int voidCnt = 0;
        for (int i = 0; i < TrinketSlotType.SLOT_COUNT; i++) {
            ItemStack s = box.getItem(i);
            TrinketDefs.Def def = TrinketDefs.of(s);
            MagicEnergyData d = MagicEnergyData.of(s);
            if (def == null || d == null || !d.hasGem()) {
                continue;
            }
            if (def.faction() == TrinketDefs.Faction.MOON && d.moon() > 0) {
                moon++;
            } else if (def.faction() == TrinketDefs.Faction.VOID && d.voidEnergy() > 0) {
                voidCnt++;
            }
        }
        return moon > 0 && moon == voidCnt;
    }

    /** 能耗倍率：虚月平衡共鸣时返回 0.75，否则 1.0。 */
    public static double costMultiplier(ServerPlayer player) {
        return isBalanced(player) ? 0.75 : 1.0;
    }

    public static int scaleCost(ServerPlayer player, int baseCost) {
        return Math.max(1, (int) Math.round(baseCost * costMultiplier(player)));
    }

    public static void forgetPlayer(UUID uuid) {
        LAST_SAFE.remove(uuid);
        ANCHOR_COOLDOWN.remove(uuid);
        BRACER_USES.remove(uuid);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (!(sp.level() instanceof ServerLevel sl) || !MagicScope.allowed(sl)) {
                clearModifiers(sp);
                continue;
            }
            if (tickCounter % 10 == 0) {
                tickPlayer(sp, sl);
            }
        }
    }

    private static void tickPlayer(ServerPlayer sp, ServerLevel sl) {
        UUID id = sp.getUUID();
        ANCHOR_COOLDOWN.computeIfPresent(id, (k, v) -> v > 10 ? v - 10 : null);

        // 记录安全站立点（供虚空锚点披风救命使用）
        if (sp.onGround() && sp.getY() > -50) {
            LAST_SAFE.put(id, new SafePoint(sl.dimension().identifier().toString(),
                    sp.getX(), sp.getY(), sp.getZ()));
        }

        SimpleContainer box = TrinketStorage.container(sp);
        ItemStack wrist = box.getItem(TrinketSlotType.WRIST.index());
        ItemStack cloak = box.getItem(TrinketSlotType.CLOAK.index());

        // 1. 织空护腕：方块触及距离 +2.5（满容量/高阶 +3.5）
        double rangeBonus = 0.0;
        if (!wrist.isEmpty() && wrist.is(MagicRegistry.VOID_BRACER)) {
            MagicEnergyData d = MagicEnergyData.of(wrist);
            if (d != null && d.isVoidGem() && d.voidEnergy() > 0) {
                rangeBonus = d.maxVoid() >= MagicEnergyData.BLAZING_CAPACITY ? 3.5 : 2.5;
            }
        }
        setModifier(sp, Attributes.BLOCK_INTERACTION_RANGE, MOD_RANGE, rangeBonus,
                AttributeModifier.Operation.ADD_VALUE);

        // 2. 踏月脚环：检查两个戒指槽
        ItemStack anklet = findEquipped(box, MagicRegistry.LUNAR_ANKLET);
        boolean ankletActive = false;
        if (!anklet.isEmpty()) {
            MagicEnergyData d = MagicEnergyData.of(anklet);
            if (d != null && d.isMoonGem() && d.moon() > 0) {
                ankletActive = true;
                if (tickCounter % 200 == 0) {
                    d.withMoon(d.moon() - scaleCost(sp, 1)).writeTo(anklet);
                }
            }
        }
        setModifier(sp, Attributes.STEP_HEIGHT, MOD_STEP, ankletActive ? 0.5 : 0.0,
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.SAFE_FALL_DISTANCE, MOD_FALL, ankletActive ? 4.0 : 0.0,
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.GRAVITY, MOD_GRAVITY, ankletActive ? -0.015 : 0.0,
                AttributeModifier.Operation.ADD_VALUE);

        // 3. 虚空锚点披风：跌落 <= -55 自动拉回安全点
        if (!cloak.isEmpty() && cloak.is(MagicRegistry.VOID_ANCHOR_CLOAK)
                && sp.getY() <= -55.0 && !ANCHOR_COOLDOWN.containsKey(id)) {
            MagicEnergyData d = MagicEnergyData.of(cloak);
            int cost = scaleCost(sp, 200);
            if (d != null && d.isVoidGem() && d.voidEnergy() >= cost) {
                SafePoint safe = LAST_SAFE.get(id);
                if (safe != null && safe.dimId().equals(sl.dimension().identifier().toString())) {
                    d.withVoid(d.voidEnergy() - cost).writeTo(cloak);
                    sp.resetFallDistance();
                    sp.setDeltaMovement(0, 0, 0);
                    sp.teleportTo(sl, safe.x(), safe.y() + 0.2, safe.z(),
                            java.util.Set.of(), sp.getYRot(), sp.getXRot(), false);
                    ANCHOR_COOLDOWN.put(id, 600);
                    sl.playSound(null, BlockPos.containing(safe.x(), safe.y(), safe.z()),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.1F);
                    sp.sendOverlayMessage(Component.literal("\u00A75✦ 虚空锚点披风已将你拉回安全位置（消耗 " + cost + " 虚空能）"));
                }
            }
        }

        // 4. 汲月指环：夜晚露天每 20 tick 为身上一件未满月光载体充能 +8
        if (tickCounter % 20 == 0 && !sl.isBrightOutside() && sl.canSeeSky(sp.blockPosition())) {
            ItemStack siphon = findEquipped(box, MagicRegistry.MOON_SIPHON_RING);
            if (!siphon.isEmpty()) {
                MagicEnergyData sd = MagicEnergyData.of(siphon);
                if (sd != null && sd.isMoonGem()) {
                    siphonMoonEnergy(sp, box, sd.isBlazingMoon() ? 12 : 8);
                }
            }
        }
    }

    private static void siphonMoonEnergy(ServerPlayer sp, SimpleContainer box, int gain) {
        for (int i = 0; i < TrinketSlotType.SLOT_COUNT; i++) {
            if (tryChargeMoon(box.getItem(i), gain)) {
                return;
            }
        }
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (tryChargeMoon(inv.getItem(i), gain)) {
                return;
            }
        }
    }

    private static boolean tryChargeMoon(ItemStack stack, int gain) {
        if (stack.isEmpty()) {
            return false;
        }
        MagicEnergyData d = MagicEnergyData.of(stack);
        if (d != null && d.isMoonGem() && d.moon() < d.maxMoon()) {
            d.withMoon(d.moon() + gain).writeTo(stack);
            return true;
        }
        return false;
    }

    private static ItemStack findEquipped(SimpleContainer box, Item item) {
        for (int i = 0; i < TrinketSlotType.SLOT_COUNT; i++) {
            ItemStack s = box.getItem(i);
            if (!s.isEmpty() && s.is(item)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void consumeBracerUse(ServerPlayer sp) {
        SimpleContainer box = TrinketStorage.container(sp);
        ItemStack wrist = box.getItem(TrinketSlotType.WRIST.index());
        if (wrist.isEmpty() || !wrist.is(MagicRegistry.VOID_BRACER)) {
            return;
        }
        MagicEnergyData d = MagicEnergyData.of(wrist);
        if (d == null || !d.isVoidGem() || d.voidEnergy() <= 0) {
            return;
        }
        int next = BRACER_USES.getOrDefault(sp.getUUID(), 0) + 1;
        if (next >= 20) {
            BRACER_USES.put(sp.getUUID(), 0);
            d.withVoid(d.voidEnergy() - scaleCost(sp, 1)).writeTo(wrist);
        } else {
            BRACER_USES.put(sp.getUUID(), next);
        }
    }

    private static void clearModifiers(ServerPlayer sp) {
        setModifier(sp, Attributes.BLOCK_INTERACTION_RANGE, MOD_RANGE, 0.0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.STEP_HEIGHT, MOD_STEP, 0.0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.SAFE_FALL_DISTANCE, MOD_FALL, 0.0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(sp, Attributes.GRAVITY, MOD_GRAVITY, 0.0, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void setModifier(ServerPlayer sp, Holder<Attribute> attr, Identifier modId,
                                    double value, AttributeModifier.Operation op) {
        AttributeInstance inst = sp.getAttribute(attr);
        if (inst == null) {
            return;
        }
        if (Math.abs(value) < 1e-6) {
            if (inst.getModifier(modId) != null) {
                inst.removeModifier(modId);
            }
            return;
        }
        AttributeModifier existing = inst.getModifier(modId);
        if (existing == null || Math.abs(existing.amount() - value) > 1e-6) {
            inst.removeModifier(modId);
            inst.addTransientModifier(new AttributeModifier(modId, value, op));
        }
    }
}
