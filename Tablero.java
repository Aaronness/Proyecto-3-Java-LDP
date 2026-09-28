import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tablero {
    private char[][] matriz = new char[6][6];
    private int casillasOcupadas;
    private int cajasObjetivoDisponibles;
    private boolean simulacionActiva = true;
    private int tickActual = 1;
    private int cajasExtraidas = 0;
    private int saturacionesTablero = 0;
    private int robotsAgotados = 0;
    private List<Caja> listaCajasActivas = new ArrayList<>();
    private int numProductores;

    public List<Caja> getListaCajas() { return listaCajasActivas; }
    
    public boolean noEstaAtascado()
    { return (matriz[4][5] == 'O' && matriz[3][5] == 'R') || (matriz[5][4] == 'O' && matriz[5][3] == 'R'); }
    
    public synchronized Caja buscarCaja(int x, int y){ //Pasale la ubicacion de la caja, y te retorna el objeto de la lista
        for (Caja c : listaCajasActivas){
            if (c.getX() == x && c.getY() == y) return c;
        }
        return null;
    }

    public void actualizarCoordenadas(Caja caja, int nuevoX, int nuevoY) {
        caja.setX(nuevoX);
        caja.setY(nuevoY);
    }

    public Tablero(Robot robots[], int cant_robots, int cant_cajas, Caja cajas[], int numProductores){
        this.casillasOcupadas = cant_robots + cant_cajas;
        this.numProductores = numProductores;

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
                cajasObjetivoDisponibles++;
            }
            else{
                matriz[posicion[0]][posicion[1]] = 'X';
            }
            
            cajas[i].setX(posicion[0]);
            cajas[i].setY(posicion[1]);
            listaCajasActivas.add(cajas[i]);
        }

    }

    public synchronized char[][] obtenerCopiaMatriz() {
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

    public void imprimirReporteFinal() {
        System.out.println("\n=== REPORTE FINAL DEL ALMACÉN ===");
        System.out.println("- Cajas Objetivo extraídas (llevadas a 5,5): " + cajasExtraidas);
        System.out.println("- Saturaciones del tablero (intentos fallidos de productores): " + saturacionesTablero);
        System.out.println("- Robots que finalizaron por batería agotada: " + robotsAgotados);
        
        System.out.println("\n=== ESTADO FINAL DEL TABLERO ===");
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                // Se imprime cada casilla separada por un espacio para que se vea como una cuadrícula
                System.out.print(matriz[i][j] + " ");
            }
            System.out.println();
        }
    }

    public synchronized boolean estaActiva() { return simulacionActiva; }

    public synchronized void registrarRobotAgotado(Robot robot) {
        robotsAgotados++;
        matriz[robot.getX()][robot.getY()] = '.'; // El robot es retirado de la matriz

        casillasOcupadas--;
        
        if(robot.getCaja() != null) {
            Caja objetivo = robot.getCaja();
            objetivo.setTieneRobot(false);
            robot.setCaja(null);
        }  
        robot.setX(-1);
        robot.setY(-1);


        imprimirLog("Robot-" + robot.getIdentificador(), "BATERIA_AGOTADA -> Retiro del tablero");
        notifyAll(); 
    }

    public synchronized void registrarRobotRendido(Robot robot) { // Esta funcion registra un robot rendido quitandole la caja asignada y lanzando el mensaje

        if(robot.getCaja()!= null) {
            Caja objetivo = robot.getCaja();
            objetivo.setTieneRobot(false);
            robot.setCaja(null);
        }

        imprimirLog("Robot-" + robot.getIdentificador(), "ATRAPADO -> Se rinde por interbloqueo y se apaga.");
        notifyAll(); //Notifica que el robot solto una caja objetivo a los demas hilos principales(Robots)
    }

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
        
        // REGLA DE ABORTO: Si la fábrica ya fue apagada por otro hilo, no hacemos nada.
        if (!simulacionActiva) return;

        // NUEVA REGLA: Si hay 36 cosas, o si hay 35 y la única libre es la meta (5,5), el productor espera.
        while (casillasOcupadas >= 36 || (casillasOcupadas == 35 && matriz[5][5] == '.')) {
            
            // Registrar la saturación antes de entrar en espera
            if (!noEstaAtascado()) {
                imprimirLog("MONITOR", "DEADLOCK DETECTADO -> Tablero lleno sin extracción posible. Abortando fábrica.");
                apagarFabrica(); 
                return; // Rompe el método para que este hilo Productor muera
            }

            //  Registrar la saturación antes de entrar en espera
            imprimirLog(idProductor, "ESPERA_SATURACION (Intento de insertar en tablero lleno pero con salvación posible)");
            saturacionesTablero++;
            wait(); 
    
            // Si el hilo despierta porque apagaron la fábrica desde otro lado, se retira
            if (!simulacionActiva) return;
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

        listaCajasActivas.add(nuevaCaja);
        // Imprimir la traza de ejecución obligatoria
        String tipoCaja = nuevaCaja.getObjetivo() ? "OBJETIVO" : "BLOQUEO";
        imprimirLog(idProductor, "INSERTAR_" + tipoCaja + " -> (" + fila + "," + col + ")");

        // Notificar a los robots (consumidores) que hay una nueva caja
        notifyAll();
    }

    public synchronized void esperarPorCajasObjetivo() throws InterruptedException {
        if (cajasObjetivoDisponibles == 0 && this.numProductores == 0) {
            apagarFabrica();
            return;
        }
        
        while (cajasObjetivoDisponibles == 0 && simulacionActiva) {
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
            Caja caja = buscarCaja(destinoX, destinoY);
            int destCajaX = destinoX + deltaX;
            int destCajaY = destinoY + deltaY;

            if (destCajaX == 5 && destCajaY == 5) {
                matriz[origenX][origenY] = '.';
                matriz[destinoX][destinoY] = 'R'; 
                robot.setX(destinoX);
                robot.setY(destinoY);
            
                listaCajasActivas.remove(caja); // Borramos la caja de la lista
                robot.setCaja(null);
                casillasOcupadas--;
                cajasObjetivoDisponibles--;
                cajasExtraidas++;
            
                imprimirLog(robotId, "EXTRACCION_EXITOSA -> (5,5)");

                if (cajasObjetivoDisponibles == 0 && this.numProductores == 0) {
                    apagarFabrica(); 
                }
                
                notifyAll(); 
                return true;
            }

            if (esPosicionValida(destCajaX, destCajaY)) {         
                if (matriz[destCajaX][destCajaY] == '.') {
                    matriz[destCajaX][destCajaY] = 'O'; 
                
                    actualizarCoordenadas(caja, destCajaX, destCajaY);
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

        if (matriz[destinoX][destinoY] == 'X') {
            Caja caja = buscarCaja(destinoX, destinoY);
            int destCajaX = destinoX + deltaX;
            int destCajaY = destinoY + deltaY;

            if (destCajaX == 5 && destCajaY == 5) return false;

            if (esPosicionValida(destCajaX, destCajaY)) {         
                if (matriz[destCajaX][destCajaY] == '.') {
                    matriz[destCajaX][destCajaY] = 'X'; 
                
                    actualizarCoordenadas(caja, destCajaX, destCajaY);
                    matriz[origenX][origenY] = '.';        
                    matriz[destinoX][destinoY] = 'R';         
                    robot.setX(destinoX);
                    robot.setY(destinoY);

                    imprimirLog(robotId, "EMPUJAR_BLOQUEO (" + destinoX + "," + destinoY + ") -> (" + destCajaX + "," + destCajaY + ") | Batería: " + (robot.getBateria() - 1));
                
                    notifyAll();
                    return true;
                }
            }

        }
        // Movimiento inválido 
        return false;

    }   
}
