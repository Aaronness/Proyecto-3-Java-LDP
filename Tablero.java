import java.util.Random;
import java.util.Arrays;

public class Tablero {
    private char[][] matriz = new char[6][6];
    private int casillasOcupadas;
    private int cajasObjetivoDisponibles;
    private boolean simulacionActiva = true;
    private int tickActual = 1;

    public Tablero(Robot robots[], int cant_robots, int cant_cajas, Caja cajas[]){
        this.casillasOcupadas = cant_robots + cant_cajas;

        for(int i=0; i<6; i++){
            Arrays.fill(matriz[i], '.');
        } 

        for(int i=0; i<cant_robots; i++){
            int[] posicion = casillaLibre(false);
            matriz[posicion[0]][posicion[1]] = 'R';

            robots[i].setX(posicion[0]);
            robots[i].setY(posicion[1]);
        }

        for(int i=0; i<cant_cajas; i++){
            int[] posicion = casillaLibre(true);

            if(cajas[i].getObjetivo()){
                matriz[posicion[0]][posicion[1]] = 'O';
            }
            else{
                matriz[posicion[0]][posicion[1]] = 'X';
            }

            cajas[i].setX(posicion[0]);
            cajas[i].setY(posicion[1]);
        }

    }

    public char[][] obtenerCopiaMatriz() {
        char[][] copia = new char[6][6];
        for (int i = 0; i < 6; i++) {
            copia[i] = this.matriz[i].clone(); 
        }
        return copia;
    }   

    private int[] casillaLibre(boolean esParaCaja) { //Se le pasa un booleano porque el robot si puede estar en el 5,5
        Random randomizador = new Random();
        int i, j;
        do {
            i = randomizador.nextInt(6);
            j = randomizador.nextInt(6);
        // Repetir si la casilla está ocupada O (si es una caja Y cayó en 5,5)
        } while (matriz[i][j] != '.' || (esParaCaja && i == 5 && j == 5));
    
        return new int[]{i,j};
    }

    private boolean esPosicionValida(int x, int y) {
        return x >= 0 && x < 6 && y >= 0 && y < 6;
    }

    public synchronized boolean estaActiva() { return simulacionActiva; }

    public synchronized void apagarFabrica() {
        this.simulacionActiva = false;
        notifyAll(); // Despierta a las transportadoras atascadas en wait()
    }

    public synchronized void imprimirLog(String identificador, String accion) {
        // String.format("%02d", tickActual) formatea el número para que sea 01, 02, etc., 
        // tal como muestran los ejemplos de trazas esperadas.
        String tickFormateado = String.format("%02d", tickActual);
    
        System.out.println("[Tick-" + tickFormateado + "] [" + identificador + "] " + accion);
    
        tickActual++; // Aumentamos el tiempo global para el siguiente hilo
    }

    // MÉTODO DEL PRODUCTOR
    public synchronized void insertarCaja(Caja nuevaCaja, String idProductor) throws InterruptedException {
        // Guarda condicional: si el tablero está lleno, el productor espera
        while (casillasOcupadas >= 36) {
            // Requerimiento: Registrar la saturación antes de entrar en espera
            imprimirLog(idProductor, "ESPERA_SATURACION (Intento de insertar en tablero lleno)");
            wait(); 
        }

        // Encontrar una casilla libre en la matriz
        int[] posicion = casillaLibre(true);
        int fila = posicion[0];
        int col = posicion[1];

        // Colocar la caja en la matriz de caracteres
        char simbolo = nuevaCaja.obtenerSimbolo();
        matriz[fila][col] = simbolo;    

        // Actualizar las coordenadas de la caja 
        nuevaCaja.moverA(fila, col);

        // Actualizar las variables de estado del monitor
        casillasOcupadas++;
        if (nuevaCaja.getObjetivo()) {
            cajasObjetivoDisponibles++;
        }

        // Imprimir la traza de ejecución obligatoria
        String tipoCaja = nuevaCaja.getObjetivo() ? "OBJETIVO" : "BLOQUEO";
        imprimirLog(idProductor, "INSERTAR_" + tipoCaja + " -> (" + fila + "," + col + ")");

        // Notificar a los robots (consumidores) que hay una nueva caja
        notifyAll();
    }

    public synchronized void esperarPorCajasObjetivo() throws InterruptedException {
        while (cajasObjetivoDisponibles == 0) {
            wait();
        }
    }

    public synchronized boolean intentarMovimiento(Robot robot, int deltaX, int deltaY) {
        String robotId = "Robot-" + robot.getIdentificador();
        int origenX = robot.getX();
        int origenY = robot.getY();
        int destinoX = origenX + deltaX;
        int destinoY = origenY + deltaY;

        if(!esPosicionValida(destinoX, destinoY)) return false;

        if (matriz[destinoX][destinoY] == '.') {
            matriz[origenX][origenY] = '.';  
            matriz[destinoX][destinoY] = 'R'; 
        
            robot.setX(destinoX);
            robot.setY(destinoY);
        
            imprimirLog(robotId, "MOVER (" + origenX + "," + origenY + ") -> (" + destinoX + "," + destinoY + ") | Batería: " + (robot.getBateria() - 1));
        
            notifyAll();
            return true;
        }


        if (matriz[destinoX][destinoY] == 'O') {
            int destCajaX = destinoX + deltaX;
            int destCajaY = destinoY + deltaY;

            if (destCajaX == 5 && destCajaY == 5) {
                matriz[origenX][origenY] = '.';
                matriz[destinoX][destinoY] = 'R'; 
                robot.setX(destinoX);
                robot.setY(destinoY);
            
                casillasOcupadas--;
                cajasObjetivoDisponibles--;
            
                imprimirLog(robotId, "EXTRACCION_EXITOSA -> (5,5)");
            
                notifyAll(); 
                return true;
            }

            if (esPosicionValida(destCajaX, destCajaY)) {         
                if (matriz[destCajaX][destCajaY] == '.') {
                    matriz[destCajaX][destCajaY] = 'O'; 
                
                    matriz[origenX][origenY] = '.';        
                    matriz[destinoX][destinoY] = 'R';         
                    robot.setX(destinoX);
                    robot.setY(destinoY);

                    imprimirLog(robotId, "EMPUJAR_OBJETIVO (" + destinoX + "," + destinoY + ") -> (" + destCajaX + "," + destCajaY + ") | Batería: " + (robot.getBateria() - 1));
                
                    notifyAll();
                    return true;
                }
            }
        }
        // Movimiento inválido 
        return false;

    }   
}
