package art.arcane.wormholes.commands;

import art.arcane.volmlib.util.director.annotations.Director;
import art.arcane.volmlib.util.director.annotations.Param;
import art.arcane.volmlib.util.localization.MessageArgs;
import art.arcane.volmlib.util.localization.MessageArgument;
import art.arcane.volmlib.util.localization.TextKey;
import art.arcane.wormholes.ProjectionManager;
import art.arcane.wormholes.Wormholes;
import art.arcane.wormholes.localization.WormholesLocalization;
import art.arcane.wormholes.localization.WormholesMessages;
import art.arcane.wormholes.portal.PortalConstruction;
import art.arcane.wormholes.portal.PortalType;
import art.arcane.wormholes.service.WormholesAudience;
import art.arcane.volmlib.util.scheduling.FoliaScheduler;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Director(name = "admin", descriptionKey = "command.help.admin", description = "Destructive Wormholes maintenance commands")
public class CommandAdmin {
    private static final double ADOPT_RANGE = 64.0D;
    private static final int ADOPT_LIMIT = 4096;

    private CommandBackup backup = new CommandBackup();
    private CommandPortals portals = new CommandPortals();

    @Director(name = "deleteallportals", sync = true, descriptionKey = "command.help.admin.delete_portals", description = "Delete every local portal and saved portal link")
    public void deleteAllPortals(@Param(name = "sender", contextual = true) CommandSender sender) {
        if (!sender.hasPermission("wormholes.admin.reset")) {
            send(sender, WormholesMessages.COMMAND_NO_PERMISSION);
            return;
        }
        if (!FoliaScheduler.runGlobal(Wormholes.instance, () -> {
            try {
                int deleted = Wormholes.instance.deleteAllPortalsNow();
                WormholesAudience.sendMessage(sender, Wormholes.text().component(sender,
                        WormholesMessages.COMMAND_DELETED_PORTALS,
                        countArgs(deleted)
                ));
            } catch (Throwable exception) {
                Wormholes.instance.getLogger().log(Level.SEVERE, "Failed to delete all Wormholes portals", exception);
                send(sender, WormholesMessages.COMMAND_DELETE_FAILED);
            }
        })) {
            send(sender, WormholesMessages.COMMAND_DELETE_SCHEDULE_FAILED);
        }
    }

    @Director(name = "deleteeverything", sync = true, descriptionKey = "command.help.admin.delete_everything", description = "Reset Wormholes data, config, trust, identity, and network state")
    public void deleteEverything(@Param(name = "sender", contextual = true) CommandSender sender) {
        if (!sender.hasPermission("wormholes.admin.reset")) {
            send(sender, WormholesMessages.COMMAND_NO_PERMISSION);
            return;
        }
        if (!FoliaScheduler.runGlobal(Wormholes.instance, () -> {
            try {
                Wormholes.ResetResult result = Wormholes.instance.resetEverythingNow();
                WormholesAudience.sendMessage(sender, Wormholes.text().component(sender,
                        WormholesMessages.COMMAND_RESET_EVERYTHING,
                        countArgs(result.deletedPortals())
                ));
            } catch (Throwable exception) {
                Wormholes.instance.getLogger().log(Level.SEVERE, "Failed to reset Wormholes", exception);
                send(sender, WormholesMessages.COMMAND_RESET_FAILED);
            }
        })) {
            send(sender, WormholesMessages.COMMAND_RESET_SCHEDULE_FAILED);
        }
    }

    @Director(name = "freeze", sync = true, descriptionKey = "command.help.admin.freeze", description = "Freeze all portal projections in place for a number of seconds (seconds=0 resumes)")
    public void freeze(@Param(name = "sender", contextual = true) CommandSender sender,
                       @Param(name = "seconds", descriptionKey = "command.help.admin.freeze.seconds", description = "How long to hold the frozen frame (5-300, 0 resumes now)", defaultValue = "30") int seconds) {
        if (!sender.hasPermission("wormholes.admin.projection")) {
            send(sender, WormholesMessages.COMMAND_NO_PERMISSION);
            return;
        }
        int normalized = normalizeFreezeSeconds(seconds);
        if (!FoliaScheduler.runGlobal(Wormholes.instance, () -> {
            try {
                ProjectionManager projectionManager = Wormholes.projectionManager;
                if (projectionManager == null) {
                    send(sender, WormholesMessages.COMMAND_PROJECTION_FAILED);
                    return;
                }
                if (normalized <= 0) {
                    projectionManager.freezeProjections(0L);
                    send(sender, WormholesMessages.COMMAND_PROJECTION_RESUMED);
                    return;
                }
                projectionManager.freezeProjections(normalized * 1000L);
                WormholesAudience.sendMessage(sender, Wormholes.text().component(sender,
                        WormholesMessages.COMMAND_PROJECTION_FROZEN,
                        secondsArgs(normalized)
                ));
            } catch (Throwable exception) {
                Wormholes.instance.getLogger().log(Level.SEVERE, "Failed to freeze Wormholes projections", exception);
                send(sender, WormholesMessages.COMMAND_PROJECTION_FAILED);
            }
        })) {
            send(sender, WormholesMessages.COMMAND_PROJECTION_SCHEDULE_FAILED);
        }
    }

    @Director(name = "flush", sync = true, descriptionKey = "command.help.admin.flush", description = "Revert every observer's projected blocks to ground truth and rebuild them")
    public void flush(@Param(name = "sender", contextual = true) CommandSender sender) {
        if (!sender.hasPermission("wormholes.admin.projection")) {
            send(sender, WormholesMessages.COMMAND_NO_PERMISSION);
            return;
        }
        if (!FoliaScheduler.runGlobal(Wormholes.instance, () -> {
            try {
                ProjectionManager projectionManager = Wormholes.projectionManager;
                if (projectionManager == null) {
                    send(sender, WormholesMessages.COMMAND_PROJECTION_FAILED);
                    return;
                }
                int flushed = projectionManager.flushProjections();
                WormholesAudience.sendMessage(sender, Wormholes.text().component(sender,
                        WormholesMessages.COMMAND_PROJECTION_FLUSHED,
                        countArgs(flushed)
                ));
            } catch (Throwable exception) {
                Wormholes.instance.getLogger().log(Level.SEVERE, "Failed to flush Wormholes projections", exception);
                send(sender, WormholesMessages.COMMAND_PROJECTION_FAILED);
            }
        })) {
            send(sender, WormholesMessages.COMMAND_PROJECTION_SCHEDULE_FAILED);
        }
    }

    @Director(name = "adoptrunes", sync = true, descriptionKey = "command.help.admin.adopt_runes", description = "Turn the prismarine or dark prismarine blocks joined to the one you look at, on its face, into runes")
    public void adoptRunes(@Param(name = "sender", contextual = true) CommandSender sender) {
        if (!sender.hasPermission("wormholes.admin.items")) {
            send(sender, WormholesMessages.COMMAND_NO_PERMISSION);
            return;
        }
        if (!(sender instanceof Player player)) {
            send(sender, WormholesMessages.COMMAND_ONLY_PLAYERS);
            return;
        }
        RayTraceResult hit = player.rayTraceBlocks(ADOPT_RANGE, FluidCollisionMode.NEVER);
        Block block = hit == null ? null : hit.getHitBlock();
        BlockFace face = hit == null ? null : hit.getHitBlockFace();
        PortalType type = block == null ? null : runeType(block.getType());
        if (type == null || face == null) {
            send(sender, WormholesMessages.COMMAND_RUNES_NOT_FOUND);
            return;
        }
        World world = block.getWorld();
        Material material = block.getType();
        PortalConstruction.PlanarCells found = PortalConstruction.planarCells(
                new PortalConstruction.Cell(block.getX(), block.getY(), block.getZ()),
                PortalConstruction.planeFacing(face.getModX(), face.getModY(), face.getModZ()),
                cell -> world.getBlockAt(cell.x(), cell.y(), cell.z()).getType() == material,
                ADOPT_LIMIT);
        if (found.overLimit()) {
            WormholesAudience.sendMessage(sender, Wormholes.text().component(sender,
                    WormholesMessages.COMMAND_RUNES_TOO_LARGE,
                    WormholesLocalization.args(MessageArgument.untrusted("maximum", Integer.valueOf(ADOPT_LIMIT)))));
            return;
        }
        List<Block> blocks = new ArrayList<>(found.cells().size());
        for (PortalConstruction.Cell cell : found.cells()) {
            blocks.add(world.getBlockAt(cell.x(), cell.y(), cell.z()));
        }
        int adopted = Wormholes.blockManager.adoptRunes(type, blocks);
        WormholesAudience.sendMessage(sender, Wormholes.text().component(sender,
                WormholesMessages.COMMAND_RUNES_ADOPTED,
                countArgs(adopted)));
    }

    static PortalType runeType(Material material) {
        return switch (material) {
            case PRISMARINE -> PortalType.PORTAL;
            case DARK_PRISMARINE -> PortalType.WORMHOLE;
            default -> null;
        };
    }

    static int normalizeFreezeSeconds(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.max(5, Math.min(300, seconds));
    }

    private static MessageArgs countArgs(int count) {
        return WormholesLocalization.args(MessageArgument.untrusted("count", Integer.valueOf(count)));
    }

    private static MessageArgs secondsArgs(int seconds) {
        return WormholesLocalization.args(MessageArgument.untrusted("seconds", Integer.valueOf(seconds)));
    }

    private static void send(CommandSender sender, TextKey key) {
        WormholesAudience.sendMessage(sender, Wormholes.text().component(sender, key));
    }
}
