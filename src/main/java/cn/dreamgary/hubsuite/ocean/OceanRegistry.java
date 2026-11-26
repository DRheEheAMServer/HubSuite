package cn.dreamgary.hubsuite.ocean;

import cn.dreamgary.hubsuite.HubSuite;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.nio.file.Path;

/**
 * 服务端海岛扩展总注册入口（蟹笼 + 9 种奇鱼 + 3 种海味料理 + /fishdex + /oceanevent）。
 */
public final class OceanRegistry {

    public static FishTrapBlock FISH_TRAP;

    private OceanRegistry() {
    }

    public static void register(Path configDir) {
        if (FISH_TRAP != null) {
            return;
        }
        FISH_TRAP = Registry.register(BuiltInRegistries.BLOCK, OceanBlockIds.FISH_TRAP,
                new FishTrapBlock(OceanBlockIds.fishTrapBlockProps()));
        OceanBlockIds.registerBlockEntity(FISH_TRAP, FishTrapBlockEntity::new);
        Registry.register(BuiltInRegistries.ITEM, OceanBlockIds.FISH_TRAP,
                new BlockItem(FISH_TRAP, OceanBlockIds.fishTrapItemProps()));

        for (OceanFishDefs.FishDef def : OceanFishDefs.ALL_FISH) {
            Registry.register(BuiltInRegistries.ITEM, def.id(),
                    new Item(OceanFishDefs.itemProps(def.id(), 16, def.mcRarity())));
        }
        for (OceanFishDefs.DishDef dish : OceanFishDefs.ALL_DISHES) {
            Registry.register(BuiltInRegistries.ITEM, dish.id(),
                    new SeafoodDishItem(OceanFishDefs.itemProps(dish.id(), 16, dish.rarity()), dish.path()));
        }

        FishdexStorage.init(configDir);
        OceanEventManager.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(Commands.literal("fishdex").executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer sp) {
                    FishdexMenu.open(sp);
                }
                return Command.SINGLE_SUCCESS;
            }));
            dispatcher.register(Commands.literal("oceanevent")
                    .executes(ctx -> {
                        var ev = OceanEventManager.activeEvent();
                        ctx.getSource().sendSuccess(() -> Component.literal(ev == null
                                ? "§7当前没有进行中的海岛/岩浆海事件。"
                                : "§b当前事件：" + ev.displayName + " §7(剩余 " + OceanEventManager.remainingSeconds() + "s)"), false);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("start")
                            .requires(src -> !(src.getEntity() instanceof ServerPlayer sp)
                                    || HubSuite.permissions().hasAdminPermission(sp))
                            .then(Commands.argument("type", StringArgumentType.word())
                                    .executes(ctx -> {
                                        var type = OceanEventManager.EventType.parse(
                                                StringArgumentType.getString(ctx, "type"));
                                        if (type == null) {
                                            ctx.getSource().sendFailure(Component.literal(
                                                    "可用事件：spring_tide / biolum_night / magma_surge"));
                                            return 0;
                                        }
                                        OceanEventManager.startEvent(type, ctx.getSource().getServer());
                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "§a已开启事件：" + type.displayName), true);
                                        return Command.SINGLE_SUCCESS;
                                    })))
                    .then(Commands.literal("stop")
                            .requires(src -> !(src.getEntity() instanceof ServerPlayer sp)
                                    || HubSuite.permissions().hasAdminPermission(sp))
                            .executes(ctx -> {
                                OceanEventManager.stopEvent();
                                ctx.getSource().sendSuccess(() -> Component.literal("§e已停止当前海岛事件。"), true);
                                return Command.SINGLE_SUCCESS;
                            })));
        });

        HubSuite.logger().info("海岛扩展已就绪（1 蟹笼 + 9 种奇鱼 + 3 种海味料理 + /fishdex + 海域事件）");
    }

    private static final class SeafoodDishItem extends Item {
        private final String dishPath;

        SeafoodDishItem(Properties props, String dishPath) {
            super(props);
            this.dishPath = dishPath;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            if (!sp.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
            sp.getFoodData().eat(8, 8.0F);
            switch (dishPath) {
                case "ocean_sashimi" -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 180, 0));
                    sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 180, 0));
                    sp.sendSystemMessage(Component.literal("§b✦ 享用海味刺身：水下呼吸 + 海豚的恩惠（3 分钟）"), true);
                }
                case "golden_fish_soup" -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 240, 1));
                    sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 15, 0));
                    sp.sendSystemMessage(Component.literal("§6✦ 享用金鳞鱼汤：幸运 II（4 分钟）+ 生命恢复"), true);
                }
                case "magma_seared_steak" -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0));
                    sp.sendSystemMessage(Component.literal("§c✦ 享用熔岩炙鱼排：抗火（5 分钟）"), true);
                }
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.9F, 1.0F);
            sp.getCooldowns().addCooldown(stack, 20);
            return InteractionResult.CONSUME;
        }
    }
}
