package net.zaiyers.ChannelsAutoban;

import de.themoep.bungeeplugin.PluginCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

public class ChannelsAutobanCommand extends PluginCommand<ChannelsAutoban> {
    private final ChannelsAutoban plugin;

    public ChannelsAutobanCommand(ChannelsAutoban plugin) {
        super(plugin, "channelsautoban");
        this.plugin = plugin;
    }

    @Override
    protected boolean run(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission(getCommandPermission() + ".reload")) {
                plugin.loadConfig();
                sender.sendMessage(ChatColor.RED + "[Autoban] " + ChatColor.GREEN + "Config reloaded!");
                return true;
            }
        }
        return false;
    }
}
