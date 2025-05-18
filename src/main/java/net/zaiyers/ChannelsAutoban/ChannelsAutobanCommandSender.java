package net.zaiyers.ChannelsAutoban;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class ChannelsAutobanCommandSender implements CommandSource {
    private final ChannelsAutoban plugin;

    public ChannelsAutobanCommandSender(ChannelsAutoban plugin) {
        this.plugin = plugin;
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        return Tristate.TRUE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void sendMessage(final @NotNull Identity source, final @NotNull Component message, final @NotNull MessageType type) {
        plugin.getProxy().getConsoleCommandSource().sendMessage( Component.text(plugin.getCommandSenderName() + ": ").append(message));
    }
}
