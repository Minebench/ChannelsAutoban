package net.zaiyers.ChannelsAutoban;

import java.util.Collection;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;

public class ChannelsAutobanCommandSender implements CommandSender {
	private String name;
	
	public ChannelsAutobanCommandSender(String string) {
		name = string;
	}
	public void addGroups(String... arg0) {}
	public Collection<String> getGroups() {	return null; }
	
	public String getName() {
		return name;
	}
	
	public Collection<String> getPermissions() { return null; }
	public boolean hasPermission(String arg0) {	return true; }
	public void removeGroups(String... arg0) {}
	public void sendMessage(String arg0) {
        ProxyServer.getInstance().getLogger().info(name + ": " + arg0);
    }
	public void sendMessage(BaseComponent... arg0) {
        for(BaseComponent b : arg0) {
            ProxyServer.getInstance().getLogger().info(name + ": " + b.toPlainText());
        }
    }
	public void sendMessage(BaseComponent arg0) {
        ProxyServer.getInstance().getLogger().info(name + ": " + arg0.toPlainText());
    }
	public void sendMessages(String... arg0) {
        for(String s : arg0) {
            ProxyServer.getInstance().getLogger().info(name + ": " + s);
        }
    }
	public void setPermission(String arg0, boolean arg1) {}
}
