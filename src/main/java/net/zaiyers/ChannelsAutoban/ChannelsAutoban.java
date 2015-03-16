package net.zaiyers.ChannelsAutoban;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
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
	private Map<String, List<String>> serverGroups;
	
	/**
	 * special pattern executed when ip matched
	 */
	private ChannelsAutobanPattern ippattern;
	
	/**
	 * ip whitelist
	 */
	private List<String> ipWhitelist = new ArrayList<String>();
	
	/**
	 * enable plugin
	 */
	@SuppressWarnings("unchecked")
	public void onEnable() {
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
		serverGroups = (Map<String, List<String>>) cfg.get("servergroups");
		
		// load patterns
		ArrayList<HashMap<String, Object>> patternCfgs = (ArrayList<HashMap<String, Object>>) cfg.get("patterns");
		
		for (HashMap<String, Object> patternCfg: patternCfgs) {
			patterns.add(new ChannelsAutobanPattern(patternCfg));
		}

		ippattern = new ChannelsAutobanPattern((HashMap<String, Object>) cfg.get("ipcheck"));
		ipWhitelist = cfg.getStringList("ipcheck.whitelist");
		
		// load counters
		HashMap<String, HashMap<String, Object>> counterCfgs = (HashMap<String, HashMap<String, Object>>) cfg.get("counters");
		
		for (String counterName: counterCfgs.keySet()) {
			try {
				counters.put(counterName, new ChannelsAutobanCounter(counterCfgs.get(counterName)));
			} catch (NumberFormatException e) {
				getLogger().warning("Could not load counter "+counterName+" due to invalid numbers in configuration.");
				e.printStackTrace();
			}
		}
		
		// load actions
		HashMap<String, HashMap<String, Object>> actionCfgs = (HashMap<String, HashMap<String, Object>>) cfg.get("actions");
		for (String actionName: actionCfgs.keySet()) {
			actions.put(actionName, new ChannelsAutobanAction(actionCfgs.get(actionName)));
		}
		
		// register listener
		getProxy().getPluginManager().registerListener(this, new ChannelsMessageListener(cfg.getSection("counters.spam")));
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
	 * @param sender
	 * @param pattern
	 * @param msg 
	 */
	public void increaseCounter(final ProxiedPlayer p, final ChannelsAutobanPattern pattern, final Message msg) {
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
			TextComponent reasonNotify = new TextComponent(ChatColor.RED+"[Autoban] "+ChatColor.WHITE+"<"+p.getDisplayName()+"> "+msg.getRawMessage());
			TextComponent counterNotify = new TextComponent(ChatColor.RED+"[Autoban] "+ChatColor.WHITE+p.getDisplayName()+"@"+pattern.getCounter()+": "+violations.get(uuid).get(pattern.getCounter())+"/"+counter.getMax());
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
			getProxy().getScheduler().schedule(this, new Runnable() {
				public void run() {
					if (violations.get(uuid) != null && violations.get(uuid).get(pattern.getCounter()) != null) {
						violations.get(uuid).put(pattern.getCounter(), violations.get(uuid).get(pattern.getCounter()) - 1);
						
						TextComponent counterNotify = new TextComponent(ChatColor.RED+"[Autoban] "+ChatColor.WHITE+p.getDisplayName()+"@"+pattern.getCounter()+": "+violations.get(uuid).get(pattern.getCounter()));
						for (ProxiedPlayer notify: getProxy().getPlayers()) {			
							if (notify.hasPermission("autoban.notify")) {
								notify.sendMessage(counterNotify);
							}
						}
					}
				}
				
			}, counter.getTTL(), TimeUnit.SECONDS);
		}
	}

	public List<String> getServerGroup(String group) {
		return serverGroups.get(group);
	}

	public ChannelsAutobanPattern getIPPattern() {
		return ippattern;
	}

	public List<String> getIPWhitelist() {
		return ipWhitelist;
	}
}
