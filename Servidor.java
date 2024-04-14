import java.net.*;
import java.io.*;
import java.util.*;

public class Servidor {
    private DatagramSocket socket;
    private int port;
    private int expectedSequenceNumber;
    private int windowSize; // Tamaño de la ventana
    private List<DatagramPacket> packetsInFlight; // Lista de paquetes
    private Timer timer; // Temporizador para retransmisiones

    public Servidor(int port, int windowSize) throws SocketException {
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.expectedSequenceNumber = 0;
        this.windowSize = windowSize;
        this.packetsInFlight = new ArrayList<>();
        this.timer = new Timer();
        System.out.println("Servidor inicializado en el puerto " + port + ". Esperando al cliente...");
    }

    public void receiveFile() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("archivo_recibido");

        byte[] buffer = new byte[1024];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        System.out.println("Esperando recibir paquetes del cliente...");

        while (true) {
            socket.receive(packet);
            String data = new String(packet.getData(), 0, packet.getLength());
            if (data.equals("FIN")) {
                break;
            }

            try {
                int sequenceNumber = Integer.parseInt(data);
                if (sequenceNumber == expectedSequenceNumber) {
                    // Paquete recibido en orden, escribir en el archivo y enviar ACK
                    fileOutputStream.write(packet.getData(), 0, packet.getLength());
                    System.out.println("Recibido paquete con número de secuencia: " + sequenceNumber);
                    sendAck(packet.getAddress(), packet.getPort(), sequenceNumber);
                    expectedSequenceNumber++;
                    // Mover la ventana
                    while (!packetsInFlight.isEmpty() && sequenceNumber == packetsInFlight.get(0).getData()[0]) {
                        packetsInFlight.remove(0);
                        expectedSequenceNumber++;
                    }
                    // Cancelar el temporizador si no hay más paquetes
                    if (packetsInFlight.isEmpty()) {
                        timer.cancel();
                        timer = new Timer();
                    }
                } else if (sequenceNumber > expectedSequenceNumber && sequenceNumber < expectedSequenceNumber + windowSize) {
                    // Paquete recibido fuera de orden, agregar a la lista de paquetes
                    packetsInFlight.add(packet);
                }
            } catch (NumberFormatException e) {
                System.err.println("Paquete recibido no es un número de secuencia válido: " + data);
            }
        }

        fileOutputStream.close();
        socket.close();
    }

    private void sendAck(InetAddress address, int port, int sequenceNumber) throws IOException {
        byte[] ackData = Integer.toString(sequenceNumber).getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        socket.send(ackPacket);
    }

    public static void main(String[] args) throws IOException {
        int port = 5555;
        int windowSize = 5;
        Servidor server = new Servidor(port,windowSize);
        server.receiveFile();
    }


}
