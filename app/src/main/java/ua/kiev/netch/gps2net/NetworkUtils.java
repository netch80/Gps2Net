package ua.kiev.netch.gps2net;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by netch on 10.05.17.
 */

public class NetworkUtils {

    static void send(String host, int port, String message) {
        try {
            byte[] bytes = message.getBytes();
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet =
                    new DatagramPacket(bytes, bytes.length, address, port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            Log.w("NetworkUtils", String.format("send failed: %s", e.toString()));
        }
    }
}
