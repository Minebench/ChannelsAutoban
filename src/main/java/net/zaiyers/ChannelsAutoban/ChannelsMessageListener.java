package net.zaiyers.ChannelsAutoban;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.zaiyers.Channels.ChannelsChatEvent;

public class ChannelsMessageListener implements Listener {
	@EventHandler
	public void onChannelsMessage(ChannelsChatEvent e) {
		if (!(e.getMessage().getSender() instanceof ProxiedPlayer)) {
			return;
		}
		ProxiedPlayer p = (ProxiedPlayer) e.getMessage().getSender();
		
		for (ChannelsAutobanPattern pattern: ChannelsAutoban.getInstance().getPatterns()) {
			if (pattern.matches(e.getMessage().getRawMessage())) {
				ChannelsAutoban.getInstance().increaseCounter(p, pattern, e.getMessage());
			}
		}
	}
}
