import java.net.*;
import java.io.*;
import java.util.*;

public class Cliente {
    private DatagramSocket socket; // Socket para la comunicación UDP
    private InetAddress address; // Dirección IP del servidor
    private int serverPort; // Puerto del servidor
    private int dataPort; // Puerto de datos para la conexión
    private int base; // Base de la ventana
    private int nextSeqNum; // Próximo número de secuencia a enviar
    private int windowSize; // Tamaño de la ventana
    private List<byte[]> packets; // Lista de paquetes a enviar

    // Constructor de la clase Cliente
    public Cliente(String ip, int port, int dataPort, int windowSize) throws UnknownHostException, SocketException {
        this.address = InetAddress.getByName(ip); // Se obtiene la dirección IP del servidor
        this.serverPort = port; // Se establece el puerto del servidor
        this.dataPort = dataPort; // Se establece el puerto de datos para la conexión
        this.windowSize = windowSize; // Se establece el tamaño de la ventana
        this.base = 0; // Inicialmente, la base de la ventana es 0
        this.nextSeqNum = 0; // Inicialmente, el próximo número de secuencia es 0
        this.packets = new ArrayList<>(); // Se inicializa la lista de paquetes a enviar
        this.socket = new DatagramSocket(dataPort); // Se crea el socket UDP
    }

    // Método para conectar con el servidor y enviar el archivo
    public void connectAndSendFile() throws IOException {
        System.out.println("Conectando al servidor en el host y puerto " + address.getHostAddress() + ":" + serverPort);
        socket.connect(address, serverPort); // Se establece la conexión con el servidor
        System.out.println("Conexión establecida. (Escribe 'quit' para cerrar la conexión)");

        Scanner scanner = new Scanner(System.in);
        String input;

        // Ciclo para enviar el archivo al servidor
        while (true) {
            input = scanner.nextLine(); // Se lee la entrada del usuario

            if (input.equals("quit")) {
                System.out.println("Comando quit recibido. Cerrando conexión.");
                break; // Si el usuario ingresa "quit", se cierra la conexión
            }

            // Se solicita al usuario que ingrese el tamaño del paquete, el tamaño de la ventana y la ruta del archivo
            System.out.print("Introduce el tamaño del paquete (en bytes): ");
            int packetSize = scanner.nextInt();
            System.out.print("Introduce el tamaño de la ventana: ");
            windowSize = scanner.nextInt();
            System.out.print("Introduce la ruta del archivo a enviar: ");
            String filePath = scanner.next();

            packets = splitFileIntoPackets(filePath, packetSize); // Se divide el archivo en paquetes
            sendPackets(); // Se envían los paquetes al servidor
        }

        scanner.close();
    }

    // Método para dividir el archivo en paquetes de datos
    private List<byte[]> splitFileIntoPackets(String filePath, int packetSize) throws IOException {
        List<byte[]> packets = new ArrayList<>(); // Lista para almacenar los paquetes
        FileInputStream fileInputStream = new FileInputStream(filePath); // Flujo de entrada para leer el archivo
        byte[] buffer = new byte[packetSize]; // Buffer para leer datos del archivo
        int bytesRead;

        // Se lee el archivo y se divide en paquetes
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            byte[] packet = Arrays.copyOf(buffer, bytesRead); // Se crea un paquete con los datos leídos
            packets.add(packet); // Se agrega el paquete a la lista
        }

        fileInputStream.close(); // Se cierra el flujo de entrada del archivo
        return packets; // Se devuelve la lista de paquetes
    }

    // Método para enviar los paquetes al servidor
    private void sendPackets() throws IOException {
        // Se envían los paquetes mientras haya paquetes por enviar
        while (base < packets.size()) {
            // Se envían los paquetes dentro de la ventana
            while (nextSeqNum < base + windowSize && nextSeqNum < packets.size()) {
                sendPacket(nextSeqNum); // Se envía el paquete
                nextSeqNum++; // Se incrementa el número de secuencia
            }
            receiveAcks(); // Se espera la confirmación (ACK) del servidor
        }
    }

    // Método para enviar un paquete al servidor
    private void sendPacket(int seqNum) throws IOException {
        String seqNumStr = Integer.toString(seqNum); // Se convierte el número de secuencia a cadena
        byte[] sendData = seqNumStr.getBytes(); // Se obtienen los datos del paquete en formato de bytes
        DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, address, serverPort); // Se crea el paquete
        socket.send(datagramPacket); // Se envía el paquete al servidor
        System.out.println("Enviado paquete con número de secuencia: " + seqNum);
    }

    // Método para recibir las confirmaciones (ACKs) del servidor
    private void receiveAcks() throws IOException {
        byte[] ackBuffer = new byte[1024]; // Buffer para almacenar los datos del ACK
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length); // Paquete para recibir el ACK
        socket.setSoTimeout(10000); // Se establece un tiempo de espera para recibir ACKs

        try {
            // Se espera a recibir los ACKs del servidor
            while (true) {
                socket.receive(ackPacket); // Se recibe el ACK
                int ackNum = Integer.parseInt(new String(ackPacket.getData()).trim()); // Se obtiene el número de secuencia del ACK
                System.out.println("Recibida confirmación para el paquete: " + ackNum); // Se muestra en pantalla el número de secuencia del paquete confirmado
                base = Math.max(base, ackNum + 1); // Se actualiza la base de la ventana
            }
        } catch (SocketTimeoutException e) {
            // Si no se reciben ACKs dentro del tiempo de espera, se retrocede N
            System.out.println("No se recibieron confirmaciones dentro del tiempo de espera. Retrocediendo N...");
            nextSeqNum = base; // Se retrocede el número de secuencia
        }
    }

    // Método principal para iniciar el cliente
    public static void main(String[] args) throws IOException {
        String ip = "127.0.0.1"; // Dirección IP del servidor
        int port = 5555; // Puerto del servidor
        int dataPort = 5556; // Puerto de datos para la conexión
        int windowSize = 5; // Tamaño de la ventana
        Cliente client = new Cliente(ip, port, dataPort, windowSize); // Se crea una instancia del cliente
        client.connectAndSendFile(); // Se conecta al servidor y envía el archivo
    }
}
