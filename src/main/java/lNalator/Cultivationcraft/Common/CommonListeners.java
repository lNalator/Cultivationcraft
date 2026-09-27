package lNalator.Cultivationcraft.Common;

import lNalator.Cultivationcraft.Client.ClientItemControl;
import lNalator.Cultivationcraft.Client.ClientListeners;
import lNalator.Cultivationcraft.Client.GUI.SkillHotbarOverlay;
import lNalator.Cultivationcraft.Client.Physics;
import lNalator.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources.IChunkQiSources;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Commands.BodyforgeCommand;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPart;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPartOption;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.FoodStats.QiFoodStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.Lungs.Lung.Lung;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.Lungs.Lungs;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.PlayerHealthManager;
import lNalator.Cultivationcraft.Common.Qi.QiProjectile;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Effects.Wind;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import lNalator.Cultivationcraft.Common.Qi.Stats.PlayerStatModifications;
import lNalator.Cultivationcraft.Common.Qi.Stats.StatIDs;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.Server.ServerItemControl;
import lNalator.Cultivationcraft.Server.ServerListeners;
import lNalator.Cultivationcraft.Server.SkillHotbarServer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;

@Mod.EventBusSubscriber()
public class CommonListeners {

    public static ArrayList<IChunkQiSources> tickingQiSources = new ArrayList<IChunkQiSources>();

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        //Wind.tick(event.player);

        if (!event.player.isAlive()) {
            return;
        }

        if (event.player.getCommandSenderWorld().isClientSide()) {
            ClientListeners.playerTick(event);

            if (event.player.isOnGround()) {
                Vec3 movement = new Vec3(event.player.getDeltaMovement().x, 0, event.player.getDeltaMovement().z);
                QuestHandler.progressQuest(event.player, Quest.WALK, movement.length());
            }

            if (event.player.isSwimming()) {
                QuestHandler.progressQuest(event.player, Quest.SWIM, event.player.getDeltaMovement().length());
            }
        } else {
            ServerListeners.playerTick(event);
        }

        clearStatus(event.player);
    }

    @SubscribeEvent
    public static void vanillaEvent(VanillaGameEvent event) {
        if (event.getVanillaEvent() == GameEvent.HIT_GROUND && event.getCause() instanceof Player) {
            if (event.getContext().affectedState().getBlock() instanceof SlimeBlock) {
                QuestHandler.progressQuest((Player) event.getCause(), Quest.BOUNCE, 1);
            }
        }
    }

    @SubscribeEvent
    public static void teleportEvent(EntityTeleportEvent.EnderPearl event) {
        if (event.getEntity() instanceof Player) {
            QuestHandler.progressQuest(event.getPlayer(), Quest.ENDER_TELEPORT, 1);
        }
    }

    @SubscribeEvent
    public static void entityTick(LivingEvent.LivingTickEvent event) {
        Wind.tick(event.getEntity());
    }

    // Temp to clear fire if have fire resistance
    public static void clearStatus(Player player) {
        if (!player.isOnFire()) {
            return;
        }

        // Adjust player vision in lava based on fire resistance
        PlayerStatModifications stats = BodyPartStatControl.getStats(player);
        float fireResistance = stats.getElementalStat(StatIDs.resistanceModifier, Elements.fireElement);

        if (fireResistance >= 100) {
            player.clearFire();
            player.setSharedFlagOnFire(false);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        BodyforgeCommand.register(commandDispatcher);
    }

    @SubscribeEvent
    public static void LevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.side == LogicalSide.SERVER) {
            // Clone the array list so it doesn't bork out if modified during ticking
            ArrayList<IChunkQiSources> ticking = (ArrayList<IChunkQiSources>) tickingQiSources.clone();

            for (IChunkQiSources sources : ticking) {
                if (sources == null) {
                    return;
                }

                if (sources.getDimension().compareTo(event.level.dimension().location()) == 0) {
                    boolean update = false;

                    if (sources.tick(event.level)) {
                        update = true;
                    }

                    if (update) {
                        LevelChunk chunk = event.level.getChunk(sources.getChunkPos().x, sources.getChunkPos().z);

                        // Mark the LevelChunk as dirty so it will save the updated capability
                        chunk.setUnsaved(true);

                        // Send the updated capability data to all tracking clients
                        // TODO - Is this updating too much?
                        PacketHandler.sendChunkQiSourcesToClient(chunk);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void worldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            ClientItemControl.hasLoaded = true; 
        }else {
            ServerItemControl.loaded = true;
        }
    }

    @SubscribeEvent
    public static void playerJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player) {
            Physics.applyJump((Player) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        // TODO: Allow eating of inedible items if the players stomach says they are edible

        // Cancel the eat event if the players stomach is incompatible with the food type
        if (event.getItemStack().isEdible()) {
            if (event.getEntity().getFoodData() instanceof QiFoodStats) {
                if (!((QiFoodStats) (event.getEntity().getFoodData())).isEdible(event.getItemStack())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void playerFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setDistance(Physics.reduceFallDistance((Player) event.getEntity(), event.getDistance()));
        }
    }

    @SubscribeEvent
    public static void LevelChunkLoad(ChunkEvent.Load event) {
        IChunkQiSources sources = ChunkQiSources.getChunkQiSources((LevelChunk) event.getChunk());

        // Only on server
        if (!event.getLevel().isClientSide()) {
            // If the LevelChunk's Qi sources have not been generated yet, generate them
            if (sources.getChunkPos() == null) {
                sources.setChunkPos(event.getChunk().getPos());
                sources.setDimension(((LevelChunk) event.getChunk()).getLevel().dimension().location());
                sources.generateQiSources(((LevelChunk) event.getChunk()).getLevel());

                // Mark the LevelChunk as dirty so it will save the updated capability
                event.getChunk().setUnsaved(true);

                // Send the new capability data to all tracking clients
                PacketHandler.sendChunkQiSourcesToClient((LevelChunk) event.getChunk());
            }
        }

        checkQiSourceIsTicking(sources);
    }

    @SubscribeEvent
    public static void entitySizeEvent(EntityEvent.Size event) {
        if (event.getEntity() instanceof QiProjectile && ((QiProjectile) event.getEntity()).getOwner() != null) {
            event.setNewSize(event.getOldSize().scale(((QiProjectile) event.getEntity()).getSize()));
        }
    }

    public static void checkQiSourceIsTicking(IChunkQiSources source) {
        if (source.countQiSources() > 0) {
            if (!tickingQiSources.contains(source)) {
                tickingQiSources.add(source);
            }
        }
    }

    @SubscribeEvent
    public static void BonemealEvent(BonemealEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }

        if (event.getBlock().getBlock() instanceof BonemealableBlock) {
            if (((BonemealableBlock) event.getBlock().getBlock()).isValidBonemealTarget(event.getLevel(), event.getPos(), event.getBlock(), event.getLevel().isClientSide)) {
                QuestHandler.progressQuest(event.getEntity(), Quest.GROWPLANT, 1);
            }
        }
    }

    @SubscribeEvent
    public static void LevelChunkUnload(ChunkEvent.Unload event) {
        IChunkQiSources sources = ChunkQiSources.getChunkQiSources((LevelChunk) event.getChunk());
        tickingQiSources.remove(sources);
    }

    // Fired off when an player logs into the world
    @SubscribeEvent
    public static void playerJoinsWorld(PlayerEvent.PlayerLoggedInEvent event) {
        // Set 5 seconds where the logged in player cannot take damage
        event.getEntity().hurtTime = 100;

        CultivatorStats.getCultivatorStats(event.getEntity()).setDisconnected(false);

        CultivatorTechniques.getCultivatorTechniques(event.getEntity()).determinePassives(event.getEntity());

        // On server
        if (!event.getEntity().getCommandSenderWorld().isClientSide()) {
            // Loop through every player in the server
            for (Player sendPlayer : event.getEntity().getCommandSenderWorld().players()) {
                // Send player stats to the newly joined player
                ServerItemControl.sendPlayerStats(sendPlayer, event.getEntity());

                // Do onJoin operations for every part of each player
                for (BodyPart part : BodyModifications.getBodyModifications(sendPlayer).getModifications().values()) {
                    part.onJoin(event.getEntity());
                }

                for (HashMap<String, BodyPartOption> options : BodyModifications.getBodyModifications(sendPlayer).getModificationOptions().values()) {
                    for (BodyPartOption option : options.values()) {
                        option.onJoin(event.getEntity());
                    }
                }
            }
            SkillHotbarServer.addPlayer(event.getEntity().getUUID());

            BodyPartStatControl.updateStats(event.getEntity());

            float hp = BodyModifications.getBodyModifications(event.getEntity()).getHealth();

            if (hp > 0) {
                event.getEntity().setHealth(hp);
            }
        }
    }

    // Fired off when an player respawns into the world
    @SubscribeEvent
    public static void playerRespawns(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered()) {
            QuestHandler.resetQuest(event.getEntity(), Quest.LIVE);
        }

        CultivatorStats.getCultivatorStats(event.getEntity()).setDisconnected(false);

        CultivatorTechniques.getCultivatorTechniques(event.getEntity()).determinePassives(event.getEntity());
        BodyPartStatControl.updateStats(event.getEntity());

        Wind.clearWindEffect(event.getEntity().getUUID());

        // Refill lungs on spawn
        Lungs lung = PlayerHealthManager.getLungs(event.getEntity());

        for (int i = 0; i < lung.getLungAmount(); i++) {
            Lung currentLung = lung.getConnection(i).getLung();
            currentLung.setCurrent(currentLung.getCapacity());
        }

        if (!event.getEntity().getCommandSenderWorld().isClientSide()) {
            ServerItemControl.sendPlayerStats(event.getEntity(), event.getEntity());
        }
    }

    // Fired off when an player changes dimension
    @SubscribeEvent
    public static void playerChangesDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        CultivatorStats.getCultivatorStats(event.getEntity()).setDisconnected(false);

        if (!event.getEntity().getCommandSenderWorld().isClientSide()) {
            ServerItemControl.sendPlayerStats(event.getEntity(), (Player) event.getEntity());
        }
    }

    // Fired off when an player starts tracking a target
    @SubscribeEvent
    public static void playerStartsTracking(PlayerEvent.StartTracking event) {
        if (!event.getEntity().getCommandSenderWorld().isClientSide()) {
            if (event.getTarget() instanceof Player) {
                ServerItemControl.sendPlayerStats(event.getEntity(), (Player) event.getTarget());
            }
        }
    }

    // Fired off when an player starts watching a LevelChunk
    @SubscribeEvent
    public static void onLevelChunkWatch(ChunkWatchEvent.Watch event) {
        if (!event.getLevel().isClientSide()) {
            PacketHandler.sendChunkQiSourcesToClient(event.getLevel().getChunk(event.getPos().x, event.getPos().z), event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void playerDisconnects(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().isDeadOrDying()) {
            ICultivatorStats stats = CultivatorStats.getCultivatorStats(event.getEntity());

            if (stats != null) {
                stats.setDisconnected(true);
            }
        }

        if (!event.getEntity().getCommandSenderWorld().isClientSide()) {
            SkillHotbarServer.removePlayer(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();

        // Copy across the player's capabilities to the revived entity
        CultivatorStats.getCultivatorStats(event.getEntity()).readNBT(CultivatorStats.getCultivatorStats(event.getOriginal()).writeNBT());
        BodyModifications.getBodyModifications(event.getEntity()).read(BodyModifications.getBodyModifications(event.getOriginal()).write());
        CultivatorTechniques.getCultivatorTechniques(event.getEntity()).readNBT(CultivatorTechniques.getCultivatorTechniques(event.getOriginal()).writeNBT());

        event.getOriginal().invalidateCaps();

        if (event.isWasDeath()) {
            QuestHandler.resetQuest(event.getEntity(), Quest.LIVE);

            // If player dies while tribulating, reset current stage of cultivation
            if (CultivatorStats.getCultivatorStats(event.getEntity()).getCultivationType() == CultivationTypes.QI_CONDENSER) {
                if (CultivatorStats.getCultivatorStats(event.getEntity()).getCultivation().isTribulating()) {
                    CultivatorStats.getCultivatorStats(event.getEntity()).setCultivation(CultivatorStats.getCultivatorStats(event.getEntity()).getCultivation().getPreviousCultivation());
                }
            }
        }

    }

    @SubscribeEvent
    public static void playerInteract(PlayerInteractEvent.RightClickBlock event) {
        cancelPlacement(event);
    }

    @SubscribeEvent
    public static void playerInteract(PlayerInteractEvent.RightClickItem event) {
        cancelPlacement(event);
    }

    protected static void cancelPlacement(PlayerInteractEvent event) {
        // Cancel placing item if the SkillHotbar is active
        if (event.getLevel().isClientSide()) {
            if (SkillHotbarOverlay.isActive()) {
                event.setCanceled(true);
            }
        } else if (SkillHotbarServer.isActive(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }
}
