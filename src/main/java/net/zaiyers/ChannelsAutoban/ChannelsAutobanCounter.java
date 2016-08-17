package net.zaiyers.ChannelsAutoban;

import net.md_5.bungee.config.Configuration;

import java.util.HashMap;

public class ChannelsAutobanCounter {
	private int max = -1;
	private int ttl = 0;
	private String action;
	private String reason = "";
	
	public ChannelsAutobanCounter(Configuration cfg) throws NumberFormatException {
		if (cfg.get("max") != null) {
			max = cfg.getInt("max");
		}
		if (cfg.get("ttl") != null) {
			ttl = cfg.getInt("ttl");
		}
		if (cfg.get("action") != null) {
			action = cfg.getString("action");
		}
		if (cfg.get("reason") != null) {
			reason = cfg.getString("reason");
		}
	}
	
	public String getAction() {
		return action;
	}
	
	public String getReason() {
		return reason;
	}
	
	public int getMax() {
		return max;
	}
	
	public int getTTL() {
		return ttl;
	}
}
