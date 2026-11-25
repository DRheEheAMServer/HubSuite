package cn.dreamgary.hubsuite.trinket;

import cn.dreamgary.hubsuite.HubSuite;
import cn.dreamgary.hubsuite.magic.MagicScope;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家饰品栏存储与跨维度隔离（内存 + 按维度落盘 {@code config/hubsuite/trinkets/<uuid>.nbt}）。
 */
public final class TrinketStorage {

    private static final Map<UUID, SimpleContainer> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, ItemStack[]>> STASH = new ConcurrentHashMap<>();
    private static Path storeDir;

    private TrinketStorage() {
    }

    public static void init(Path configDir) {
        storeDir = configDir.resolve("trinkets");
        try {
            Files.createDirectories(storeDir);
        } catch (Exception e) {
            HubSuite.logger().warn("创建饰品存储目录失败：{}", e.toString());
        }
    }

    public static SimpleContainer container(ServerPlayer player) {
        return ACTIVE.computeIfAbsent(player.getUUID(), id -> new SimpleContainer(TrinketSlotType.SLOT_COUNT));
    }

    public static void openMenu(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!MagicScope.allowed(player.level())) {
            player.sendSystemMessage(Component.literal("\u00A77虚月饰品栏只能在经典空岛使用。"), true);
            return;
        }
        SimpleContainer box = container(player);
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new TrinketMenu(syncId, inv, box),
                Component.literal("虚月饰品栏")));
    }

    /** 潜行右键快捷穿戴饰品。 */
    public static boolean quickEquip(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        TrinketDefs.Def def = TrinketDefs.of(stack);
        if (def == null) {
            return false;
        }
        SimpleContainer box = container(player);
        int targetSlot = -1;
        for (TrinketSlotType type : TrinketSlotType.values()) {
            if (!type.acceptsCategory(def.category())) {
                continue;
            }
            if (box.getItem(type.index()).isEmpty()) {
                targetSlot = type.index();
                break;
            }
            if (targetSlot < 0) {
                targetSlot = type.index();
            }
        }
        if (targetSlot < 0) {
            return false;
        }
        ItemStack prev = box.getItem(targetSlot).copy();
        ItemStack worn = stack.split(1);
        box.setItem(targetSlot, worn);
        if (!prev.isEmpty() && !player.getInventory().add(prev)) {
            player.drop(prev, false);
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
        player.sendOverlayMessage(Component.literal(
                "\u00A7b✦ 已佩戴至饰品槽【" + TrinketSlotType.byIndex(targetSlot).label() + "】"));
        return true;
    }

    public static void onCapture(ServerPlayer player, ResourceKey<Level> dimension) {
        if (player == null || dimension == null) {
            return;
        }
        SimpleContainer box = container(player);
        ItemStack[] snap = new ItemStack[TrinketSlotType.SLOT_COUNT];
        for (int i = 0; i < snap.length; i++) {
            snap[i] = box.getItem(i).copy();
        }
        String dimId = dimension.identifier().toString();
        STASH.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(dimId, snap);
        saveToDisk(player.getUUID(), player.level().getServer());
    }

    public static void onApply(ServerPlayer player, ResourceKey<Level> dimension) {
        if (player == null || dimension == null) {
            return;
        }
        String dimId = dimension.identifier().toString();
        Map<String, ItemStack[]> perWorld = STASH.computeIfAbsent(player.getUUID(),
                k -> loadFromDisk(k, player.level().getServer()));
        ItemStack[] snap = perWorld.get(dimId);
        SimpleContainer box = container(player);
        for (int i = 0; i < TrinketSlotType.SLOT_COUNT; i++) {
            box.setItem(i, snap != null && i < snap.length && snap[i] != null ? snap[i].copy() : ItemStack.EMPTY);
        }
    }

    public static void resetDimension(ServerPlayer player, ResourceKey<Level> dimension) {
        if (player == null || dimension == null) {
            return;
        }
        String dimId = dimension.identifier().toString();
        ItemStack[] empty = emptyArray();
        STASH.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(dimId, empty);
        if (player.level().dimension().equals(dimension)) {
            SimpleContainer box = container(player);
            for (int i = 0; i < TrinketSlotType.SLOT_COUNT; i++) {
                box.setItem(i, ItemStack.EMPTY);
            }
        }
        saveToDisk(player.getUUID(), player.level().getServer());
    }

    public static void forget(UUID uuid, MinecraftServer server) {
        if (uuid == null) {
            return;
        }
        saveToDisk(uuid, server);
        ACTIVE.remove(uuid);
        STASH.remove(uuid);
    }

    public static void saveAll(MinecraftServer server) {
        for (UUID uuid : STASH.keySet()) {
            saveToDisk(uuid, server);
        }
    }

    private static ItemStack[] emptyArray() {
        ItemStack[] arr = new ItemStack[TrinketSlotType.SLOT_COUNT];
        java.util.Arrays.fill(arr, ItemStack.EMPTY);
        return arr;
    }

    private static void saveToDisk(UUID uuid, MinecraftServer server) {
        if (storeDir == null || server == null) {
            return;
        }
        Map<String, ItemStack[]> perWorld = STASH.get(uuid);
        if (perWorld == null) {
            return;
        }
        try {
            DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
            CompoundTag root = new CompoundTag();
            for (var entry : perWorld.entrySet()) {
                ListTag list = new ListTag();
                ItemStack[] arr = entry.getValue();
                for (int i = 0; i < TrinketSlotType.SLOT_COUNT; i++) {
                    ItemStack s = arr != null && i < arr.length ? arr[i] : ItemStack.EMPTY;
                    if (s == null || s.isEmpty()) {
                        continue;
                    }
                    CompoundTag itemTag = (CompoundTag) ItemStack.CODEC.encodeStart(ops, s).result().orElse(null);
                    if (itemTag != null) {
                        itemTag.putInt("Slot", i);
                        list.add(itemTag);
                    }
                }
                root.put(entry.getKey(), list);
            }
            Path file = storeDir.resolve(uuid + ".nbt");
            NbtIo.writeCompressed(root, file);
        } catch (Throwable t) {
            HubSuite.logger().warn("保存饰品数据 {} 失败：{}", uuid, t.toString());
        }
    }

    private static Map<String, ItemStack[]> loadFromDisk(UUID uuid, MinecraftServer server) {
        Map<String, ItemStack[]> map = new ConcurrentHashMap<>();
        if (storeDir == null || server == null) {
            return map;
        }
        Path file = storeDir.resolve(uuid + ".nbt");
        if (!Files.isRegularFile(file)) {
            return map;
        }
        try {
            DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
            CompoundTag root = NbtIo.readCompressed(file, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            for (String dimId : root.keySet()) {
                ItemStack[] arr = emptyArray();
                ListTag list = root.getListOrEmpty(dimId);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag itemTag = list.getCompoundOrEmpty(i);
                    int slot = itemTag.getIntOr("Slot", -1);
                    if (slot >= 0 && slot < TrinketSlotType.SLOT_COUNT) {
                        arr[slot] = ItemStack.CODEC.parse(ops, itemTag).result().orElse(ItemStack.EMPTY);
                    }
                }
                map.put(dimId, arr);
            }
        } catch (Throwable t) {
            HubSuite.logger().warn("读取饰品数据 {} 失败：{}", uuid, t.toString());
        }
        return map;
    }
}
