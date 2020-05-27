package net.zaiyers.ChannelsAutoban;

import java.util.Map;
import java.util.regex.Matcher;
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
     * replace the matched pattern?
     */
    private String replace;

    /**
     * name of the counter
     */
    private String counter;

    /**
     * construct object from config
     * @param patternCfg
     */
    public ChannelsAutobanPattern(Map<String, Object> cfg) {
        boolean fuzzy = false;
        if (cfg.get("fuzzy") != null) {
            fuzzy = (Boolean) cfg.get("fuzzy");
        }

        if (cfg.get("pattern") != null) {
            if (fuzzy) {
                pattern = Pattern.compile(".*?(" + cfg.get("pattern") + ").*?", Pattern.CASE_INSENSITIVE );
            } else {
                pattern = Pattern.compile("(" + cfg.get("pattern") + ")");
            }
        }

        if (cfg.get("counter") != null) {
            counter = (String) cfg.get("counter");
        }
        if (cfg.get("hide") != null) {
            hide = (Boolean) cfg.get("hide");
        }
        if (cfg.get("replace") != null) {
            replace = (String) cfg.get("replace");
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
     * should we replace it with something? if so with what?
     * @return replace
     */
    public String getReplace() {
        return replace;
    }

    /**
     * get a new Matcher from this pattern
     * @param msg the message to match against
     * @return the Matcher
     */
    public Matcher matcher(String msg) {
        return pattern.matcher(msg);
    }
}
