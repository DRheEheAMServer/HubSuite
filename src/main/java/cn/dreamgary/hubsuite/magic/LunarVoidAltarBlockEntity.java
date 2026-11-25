package cn.dreamgary.hubsuite.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 神秘农业（Mystical Agriculture）式中心虚月祭坛方块实体。
 */
public class LunarVoidAltarBlockEntity extends BlockEntity implements WorldlyContainer {

    /** 周围 8 个基座相对坐标（标准 3×3 环形阵）。 */
    public static final int[][] PEDESTAL_OFFSETS = {
            {-3, 0}, {3, 0}, {0, -3}, {0, 3},
            {-2, -2}, {-2, 2}, {2, -2}, {2, 2}
    };

    private static final int[] NO_SLOTS = new int[0];
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private UUID displayUuid;
    private int exposureTick;

    // 进行中的仪式状态
    private String activeRecipeId = "";
    private int progressTicks;
    private int targetTicks;

    public LunarVoidAltarBlockEntity(BlockPos pos, BlockState state) {
        super(MagicBlockEntities.LUNAR_VOID_ALTAR, pos, state);
    }

    public ItemStack held() {
        return items.get(0);
    }

    public void setHeld(ItemStack stack) {
        items.set(0, stack == null ? ItemStack.EMPTY : stack);
        setChanged();
        if (level instanceof ServerLevel sl) {
            displayUuid = AltarDisplayHelper.sync(sl, worldPosition, displayUuid, held(), 1.25);
        }
    }

    public void clearDisplay(ServerLevel sl) {
        AltarDisplayHelper.remove(sl, worldPosition, displayUuid);
        displayUuid = null;
    }

    public boolean isRunning() {
        return activeRecipeId != null && !activeRecipeId.isBlank();
    }

    public void cancelRitual() {
        activeRecipeId = "";
        progressTicks = 0;
        targetTicks = 0;
        setChanged();
    }

    /** 尝试启动仪式（空手潜行右键或红石信号触发）。 */
    public void tryStart(ServerLevel sl, ServerPlayer actor) {
        if (!MagicScope.allowed(sl)) {
            if (actor != null) {
                actor.sendSystemMessage(Component.literal("\u00A77虚月祭坛只能在经典空岛使用。"), true);
            }
            return;
        }
        if (isRunning()) {
            return;
        }
        if (DaylightExposure.isExposedDaylight(sl, worldPosition.above())) {
            if (actor != null) {
                actor.sendSystemMessage(Component.literal("\u00A7c白天阳光过强，无法在露天启动虚月仪式。"), true);
            }
            return;
        }
        List<MagicPedestalBlockEntity> pedestals = findPedestals(sl);
        List<ItemStack> stacks = new ArrayList<>(pedestals.size());
        for (MagicPedestalBlockEntity p : pedestals) {
            stacks.add(p.held());
        }
        MagicRecipes.Recipe recipe = MagicRecipes.match(held(), stacks);
        if (recipe == null) {
            if (actor != null) {
                actor.sendSystemMessage(Component.literal("\u00A7e未匹配到有效仪式配方（请检查中心主材与周围基座配材）。"), true);
            }
            return;
        }
        float bright = sl.getMoonBrightness(worldPosition);
        if (!MoonPhaseMath.matchesRange(bright, recipe.minMoonBright(), recipe.maxMoonBright())) {
            if (actor != null) {
                actor.sendSystemMessage(Component.literal(
                        "\u00A7c月相不符！当前为" + MoonPhaseMath.describeBrightness(bright)
                                + "，该仪式需要特定月相。"), true);
            }
            return;
        }
        int moonAvail = MagicRecipes.totalMoonEnergy(stacks);
        int voidAvail = MagicRecipes.totalVoidEnergy(stacks);
        if (moonAvail < recipe.moonCost() || voidAvail < recipe.voidCost()) {
            if (actor != null) {
                actor.sendSystemMessage(Component.literal(
                        "\u00A7c基座能量不足！需要 月光 " + recipe.moonCost()
                                + " / 虚空 " + recipe.voidCost()
                                + "（当前 " + moonAvail + " / " + voidAvail + "）。"), true);
            }
            return;
        }

        this.activeRecipeId = recipe.id();
        this.progressTicks = 0;
        // 消耗型操作受当前月相速率加权
        if (!recipe.isRatioBased()) {
            double speed = recipe.voidCost() > 0 ? MoonPhaseMath.voidFactor(bright) : MoonPhaseMath.moonFactor(bright);
            this.targetTicks = Math.max(20, (int) Math.round(recipe.baseTicks() / Math.max(0.25, speed)));
        } else {
            this.targetTicks = recipe.baseTicks();
        }
        setChanged();
        sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.2F);
        if (actor != null) {
            actor.sendSystemMessage(Component.literal("\u00A7b✦ 仪式已启动：" + recipe.displayName()), true);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LunarVoidAltarBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || !MagicScope.allowed(sl)) {
            return;
        }
        ItemStack held = be.held();
        if (!held.isEmpty() && sl.getGameTime() % 10L == 0L) {
            AltarDisplayHelper.tickSpin(sl, pos, be.displayUuid, sl.getGameTime());
        }

        // 每 20 tick 维护展示实体与白天暴露
        if (++be.exposureTick % 20 == 0) {
            ItemStack stack = be.held();
            if (!stack.isEmpty()) {
                be.displayUuid = AltarDisplayHelper.sync(sl, pos, be.displayUuid, stack, 1.25);
                if (DaylightExposure.isExposedDaylight(sl, pos.above())) {
                    DaylightExposure.Result res = DaylightExposure.tickExposed(sl, pos, stack);
                    if (res == DaylightExposure.Result.SHATTERED_ITEM) {
                        be.setHeld(ItemStack.EMPTY);
                    } else if (res == DaylightExposure.Result.UPDATED || res == DaylightExposure.Result.SHATTERED_GEM) {
                        be.setHeld(stack);
                    }
                }
            }
        }

        if (!be.isRunning()) {
            return;
        }
        if (DaylightExposure.isExposedDaylight(sl, pos.above())) {
            be.cancelRitual();
            sl.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 0.8F);
            return;
        }

        List<MagicPedestalBlockEntity> pedestals = be.findPedestals(sl);
        List<ItemStack> stacks = new ArrayList<>(pedestals.size());
        for (MagicPedestalBlockEntity p : pedestals) {
            stacks.add(p.held());
        }
        MagicRecipes.Recipe recipe = MagicRecipes.match(be.held(), stacks);
        if (recipe == null || !recipe.id().equals(be.activeRecipeId)) {
            be.cancelRitual();
            return;
        }

        // 每 5 tick 从有物品的基座向中心发射粒子流
        if (be.progressTicks % 5 == 0) {
            for (MagicPedestalBlockEntity p : pedestals) {
                if (p.held().isEmpty()) {
                    continue;
                }
                BlockPos pp = p.getBlockPos();
                double mx = (pos.getX() - pp.getX()) * 0.12;
                double my = 0.05;
                double mz = (pos.getZ() - pp.getZ()) * 0.12;
                sl.sendParticles(ParticleTypes.END_ROD,
                        pp.getX() + 0.5, pp.getY() + 1.15, pp.getZ() + 0.5,
                        0, mx, my, mz, 0.25);
            }
        }

        be.progressTicks++;
        if (be.progressTicks >= be.targetTicks) {
            completeRitual(sl, pos, be, pedestals, stacks, recipe);
        }
    }

    private static void completeRitual(ServerLevel sl, BlockPos pos, LunarVoidAltarBlockEntity be,
                                       List<MagicPedestalBlockEntity> pedestals, List<ItemStack> stacks,
                                       MagicRecipes.Recipe recipe) {
        if (MagicRecipes.totalMoonEnergy(stacks) < recipe.moonCost()
                || MagicRecipes.totalVoidEnergy(stacks) < recipe.voidCost()) {
            be.cancelRitual();
            return;
        }
        int needMoon = recipe.moonCost();
        int needVoid = recipe.voidCost();
        for (MagicPedestalBlockEntity p : pedestals) {
            ItemStack s = p.held();
            if (s.isEmpty()) {
                continue;
            }
            if (MagicRecipes.isEnergyCarrier(s)) {
                MagicEnergyData d = MagicEnergyData.of(s);
                if (d == null) {
                    continue;
                }
                if (d.isMoonGem() && needMoon > 0 && d.moon() > 0) {
                    int take = Math.min(needMoon, d.moon());
                    needMoon -= take;
                    d.withMoon(d.moon() - take).writeTo(s);
                    p.setHeld(s);
                } else if (d.isVoidGem() && needVoid > 0 && d.voidEnergy() > 0) {
                    int take = Math.min(needVoid, d.voidEnergy());
                    needVoid -= take;
                    d.withVoid(d.voidEnergy() - take).writeTo(s);
                    p.setHeld(s);
                }
            } else {
                // 消耗配材
                p.setHeld(ItemStack.EMPTY);
            }
        }

        if (recipe.isPhaseShift()) {
            long now = sl.getOverworldClockTime();
            long timeOfDay = now % 24000L;
            if (timeOfDay < 13000L || timeOfDay > 23000L) {
                timeOfDay = 15000L;
            }
            long currentDay = now / 24000L;
            long remainder = currentDay % 8L;
            long deltaDays = (recipe.targetMoonPhase() - remainder + 8L) % 8L;
            long nextTotal = (currentDay + deltaDays) * 24000L + timeOfDay;
            var clock = sl.registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                    .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
            sl.clockManager().setTotalTicks(clock, nextTotal);
            be.setHeld(ItemStack.EMPTY);
            for (ServerPlayer sp : sl.players()) {
                if (sp.blockPosition().distSqr(pos) <= 1024.0) {
                    sp.sendSystemMessage(Component.literal(
                            "\u00A7d✦ " + recipe.displayName() + " 完成！夜空月相已切换。"), true);
                }
            }
        } else {
            ItemStack out = new ItemStack(recipe.output(), recipe.outputCount());
            if (out.is(MagicRegistry.MOONWING_STAFF) || out.is(MagicRegistry.VOID_CHARM)
                    || out.is(MagicRegistry.VOID_BUILDER_STAFF) || out.is(MagicRegistry.VOID_BRACER)
                    || out.is(MagicRegistry.LUNAR_ANKLET) || out.is(MagicRegistry.VOID_ANCHOR_CLOAK)
                    || out.is(MagicRegistry.MOON_SIPHON_RING)) {
                MagicEnergyData.emptyArtifact().writeTo(out);
            }
            be.setHeld(out);
        }
        be.cancelRitual();
        sl.sendParticles(ParticleTypes.FIREWORK,
                pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.08);
        sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.3F);
    }

    private List<MagicPedestalBlockEntity> findPedestals(ServerLevel sl) {
        List<MagicPedestalBlockEntity> list = new ArrayList<>(8);
        for (int[] off : PEDESTAL_OFFSETS) {
            BlockPos p = worldPosition.offset(off[0], 0, off[1]);
            if (sl.getBlockEntity(p) instanceof MagicPedestalBlockEntity pe) {
                list.add(pe);
            }
        }
        return list;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? items.get(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = slot == 0 ? ContainerHelper.removeItem(items, 0, amount) : ItemStack.EMPTY;
        if (!taken.isEmpty()) {
            setHeld(items.get(0));
            cancelRitual();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == 0 ? ContainerHelper.takeItem(items, 0) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            setHeld(stack);
            cancelRitual();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.set(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }
}
