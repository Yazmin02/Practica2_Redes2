import java.net.*;
import java.io.*;
import java.util.*;

public class Cliente {
    private DatagramSocket socket;
    private InetAddress address;
    private int serverPort;
    private int dataPort;
    private int base; // Base de la ventana
    private int nextSeqNum;
    private int windowSize;
    private List<byte[]> packets; // Lista de paquetes a enviar

    public Cliente(String ip, int port, int dataPort, int windowSize) throws UnknownHostException, SocketException {
        this.address = InetAddress.getByName(ip);
        this.serverPort = port;
        this.dataPort = dataPort;
        this.windowSize = windowSize;
        this.base = 0;
        this.nextSeqNum = 0;
        this.packets = new ArrayList<>();
        this.socket = new DatagramSocket(dataPort);
    }

    public void connectAndSendFile() throws IOException {
        System.out.println("Conectando al servidor en el host y puerto " + address.getHostAddress() + ":" + serverPort);
        socket.connect(address, serverPort);
        System.out.println("Conexión establecida. (Escribe 'quit' para cerrar la conexión)");

        Scanner scanner = new Scanner(System.in);
        String input;

        while (true) {
            input = scanner.nextLine();

            if (input.equals("quit")) {
                System.out.println("Comando quit recibido.");
                break;
            }

            System.out.print("Introduce el tamaño del paquete (en bytes): ");
            int packetSize = scanner.nextInt();
            System.out.print("Introduce el tamaño de la ventana: ");
            windowSize = scanner.nextInt();
            System.out.print("Introduce la ruta del archivo a enviar: ");
            String filePath = scanner.next();

            packets = splitFileIntoPackets(filePath, packetSize);
            sendPackets();
        }

        scanner.close();
    }

    private List<byte[]> splitFileIntoPackets(String filePath, int packetSize) throws IOException {
        List<byte[]> packets = new ArrayList<>();
        FileInputStream fileInputStream = new FileInputStream(filePath);
        byte[] buffer = new byte[packetSize];
        int bytesRead;

        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            byte[] packet = Arrays.copyOf(buffer, bytesRead);
            packets.add(packet);
        }

        fileInputStream.close();
        return packets;
    }

    private void sendPackets() throws IOException {
        while (base < packets.size()) {
            while (nextSeqNum < base + windowSize && nextSeqNum < packets.size()) {
                sendPacket(nextSeqNum);
                nextSeqNum++;
            }
            receiveAcks();
        }
    }

    private void sendPacket(int seqNum) throws IOException {
        String seqNumStr = Integer.toString(seqNum);
        byte[] sendData = seqNumStr.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);
        socket.send(datagramPacket);
        System.out.println("Enviado paquete con número de secuencia: " + seqNum);
    }


    private void receiveAcks() throws IOException {
        byte[] ackBuffer = new byte[1024];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
        socket.setSoTimeout(10000); //  tiempo de espera para recibir ACKs

        try {
            while (true) {
                socket.receive(ackPacket);
                int ackNum = Integer.parseInt(new String(ackPacket.getData()).trim());
                System.out.println("Recibida confirmación para el paquete: " + ackNum);
                base = Math.max(base, ackNum + 1);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("No se recibieron confirmaciones dentro del tiempo de espera. Retrocediendo N...");
            nextSeqNum = base; // Retroceder N
        }
    }

    public static void main(String[] args) throws IOException {
        String ip = "127.0.0.1"; // Dirección IP
        int port = 5555; // Puerto
        int dataPort = 5556; // Puerto de datos para la conexión
        int windowSize = 5;
        Cliente client = new Cliente(ip, port, dataPort, windowSize);
        client.connectAndSendFile();
    }
}
