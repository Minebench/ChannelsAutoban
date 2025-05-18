package net.zaiyers.ChannelsAutoban;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.zaiyers.Channels.command.AbstractCommandExecutor;
import org.slf4j.event.Level;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;

public class ChannelsAutobanCommand extends AbstractCommandExecutor {
    private final ChannelsAutoban plugin;

    public ChannelsAutobanCommand(ChannelsAutoban plugin) {
        super("channelsautoban", "channelsautoban.command", "autoban");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission(getPermission() + ".reload")) {
                try {
                    plugin.loadConfig();
                    sender.sendMessage(Component.text("[Autoban] ").color(NamedTextColor.RED)
                            .append(Component.text("Config reloaded!").color(NamedTextColor.GREEN)));
                } catch (SerializationException e) {
                    sender.sendMessage(Component.text("[Autoban] ").color(NamedTextColor.RED)
                            .append(Component.text("Error while reloading config! " + e.getMessage()).color(NamedTextColor.RED)));
                    plugin.log(Level.ERROR, "Error while reloading config!", e);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args) {
        return List.of("reload");
    }
}
