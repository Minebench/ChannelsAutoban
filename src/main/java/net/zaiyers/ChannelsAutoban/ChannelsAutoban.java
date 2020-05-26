package net.zaiyers.ChannelsAutoban;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.zaiyers.BungeeRPC.BungeeRPC;
import net.zaiyers.Channels.message.Message;

public class ChannelsAutoban extends Plugin {
    /**
     * configuration
     */
    private Configuration cfg;
    private final static ConfigurationProvider ymlCfg = ConfigurationProvider.getProvider( YamlConfiguration.class );
    private File configFile;

    /**
     * patterns to look for
     */
    private List<ChannelsAutobanPattern> patterns = new ArrayList<ChannelsAutobanPattern>();

    /**
     * counter configurations
     */
    private HashMap<String, ChannelsAutobanCounter> counters = new HashMap<String, ChannelsAutobanCounter>();

    /**
     * counter values
     */
    private Map<String, HashMap<String, Integer>> violations = new HashMap<String, HashMap<String, Integer>>();

    /**
     * action configurations
     */
    private HashMap<String, ChannelsAutobanAction> actions = new HashMap<String, ChannelsAutobanAction>();

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

        configFile = new File(this.getDataFolder()+File.separator+"config.yml");
        if (!configFile.exists()) {
            try {
                createDefaultConfig(configFile);
            } catch (IOException e) {
                e.printStackTrace();
                getLogger().severe("Unable to create configuration.");
                return;
            }
        } else {
            try {
                cfg = ConfigurationProvider.getProvider( YamlConfiguration.class ).load( configFile );
            } catch (IOException e) {
                getLogger().severe("Unable to load configuration.");
                e.printStackTrace();
                return;
            }
        }

        commandSenderName = cfg.getString("commandsender", "Autoban");
        serverGroups = cfg.getSection("servergroups");

        // load patterns
        ArrayList<HashMap<String, Object>> patternCfgs = (ArrayList<HashMap<String, Object>>) cfg.get("patterns");

        for (HashMap<String, Object> patternCfg: patternCfgs) {
            patterns.add(new ChannelsAutobanPattern(patternCfg));
        }

        Map<String, Object> ipCheck = new LinkedHashMap<String, Object>();
        for (String key : cfg.getSection("ipcheck").getKeys()) {
            ipCheck.put(key, cfg.get("ipcheck." + key));
        }
        ippattern = new ChannelsAutobanPattern(ipCheck);
        ipWhitelist = cfg.getStringList("ipcheck.whitelist");

        // load counters
        Configuration counterCfgs =  cfg.getSection("counters");

        for (String counterName: counterCfgs.getKeys()) {
            try {
                counters.put(counterName, new ChannelsAutobanCounter(counterCfgs.getSection(counterName)));
            } catch (NumberFormatException e) {
                getLogger().warning("Could not load counter "+counterName+" due to invalid numbers in configuration.");
                e.printStackTrace();
            }
        }

        // load actions
        Configuration actionCfgs = cfg.getSection("actions");
        for (String actionName: actionCfgs.getKeys()) {
            actions.put(actionName, new ChannelsAutobanAction(this, actionCfgs.getSection(actionName)));
        }

        // register listener
        getProxy().getPluginManager().registerListener(this, new ChannelsMessageListener(this));
    }

    /**
     * create default configuration
     * @param configFile
     * @throws IOException
     */
    private void createDefaultConfig(File configFile) throws IOException {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        configFile.createNewFile();
        cfg = ymlCfg.load(new InputStreamReader(getResourceAsStream("config.yml")));
        ymlCfg.save(cfg, configFile);
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

        final String uuid = p.getUniqueId().toString();
        if (violations.get(uuid) == null) {
            violations.put(uuid, new HashMap<String, Integer>());
        }
        if (violations.get(uuid).get(pattern.getCounter()) == null) {
            violations.get(uuid).put(pattern.getCounter(), 1);
        } else {
            violations.get(uuid).put(pattern.getCounter(), violations.get(uuid).get(pattern.getCounter()) + 1 );
        }

        // execute
        ChannelsAutobanCounter counter = counters.get(pattern.getCounter());
        if (counter == null) {
            getLogger().warning("No counter named '"+pattern.getCounter()+"' defined.");
        } else {
            // notify
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(msg.getRawMessage()).create());
            BaseComponent[] reasonNotify = new ComponentBuilder("[Autoban] ").color(ChatColor.RED)
                    .append("<"+p.getDisplayName()+"> ").color(ChatColor.WHITE).event(hover)
                    .append(matcher == null ? pattern.getCounter() : matcher.group()).color(ChatColor.WHITE).event(hover)
                    .create();
            BaseComponent[] counterNotify = TextComponent.fromLegacyText(ChatColor.RED+"[Autoban] "+ChatColor.WHITE+p.getDisplayName()+"@"+pattern.getCounter()+": "+violations.get(uuid).get(pattern.getCounter())+"/"+counter.getMax());
            getProxy().getConsole().sendMessage(reasonNotify);
            getProxy().getConsole().sendMessage(counterNotify);
            for (ProxiedPlayer notify: getProxy().getPlayers()) {
                if (notify.hasPermission("autoban.notify")) {
                    notify.sendMessage(reasonNotify);
                    notify.sendMessage(counterNotify);
                }
            }

            if (violations.get(uuid).get(pattern.getCounter()) >= counter.getMax()) {
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
                if (violations.get(uuid) != null && violations.get(uuid).get(pattern.getCounter()) != null) {
                    violations.get(uuid).put(pattern.getCounter(), violations.get(uuid).get(pattern.getCounter()) - 1);

                    BaseComponent[] counterNotify1 = TextComponent.fromLegacyText(ChatColor.RED+"[Autoban] "+ChatColor.WHITE+p.getDisplayName()+"@"+pattern.getCounter()+": "+violations.get(uuid).get(pattern.getCounter()));
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

    /**
     * get the plugin's config
     * @return the plugin's config
     */
    public Configuration getConfig() {
        return cfg;
    }
}
