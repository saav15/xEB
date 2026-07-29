package org.xeb.xeb.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.xeb.xeb.Xeb;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class XebCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var cooldownsCommand = Commands.literal("cooldowns")
            .then(
                Commands.literal("on")
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
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
                        net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                        player.getPersistentData().putBoolean("xebDevCooldownsDisabled", false);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.GREEN + "[xEB Dev] " +
                                net.minecraft.ChatFormatting.GRAY + "Cooldowns RE-ENABLED — normal behavior restored."),
                                false);
                        return 1;
                    })
            );

        var curiosCommand = Commands.literal("curios")
            .executes(ctx -> {
                net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                boolean loaded = net.minecraftforge.fml.ModList.get().isLoaded("curios");
                ctx.getSource().sendSuccess(() -> Component.literal(
                        net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Curios mod loaded: " + loaded), false);
                
                java.util.List<net.minecraft.world.item.ItemStack> curios = org.xeb.xeb.compat.ModCompatManager.getCuriosItems(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Curios items count: " + curios.size()), false);
                
                for (net.minecraft.world.item.ItemStack stack : curios) {
                    ctx.getSource().sendSuccess(() -> Component.literal(" - " + stack.getItem().toString() + " (x" + stack.getCount() + ")"), false);
                }
                
                org.xeb.xeb.extremeburst.ExtremeBurstRegistry.ExtremeBurstEntry active = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.findActiveBurst(player);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Active burst from findActiveBurst: " + (active != null ? active.curioItem.toString() : "null")), false);
                        
                if (active != null) {
                    boolean inSlot = org.xeb.xeb.extremeburst.ExtremeBurstRegistry.isInCurioSlot(player, active);
                    ctx.getSource().sendSuccess(() -> Component.literal(net.minecraft.ChatFormatting.GREEN + "[xEB Dev] isInCurioSlot: " + inSlot), false);
                }

                for (org.xeb.xeb.compat.ModCompatAdapter adapter : org.xeb.xeb.compat.ModCompatManager.getAdapters()) {
                    if (adapter instanceof org.xeb.xeb.compat.adapter.CuriosAdapter curiosAdapter) {
                        String diag = curiosAdapter.getDiagnosticInfo(player);
                        for (String line : diag.split("\n")) {
                            ctx.getSource().sendSuccess(() -> Component.literal(net.minecraft.ChatFormatting.AQUA + "[xEB Curios Diag] " + line), false);
                        }
                    }
                }
                return 1;
            });

        var loggCommand = Commands.literal("logg")
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
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer targetPlayer = ctx.getSource().getPlayerOrException();
                        for (int i = 1; i <= 33; i++) {
                            targetPlayer.getPersistentData().putBoolean("xebUnlockedBitacora" + i, false);
                        }
                        org.xeb.xeb.event.EnigmaBiosHandler.syncBitacoras(targetPlayer, -1);
                        ctx.getSource().sendSuccess(() -> Component.literal("Successfully cleared all Enigma Bios logs for " + targetPlayer.getScoreboardName()), true);
                        return 1;
                    })
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
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer targetPlayer = ctx.getSource().getPlayerOrException();
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

        var stevenCommand = Commands.literal("steven")
            .executes(ctx -> {
                net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                org.xeb.xeb.event.StevenSpawnHandler.spawnStevenStalker(player.serverLevel(), player, "CHARGED_PUNCH", 15, false);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        net.minecraft.ChatFormatting.DARK_PURPLE + "[xEB Dev] Steven summoned with default attack (CHARGED_PUNCH, 15s)"), true);
                return 1;
            })
            .then(
                Commands.argument("attackType", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"charged_punch", "laser", "punch_barrage", "flower_dance", "tap_head"}, b))
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String attack = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "attackType").toUpperCase();
                        org.xeb.xeb.event.StevenSpawnHandler.spawnStevenStalker(player.serverLevel(), player, attack, 15, false);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.DARK_PURPLE + "[xEB Dev] Steven summoned with attack: " + attack), true);
                        return 1;
                    })
                    .then(
                        Commands.argument("duration", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"5", "10", "15", "30", "60", "full"}, b))
                            .executes(ctx -> {
                                net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                                String attack = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "attackType").toUpperCase();
                                String durStr = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "duration");
                                boolean isFull = "full".equalsIgnoreCase(durStr);
                                int seconds = 15;
                                if (!isFull) {
                                    try { seconds = Integer.parseInt(durStr); } catch (NumberFormatException ignored) {}
                                }
                                final int finalSecs = seconds;
                                org.xeb.xeb.event.StevenSpawnHandler.spawnStevenStalker(player.serverLevel(), player, attack, finalSecs, isFull);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        net.minecraft.ChatFormatting.DARK_PURPLE + "[xEB Dev] Steven summoned: attack=" + attack + ", mode=" + (isFull ? "FULL" : finalSecs + "s")), true);
                                return 1;
                            })
                    )
            );

        var riftCommand = Commands.literal("rift")
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
                                player.getX() - 32.0D, player.getY() - 32.0D, player.getZ() - 32.0D,
                                player.getX() + 32.0D, player.getY() + 32.0D, player.getZ() + 32.0D
                        );
                        java.util.List<org.xeb.xeb.entity.ShatteredRiftEntity> rifts = player.level().getEntitiesOfClass(
                                org.xeb.xeb.entity.ShatteredRiftEntity.class, area
                        );
                        int count = rifts.size();
                        for (org.xeb.xeb.entity.ShatteredRiftEntity rift : rifts) {
                            rift.discard();
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.GREEN + "[xEB Dev] Despawned " + count + " Shattered Rift(s) in a 64x64 area around you."),
                                true);
                        return 1;
                    })
            )
            .then(
                Commands.literal("find")
                    .executes(ctx -> {
                        net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                        net.minecraft.server.level.ServerLevel level = player.serverLevel();

                        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(3000.0D, 500.0D, 3000.0D);
                        java.util.List<org.xeb.xeb.entity.ShatteredRiftEntity> rifts = level.getEntitiesOfClass(
                                org.xeb.xeb.entity.ShatteredRiftEntity.class, searchBox
                        );

                        if (rifts.isEmpty()) {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    net.minecraft.ChatFormatting.YELLOW + "[xEB Dev] No Shattered Rifts found within 3000 blocks."), false);
                            return 1;
                        }

                        java.util.Map<Integer, java.util.List<org.xeb.xeb.entity.ShatteredRiftEntity>> byDifficulty = new java.util.TreeMap<>();
                        for (org.xeb.xeb.entity.ShatteredRiftEntity rift : rifts) {
                            byDifficulty.computeIfAbsent(rift.getDifficulty(), k -> new java.util.ArrayList<>()).add(rift);
                        }

                        for (java.util.List<org.xeb.xeb.entity.ShatteredRiftEntity> diffList : byDifficulty.values()) {
                            diffList.sort(java.util.Comparator.comparingDouble(r -> player.distanceToSqr(r)));
                        }

                        ctx.getSource().sendSuccess(() -> Component.literal(
                                net.minecraft.ChatFormatting.GOLD + "[xEB Dev] Found " + rifts.size() + " Shattered Rift(s) near you:"), false);

                        for (java.util.Map.Entry<Integer, java.util.List<org.xeb.xeb.entity.ShatteredRiftEntity>> entry : byDifficulty.entrySet()) {
                            int diff = entry.getKey();
                            java.util.List<org.xeb.xeb.entity.ShatteredRiftEntity> diffRifts = entry.getValue();

                            net.minecraft.ChatFormatting diffColor;
                            String diffName;
                            if (diff == 0) {
                                diffColor = net.minecraft.ChatFormatting.AQUA;
                                diffName = "Dificultad 0 (Azul Místico)";
                            } else if (diff == 1) {
                                diffColor = net.minecraft.ChatFormatting.GREEN;
                                diffName = "Dificultad 1 (Verde Esmeralda)";
                            } else if (diff == 2) {
                                diffColor = net.minecraft.ChatFormatting.RED;
                                diffName = "Dificultad 2 (Rojo Carmesí)";
                            } else {
                                diffColor = net.minecraft.ChatFormatting.LIGHT_PURPLE;
                                diffName = "Dificultad 3 (Arcoíris / Cósmica)";
                            }

                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    diffColor + "--- " + diffName + " (" + diffRifts.size() + ") ---"), false);

                            for (org.xeb.xeb.entity.ShatteredRiftEntity rift : diffRifts) {
                                int bx = rift.blockPosition().getX();
                                int by = rift.blockPosition().getY();
                                int bz = rift.blockPosition().getZ();
                                int dist = (int) Math.sqrt(player.distanceToSqr(rift));
                                String tpCmd = "/tp @s " + bx + " " + by + " " + bz;

                                net.minecraft.network.chat.MutableComponent coordComp = Component.literal("[" + bx + ", " + by + ", " + bz + "]")
                                        .withStyle(style -> style
                                                .withColor(net.minecraft.ChatFormatting.YELLOW)
                                                .withUnderlined(true)
                                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, tpCmd))
                                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                                        Component.literal("Click para teletransportarte a la grieta en (" + bx + ", " + by + ", " + bz + ")")))
                                        );

                                net.minecraft.network.chat.MutableComponent line = Component.literal("  • ")
                                        .append(coordComp)
                                        .append(Component.literal(" (" + dist + "m de distancia)").withStyle(net.minecraft.ChatFormatting.GRAY));

                                ctx.getSource().sendSuccess(() -> line, false);
                            }
                        }

                        return 1;
                    })
            );

        var voteCommand = Commands.literal("vote")
            .then(Commands.literal("yes").executes(ctx -> {
                net.minecraft.server.level.ServerPlayer player = ctx.getSource().getPlayerOrException();
                org.xeb.xeb.world.MoonTearVoteManager.startOrAddVote(player);
                return 1;
            }));

        var devCommand = Commands.literal("dev")
            .requires(source -> source.hasPermission(2))
            .then(cooldownsCommand)
            .then(curiosCommand)
            .then(loggCommand)
            .then(riftCommand)
            .then(stevenCommand);

        dispatcher.register(
            Commands.literal("xeb")
                .then(voteCommand)
                .then(
                    Commands.literal("summon")
                        .requires(source -> source.hasPermission(2))
                        .then(
                            Commands.argument("entity", StringArgumentType.string())
                                .suggests(XebCommand::suggestEntityTypes)
                                .executes(ctx -> executeSpawn(ctx, ""))
                                .then(
                                    Commands.argument("medallions", StringArgumentType.greedyString())
                                        .suggests(XebCommand::suggestMedallions)
                                        .executes(ctx -> executeSpawn(ctx, StringArgumentType.getString(ctx, "medallions")))
                                )
                        )
                )
                .then(
                    Commands.literal("elitemeter")
                        .requires(source -> source.hasPermission(2))
                        .then(
                            Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(
                                    Commands.literal("set")
                                        .then(
                                            Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    net.minecraft.server.level.ServerPlayer player = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
                                                    int lvl = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");
                                                    // 0 = remove manual override → revert to advancement-based counting.
                                                    if (lvl == 0) {
                                                        player.getPersistentData().remove("xebEliteMeterLevel");
                                                        ctx.getSource().sendSuccess(() -> Component.literal("Reset Elite Meter override for " + player.getScoreboardName() + " (advancement-based counting restored)"), true);
                                                    } else {
                                                        player.getPersistentData().putInt("xebEliteMeterLevel", lvl);
                                                        ctx.getSource().sendSuccess(() -> Component.literal("Set Elite Meter level of " + player.getScoreboardName() + " to " + lvl), true);
                                                    }
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
                        .requires(source -> source.hasPermission(2))
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

    private static CompletableFuture<Suggestions> suggestEntityTypes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        Set<String> added = new HashSet<>();

        for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            String fullId = key.toString();
            String path = key.getPath();

            if (remaining.isEmpty() || path.toLowerCase(Locale.ROOT).contains(remaining) || fullId.toLowerCase(Locale.ROOT).contains(remaining)) {
                if (added.add(path)) {
                    builder.suggest(path);
                }
                if (added.add(fullId)) {
                    builder.suggest(fullId);
                }
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMedallions(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining();
        int lastSpace = input.lastIndexOf(' ');
        String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);
        String lastWord = (lastSpace == -1 ? input : input.substring(lastSpace + 1)).toLowerCase(Locale.ROOT);

        if (lastWord.contains(":")) {
            String base = lastWord.substring(0, lastWord.indexOf(':'));
            for (int count : new int[]{1, 2, 5, 10}) {
                String candidate = base + ":" + count;
                if (candidate.startsWith(lastWord)) {
                    builder.suggest(prefix + candidate);
                }
            }
            return builder.buildFuture();
        }

        List<String> prefixes = List.of("golden", "silver", "bronze", "g", "s", "b");
        Set<String> suggestions = new LinkedHashSet<>();

        for (String p : prefixes) {
            suggestions.add(p + "all");
        }

        for (EliteBuff buff : EliteBuffRegistry.getAll()) {
            String id = buff.getId();
            for (String p : prefixes) {
                suggestions.add(p + id);
                if (id.equalsIgnoreCase("mad")) {
                    suggestions.add(p + "madness");
                }
            }
        }

        for (String s : suggestions) {
            if (lastWord.isEmpty() || s.toLowerCase(Locale.ROOT).contains(lastWord)) {
                builder.suggest(prefix + s);
            }
        }

        return builder.buildFuture();
    }

    private static EntityType<?> resolveEntityType(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim();

        if (trimmed.contains(":")) {
            ResourceLocation loc = ResourceLocation.tryParse(trimmed);
            if (loc != null && ForgeRegistries.ENTITY_TYPES.containsKey(loc)) {
                return ForgeRegistries.ENTITY_TYPES.getValue(loc);
            }
        }

        String path = trimmed.toLowerCase(Locale.ROOT);
        // Priority 1: minecraft:path
        ResourceLocation mcLoc = new ResourceLocation("minecraft", path);
        if (ForgeRegistries.ENTITY_TYPES.containsKey(mcLoc)) {
            return ForgeRegistries.ENTITY_TYPES.getValue(mcLoc);
        }

        // Priority 2: xeb:path
        ResourceLocation xebLoc = new ResourceLocation(Xeb.MODID, path);
        if (ForgeRegistries.ENTITY_TYPES.containsKey(xebLoc)) {
            return ForgeRegistries.ENTITY_TYPES.getValue(xebLoc);
        }

        // Priority 3: any registered entity path matching
        for (Map.Entry<net.minecraft.resources.ResourceKey<EntityType<?>>, EntityType<?>> entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            if (entry.getKey().location().getPath().equalsIgnoreCase(path)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static EliteBuff findBuffByIdOrAlias(String key) {
        if (key.isEmpty()) return null;
        EliteBuff exact = EliteBuffRegistry.getById(key);
        if (exact != null) return exact;

        if (key.equalsIgnoreCase("madness") || key.equalsIgnoreCase("mad")) {
            EliteBuff madBuff = EliteBuffRegistry.getById("mad");
            if (madBuff != null) return madBuff;
        }

        for (EliteBuff buff : EliteBuffRegistry.getAll()) {
            if (buff.getId().equalsIgnoreCase(key) || buff.getId().toLowerCase(Locale.ROOT).startsWith(key)) {
                return buff;
            }
        }
        return null;
    }

    private static void parseAndAddMedallions(String medallionsStr, List<MedallionData> medallions) {
        if (medallionsStr == null || medallionsStr.trim().isEmpty()) return;
        String[] tokens = medallionsStr.trim().split("\\s+");

        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) continue;

            String namePart = token;
            int count = 1;

            int colonIdx = token.indexOf(':');
            if (colonIdx != -1) {
                namePart = token.substring(0, colonIdx);
                String countStr = token.substring(colonIdx + 1);
                try {
                    count = Math.min(50, Math.max(1, Integer.parseInt(countStr)));
                } catch (NumberFormatException ignored) {
                    count = 1;
                }
            }

            namePart = namePart.toLowerCase(Locale.ROOT);

            MedallionType tier = MedallionType.LEGENDARY; // default to gold
            String buffKey = namePart;

            if (namePart.startsWith("golden")) {
                tier = MedallionType.LEGENDARY;
                buffKey = namePart.substring("golden".length());
            } else if (namePart.startsWith("silver")) {
                tier = MedallionType.RARE;
                buffKey = namePart.substring("silver".length());
            } else if (namePart.startsWith("bronze")) {
                tier = MedallionType.COMMON;
                buffKey = namePart.substring("bronze".length());
            } else if (namePart.startsWith("g")) {
                tier = MedallionType.LEGENDARY;
                buffKey = namePart.substring(1);
            } else if (namePart.startsWith("s")) {
                tier = MedallionType.RARE;
                buffKey = namePart.substring(1);
            } else if (namePart.startsWith("b")) {
                tier = MedallionType.COMMON;
                buffKey = namePart.substring(1);
            }

            if (buffKey.equals("all")) {
                for (EliteBuff buff : EliteBuffRegistry.getAll()) {
                    for (int c = 0; c < count; c++) {
                        medallions.add(new MedallionData(buff, tier, UUID.randomUUID()));
                    }
                }
                continue;
            }

            EliteBuff buff = findBuffByIdOrAlias(buffKey);
            if (buff != null) {
                for (int c = 0; c < count; c++) {
                    medallions.add(new MedallionData(buff, tier, UUID.randomUUID()));
                }
            }
        }
    }

    private static int executeSpawn(CommandContext<CommandSourceStack> ctx, String medallionsStr) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String entityArg = StringArgumentType.getString(ctx, "entity");
        EntityType<?> entityType = resolveEntityType(entityArg);

        if (entityType == null) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityArg));
            return 0;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);

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
        parseAndAddMedallions(medallionsStr, medallions);

        if (!medallions.isEmpty()) {
            MedallionManager.saveMedallions(living, medallions);
            for (MedallionData m : medallions) {
                m.getBuff().onAttach(living, m.getUniqueId());
            }
            MedallionManager.refreshDimensionsIfNeeded(living, medallions);
        } else {
            MedallionManager.assignRandomMedallions(living, level);
        }

        level.addFreshEntityWithPassengers(living);
        MedallionManager.syncToTracking(living);

        net.minecraft.network.chat.MutableComponent feedback = Component.literal("Spawned elite ")
                .append(Component.literal(entityId != null ? entityId.toString() : entityArg).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" with "));

        if (medallions.isEmpty()) {
            List<MedallionData> assigned = MedallionManager.getMedallions(living);
            if (assigned.isEmpty()) {
                feedback = feedback.append(Component.literal("no medallions").withStyle(net.minecraft.ChatFormatting.GRAY));
            } else {
                for (int i = 0; i < assigned.size(); i++) {
                    MedallionData m = assigned.get(i);
                    net.minecraft.ChatFormatting color = switch (m.getTier()) {
                        case COMMON -> net.minecraft.ChatFormatting.GOLD;
                        case RARE -> net.minecraft.ChatFormatting.AQUA;
                        default -> net.minecraft.ChatFormatting.YELLOW;
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
                    case COMMON -> net.minecraft.ChatFormatting.GOLD;
                    case RARE -> net.minecraft.ChatFormatting.AQUA;
                    default -> net.minecraft.ChatFormatting.YELLOW;
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
