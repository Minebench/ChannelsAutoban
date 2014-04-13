package net.zaiyers.ChannelsAutoban;

import java.util.ArrayList;
import java.util.HashMap;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.zaiyers.BungeeRPC.BungeeRPC;

public class ChannelsAutobanAction {
	private boolean kick = false;
	
	/**
	 * commands to perform on the players server
	 */
	private ArrayList<String> playerServerCmds = new ArrayList<String>();
	
	/**
	 * commands to perform on servergroups
	 */
	private HashMap<String, ArrayList<String>> serverGroupCommands = new HashMap<String, ArrayList<String>>();
	
	/**
	 * @param cfg
	 */
	@SuppressWarnings("unchecked")
	public ChannelsAutobanAction(HashMap<String, Object> cfg) {
		if (cfg.get("kick") != null) {
			kick = Boolean.parseBoolean((String) cfg.get("kick"));
		}
		if (cfg.get("playerserver") != null) {
			playerServerCmds = (ArrayList<String>) cfg.get("playerserver");
		}
		if (cfg.get("groups") != null) {
			serverGroupCommands = (HashMap<String, ArrayList<String>>) cfg.get("groups");
		}
	}

	public void execute(ProxiedPlayer p, ChannelsAutobanPattern pattern) {
		if (kick) {
			p.disconnect(new TextComponent(pattern.getReason()+" (Autoban)"));
		}
		
		for (String cmd: playerServerCmds) {
			BungeeRPC.getInstance().sendToBukkit(p.getServer().getInfo(), ChannelsAutoban.getInstance().getCommandSenderName(), cmd);
		}
		
		for (String group: serverGroupCommands.keySet()) {
			for (String server: ChannelsAutoban.getInstance().getServerGroup(group)) {
				ServerInfo serverInfo = BungeeRPC.getInstance().getProxy().getServerInfo(server);
				if (serverInfo == null) {
					ChannelsAutoban.getInstance().getLogger().warning("Unknown server: "+server);
				} else {
					for (String cmd: serverGroupCommands.get(group)) {
						BungeeRPC.getInstance().sendToBukkit(serverInfo, ChannelsAutoban.getInstance().getCommandSenderName(), cmd);
					}
				}
			}
		}
	}
}
