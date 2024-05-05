import java.io.*;
import java.net.*;

public class Servidor {
    public static void main(String[] args) {
        try {
            DatagramSocket servidorSocket = new DatagramSocket(9876);
            byte[] recibirDatos = new byte[1024];
            int ackNum = 0;

            while (true) {
                DatagramPacket recibirPaquete = new DatagramPacket(recibirDatos, recibirDatos.length);
                servidorSocket.receive(recibirPaquete);

                // Procesar el paquete recibido (guardar en un archivo, etc.)
                FileOutputStream fos = new FileOutputStream("archivo_recibido.txt", true);
                fos.write(recibirDatos, 0, recibirPaquete.getLength());
                fos.close();

                // Enviar confirmación al cliente (ACK)
                InetAddress direccionCliente = recibirPaquete.getAddress();
                int puertoCliente = recibirPaquete.getPort();
                String ack = String.valueOf(ackNum);
                byte[] enviarDatos = ack.getBytes();
                DatagramPacket enviarPaquete = new DatagramPacket(enviarDatos, enviarDatos.length, direccionCliente, puertoCliente);
                servidorSocket.send(enviarPaquete);
                System.out.println("ACK enviado: " + ackNum); // Imprimir ACK enviado
                ackNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
