package com.monitor.agent.net;

import com.monitor.model.Metric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Sends metrics via UDP (fire and forget).
 * Suitable for high-frequency, loss-tolerant data.
 */
public class UdpSender {
    
    private final String serverHost;
    private final int serverPort;
    private DatagramSocket socket;
    
    public UdpSender(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }
    
    /**
     * Initialize the UDP socket.
     */
    public void init() throws IOException {
        this.socket = new DatagramSocket();
        System.out.println("[UdpSender] Initialized, will send to " + serverHost + ":" + serverPort);
    }
    
    /**
     * Send a metric to the server via UDP.
     * @param metric The metric to send
     */
    public void sendMetric(Metric metric) {
        try {
            byte[] data = serialize(metric);
            InetAddress address = InetAddress.getByName(serverHost);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, serverPort);
            socket.send(packet);
            System.out.println("[UdpSender] Sent: " + metric);
        } catch (IOException e) {
            System.err.println("[UdpSender] Failed to send metric: " + e.getMessage());
        }
    }
    
    /**
     * Serialize a Metric object to byte array.
     */
    private byte[] serialize(Metric metric) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(metric);
            return baos.toByteArray();
        }
    }
    
    /**
     * Close the socket.
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("[UdpSender] Closed");
        }
    }
}
