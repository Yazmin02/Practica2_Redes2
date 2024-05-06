import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Cliente {

    private static final String UDP_IP = "127.0.0.1";
    private static final int UDP_PORT = 1234;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el número de ventana: ");
        int WINDOW_SIZE = scanner.nextInt();
        System.out.print("Ingrese el tamaño del paquete en bytes: ");
        int PACKET_SIZE = scanner.nextInt();
        System.out.print("Ingrese el nombre del archivo a enviar: ");
        String fileName = scanner.next();
        scanner.close();

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            byte[] buffer = new byte[PACKET_SIZE];
            InetAddress address = InetAddress.getByName(UDP_IP);

            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);

            int base = 0;
            int nextSeqNum = 0;

            while (base < file.length()) {
                int bytesRead = fis.read(buffer);
                if (bytesRead > 0) {
                    byte[] segment = (nextSeqNum + ":" + new String(buffer, 0, bytesRead)).getBytes();
                    DatagramPacket packet = new DatagramPacket(segment, segment.length, address, UDP_PORT);
                    socket.send(packet);
                    System.out.println("Enviando segmento: " + new String(segment));
                    nextSeqNum++;
                } else {
                    System.out.println("No hay más datos para leer del archivo.");
                    break;
                }

                while (base < nextSeqNum) {
                    DatagramPacket ackPacket = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                    socket.receive(ackPacket);
                    String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                    int ackNum = Integer.parseInt(ack);
                    System.out.println("Recibido ACK: " + ackNum);
                    base = Math.max(base, ackNum + 1);
                }
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
