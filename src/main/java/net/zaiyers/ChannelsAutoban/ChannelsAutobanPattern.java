package net.zaiyers.ChannelsAutoban;

import java.util.HashMap;
import java.util.regex.Pattern;

public class ChannelsAutobanPattern {	
	/**
	 * the actual pattern
	 */
	private Pattern pattern;

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
	public ChannelsAutobanPattern(HashMap<String, Object> cfg) {
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
		}
		
		if (cfg.get("counter") != null) {
			counter = (String) cfg.get("counter");
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
