package net.zaiyers.ChannelsAutoban;

import java.util.Collection;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;

public class ChannelsAutobanCommandSender implements CommandSender {
    private final ChannelsAutoban plugin;

    public ChannelsAutobanCommandSender(ChannelsAutoban plugin) {
        this.plugin = plugin;
    }
    public void addGroups(String... arg0) {}
    
    public Collection<String> getGroups() {
        return null; 
    }

    public String getName() {
        return plugin.getCommandSenderName();
    }

    public Collection<String> getPermissions() {
        return null; 
    }
    
    public boolean hasPermission(String arg0) {
        return true; 
    }
    
    public void removeGroups(String... arg0) {}
    
    public void sendMessage(String arg0) {
        plugin.getProxy().getLogger().info(getName() + ": " + arg0);
    }
    
    public void sendMessage(BaseComponent... arg0) {
        for(BaseComponent b : arg0) {
            plugin.getProxy().getLogger().info(getName() + ": " + b.toPlainText());
        }
    }
    
    public void sendMessage(BaseComponent arg0) {
        plugin.getProxy().getLogger().info(getName() + ": " + arg0.toPlainText());
    }
    
    public void sendMessages(String... arg0) {
        for(String s : arg0) {
            plugin.getProxy().getLogger().info(getName() + ": " + s);
        }
    }
    
    public void setPermission(String arg0, boolean arg1) {}
}
