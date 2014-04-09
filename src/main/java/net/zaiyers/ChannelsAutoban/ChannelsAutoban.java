package net.zaiyers.ChannelsAutoban;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.zaiyers.Channels.Channels;

public class ChannelsAutoban extends Plugin {
	/**
	 * configuration
	 */
	private Configuration cfg;
	private final static ConfigurationProvider ymlCfg = ConfigurationProvider.getProvider( YamlConfiguration.class );
	private File configFile; 
	
	/**
	 * enable plugin
	 */
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
		}
		
		
	}

	/**
	 * create default configuration
	 * @param configFile
	 * @throws IOException
	 */
	private void createDefaultConfig(File configFile) throws IOException {
		configFile.createNewFile();
		cfg = ymlCfg.load(new InputStreamReader(Channels.getInstance().getResourceAsStream("config.yml")));
		ymlCfg.save(cfg, configFile);
	}
}
