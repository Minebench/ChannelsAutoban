package net.zaiyers.ChannelsAutoban;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.themoep.connectorplugin.velocity.VelocityConnectorPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.zaiyers.Channels.command.AbstractCommandExecutor;
import net.zaiyers.Channels.message.Message;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.spongepowered.configurate.serialize.SerializationException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

public class ChannelsAutoban {

    private final ProxyServer proxy;
    private final Logger logger;
    private final FileConfiguration config;

    /**
     * patterns to look for
     */
    private List<ChannelsAutobanPattern> patterns = new ArrayList<>();

    /**
     * counter configurations
     */
    private Map<String, ChannelsAutobanCounter> counters = new HashMap<>();

    /**
     * counter values
     */
    private Table<UUID, String, Integer> violations = HashBasedTable.create();

    /**
     * action configurations
     */
    private Map<String, ChannelsAutobanAction> actions = new HashMap<>();

    /**
     * name of commandsender
     */
    private String commandSenderName;

    /**
     * list of servergroups
     */
    private Map<String, Object> serverGroups;

    /**
     * special pattern executed when ip matched
     */
    private ChannelsAutobanPattern ippattern;

    /**
     * ip whitelist
     */
    private List<String> ipWhitelist = new ArrayList<>();

    /**
     * ConnectorPlugin instance
     */
    private VelocityConnectorPlugin connectorPlugin;

    @Inject
    public ChannelsAutoban(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = logger;
        config = new FileConfiguration(this, dataFolder.resolve("config.yml"));
    }

    /**
     * executed on startup
     */
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Optional<PluginContainer> connectorPlugin = getProxy().getPluginManager().getPlugin("connectorplugin");
        if (connectorPlugin.isPresent()) {
            this.connectorPlugin = (VelocityConnectorPlugin) connectorPlugin.get().getInstance().get();
        }

        try {
            loadConfig();
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        // register listener
        getProxy().getEventManager().register(this, new ChannelsMessageListener(this));

        // register command
        registerCommand(new ChannelsAutobanCommand(this));
    }

    private void registerCommand(AbstractCommandExecutor command) {
        getProxy().getCommandManager().register(getProxy().getCommandManager()
                .metaBuilder(command.getName())
                .aliases(command.getAliases())
                .plugin(this)
                .build(), command);
    }

    void loadConfig() throws SerializationException {
        FileConfiguration cfg = getConfig();
        cfg.load();
        commandSenderName = cfg.getString("commandsender", "Autoban");
        serverGroups = cfg.getSection("servergroups");

        // load patterns
        List<Map> patternCfgs = cfg.getRawConfig("patterns").getList(Map.class);

        for (Map<String, Object> patternCfg: patternCfgs) {
            try {
                patterns.add(new ChannelsAutobanPattern(patternCfg));
            } catch (IllegalArgumentException e) {
                log(Level.ERROR, "Pattern config is invalid! " + e.getMessage() + " " + patternCfg);
            }
        }

        Map<String, Object> ipCheck = new LinkedHashMap<>(cfg.getSection("ipcheck"));
        try {
            ippattern = new ChannelsAutobanPattern(ipCheck);
        } catch (IllegalArgumentException e) {
            log(Level.ERROR, "IP Pattern config is invalid! " + e.getMessage() + " " + ipCheck);
        }
        ipWhitelist = cfg.getStringList("ipcheck.whitelist");

        // load counters
        Map<String, Object> counterCfgs =  cfg.getSection("counters");

        for (String counterName: counterCfgs.keySet()) {
            try {
                counters.put(counterName, new ChannelsAutobanCounter((Map<String, Object>) counterCfgs.get(counterName)));
            } catch (Exception e) {
                log(Level.WARN, "Could not load counter "+counterName+" due to invalid configuration. " + e.getMessage());
            }
        }

        // load actions
        Map<String, Object> actionCfgs = cfg.getSection("actions");
        for (String actionName: actionCfgs.keySet()) {
            try {
                actions.put(actionName, new ChannelsAutobanAction(this, (Map<String, Object>) actionCfgs.get(actionName)));
            } catch (Exception e) {
                log(Level.WARN, "Could not load action "+actionName+" due to invalid configuration. " + e.getMessage());
            }
        }
    }

    public List<ChannelsAutobanPattern> getPatterns() {
        return patterns;
    }

    public String getCommandSenderName() {
        return commandSenderName;
    }

    /**
     * increase violation counter
     * @param p The sender
     * @param pattern
     * @param msg
     * @param matcher
     */
    public void increaseCounter(final Player p, final ChannelsAutobanPattern pattern, final Message msg, Matcher matcher) {
        if (pattern.getCounter() == null) {
            return;
        }

        final UUID uuid = p.getUniqueId();
        if (!violations.contains(uuid, pattern.getCounter())) {
            violations.put(uuid, pattern.getCounter(), 1);
        } else {
            violations.put(uuid, pattern.getCounter(), violations.get(uuid, pattern.getCounter()) + 1);
        }

        // execute
        ChannelsAutobanCounter counter = counters.get(pattern.getCounter());
        if (counter == null) {
            log(Level.WARN, "No counter named '"+pattern.getCounter()+"' defined.");
        } else {
            // notify
            String matched = matcher == null ? pattern.getCounter() : matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
            HoverEvent hover = HoverEvent.showText(Component.text(msg.getRawMessage()));
            Component reasonNotify = Component.text("[Autoban] ").color(NamedTextColor.RED)
                    .append(Component.text(p.getUsername() + ": ")).color(NamedTextColor.WHITE)
                    .append(Component.text(matched)).color(NamedTextColor.WHITE).hoverEvent(hover);
            Component counterNotify = Component.text("[Autoban] ").color(NamedTextColor.RED)
                    .append(Component.text(p.getUsername() + "@" + pattern.getCounter() + ": "
                            + violations.get(uuid, pattern.getCounter()) + "/" + counter.getMax())).color(NamedTextColor.WHITE);
            getProxy().getConsoleCommandSource().sendMessage(reasonNotify);
            getProxy().getConsoleCommandSource().sendMessage(counterNotify);
            for (Player notify: getProxy().getAllPlayers()) {
                if (notify.hasPermission("autoban.notify")) {
                    notify.sendMessage(reasonNotify);
                    notify.sendMessage(counterNotify);
                }
            }

            if (violations.get(uuid, pattern.getCounter()) >= counter.getMax()) {
                ChannelsAutobanAction action = actions.get(counter.getAction());
                if (action == null) {
                    log(Level.WARN, "No action named '"+counter.getAction()+"' defined.");
                } else {
                    action.execute(p, counter);
                    return;
                }
            }

            // decrease counter
            getProxy().getScheduler().buildTask(this, () -> {
                if (violations.contains(uuid, pattern.getCounter())) {
                    violations.put(uuid, pattern.getCounter(), violations.get(uuid, pattern.getCounter()) - 1);

                    Component counterNotify1 = Component.text("[Autoban] ").color(NamedTextColor.RED)
                            .append(Component.text(p.getUsername() + "@" + pattern.getCounter() + ": "
                                    + violations.get(uuid, pattern.getCounter()) + "/" + counter.getMax())).color(NamedTextColor.WHITE);
                    getProxy().getConsoleCommandSource().sendMessage(counterNotify1);
                    for (Player notify: getProxy().getAllPlayers()) {
                        if (notify.hasPermission("autoban.notify")) {
                            notify.sendMessage(counterNotify1);
                        }
                    }
                }
            }).delay(counter.getTTL(), TimeUnit.SECONDS);
        }
    }

    public List<String> getServerGroup(String group) {
        return (List<String>) serverGroups.get(group);
    }

    public ChannelsAutobanPattern getIPPattern() {
        return ippattern;
    }

    public List<String> getIPWhitelist() {
        return ipWhitelist;
    }

    /**
     * get the optional ConnectorPlugin dependency
     * @return ConnectorPlugin or <tt>null</tt> if it isn't installed
     */
    VelocityConnectorPlugin getConnectorPlugin() {
        return connectorPlugin;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void log(org.slf4j.event.Level level, String message) {
        logger.atLevel(level).log(message);
    }

    public void log(org.slf4j.event.Level level, String message, Throwable throwable) {
        logger.atLevel(level).setCause(throwable).log(message);
    }


}
