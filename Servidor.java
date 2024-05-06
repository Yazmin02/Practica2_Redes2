import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Servidor {

    private static final int UDP_PORT = 1234;
    private static final int BUFFER_SIZE = 4096;

    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(UDP_PORT);
            System.out.println("Servidor escuchando en " + InetAddress.getLocalHost().getHostAddress() + ":" + UDP_PORT);
            while (true) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength());
                String[] packetData = data.split(":", 2);
                if (packetData.length == 2) {
                    String seqNum = packetData[0];
                    String payload = packetData[1];
                    System.out.println("Recibido segmento: " + seqNum + " " + payload);
                    String ack = seqNum;
                    DatagramPacket ackPacket = new DatagramPacket(ack.getBytes(), ack.getBytes().length, packet.getAddress(), packet.getPort());
                    socket.send(ackPacket);
                } else {
                    System.out.println("Datos recibidos no válidos: " + data);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
