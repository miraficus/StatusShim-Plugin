package org.retromc.templateplugin;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.logging.Level;

/**
 * A placeholder class for intercepting legacy ping packets (0xFE, 0x01).
 * Currently logs server status and prepares for deeper packet-level integration.
 */
public class LegacyPingListener {
    private final TemplatePlugin plugin;
    private final Server server;

    public LegacyPingListener(TemplatePlugin plugin) {
        this.plugin = plugin;
        this.server = Bukkit.getServer();
        initialize();
    }

    private void initialize() {
        plugin.logger(Level.INFO, "LegacyPingListener initialized.");

        // Log current server status to console for debugging
        String motd = plugin.getConfig().getConfigString("settings.ping-response.motd");
        int onlinePlayers = server.getOnlinePlayers().length;
        int maxPlayers = server.getMaxPlayers();

        plugin.logger(Level.INFO, "Ping MOTD: " + motd);
        plugin.logger(Level.INFO, "Online Players: " + onlinePlayers);
        plugin.logger(Level.INFO, "Max Players: " + maxPlayers);

        // This is where you'd hook into Poseidon's networking layer to intercept ping packets
        // For now, this just confirms the plugin is ready to respond
    }

    // Future method: respondToPing(NetworkManager manager) or similar
}
