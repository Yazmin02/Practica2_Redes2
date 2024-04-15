import java.net.*;
import java.io.*;
import java.util.*;

public class Servidor {
    private DatagramSocket socket; // Socket para la comunicación UDP
    private int port; // Puerto del servidor
    private int expectedSequenceNumber; // Número de secuencia esperado
    private int windowSize; // Tamaño de la ventana
    private List<DatagramPacket> packetsInFlight; // Lista de paquetes en vuelo
    private Timer timer; // Temporizador para retransmisiones

    // Constructor de la clase Servidor
    public Servidor(int port, int windowSize) throws SocketException {
        this.port = port; // Se establece el puerto del servidor
        this.socket = new DatagramSocket(port); // Se crea el socket UDP
        this.expectedSequenceNumber = 0; // Inicialmente, el número de secuencia esperado es 0
        this.windowSize = windowSize; // Se establece el tamaño de la ventana
        this.packetsInFlight = new ArrayList<>(); // Se inicializa la lista de paquetes en vuelo
        this.timer = new Timer(); // Se crea el temporizador
        System.out.println("Servidor inicializado en el puerto " + port + ". Esperando al cliente...");
    }

    // Método para recibir el archivo del cliente
    public void receiveFile() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("archivo_recibido"); // Flujo de salida para escribir en el archivo

        byte[] buffer = new byte[1024]; // Buffer para almacenar los datos del paquete recibido

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // Paquete para recibir los datos del cliente

        System.out.println("Esperando recibir paquetes del cliente...");

        while (true) {
            socket.receive(packet); // Se recibe el paquete del cliente
            String data = new String(packet.getData(), 0, packet.getLength()); // Se convierten los datos del paquete a cadena

            if (data.equals("FIN")) {
                break; // Si se recibe "FIN", se termina la recepción del archivo
            }

            try {
                int sequenceNumber = Integer.parseInt(data); // Se convierte la cadena a un número de secuencia

                if (sequenceNumber == expectedSequenceNumber) {
                    // Paquete recibido en orden, se escribe en el archivo y se envía un ACK
                    fileOutputStream.write(packet.getData(), 0, packet.getLength());
                    System.out.println("Recibido paquete con número de secuencia: " + sequenceNumber);
                    sendAck(packet.getAddress(), packet.getPort(), sequenceNumber); // Se envía un ACK al cliente
                    expectedSequenceNumber++; // Se incrementa el número de secuencia esperado

                    // Se mueve la ventana
                    while (!packetsInFlight.isEmpty() && sequenceNumber == packetsInFlight.get(0).getData()[0]) {
                        packetsInFlight.remove(0);
                        expectedSequenceNumber++;
                    }

                    // Se cancela el temporizador si no hay más paquetes en vuelo
                    if (packetsInFlight.isEmpty()) {
                        timer.cancel();
                        timer = new Timer();
                    }
                } else if (sequenceNumber > expectedSequenceNumber && sequenceNumber < expectedSequenceNumber + windowSize) {
                    // Paquete recibido fuera de orden, se agrega a la lista de paquetes en vuelo
                    packetsInFlight.add(packet);
                }
            } catch (NumberFormatException e) {
                System.err.println("Paquete recibido no es un número de secuencia válido: " + data);
            }
        }

        fileOutputStream.close(); // Se cierra el flujo de salida del archivo
        socket.close(); // Se cierra el socket del servidor
    }

    // Método para enviar un ACK al cliente
    private void sendAck(InetAddress address, int port, int sequenceNumber) throws IOException {
        byte[] ackData = Integer.toString(sequenceNumber).getBytes(); // Se convierte el número de secuencia a bytes
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port); // Se crea el paquete ACK
        socket.send(ackPacket); // Se envía el ACK al cliente
    }

    // Método principal para iniciar el servidor
    public static void main(String[] args) throws IOException {
        int port = 5555; // Puerto del servidor
        int windowSize = 5; // Tamaño de la ventana
        Servidor server = new Servidor(port, windowSize); // Se crea una instancia del servidor
        server.receiveFile(); // Se inicia la recepción del archivo del cliente
    }
}
