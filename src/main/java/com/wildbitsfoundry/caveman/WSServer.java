package com.wildbitsfoundry.caveman;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WSServer extends WebSocketServer {
    private ICaveRunner<String, String> _runner;

    public WSServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public WSServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
        System.out.printf("%n%s / joined++%n", conn.getRemoteSocketAddress().toString().split("/")[1]);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // this.sendToAll( conn + " has left the room!" );
        System.out.printf("%n%s / left--%n", conn.getRemoteSocketAddress().toString().split("/")[1]);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Add ws_chat for testing
        // this way I can ping the server
        // by sending ws_chat:<message_to_echo>
        if (message.startsWith("ws_chat")) {
            this.sendToAll(message.split(":")[1]);
        } else {
            String connection = conn.getRemoteSocketAddress().toString().split("/")[1];
            long startTripTime = System.nanoTime();

            System.out.printf("%n%s: sent %d bytes%n", connection, message.length());
            String output = "";
            try {
                output = this._runner.processServerMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.printf("server response: %d bytes%n", output.length());
			conn.send(output);

            long estimatedTripTime = System.nanoTime() - startTripTime;
            System.out.printf("round trip time: %f seconds%n", estimatedTripTime * 1e-9);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (ex.getMessage() == null) {
            if (conn.isClosed()) {
                // logger server was interrupted by user
            } else {
                // logger some connection dropped while waiting for a response
                // possible timeout
            }
            return;
        }
        if (ex.getMessage().equals("Address already in use: bind")) {
            System.out.println("Address already in use");

            // logger server port in use

            return;
        }
        if (ex.getMessage().equals("An existing connection was forcibly closed by the remote host")) {
            // logger this conn was forced out
            // System.out.printf("%n%s / left--%n",
            // conn.getRemoteSocketAddress().toString().split("/")[1]);
            // System.out.println("Connection closed by remote host");
            return;
        }

        // logger shouldn't have gotten this far
        // else
        // {
        // ex.printStackTrace();
        // if( conn != null )
        // {
        // // some errors like port binding failed may not be assignable to a specific
        // websocket
        // }
        // }
    }

    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    public void sendToAll(String text) {
        Set<WebSocket> con = connections();
        Set<WebSocket> copyConnections;
        synchronized (con) {
            copyConnections = new HashSet<>(con);
        }
        for (WebSocket c : copyConnections) {
            c.send(text);
        }

    }

    public void setCaveRunner(ICaveRunner<String, String> runner) {
        this._runner = runner;
    }
}