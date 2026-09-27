import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Error: Proporciona el archivo de entrada como argumento.");
            return;
        }

        String rutaArchivo = args[0];
        int cajasObj = 0, cajasBloq = 0, numRobots = 0, bateria = 0, numProductores = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            cajasObj = Integer.parseInt(br.readLine().split(", ")[1]);
            cajasBloq = Integer.parseInt(br.readLine().split(", ")[1]);
            
            String[] datosRobots = br.readLine().split(", ");
            numRobots = Integer.parseInt(datosRobots[1]);
            bateria = Integer.parseInt(datosRobots[2]);
            
            numProductores = Integer.parseInt(br.readLine().split(", ")[1]);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error al procesar el archivo: " + e.getMessage());
            return;
        }

        int totalCajas = cajasObj + cajasBloq;


        Robot[] arregloRobots = new Robot[numRobots];
        for (int i = 0; i < numRobots; i++) {
            
            arregloRobots[i] = new Robot(i + 1, bateria, 0, 0, null); 
        }

        Caja[] arregloCajas = new Caja[totalCajas];
        for (int i = 0; i < totalCajas; i++) {
            boolean esObjetivo = (i < cajasObj); 

            arregloCajas[i] = new Caja(esObjetivo); 
        }

        Tablero elTablero = new Tablero(arregloRobots, numRobots, totalCajas, arregloCajas);

        for (int i = 1; i <= numProductores; i++) {
            Transportadora t = new Transportadora(String.valueOf(i), elTablero);
            
            Thread hiloT = new Thread(t);
            hiloT.start();
        }
        // 1 y 2. Vincular el tablero real a los robots y arrancar sus hilos
        Thread[] hilosRobots = new Thread[numRobots];

        for (int i = 0; i < numRobots; i++) {
            arregloRobots[i].setTablero(elTablero); // Requiere que tengas el método setTablero() en la clase Robot
            
            hilosRobots[i] = new Thread(arregloRobots[i]);
            hilosRobots[i].start();
        }

        // 3. Sincronización final: El Main espera a que los robots mueran
        try {

            for (int i = 0; i < numRobots; i++) {
                hilosRobots[i].join(); // Pausa el hilo principal hasta que el robot 'i' termine su run()
            }

        } catch (InterruptedException e) {
            System.out.println("El hilo principal fue interrumpido.");
        }

        elTablero.apagarFabrica();
        // elTablero.imprimirReporte(); // Llama a tu método final aquí

    }
}