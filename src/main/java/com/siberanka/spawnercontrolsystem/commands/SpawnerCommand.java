package com.siberanka.spawnercontrolsystem.commands;

import com.siberanka.spawnercontrolsystem.SpawnerControlSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SpawnerCommand implements CommandExecutor {

    private final SpawnerControlSystem plugin;

    public SpawnerCommand(SpawnerControlSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("scc.admin")) {
            sender.sendMessage(plugin.getLangManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.loadPlugin();
            sender.sendMessage(plugin.getLangManager().getMessage("reload-success"));
            return true;
        }

        sender.sendMessage(plugin.getLangManager().getMessage("prefix") + "§bCommands:\n" +
                "§7/scc reload - Reloads configuration and language in real-time.");
        return true;
    }
}
