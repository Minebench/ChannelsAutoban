package net.zaiyers.ChannelsAutoban;

import java.util.Map;

public class ChannelsAutobanCounter {
    private int max = -1;
    private int ttl = 0;
    private String action;
    private String reason = "";

    public ChannelsAutobanCounter(Map<String, Object> cfg) throws NumberFormatException {
        if (cfg.get("max") != null) {
            max = (int) cfg.get("max");
        }
        if (cfg.get("ttl") != null) {
            ttl = (int) cfg.get("ttl");
        }
        if (cfg.get("action") != null) {
            action = (String) cfg.get("action");
        }
        if (cfg.get("reason") != null) {
            reason = (String) cfg.get("reason");
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
