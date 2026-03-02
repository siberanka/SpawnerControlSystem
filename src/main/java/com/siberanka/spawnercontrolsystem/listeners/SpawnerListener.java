package com.siberanka.spawnercontrolsystem.listeners;

import com.siberanka.spawnercontrolsystem.SpawnerControlSystem;
import com.siberanka.spawnercontrolsystem.utils.BlockPos;
import com.siberanka.spawnercontrolsystem.utils.ConditionEngine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

public class SpawnerListener implements Listener {

    private final SpawnerControlSystem plugin;

    public SpawnerListener(SpawnerControlSystem plugin) {
        this.plugin = plugin;
    }

    // Checking phase with HIGH priority to interact with other plugins cleanly
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerPlaceCheck(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER)
            return;

        Player player = event.getPlayer();
        BlockPos pos = new BlockPos(event.getBlockPlaced().getLocation());

        // Anti-exploit: Prevent placing a spawner where one is already tracked in cache
        // (fixes spoofing)
        if (plugin.getCacheManager().isSpawner(pos)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfigurationManager().isPlaceChecksEnabled()) {
            List<String> enabledWorlds = plugin.getConfigurationManager().getPlaceWorlds();
            if (enabledWorlds.contains("*") || enabledWorlds.contains(pos.world)) {
                List<List<String>> conditionGroups = plugin.getConfigurationManager().getPlaceConditionGroups();
                if (!conditionGroups.isEmpty()) {
                    boolean passed = ConditionEngine.evaluateAnyGroup(player, conditionGroups);
                    if (!passed) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getLangManager().getMessage("cannot-place"));
                    }
                }
            }
        }
    }

    // Database logging phase exclusively for truly successful placements
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerPlaceSuccess(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPAWNER)
            return;

        BlockPos pos = new BlockPos(event.getBlockPlaced().getLocation());
        plugin.getDatabaseManager().addSpawnerAsync(pos);
        plugin.getLoggerManager().logEvent(event.getPlayer(), pos, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerBreakCheck(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER)
            return;

        Player player = event.getPlayer();
        BlockPos pos = new BlockPos(event.getBlock().getLocation());
        boolean isNatural = !plugin.getCacheManager().isSpawner(pos);

        if (plugin.getConfigurationManager().isBreakChecksEnabled()) {
            List<String> enabledWorlds = plugin.getConfigurationManager().getBreakWorlds();
            if (enabledWorlds.contains("*") || enabledWorlds.contains(pos.world)) {
                List<List<String>> conditionGroups = plugin.getConfigurationManager().getBreakConditionGroups();
                if (!conditionGroups.isEmpty()) {
                    boolean passed = ConditionEngine.evaluateAnyGroup(player, conditionGroups);

                    if (!passed) {
                        if (isNatural && player.hasPermission("scc.admin")) {
                            player.sendMessage(plugin.getLangManager().getMessage("admin-bypass-break"));
                        } else {
                            event.setCancelled(true);
                            player.sendMessage(plugin.getLangManager().getMessage("cannot-break"));
                        }
                    }
                }
            }
        }
    }

    // Database clearing logic exclusively for true breaks
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerBreakSuccess(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER)
            return;

        BlockPos pos = new BlockPos(event.getBlock().getLocation());

        if (plugin.getCacheManager().isSpawner(pos)) {
            plugin.getDatabaseManager().removeSpawnerAsync(pos);
        }
        plugin.getLoggerManager().logEvent(event.getPlayer(), pos, false);
    }
}
