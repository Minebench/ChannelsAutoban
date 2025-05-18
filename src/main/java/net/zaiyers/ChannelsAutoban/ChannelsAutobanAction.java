package net.zaiyers.ChannelsAutoban;

import com.velocitypowered.api.proxy.Player;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.minedown.adventure.Replacer;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Map;

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
    private Map<String, Object> serverGroupCommands;

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
    public ChannelsAutobanAction(ChannelsAutoban plugin, Map<String, Object> cfg) {
        this.plugin = plugin;
        sender = new ChannelsAutobanCommandSender(plugin);

        if (cfg.get("kick") != null) {
            kick = (boolean) cfg.get("kick");
        }
        if (cfg.get("note") != null) {
            notes = (List<String>) cfg.get("note");
        }
        if (cfg.get("playerserver") != null) {
            playerServerCmds = (List<String>) cfg.get("playerserver");
        }
        if (cfg.get("groups") != null) {
            serverGroupCommands = (Map<String, Object>) cfg.get("groups");
        }
        if (cfg.get("local") != null) {
            localCmds = (List<String>) cfg.get("local");
        }
    }

    public void execute(Player p, ChannelsAutobanCounter counter) {

        if (notes != null) {
            for (String msg : notes) {
                p.sendMessage(MineDown.parse(msg, "name", p.getUsername(), "reason", counter.getReason()));
            }
        }
        
        if (playerServerCmds != null && plugin.getConnectorPlugin() != null && p.getCurrentServer().isPresent()) {
            String playerServer = p.getCurrentServer().get().getServerInfo().getName();
            for (String cmd : playerServerCmds) {
                cmd = Replacer.replaceIn(cmd, "name", p.getUsername(), "reason", counter.getReason());
                plugin.getConnectorPlugin().getBridge().runServerConsoleCommand(playerServer, cmd);
            }
        }

        if (serverGroupCommands != null && plugin.getConnectorPlugin() != null) {
            for (String group : serverGroupCommands.keySet()) {
                for (String server : plugin.getServerGroup(group)) {
                    if (plugin.getProxy().getServer(server).isEmpty()) {
                        plugin.log(Level.WARN, "Unknown server: " + server);
                    } else {
                        for (String cmd : (List<String>) serverGroupCommands.get(group)) {
                            cmd = Replacer.replaceIn(cmd, "name", p.getUsername(), "reason", counter.getReason());
                            plugin.getConnectorPlugin().getBridge().runServerConsoleCommand(server, cmd);
                        }
                    }
                }
            }
        }

        if (localCmds != null) {
            for (String cmd : localCmds) {
                cmd = Replacer.replaceIn(cmd, "name", p.getUsername(), "reason", counter.getReason());
                plugin.getProxy().getCommandManager().executeImmediatelyAsync(sender, cmd);
            }
        }

        if (kick) {
            p.disconnect(MineDown.parse(counter.getReason() + " (Autoban)"));
        }
    }
}
