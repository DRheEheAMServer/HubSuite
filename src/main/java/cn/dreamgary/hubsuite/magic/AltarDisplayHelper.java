package cn.dreamgary.hubsuite.magic;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 祭坛 / 基座上方悬浮展示物品的 {@link Display.ItemDisplay} 管理工具。
 *
 * <p>展示物品固定在台面上方指定高度，绕自身 Y 轴平稳匀速旋转，不做 Y 轴上下浮动。
 */
public final class AltarDisplayHelper {

    public static final String TAG = "hubsuite_magic_display";
    private static final Vector3f DISPLAY_SCALE = new Vector3f(0.55F, 0.55F, 0.55F);
    private static final int SPIN_STEP_TICKS = 10;

    private AltarDisplayHelper() {
    }

    public static UUID sync(ServerLevel level, BlockPos pos, UUID currentUuid, ItemStack stack, double yOffset) {
        if (level == null || pos == null) {
            return null;
        }
        Display.ItemDisplay display = find(level, pos, currentUuid);
        if (stack == null || stack.isEmpty()) {
            if (display != null) {
                display.discard();
            }
            return null;
        }
        if (display == null) {
            display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
            display.setPos(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
            display.setBillboardConstraints(Display.BillboardConstraints.FIXED);
            display.setTransformationInterpolationDelay(0);
            display.setTransformationInterpolationDuration(SPIN_STEP_TICKS);
            display.setTransformation(spinTransformation(level.getGameTime()));
            display.addTag(TAG);
            level.addFreshEntity(display);
        } else {
            display.setPos(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
            if (display.getBillboardConstraints() != Display.BillboardConstraints.FIXED) {
                display.setBillboardConstraints(Display.BillboardConstraints.FIXED);
            }
        }
        ItemStack shown = stack.copy();
        shown.setCount(1);
        display.setItemStack(shown);
        return display.getUUID();
    }

    /** 每 {@value #SPIN_STEP_TICKS} tick 更新一次目标旋转角，由客户端原生插值实现丝滑旋转（无 Y 轴位移）。 */
    public static void tickSpin(ServerLevel level, BlockPos pos, UUID currentUuid, long gameTime) {
        if (level == null || pos == null) {
            return;
        }
        Display.ItemDisplay display = find(level, pos, currentUuid);
        if (display == null) {
            return;
        }
        if (display.getBillboardConstraints() != Display.BillboardConstraints.FIXED) {
            display.setBillboardConstraints(Display.BillboardConstraints.FIXED);
        }
        display.setTransformationInterpolationDelay(0);
        display.setTransformationInterpolationDuration(SPIN_STEP_TICKS);
        display.setTransformation(spinTransformation(gameTime));
    }

    private static Transformation spinTransformation(long gameTime) {
        long step = (gameTime / SPIN_STEP_TICKS) % 8L;
        float angle = (float) (step * (Math.PI / 4.0));
        Quaternionf rot = new Quaternionf().rotateY(angle);
        return new Transformation(null, rot, DISPLAY_SCALE, null);
    }

    public static void remove(ServerLevel level, BlockPos pos, UUID currentUuid) {
        if (level == null || pos == null) {
            return;
        }
        Display.ItemDisplay display = find(level, pos, currentUuid);
        if (display != null) {
            display.discard();
        }
    }

    private static Display.ItemDisplay find(ServerLevel level, BlockPos pos, UUID currentUuid) {
        if (currentUuid != null) {
            Entity e = level.getEntity(currentUuid);
            if (e instanceof Display.ItemDisplay d && d.isAlive()) {
                return d;
            }
        }
        AABB box = new AABB(pos).inflate(0.5, 1.5, 0.5);
        for (Display.ItemDisplay d : level.getEntities(EntityType.ITEM_DISPLAY, box, e -> e.entityTags().contains(TAG))) {
            return d;
        }
        return null;
    }
}
