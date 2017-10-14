package net.zaiyers.ChannelsAutoban;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.zaiyers.Channels.ChannelsChatEvent;
import net.zaiyers.Channels.message.Message;

public class ChannelsMessageListener implements Listener {
    private static final Pattern IPPattern = Pattern.compile(".*?([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})(:[0-9]{3,5}|).*?");

    private final ChannelsAutoban plugin;

    /**
     * remember last messages until ttl runs out
     */
    private HashMap<String, ArrayList<Message>> msgHistory = new HashMap<String, ArrayList<Message>>();

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
            rate = spamCfg.getFloat("rate");
            denyRepeat = spamCfg.getBoolean("deny-repeat");

            HashMap<String, Object> spamPatternCfg = new HashMap<String, Object>();
            spamPatternCfg.put("counter", "spam");
            spamPattern = new ChannelsAutobanPattern(spamPatternCfg);
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
                plugin.increaseCounter(p, pattern, e.getMessage());
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

            if (serverAlive(host, port)) {
                if (plugin.getIPPattern().doHide()) {
                    e.setCancelled(true);
                } else if ((plugin.getIPPattern().getReplace() != null)) {
                    e.getMessage().setRawMessage(ipMatcher.replaceAll((plugin.getIPPattern().getReplace())));
                }

                if (!plugin.getIPWhitelist().contains(host)) {
                    plugin.increaseCounter(p, plugin.getIPPattern(), e.getMessage());
                }
            }
        }

        final String uuid = p.getUniqueId().toString();
        if (!msgHistory.containsKey(uuid)) {
            msgHistory.put(uuid, new ArrayList<Message>());
        }

        // check for spam
        if (rate > 0) {
            // repetitions
            if (denyRepeat) {
                for (Message m: msgHistory.get(uuid)) {
                    if (m.getRawMessage().equals(e.getMessage().getRawMessage())) {
                        e.setCancelled(true);
                    }
                }
            }

            final Message msg = e.getMessage();
            if (!e.isCancelled()) {
                msgHistory.get(uuid).add(msg);

                // remove from history after ttl seconds
                plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                    public void run() {
                        msgHistory.get(uuid).remove(msg);
                    }

                }, ttl, TimeUnit.SECONDS);

                // check for rate
                if ( (float) msgHistory.get(uuid).size() / (float) ttl > rate) {
                    plugin.increaseCounter(p, spamPattern, e.getMessage());
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
