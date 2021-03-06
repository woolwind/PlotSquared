////////////////////////////////////////////////////////////////////////////////////////////////////
// PlotSquared - A plot manager and world generator for the Bukkit API                             /
// Copyright (c) 2014 IntellectualSites/IntellectualCrafters                                       /
//                                                                                                 /
// This program is free software; you can redistribute it and/or modify                            /
// it under the terms of the GNU General Public License as published by                            /
// the Free Software Foundation; either version 3 of the License, or                               /
// (at your option) any later version.                                                             /
//                                                                                                 /
// This program is distributed in the hope that it will be useful,                                 /
// but WITHOUT ANY WARRANTY; without even the implied warranty of                                  /
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                   /
// GNU General Public License for more details.                                                    /
//                                                                                                 /
// You should have received a copy of the GNU General Public License                               /
// along with this program; if not, write to the Free Software Foundation,                         /
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA                               /
//                                                                                                 /
// You can contact us via: support@intellectualsites.com                                           /
////////////////////////////////////////////////////////////////////////////////////////////////////
package com.intellectualcrafters.plot.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

import com.intellectualcrafters.plot.PlotSquared;
import com.intellectualcrafters.plot.object.ChunkLoc;
import com.intellectualcrafters.plot.object.OfflinePlotPlayer;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.BlockManager;
import com.intellectualcrafters.plot.util.ExpireManager;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public class DebugExec extends SubCommand {
    public DebugExec() {
        super("debugexec", "plots.admin", "Multi-purpose debug command", "debugexec", "exec", CommandCategory.DEBUG, false);
    }

    @Override
    public boolean execute(final PlotPlayer player, final String... args) {
        final List<String> allowed_params = Arrays.asList(new String[] { "stop-expire", "start-expire", "show-expired", "update-expired", "seen", "trim-check" });
        if (args.length > 0) {
            final String arg = args[0].toLowerCase();
            switch (arg) {
                case "stop-expire": {
                    if (ExpireManager.task != -1) {
                        Bukkit.getScheduler().cancelTask(ExpireManager.task);
                    } else {
                        return MainUtil.sendMessage(null, "Task already halted");
                    }
                    ExpireManager.task = -1;
                    return MainUtil.sendMessage(null, "Cancelled task.");
                }
                case "start-expire": {
                    if (ExpireManager.task == -1) {
                        ExpireManager.runTask();
                    } else {
                        return MainUtil.sendMessage(null, "Plot expiry task already started");
                    }
                    return MainUtil.sendMessage(null, "Started plot expiry task");
                }
                case "update-expired": {
                    if (args.length > 1) {
                        final String world = args[1];
                        if (!BlockManager.manager.isWorld(world)) {
                            return MainUtil.sendMessage(null, "Invalid world: " + args[1]);
                        }
                        MainUtil.sendMessage(null, "Updating expired plot list");
                        ExpireManager.updateExpired(args[1]);
                        return true;
                    }
                    return MainUtil.sendMessage(null, "Use /plot debugexec update-expired <world>");
                }
                case "show-expired": {
                    if (args.length > 1) {
                        final String world = args[1];
                        if (!BlockManager.manager.isWorld(world)) {
                            return MainUtil.sendMessage(null, "Invalid world: " + args[1]);
                        }
                        if (!ExpireManager.expiredPlots.containsKey(args[1])) {
                            return MainUtil.sendMessage(null, "No task for world: " + args[1]);
                        }
                        MainUtil.sendMessage(null, "Expired plots (" + ExpireManager.expiredPlots.get(args[1]).size() + "):");
                        for (final Entry<Plot, Long> entry : ExpireManager.expiredPlots.get(args[1]).entrySet()) {
                            final Plot plot = entry.getKey();
                            final Long stamp = entry.getValue();
                            MainUtil.sendMessage(null, " - " + plot.world + ";" + plot.id.x + ";" + plot.id.y + ";" + UUIDHandler.getName(plot.owner) + " : " + stamp);
                        }
                        return true;
                    }
                    return MainUtil.sendMessage(null, "Use /plot debugexec show-expired <world>");
                }
                case "seen": {
                    if (args.length != 2) {
                        return MainUtil.sendMessage(null, "Use /plot debugexec seen <player>");
                    }
                    final UUID uuid = UUIDHandler.getUUID(args[1]);
                    if (uuid == null) {
                        return MainUtil.sendMessage(null, "player not found: " + args[1]);
                    }
                    final OfflinePlotPlayer op = UUIDHandler.uuidWrapper.getOfflinePlayer(uuid);
                    if ((op == null) || (op.getLastPlayed() == 0)) {
                        return MainUtil.sendMessage(null, "player hasn't connected before: " + args[1]);
                    }
                    final Timestamp stamp = new Timestamp(op.getLastPlayed());
                    final Date date = new Date(stamp.getTime());
                    MainUtil.sendMessage(null, "PLAYER: " + args[1]);
                    MainUtil.sendMessage(null, "UUID: " + uuid);
                    MainUtil.sendMessage(null, "Object: " + date.toGMTString());
                    MainUtil.sendMessage(null, "GMT: " + date.toGMTString());
                    MainUtil.sendMessage(null, "Local: " + date.toLocaleString());
                    return true;
                }
                case "trim-check": {
                    if (args.length != 2) {
                        MainUtil.sendMessage(null, "Use /plot debugexec trim-check <world>");
                        MainUtil.sendMessage(null, "&7 - Generates a list of regions to trim");
                        return MainUtil.sendMessage(null, "&7 - Run after plot expiry has run");
                    }
                    final String world = args[1];
                    if (!BlockManager.manager.isWorld(world) || !PlotSquared.isPlotWorld(args[1])) {
                        return MainUtil.sendMessage(null, "Invalid world: " + args[1]);
                    }
                    final ArrayList<ChunkLoc> empty = new ArrayList<>();
                    final boolean result = Trim.getTrimRegions(empty, world, new Runnable() {
                        @Override
                        public void run() {
                            Trim.sendMessage("Processing is complete! Here's how many chunks would be deleted:");
                            Trim.sendMessage(" - MCA #: " + empty.size());
                            Trim.sendMessage(" - CHUNKS: " + (empty.size() * 1024) + " (max)");
                            Trim.sendMessage("Exporting log for manual approval...");
                            final File file = new File(PlotSquared.IMP.getDirectory() + File.separator + "trim.txt");
                            PrintWriter writer;
                            try {
                                writer = new PrintWriter(file);
                                for (final ChunkLoc loc : empty) {
                                    writer.println(world + "/region/r." + loc.x + "." + loc.z + ".mca");
                                }
                                writer.close();
                                Trim.sendMessage("File saved to 'plugins/PlotSquared/trim.txt'");
                            } catch (final FileNotFoundException e) {
                                e.printStackTrace();
                                Trim.sendMessage("File failed to save! :(");
                            }
                            Trim.sendMessage("How to get the chunk coords from a region file:");
                            Trim.sendMessage(" - Locate the x,z values for the region file (the two numbers which are separated by a dot)");
                            Trim.sendMessage(" - Multiply each number by 32; this gives you the starting position");
                            Trim.sendMessage(" - Add 31 to each number to get the end position");
                        }
                    });
                    if (!result) {
                        MainUtil.sendMessage(null, "Trim task already started!");
                    }
                    return result;
                }
            }
        }
        MainUtil.sendMessage(player, "Possible sub commands: /plot debugexec <" + StringUtils.join(allowed_params, "|") + ">");
        return true;
    }
}
