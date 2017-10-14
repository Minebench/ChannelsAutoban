package net.zaiyers.ChannelsAutoban;

import java.util.List;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class ChannelsAutobanAction {

    private final ChannelsAutoban plugin;

    /**
     * kick the player?
     */
    private boolean kick = false;

    /**
     * commands to perform on the players server
     */
    private List<String> playerServerCmds;

    /**
     * commands to perform on servergroups
     */
    private Configuration serverGroupCommands;

    /**
     * commands to perform locally
     */
    private List<String> localCmds;

    /**
     * messages to send to the player
     */
    private List<String> notes;

    /**
     * perform commands using this guy
     */
    private ChannelsAutobanCommandSender sender;

    /**
     * @param cfg
     */
    @SuppressWarnings("unchecked")
    public ChannelsAutobanAction(ChannelsAutoban plugin, Configuration cfg) {
        this.plugin = plugin;
        sender = new ChannelsAutobanCommandSender(plugin);

        if (cfg.get("kick") != null) {
            kick = cfg.getBoolean("kick");
        }
        if (cfg.get("note") != null) {
            notes = cfg.getStringList("note");
        }
        if (cfg.get("playerserver") != null) {
            playerServerCmds = cfg.getStringList("playerserver");
        }
        if (cfg.get("groups") != null) {
            serverGroupCommands = cfg.getSection("groups");
        }
        if (cfg.get("local") != null) {
            localCmds = cfg.getStringList("local");
        }
    }

    public void execute(ProxiedPlayer p, ChannelsAutobanCounter counter) {

        if (notes != null) {
            for (String msg : notes) {
                msg = msg.replaceAll("%name%", p.getName()).replaceAll("%reason%", counter.getReason());
                p.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', msg)));
            }
        }
        
        if (playerServerCmds != null && plugin.getBungeeRpc() != null) {
            for (String cmd : playerServerCmds) {
                cmd = cmd.replaceAll("%name%", p.getName()).replaceAll("%reason%", counter.getReason());
                plugin.getBungeeRpc().sendToBukkit(p.getServer().getInfo(), ChannelsAutoban.getInstance().getCommandSenderName(), cmd);
            }
        }

        if (serverGroupCommands != null && plugin.getBungeeRpc() != null) {
            for (String group : serverGroupCommands.getKeys()) {
                for (String server : plugin.getServerGroup(group)) {
                    ServerInfo serverInfo = plugin.getProxy().getServerInfo(server);
                    if (serverInfo == null) {
                        plugin.getLogger().warning("Unknown server: " + server);
                    } else {
                        for (String cmd : serverGroupCommands.getStringList(group)) {
                            cmd = cmd.replaceAll("%name%", p.getName())
                                    .replaceAll("%reason%", counter.getReason());
                            plugin.getBungeeRpc().sendToBukkit(serverInfo, ChannelsAutoban.getInstance().getCommandSenderName(), cmd);
                        }
                    }
                }
            }
        }

        if (localCmds != null) {
            for (String cmd : localCmds) {
                cmd = cmd.replaceAll("%name%", p.getName()).replaceAll("%reason%", counter.getReason());
                plugin.getProxy().getPluginManager().dispatchCommand(sender, cmd);
            }
        }

        if (kick) {
            p.disconnect(new TextComponent(counter.getReason()+" (Autoban)"));
        }
    }
}
