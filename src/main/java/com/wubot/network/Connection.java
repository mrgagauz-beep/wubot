package com.wubot.network;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * KryoNet connection wrapper.
 * Handles TCP connection to game server and queues incoming packets.
 */
public class Connection {
    private static final Logger log = LoggerFactory.getLogger(Connection.class);

    private static final int WRITE_BUFFER = 16384;
    private static final int OBJECT_BUFFER = 16384;
    private static final int TIMEOUT_MS = 15000;

    private final Client client;
    private final ConcurrentLinkedQueue<Object> incomingQueue;
    private volatile boolean connected;

    public Connection() {
        this.client = new Client(WRITE_BUFFER, OBJECT_BUFFER);
        this.incomingQueue = new ConcurrentLinkedQueue<>();
        this.connected = false;

        setupListener();
    }

    private void setupListener() {
        client.addListener(new Listener() {
            @Override
            public void connected(com.esotericsoftware.kryonet.Connection connection) {
                connected = true;
                log.info("Connected to server");
            }

            @Override
            public void disconnected(com.esotericsoftware.kryonet.Connection connection) {
                connected = false;
                log.info("Disconnected from server");
            }

            @Override
            public void received(com.esotericsoftware.kryonet.Connection connection, Object object) {
                if (object != null) {
                    incomingQueue.offer(object);
                    // Log all received packet types at DEBUG level for debugging
                    log.debug("Received packet: {}", object.getClass().getSimpleName());
                }
            }
        });
    }

    /**
     * Connect to game server (TCP only, no UDP).
     *
     * @param host server hostname
     * @param port TCP port
     * @throws IOException if connection fails
     */
    public void connect(String host, int port) throws IOException {
        log.info("Connecting to {}:{} (TCP only)...", host, port);

        // Register packets before connecting
        PacketRegistry.register(client.getKryo());

        // Start client thread
        client.start();

        // Connect TCP only (game doesn't use UDP)
        client.connect(TIMEOUT_MS, host, port);

        log.info("Connection established");
    }

    /**
     * Disconnect from server.
     */
    public void disconnect() {
        log.info("Disconnecting...");
        client.close();
        connected = false;
    }

    /**
     * Check if connected to server.
     */
    public boolean isConnected() {
        return connected && client.isConnected();
    }

    /**
     * Poll all received packets from queue.
     *
     * @return list of packets (may be empty)
     */
    public List<Object> pollPackets() {
        List<Object> packets = new ArrayList<>();
        Object packet;
        while ((packet = incomingQueue.poll()) != null) {
            packets.add(packet);
        }
        return packets;
    }

    /**
     * Send packet to server.
     *
     * @param packet packet to send
     */
    public void send(Object packet) {
        if (!isConnected()) {
            log.warn("Cannot send packet - not connected");
            return;
        }
        client.sendTCP(packet);
        log.trace("Sent: {}", packet.getClass().getSimpleName());
    }

    /**
     * Get underlying KryoNet client (for advanced usage).
     */
    public Client getClient() {
        return client;
    }
}
