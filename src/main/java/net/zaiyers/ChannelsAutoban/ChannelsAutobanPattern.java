package net.zaiyers.ChannelsAutoban;

import java.util.HashMap;
import java.util.regex.Pattern;

import javax.naming.ConfigurationException;

public class ChannelsAutobanPattern {	
	/**
	 * the actual pattern
	 */
	private Pattern pattern;
	
	/**
	 * reason, inserted in action
	 */
	private String reason;
	
	/**
	 * hide messages matching this pattern
	 */
	private boolean hide = false;
	
	/**
	 * name of the counter
	 */
	private String counter;
		
	/**
	 * construct object from config
	 * @param patternCfg
	 */
	public ChannelsAutobanPattern(HashMap<String, Object> cfg) throws ConfigurationException {
		boolean fuzzy = false;
		if (cfg.get("fuzzy") != null) {
			fuzzy = (Boolean) cfg.get("fuzzy");
		}
		
		if (cfg.get("pattern") != null) {
			if (fuzzy) {
				pattern = Pattern.compile(".*?" + ((String) cfg.get("pattern")) + ".*?", Pattern.CASE_INSENSITIVE );
			} else {
				pattern = Pattern.compile((String) cfg.get("pattern"));
			}
		} else {
			throw new ConfigurationException("No pattern defined.");
		}
		if (cfg.get("counter") != null) {
			counter = (String) cfg.get("counter");
		}
		if (cfg.get("reason") != null) {
			reason = (String) cfg.get("reason");
		}
		if (cfg.get("hide") != null) {
			hide = (Boolean) cfg.get("hide");
		}
	}
		
	/**
	 * get name of counter
	 * @return action
	 */
	public String getCounter() {
		return counter;
	}
	
	/**
	 * get reason for executing
	 * @return reason
	 */
	public String getReason() {
		return reason;
	}
	
	/**
	 * should I hide a message?
	 * @return hide
	 */
	public boolean doHide() {
		return hide;
	}
	
	/**
	 * check if string matches pattern
	 * @param msg
	 * @return true if string matches pattern
	 */
	public boolean matches(String msg) {
		return pattern.matcher(msg).matches();
	}
}
