package net.zaiyers.ChannelsAutoban;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.zaiyers.Channels.events.ChannelsChatEvent;
import net.zaiyers.Channels.message.Message;

public class ChannelsMessageListener implements Listener {
    private static final Pattern IPPattern = Pattern.compile(".*?([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})(:[0-9]{3,5}|).*?");

    private final ChannelsAutoban plugin;

    /**
     * remember last messages until ttl runs out
     */
    private Map<UUID, Cache<String, Message>> msgHistory = new HashMap<>();

    /**
     * delete from history after tll seconds
     */
    private int ttl = 0;

    /**
     * increase counter when msgrate exceeds rate msg/s
     */
    private float rate = 0;

    /**
     * special pattern for spam
     */
    private ChannelsAutobanPattern spamPattern;

    /**
     * deny repeating messages
     */
    private boolean denyRepeat = false;

    public ChannelsMessageListener(ChannelsAutoban plugin) {
        this.plugin = plugin;
        Configuration spamCfg = plugin.getConfig().getSection("counters.spam");
        if (spamCfg != null) {
            ttl = spamCfg.getInt("ttl");
            if (ttl < 0) ttl = 0;
            rate = spamCfg.getFloat("rate");
            denyRepeat = spamCfg.getBoolean("deny-repeat");

            HashMap<String, Object> spamPatternCfg = new HashMap<String, Object>();
            spamPatternCfg.put("counter", "spam");
            spamPattern = new ChannelsAutobanPattern(spamPatternCfg);

            plugin.getLogger().info("Spam config: ttl=" + ttl + "s, rate=" + rate + ", denyRepeat=" + denyRepeat);
        }
    }

    @EventHandler
    public void onChannelsMessage(ChannelsChatEvent e) {
        if (!(e.getMessage().getSender() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer p = (ProxiedPlayer) e.getMessage().getSender();

        if (p.hasPermission("autoban.exempt")) {
            return;
        }

        for (ChannelsAutobanPattern pattern : plugin.getPatterns()) {
            Matcher matcher = pattern.matcher(e.getMessage().getRawMessage());
            if (matcher.matches()) {
                plugin.increaseCounter(p, pattern, e.getMessage(), matcher);
                if (pattern.doHide()) {
                    e.setCancelled(true);
                } else if (pattern.getReplace() != null) {
                    e.getMessage().setRawMessage(matcher.replaceAll(pattern.getReplace()));
                }
            }
        }

        Matcher ipMatcher = IPPattern.matcher(e.getMessage().getRawMessage());
        if (ipMatcher.matches()) {
            String host = getHost(e.getMessage().getRawMessage());
            int port = getPort(e.getMessage().getRawMessage());

            if (!plugin.getIPWhitelist().contains(host) && serverAlive(host, port)) {
                if (plugin.getIPPattern().doHide()) {
                    e.setCancelled(true);
                } else if ((plugin.getIPPattern().getReplace() != null)) {
                    e.getMessage().setRawMessage(ipMatcher.replaceAll((plugin.getIPPattern().getReplace())));
                }
                plugin.increaseCounter(p, plugin.getIPPattern(), e.getMessage(), ipMatcher);
            }
        }

        // check for spam
        if (rate > 0) {
            final UUID uuid = p.getUniqueId();

            Cache<String, Message> senderCache = msgHistory.get(uuid);
            // repetitions
            if (denyRepeat
                    && senderCache != null
                    && senderCache.size() > 0
                    && senderCache.getIfPresent(e.getMessage().getRawMessage().toLowerCase()) != null) {
                e.setCancelled(true);
            }

            if (!e.isCancelled()) {
                if (senderCache == null) {
                    senderCache = CacheBuilder.newBuilder().expireAfterWrite(ttl, TimeUnit.SECONDS).build();
                    msgHistory.put(uuid, senderCache);
                }
                senderCache.put(e.getMessage().getRawMessage().toLowerCase(), e.getMessage());

                // check for rate
                if ((float) senderCache.size() / (float) ttl > rate) {
                    plugin.increaseCounter(p, spamPattern, e.getMessage(), null);
                }
            }
        }
    }

    private boolean serverAlive(String host, int port) {
        try {
            if (!host.equals("127.0.0.1") && !host.equals("0.0.0.0")) {
                InetSocketAddress sockaddr = new InetSocketAddress(host, port);
                Socket s = new Socket();
                s.connect(sockaddr, 250);
                s.close();
                plugin.getProxy().getLogger().info("[Autoban] Host "+host+":"+port+" is reachable.");
                return true;
            }
        } catch (IOException e) {
            plugin.getProxy().getLogger().info("[Autoban] Host "+host+":"+port+" is NOT reachable.");
        }
        return false;
    }

    /**
     * get hostip from string
     * @param msg
     * @return
     */
    private static String getHost(String msg) {
        Matcher matcher = IPPattern.matcher(msg);

        matcher.find();
        return matcher.group(1);
    }

    /**
     * get serverport from string
     * @param msg
     * @return
     */
    private static int getPort(String msg) {
        Matcher matcher = IPPattern.matcher(msg);

        matcher.find();
        if (matcher.group(2) == null || matcher.group(2).length() == 0) {
            return 25565;
        } else {
            int port = Integer.parseInt(matcher.group(2).replaceAll(":", ""));
            if (port != 0) { return port; }
        }

        return 25565;
    }
}
