import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

class SlidingWindow {
    private int size;
    private int base;
    private int nextSeqNum;
    private int totalPackets;
    private boolean[] acksReceived;

    private static final int RETROCESO_N = 4;

    public SlidingWindow(int size, int totalPackets) {
        this.size = size;
        this.base = 0;
        this.nextSeqNum = 0;
        this.totalPackets = totalPackets;
        this.acksReceived = new boolean[totalPackets];
    }

    public void waitForAcks(DatagramSocket socket) throws IOException {
        long startTime = System.currentTimeMillis();
        byte[] receiveData = new byte[65507]; // Tamaño máximo del paquete UDP
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        while (System.currentTimeMillis() - startTime < 1000) {
            socket.setSoTimeout(1000); // Timeout de 1 segundo
            try {
                socket.receive(receivePacket);
                int ackNum = receiveData[0]; // Suponiendo que el número de ACK se encuentra en el primer byte recibido
                if (ackNum >= base && ackNum < base + size) {
                    acksReceived[ackNum] = true;
                    System.out.println("ACK recibido: " + ackNum); // Imprimir ACK recibido
                }
            } catch (SocketTimeoutException e) {
                // Retroceder N paquetes si no se reciben todos los ACKs dentro del tiempo de espera
                int retroceder = 0;
                for (int i = base; i < Math.min(base + size, totalPackets); i++) {
                    if (!acksReceived[i]) {
                        retroceder++;
                        if (retroceder > RETROCESO_N) {
                            base = i;
                            break;
                        }
                    }
                }
                return;
            }
        }
    }

    public boolean allAcksReceived() {
        for (int i = base; i < Math.min(base + size, totalPackets); i++) {
            if (!acksReceived[i]) {
                return false;
            }
        }
        return true;
    }

    public void moveWindow() {
        while (base < totalPackets && acksReceived[base]) {
            base++;
        }
    }

    public int getTotalPackets() {
        return totalPackets;
    }
}
