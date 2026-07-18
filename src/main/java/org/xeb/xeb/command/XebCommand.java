package org.xeb.xeb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.medallion.MedallionData;
import org.xeb.xeb.medallion.MedallionManager;
import org.xeb.xeb.medallion.MedallionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class XebCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> devCommand = Commands.literal("dev")
            .requires(source -> source.hasPermission(4));

        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> cooldownsCommand = Commands.literal("cooldowns")
            .then(
                Commands.literal("on")
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer player =
                                ctx.getSource().getPlayerOrException();
                        player.getPersistentData().putBoolean("xebDevCooldownsDisabled", true);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.GREEN + "[xEB Dev] " +
                                net.minecraft.ChatFormatting.YELLOW + "Cooldowns DISABLED — abilities can be spammed freely."),
                                false);
                        return 1;
                    })
            )
            .then(
                Commands.literal("off")
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer player =
                                ctx.getSource().getPlayerOrException();
                        player.getPersistentData().putBoolean("xebDevCooldownsDisabled", false);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.GREEN + "[xEB Dev] " +
                                net.minecraft.ChatFormatting.GRAY + "Cooldowns RE-ENABLED — normal behavior restored."),
                                false);
                        return 1;
                    })
            );

        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> curiosCommand = Commands.literal("curios")
            .executes(ctx -> {
                net.minecraft.server.level.ServerPlayer player =
                        ctx.getSource().getPlayerOrException();
                
                boolean loaded = net.minecraftforge.fml.ModList.get().isLoaded("curios");
                ctx.getSource().sendSuccess(() -> Component.literal(
                        net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Curios mod loaded: " + loaded), false);
                
                java.util.List<net.minecraft.world.item.ItemStack> curios =
                        org.xeb.xeb.compat.ModCompatManager.getCuriosItems(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Curios items count: " + curios.size()), false);
                
                for (net.minecraft.world.item.ItemStack stack : curios) {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            " - " + stack.getItem().toString() + " (x" + stack.getCount() + ")"), false);
                }
                
                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry active =
                        org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Active burst from findActiveBurst: " + (active != null ? active.curioItem.toString() : "null")), false);
                        
                if (active != null) {
                    boolean inSlot = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.isInCurioSlot(player, active);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            net.minecraft.ChatFormatting.GREEN + "[xEB Dev] isInCurioSlot: " + inSlot), false);
                }

                for (org.xeb.xeb.compat.ModCompatAdapter adapter : org.xeb.xeb.compat.ModCompatManager.getAdapters()) {
                    if (adapter instanceof org.xeb.xeb.compat.adapter.CuriosAdapter curiosAdapter) {
                        String diag = curiosAdapter.getDiagnosticInfo(player);
                        for (String line : diag.split("\n")) {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    net.minecraft.ChatFormatting.AQUA + "[xEB Curios Diag] " + line), false);
                        }
                    }
                }
                
                return 1;
            });

        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> loggCommand = Commands.literal("logg")
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument("logNumber", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 33))
                            .then(
                                Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                    .executes(ctx -> {
                                        net.minecraft.server.level.ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                        int num = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "logNumber");
                                        boolean alreadyUnlocked = targetPlayer.getPersistentData().getBoolean("xebUnlockedBitacora" + num);
                                        if (!alreadyUnlocked) {
                                            targetPlayer.getPersistentData().putBoolean("xebUnlockedBitacora" + num, true);
                                            org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(targetPlayer, num);
                                        } else {
                                            org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(targetPlayer, -1);
                                        }
                                        ctx.getSource().sendSuccess(() -> Component.literal("Successfully unlocked Log #" + num + " for " + targetPlayer.getScoreboardName()), true);
                                        return 1;
                                    })
                            )
                    )
            )
            .then(
                Commands.literal("revoke")
                    .then(
                        Commands.argument("logNumber", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 33))
                            .then(
                                Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                    .executes(ctx -> {
                                        net.minecraft.server.level.ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                        int num = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "logNumber");
                                        targetPlayer.getPersistentData().putBoolean("xebUnlockedBitacora" + num, false);
                                        org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(targetPlayer, -1);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Successfully revoked Log #" + num + " for " + targetPlayer.getScoreboardName()), true);
                                        return 1;
                                    })
                            )
                    )
            )
            .then(
                Commands.literal("clear")
                    .then(
                        Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                            .executes(ctx -> {
                                net.minecraft.server.level.ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                for (int i = 1; i <= 33; i++) {
                                    targetPlayer.getPersistentData().putBoolean("xebUnlockedBitacora" + i, false);
                                }
                                org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(targetPlayer, -1);
                                ctx.getSource().sendSuccess(() -> Component.literal("Successfully cleared all Enigma Bios logs for " + targetPlayer.getScoreboardName()), true);
                                return 1;
                             })
                    )
            )
            .then(
                Commands.literal("addall")
                    .then(
                        Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                            .executes(ctx -> {
                                 net.minecraft.server.level.ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                 for (int i = 1; i <= 33; i++) {
                                     boolean already = targetPlayer.getPersistentData().getBoolean("xebUnlockedBitacora" + i);
                                     if (!already) {
                                         targetPlayer.getPersistentData().putBoolean("xebUnlockedBitacora" + i, true);
                                         org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(targetPlayer, i);
                                     }
                                 }
                                 org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(targetPlayer, -1);
                                 ctx.getSource().sendSuccess(() -> Component.literal("Successfully unlocked all Enigma Bios logs for " + targetPlayer.getScoreboardName()), true);
                                 return 1;
                            })
                    )
            );

        com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> riftCommand = Commands.literal("rift")
            .then(
                Commands.literal("spawn")
                    .then(
                        Commands.argument("difficulty", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                            .executes(ctx -> {
                                net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                                int difficulty = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "difficulty");
                                org.xeb.xeb.entity.ShatteredRiftEntity rift = new org.xeb.xeb.entity.ShatteredRiftEntity(player.level(), player.getX(), player.getY(), player.getZ(), difficulty);
                                player.level().addFreshEntity(rift);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Spawned Shattered Rift with difficulty " + difficulty + " at your feet."),
                                        true);
                                return 1;
                            })
                    )
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                        org.xeb.xeb.entity.ShatteredRiftEntity rift = new org.xeb.xeb.entity.ShatteredRiftEntity(player.level(), player.getX(), player.getY(), player.getZ(), 0);
                        player.level().addFreshEntity(rift);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Spawned Shattered Rift with difficulty 0 (Blue) at your feet."),
                                true);
                        return 1;
                    })
            )
            .then(
                Commands.literal("despawn")
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
                                player.getX() - 1.5D, player.getY() - 1.5D, player.getZ() - 1.5D,
                                player.getX() + 1.5D, player.getY() + 1.5D, player.getZ() + 1.5D
                        );
                        java.util.List<org.xeb.xeb.entity.ShatteredRiftEntity> rifts = player.level().getEntitiesOfClass(
                                org.xeb.xeb.entity.ShatteredRiftEntity.class, area
                        );
                        int count = rifts.size();
                        for (org.xeb.xeb.entity.ShatteredRiftEntity rift : rifts) {
                            rift.discard();
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Despawned " + count + " Shattered Rift(s) in a 3x3 area around you."),
                                true);
                        return 1;
                    })
            );

        devCommand.then(cooldownsCommand).then(curiosCommand).then(loggCommand).then(riftCommand);

        dispatcher.register(
            Commands.literal("xeb")
                // Root is public, specific sub-commands require permissions
                .then(
                    Commands.literal("summon")
                        .requires(source -> source.hasPermission(2)) // Admin only
                        .then(
                            Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
                                        builder.suggest(key.toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeSpawn(ctx, ""))
                                .then(
                                    Commands.argument("medallions", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> {
                                            String input = builder.getRemaining();
                                            int lastSpace = input.lastIndexOf(' ');
                                            String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);
                                            String lastWord = lastSpace == -1 ? input : input.substring(lastSpace + 1);

                                            for (EliteBuff buff : EliteBuffRegistry.getAll()) {
                                                String bSuggest = "bronze" + buff.getId();
                                                String sSuggest = "silver" + buff.getId();
                                                String gSuggest = "golden" + buff.getId();

                                                if (bSuggest.startsWith(lastWord)) {
                                                    builder.suggest(prefix + bSuggest);
                                                }
                                                if (sSuggest.startsWith(lastWord)) {
                                                    builder.suggest(prefix + sSuggest);
                                                }
                                                if (gSuggest.startsWith(lastWord)) {
                                                    builder.suggest(prefix + gSuggest);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> executeSpawn(ctx, StringArgumentType.getString(ctx, "medallions")))
                                )
                        )
                )
                .then(
                    Commands.literal("elitemeter")
                        .requires(source -> source.hasPermission(2)) // Admin only
                        .then(
                            Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(
                                    Commands.literal("set")
                                        .then(
                                            Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    net.minecraft.server.level.ServerPlayer player = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                                    int lvl = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");
                                                    player.getPersistentData().putInt("xebEliteMeterLevel", lvl);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Set Elite Meter level of " + player.getScoreboardName() + " to " + lvl), true);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(
                                    Commands.literal("get")
                                        .executes(ctx -> {
                                            net.minecraft.server.level.ServerPlayer player = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                            int lvl = MedallionManager.getEliteMeterLevel(player);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Elite Meter level of " + player.getScoreboardName() + " is " + lvl), true);
                                            return lvl;
                                        })
                                )
                                .then(
                                    Commands.literal("add")
                                        .then(
                                            Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    net.minecraft.server.level.ServerPlayer player = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                                    int amt = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
                                                    int current = MedallionManager.getEliteMeterLevel(player);
                                                    int next = Math.max(1, current + amt);
                                                    player.getPersistentData().putInt("xebEliteMeterLevel", next);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Added " + amt + " levels. Elite Meter level of " + player.getScoreboardName() + " is now " + next), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("permanight")
                        .requires(source -> source.hasPermission(2)) // Admin only
                        .then(
                            Commands.literal("start")
                                .executes(ctx -> {
                                    net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
                                    org.xeb.xeb.world.PermanightSavedData data = org.xeb.xeb.world.PermanightSavedData.get(level);
                                    if (data.isActive()) {
                                        ctx.getSource().sendFailure(Component.literal("Elite Permanight is already active!"));
                                        return 0;
                                    }
                                    
                                    data.setActive(true);
                                    data.setTicksRemaining(24000);
                                    data.setForceNextNight(false);
                                    
                                    level.playSound(null, level.players().isEmpty() ? 0 : level.players().get(0).getX(), 
                                            level.players().isEmpty() ? 0 : level.players().get(0).getY(), 
                                            level.players().isEmpty() ? 0 : level.players().get(0).getZ(), 
                                            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 5.0F, 0.5F);
                                            
                                    level.players().forEach(p -> {
                                        p.sendSystemMessage(Component.translatable("chat.xeb.permanight.start"));
                                    });
                                    
                                    org.xeb.xeb.network.XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), 
                                            new org.xeb.xeb.network.PermanightSyncPacket(true));
                                            
                                    ctx.getSource().sendSuccess(() -> Component.literal("Started Elite Permanight!"), true);
                                    return 1;
                                })
                        )
                        .then(
                            Commands.literal("stop")
                                .executes(ctx -> {
                                    net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
                                    org.xeb.xeb.world.PermanightSavedData data = org.xeb.xeb.world.PermanightSavedData.get(level);
                                    if (!data.isActive()) {
                                        ctx.getSource().sendFailure(Component.literal("Elite Permanight is not active!"));
                                        return 0;
                                    }
                                    
                                    data.setActive(false);
                                    data.setTicksRemaining(0);
                                    
                                    org.xeb.xeb.network.XEBNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), 
                                            new org.xeb.xeb.network.PermanightSyncPacket(false));
                                            
                                    level.players().forEach(p -> {
                                        p.sendSystemMessage(Component.translatable("chat.xeb.permanight.end"));
                                    });
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal("Stopped Elite Permanight!"), true);
                                    return 1;
                                })
                        )
                )
                .then(
                    Commands.literal("elitemastery")
                        // Public player mastery info check (requires permission 0/everyone)
                        .executes(ctx -> {
                            net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int lvl = MedallionManager.getEliteMeterLevel(player);
                            
                            player.sendSystemMessage(Component.translatable("chat.xeb.mastery.info", lvl));
                            
                            String descKey;
                            if (lvl >= 10) {
                                descKey = "chat.xeb.mastery.desc.10";
                            } else if (lvl >= 7) {
                                descKey = "chat.xeb.mastery.desc.7";
                            } else if (lvl >= 4) {
                                descKey = "chat.xeb.mastery.desc.4";
                            } else {
                                descKey = "chat.xeb.mastery.desc.1";
                            }
                            player.sendSystemMessage(Component.translatable(descKey));
                            return lvl;
                        })
                )
                .then(devCommand)
        );
    }

    private static int executeSpawn(CommandContext<CommandSourceStack> ctx, String medallionsStr) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation entityId = ResourceLocationArgument.getId(ctx, "entity");
        
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (entityType == null) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityId));
            return 0;
        }

        Vec3 pos = source.getPosition();
        ServerLevel level = source.getLevel();

        Entity entity = entityType.create(level);
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("Entity is not a LivingEntity."));
            return 0;
        }

        living.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, 0.0F);

        if (living instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
        }

        List<MedallionData> medallions = new ArrayList<>();
        if (medallionsStr != null && !medallionsStr.trim().isEmpty()) {
            String[] tokens = medallionsStr.trim().split("\\s+");
            for (String token : tokens) {
                MedallionType tier = MedallionType.LEGENDARY; // default to gold
                String buffId = token;

                if (token.startsWith("bronze")) {
                    tier = MedallionType.COMMON;
                    buffId = token.substring("bronze".length());
                } else if (token.startsWith("silver")) {
                    tier = MedallionType.RARE;
                    buffId = token.substring("silver".length());
                } else if (token.startsWith("golden")) {
                    tier = MedallionType.LEGENDARY;
                    buffId = token.substring("golden".length());
                }

                EliteBuff buff = EliteBuffRegistry.getById(buffId);
                if (buff != null) {
                    medallions.add(new MedallionData(buff, tier, UUID.randomUUID()));
                }
            }
        }

        if (!medallions.isEmpty()) {
            MedallionManager.saveMedallions(living, medallions);
            for (MedallionData m : medallions) {
                m.getBuff().onAttach(living, m.getUniqueId());
            }
            MedallionManager.refreshDimensionsIfNeeded(living, medallions);
        } else {
            // Default to random medallions
            MedallionManager.assignRandomMedallions(living, level);
        }

        level.addFreshEntityWithPassengers(living);
        MedallionManager.syncToTracking(living);

        // Build colored response component
        net.minecraft.network.chat.MutableComponent feedback = Component.literal("Spawned elite ")
                .append(Component.literal(entityId.getPath()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" with "));

        if (medallions.isEmpty()) {
            // If spawned with default random medallions, retrieve what was assigned
            List<MedallionData> assigned = MedallionManager.getMedallions(living);
            if (assigned.isEmpty()) {
                feedback = feedback.append(Component.literal("no medallions").withStyle(net.minecraft.ChatFormatting.GRAY));
            } else {
                for (int i = 0; i < assigned.size(); i++) {
                    MedallionData m = assigned.get(i);
                    net.minecraft.ChatFormatting color = switch (m.getTier()) {
                        case COMMON -> net.minecraft.ChatFormatting.GOLD; // Bronze/Orange
                        case RARE -> net.minecraft.ChatFormatting.AQUA; // Silver/Blue
                        default -> net.minecraft.ChatFormatting.YELLOW; // Golden/Yellow
                    };
                    if (i > 0) {
                        feedback = feedback.append(", ");
                    }
                    feedback = feedback.append(Component.literal("[" + m.getTier().name() + " ").append(m.getBuff().getDisplayName()).append("]").withStyle(color));
                }
            }
        } else {
            for (int i = 0; i < medallions.size(); i++) {
                MedallionData m = medallions.get(i);
                net.minecraft.ChatFormatting color = switch (m.getTier()) {
                    case COMMON -> net.minecraft.ChatFormatting.GOLD; // Bronze/Orange
                    case RARE -> net.minecraft.ChatFormatting.AQUA; // Silver/Blue
                    default -> net.minecraft.ChatFormatting.YELLOW; // Golden/Yellow
                };
                if (i > 0) {
                    feedback = feedback.append(", ");
                }
                feedback = feedback.append(Component.literal("[" + m.getTier().name() + " ").append(m.getBuff().getDisplayName()).append("]").withStyle(color));
            }
        }

        final Component finalFeedback = feedback;
        source.sendSuccess(() -> finalFeedback, true);
        return 1;
    }
}
