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
        dispatcher.register(
            Commands.literal("xeb")
                .requires(source -> source.hasPermission(2)) // Require OP permission level 2 (same as summon)
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
                                        String bSuggest = "b," + buff.getId();
                                        String sSuggest = "s," + buff.getId();
                                        String gSuggest = "g," + buff.getId();

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

                if (token.contains(",")) {
                    String[] parts = token.split(",", 2);
                    if (parts.length == 2) {
                        String prefix = parts[0];
                        String rest = parts[1];
                        if ((prefix.equals("b") || prefix.equals("s") || prefix.equals("g")) && EliteBuffRegistry.getById(rest) != null) {
                            buffId = rest;
                            tier = switch (prefix) {
                                case "b" -> MedallionType.COMMON;
                                case "s" -> MedallionType.RARE;
                                default -> MedallionType.LEGENDARY;
                            };
                        }
                    }
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

        source.sendSuccess(() -> Component.literal("Spawned elite " + entityId.getPath() + " with " + medallions.size() + " custom medallions."), true);
        return 1;
    }
}
