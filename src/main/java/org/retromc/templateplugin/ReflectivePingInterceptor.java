package org.retromc.templateplugin;

import org.bukkit.Bukkit;
import org.retromc.templateplugin.TemplatePlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * Reflectively inspects Poseidon's internal server structure to locate networking components.
 * Logs fields inside CraftServer, MinecraftServer, NetworkListenThread, and each pending connection.
 * Adds a repeating task to scan pendingConnections every second and injects ping responses safely.
 */
public class ReflectivePingInterceptor {
    private final TemplatePlugin plugin;

    public ReflectivePingInterceptor(TemplatePlugin plugin) {
        this.plugin = plugin;

        try {
            Object serverInstance = Bukkit.getServer();
            Class<?> serverClass = serverInstance.getClass();
            plugin.logger(Level.INFO, "Inspecting server class: " + serverClass.getName());

            for (Field f : serverClass.getDeclaredFields()) {
                f.setAccessible(true);
                Object value = f.get(serverInstance);
                plugin.logger(Level.INFO, "Field: " + f.getName() + " → " +
                        (value != null ? value.getClass().getName() : "null"));
            }

            Field consoleField = serverClass.getDeclaredField("console");
            consoleField.setAccessible(true);
            Object minecraftServer = consoleField.get(serverInstance);
            Class<?> mcServerClass = minecraftServer.getClass();
            plugin.logger(Level.INFO, "Inspecting MinecraftServer class: " + mcServerClass.getName());

            for (Field f : mcServerClass.getDeclaredFields()) {
                f.setAccessible(true);
                Object value = f.get(minecraftServer);
                plugin.logger(Level.INFO, "Field: " + f.getName() + " → " +
                        (value != null ? value.getClass().getName() : "null"));
            }

            Field nltField = mcServerClass.getDeclaredField("networkListenThread");
            nltField.setAccessible(true);
            Object nltInstance = nltField.get(minecraftServer);
            Class<?> nltClass = nltInstance.getClass();
            plugin.logger(Level.INFO, "Inspecting NetworkListenThread class: " + nltClass.getName());

            for (Field f : nltClass.getDeclaredFields()) {
                f.setAccessible(true);
                Object value = f.get(nltInstance);
                plugin.logger(Level.INFO, "Field: " + f.getName() + " → " +
                        (value != null ? value.getClass().getName() : "null"));
            }

            plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                try {
                    Field pendingField = nltClass.getDeclaredField("pendingConnections");
                    pendingField.setAccessible(true);
                    Object pendingList = pendingField.get(nltInstance);

                    if (pendingList instanceof List<?>) {
                        for (Object conn : (List<?>) pendingList) {
                            Class<?> connClass = conn.getClass();
                            plugin.logger(Level.INFO, "Live connection: " + connClass.getName());

                            for (Field f : connClass.getDeclaredFields()) {
                                f.setAccessible(true);
                                Object value = f.get(conn);
                                plugin.logger(Level.INFO, "  ↳ Field: " + f.getName() + " → " +
                                        (value != null ? value.getClass().getName() : "null"));
                            }

                            if (connClass.getName().equals("net.minecraft.server.NetLoginHandler")) {
                                try {
                                    Field finishedField = connClass.getDeclaredField("finishedProcessing");
                                    finishedField.setAccessible(true);
                                    boolean finished = (Boolean) finishedField.get(conn);

                                    Field loginPacketField = connClass.getDeclaredField("receivedLoginPacket");
                                    loginPacketField.setAccessible(true);
                                    boolean receivedLogin = (Boolean) loginPacketField.get(conn);

                                    Field usernameField = connClass.getDeclaredField("username");
                                    usernameField.setAccessible(true);
                                    Object username = usernameField.get(conn);

                                    // Only inject if it's likely a ping (e.g. mcstatus or legacy)
                                    if (!finished && !receivedLogin && username == null) {
                                        plugin.logger(Level.INFO, "Detected likely ping probe (mcstatus or legacy). Injecting response.");

                                        Field nmField = connClass.getDeclaredField("networkManager");
                                        nmField.setAccessible(true);
                                        Object networkManager = nmField.get(conn);

                                        String response = "§1\0Back2Beta Server\0" +
                                                plugin.getServer().getMaxPlayers() + "\0Beta 1.7.3";

                                        Class<?> packetClass = Class.forName("net.minecraft.server.Packet255KickDisconnect");
                                        Object packet = packetClass.getConstructor(String.class).newInstance(response);

                                        Method queueMethod = networkManager.getClass().getMethod(
                                                "queue", Class.forName("net.minecraft.server.Packet"));
                                        queueMethod.invoke(networkManager, packet);

                                        plugin.logger(Level.INFO, "Injected ping response to NetLoginHandler");
                                    }
                                } catch (Exception e) {
                                    plugin.logger(Level.SEVERE, "Failed to inject ping response: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.logger(Level.SEVERE, "Polling error: " + e.getMessage());
                }
            }, 20L, 20L); // every 1 second

        } catch (Exception e) {
            plugin.logger(Level.SEVERE, "Failed to reflect Poseidon internals: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
