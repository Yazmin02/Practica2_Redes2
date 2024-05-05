import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            // Solicitar al usuario la ruta del archivo a enviar
            System.out.print("Ingrese la ruta del archivo a enviar: ");
            String rutaArchivo = reader.readLine();

            // Solicitar al usuario el tamaño del paquete
            System.out.print("Ingrese el tamaño del paquete: ");
            int tamanoPaquete = Integer.parseInt(reader.readLine());

            // Solicitar al usuario el tamaño de la ventana
            System.out.print("Ingrese el tamaño de la ventana: ");
            int tamanoVentana = Integer.parseInt(reader.readLine());

            DatagramSocket clienteSocket = new DatagramSocket();
            InetAddress direccionServidor = InetAddress.getByName("localhost");
            int puertoServidor = 9876;

            // Archivo a enviar
            File archivo = new File(rutaArchivo);
            FileInputStream fis = new FileInputStream(archivo);
            byte[] buffer = new byte[(int) archivo.length()];
            fis.read(buffer);

            // Definir ventana deslizante
            SlidingWindow ventana = new SlidingWindow(tamanoVentana, (int) Math.ceil((double) buffer.length / tamanoPaquete));
            int base = 0;

            while (base < ventana.getTotalPackets()) {
                for (int i = base; i < Math.min(base + tamanoVentana, ventana.getTotalPackets()); i++) {
                    int offset = i * tamanoPaquete;
                    int length = Math.min(tamanoPaquete, buffer.length - offset);
                    byte[] datosPaquete = new byte[length];
                    System.arraycopy(buffer, offset, datosPaquete, 0, length);
                    DatagramPacket enviarPaquete = new DatagramPacket(datosPaquete, length, direccionServidor, puertoServidor);
                    clienteSocket.send(enviarPaquete);
                    System.out.println("Paquete enviado: " + i); // Imprimir número de paquete enviado
                }

                // Esperar un tiempo prudencial para los ACKs
                ventana.waitForAcks(clienteSocket);

                // Mover la ventana deslizante después de esperar los ACKs
                ventana.moveWindow();
            }

            clienteSocket.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
