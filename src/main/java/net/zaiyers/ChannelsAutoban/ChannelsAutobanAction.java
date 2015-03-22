package net.zaiyers.ChannelsAutoban;

import java.util.List;
import java.util.Map;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.zaiyers.BungeeRPC.BungeeRPC;

public class ChannelsAutobanAction {
	private boolean kick = false;
	
	/**
	 * commands to perform on the players server
	 */
	private List<String> playerServerCmds;
	
	/**
	 * commands to perform on servergroups
	 */
	private Map<String, List<String>> serverGroupCommands;
	
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
	public ChannelsAutobanAction(Map<String, Object> cfg) {
		sender = new ChannelsAutobanCommandSender(ChannelsAutoban.getInstance().getCommandSenderName());
		
		if (cfg.get("kick") != null) {
			kick = (Boolean) cfg.get("kick");
		}
        if (cfg.get("note") != null) {
            notes = (List<String>) cfg.get("note");
        }
		if (cfg.get("playerserver") != null) {
			playerServerCmds = (List<String>) cfg.get("playerserver");
		}
		if (cfg.get("groups") != null) {
			serverGroupCommands = (Map<String, List<String>>) cfg.get("groups");
		}		
		if (cfg.get("local") != null) {
			localCmds = (List<String>) cfg.get("local");
		}
	}

	public void execute(ProxiedPlayer p, ChannelsAutobanCounter counter) {

        if(notes != null)
            for(String msg: notes) {
                msg = msg.replaceAll("%name%", p.getName()).replaceAll("%reason%", counter.getReason());
                p.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', msg)));
            }
        
        if(playerServerCmds != null)
            for (String cmd: playerServerCmds) {
                cmd = cmd.replaceAll("%name%", p.getName()).replaceAll("%reason%", counter.getReason());
                BungeeRPC.getInstance().sendToBukkit(p.getServer().getInfo(), ChannelsAutoban.getInstance().getCommandSenderName(), cmd);
            }
        if(serverGroupCommands != null)
            for (String group: serverGroupCommands.keySet()) {
                for (String server: ChannelsAutoban.getInstance().getServerGroup(group)) {
                    ServerInfo serverInfo = BungeeRPC.getInstance().getProxy().getServerInfo(server);
                    if (serverInfo == null) {
                        ChannelsAutoban.getInstance().getLogger().warning("Unknown server: "+server);
                    } else {
                        for (String cmd: serverGroupCommands.get(group)) {
                            cmd = cmd	.replaceAll("%name%", p.getName())
                                    .replaceAll("%reason%", counter.getReason());
                            BungeeRPC.getInstance().sendToBukkit(serverInfo, ChannelsAutoban.getInstance().getCommandSenderName(), cmd);
                        }
                    }
                }
            }

        if(localCmds != null)
            for (String cmd: localCmds) {
                cmd = cmd.replaceAll("%name%", p.getName()).replaceAll("%reason%", counter.getReason());
                ProxyServer.getInstance().getPluginManager().dispatchCommand(sender, cmd);
            }
		
		if (kick) {
			p.disconnect(new TextComponent(counter.getReason()+" (Autoban)"));
		}
	}
}
