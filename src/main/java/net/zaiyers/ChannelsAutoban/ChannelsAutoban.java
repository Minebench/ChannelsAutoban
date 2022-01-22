package net.zaiyers.ChannelsAutoban;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import de.themoep.bungeeplugin.BungeePlugin;
import de.themoep.bungeeplugin.FileConfiguration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.zaiyers.BungeeRPC.BungeeRPC;
import net.zaiyers.Channels.message.Message;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;

public class ChannelsAutoban extends BungeePlugin {

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
    private Configuration serverGroups;

    /**
     * special pattern executed when ip matched
     */
    private ChannelsAutobanPattern ippattern;

    /**
     * ip whitelist
     */
    private List<String> ipWhitelist = new ArrayList<String>();

    /**
     * BungeeRPC plugin
     */
    private BungeeRPC bungeeRpc;

    /**
     * enable plugin
     */
    @SuppressWarnings("unchecked")
    public void onEnable() {
        if (getProxy().getPluginManager().getPlugin("BungeeRPC") != null) {
            bungeeRpc = (BungeeRPC) getProxy().getPluginManager().getPlugin("BungeeRPC");
        }

        loadConfig();

        // register listener
        getProxy().getPluginManager().registerListener(this, new ChannelsMessageListener(this));

        // register command
        getProxy().getPluginManager().registerCommand(this, new ChannelsAutobanCommand(this));
    }

    void loadConfig() {
        FileConfiguration cfg = getConfig();
        commandSenderName = cfg.getString("commandsender", "Autoban");
        serverGroups = cfg.getSection("servergroups");

        // load patterns
        ArrayList<HashMap<String, Object>> patternCfgs = (ArrayList<HashMap<String, Object>>) cfg.get("patterns");

        for (HashMap<String, Object> patternCfg: patternCfgs) {
            try {
                patterns.add(new ChannelsAutobanPattern(patternCfg));
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, "Pattern config is invalid! " + e.getMessage() + " " + patternCfg);
            }
        }

        Map<String, Object> ipCheck = new LinkedHashMap<String, Object>();
        for (String key : cfg.getSection("ipcheck").getKeys()) {
            ipCheck.put(key, cfg.get("ipcheck." + key));
        }
        try {
            ippattern = new ChannelsAutobanPattern(ipCheck);
        } catch (IllegalArgumentException e) {
            getLogger().log(Level.SEVERE, "IP Pattern config is invalid! " + e.getMessage() + " " + ipCheck);
        }
        ipWhitelist = cfg.getStringList("ipcheck.whitelist");

        // load counters
        Configuration counterCfgs =  cfg.getSection("counters");

        for (String counterName: counterCfgs.getKeys()) {
            try {
                counters.put(counterName, new ChannelsAutobanCounter(counterCfgs.getSection(counterName)));
            } catch (NumberFormatException e) {
                getLogger().warning("Could not load counter "+counterName+" due to invalid numbers in configuration. " + e.getMessage());
            }
        }

        // load actions
        Configuration actionCfgs = cfg.getSection("actions");
        for (String actionName: actionCfgs.getKeys()) {
            actions.put(actionName, new ChannelsAutobanAction(this, actionCfgs.getSection(actionName)));
        }
    }

    /**
     * get myself
     * @return plugin
     */
    public static ChannelsAutoban getInstance() {
        return (ChannelsAutoban) ProxyServer.getInstance().getPluginManager().getPlugin("ChannelsAutoban");
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
    public void increaseCounter(final ProxiedPlayer p, final ChannelsAutobanPattern pattern, final Message msg, Matcher matcher) {
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
            getLogger().warning("No counter named '"+pattern.getCounter()+"' defined.");
        } else {
            // notify
            String matched = matcher == null ? pattern.getCounter() : matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(msg.getRawMessage()).create());
            BaseComponent[] reasonNotify = new ComponentBuilder("[Autoban] ").color(ChatColor.RED)
                    .append(p.getDisplayName()+": ").color(ChatColor.WHITE)
                    .append(matched).color(ChatColor.WHITE).event(hover)
                    .create();
            BaseComponent[] counterNotify = TextComponent.fromLegacyText(ChatColor.RED+"[Autoban] "+ChatColor.WHITE+p.getDisplayName()+"@"+pattern.getCounter()+": "+violations.get(uuid, pattern.getCounter())+"/"+counter.getMax());
            getProxy().getConsole().sendMessage(reasonNotify);
            getProxy().getConsole().sendMessage(counterNotify);
            for (ProxiedPlayer notify: getProxy().getPlayers()) {
                if (notify.hasPermission("autoban.notify")) {
                    notify.sendMessage(reasonNotify);
                    notify.sendMessage(counterNotify);
                }
            }

            if (violations.get(uuid, pattern.getCounter()) >= counter.getMax()) {
                ChannelsAutobanAction action = actions.get(counter.getAction());
                if (action == null) {
                    getLogger().warning("No action named '"+counter.getAction()+"' defined.");
                } else {
                    action.execute(p, counter);
                    return;
                }
            }

            // decrease counter
            getProxy().getScheduler().schedule(this, () -> {
                if (violations.contains(uuid, pattern.getCounter())) {
                    violations.put(uuid, pattern.getCounter(), violations.get(uuid, pattern.getCounter()) - 1);

                    BaseComponent[] counterNotify1 = TextComponent.fromLegacyText(ChatColor.RED+"[Autoban] "+ChatColor.WHITE+p.getDisplayName()+"@"+pattern.getCounter()+": "+violations.get(uuid, pattern.getCounter()));
                    getProxy().getConsole().sendMessage(counterNotify1);
                    for (ProxiedPlayer notify: getProxy().getPlayers()) {
                        if (notify.hasPermission("autoban.notify")) {
                            notify.sendMessage(counterNotify1);
                        }
                    }
                }
            }, counter.getTTL(), TimeUnit.SECONDS);
        }
    }

    public List<String> getServerGroup(String group) {
        return serverGroups.getStringList(group);
    }

    public ChannelsAutobanPattern getIPPattern() {
        return ippattern;
    }

    public List<String> getIPWhitelist() {
        return ipWhitelist;
    }

    /**
     * get the optional BungeeRPC dependency
     * @return BungeeRPC or <tt>null</tt> if it isn't installed
     */
    BungeeRPC getBungeeRpc() {
        return bungeeRpc;
    }

}
