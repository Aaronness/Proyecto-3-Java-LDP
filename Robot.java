import java.util.LinkedList;
import java.util.Queue;

public class Robot implements Runnable {
    private int identificador;
    private int bateria;
    private int x;
    private int y;
    private Tablero tablero;
    private Caja objetivo;

    // Atributo para guardar el próximo movimiento calculado por el BFS
    private int[] proximoMovimiento;

    public void setX(int x){this.x= x;}
    public void setY(int y){this.y= y;}
    public void setTablero(Tablero tablero) {this.tablero = tablero;}

    public int getX() { return x; }
    public int getY() { return y; }
    public int getIdentificador() { return identificador;}
    public int getBateria() { return bateria;}

    
    public Robot(int identificador, int bateria, int x, int y, Tablero tablero) { // Constructorr del Robot
        this.identificador = identificador;
        this.bateria = bateria;
        this.x = x;
        this.y = y;
        this.tablero = tablero;
    }

public void calcularProximoMovimiento(){
        char[][] estadoInicial = tablero.obtenerCopiaMatriz(); 
        
        // 1. Usamos la estructura permitida para la Cola
        Queue<EstadoBFS> cola = new LinkedList<>();

        // 2. Mantenemos el arreglo tradicional para los visitados
        String[] visitados = new String[50000];
        int cantidadVisitados = 0;

        int[][] direcciones = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        EstadoBFS nodoInicial = new EstadoBFS(this.x, this.y, estadoInicial, null);
        
        // Agregar a la cola y marcar como visitado
        cola.add(nodoInicial);
        visitados[cantidadVisitados] = nodoInicial.generarHash();
        cantidadVisitados++;

        while (!cola.isEmpty()) {
            
            // Extraer el primer elemento de la cola dinámica
            EstadoBFS actual = cola.poll();

            // Condición de victoria
            if (actual.matrizEstado[5][5] == 'O') {
                this.proximoMovimiento = actual.primerMovimiento;
                return;
            }

            for (int i = 0; i < direcciones.length; i++) {
                int dx = direcciones[i][0];
                int dy = direcciones[i][1];
                
                int nx = actual.robotX + dx;
                int ny = actual.robotY + dy;

                if (nx < 0 || nx > 5 || ny < 0 || ny > 5) continue;

                char celdaDestino = actual.matrizEstado[nx][ny];

                if (celdaDestino == 'R') continue;

                char[][] nuevoTablero = actual.clonarTablero();
                boolean movimientoValido = false;

                if (celdaDestino == '.') {
                    nuevoTablero[actual.robotX][actual.robotY] = '.';
                    nuevoTablero[nx][ny] = 'R';
                    movimientoValido = true;
                } 
                else if (celdaDestino == 'O' || celdaDestino == 'X') {
                    int destCajaX = nx + dx;
                    int destCajaY = ny + dy;

                    if (destCajaX >= 0 && destCajaX <= 5 && destCajaY >= 0 && destCajaY <= 5) {
                        if (nuevoTablero[destCajaX][destCajaY] == '.') {
                            nuevoTablero[destCajaX][destCajaY] = celdaDestino;
                            nuevoTablero[nx][ny] = 'R';                        
                            nuevoTablero[actual.robotX][actual.robotY] = '.'; 
                            movimientoValido = true;
                        } 
                        else if (destCajaX == 5 && destCajaY == 5 && celdaDestino == 'O') {
                            nuevoTablero[destCajaX][destCajaY] = celdaDestino;
                            nuevoTablero[nx][ny] = 'R';
                            nuevoTablero[actual.robotX][actual.robotY] = '.'; 
                            movimientoValido = true;
                        }
                    }
                }

                if (movimientoValido) {
                    int[] primerMov = (actual.primerMovimiento == null) ? direcciones[i] : actual.primerMovimiento;
                    
                    EstadoBFS nuevoEstado = new EstadoBFS(nx, ny, nuevoTablero, primerMov);
                    String hashNuevo = nuevoEstado.generarHash();
                    
                    // Mantenemos la búsqueda lineal tradicional para los estados visitados
                    boolean yaVisitado = false;
                    for (int v = 0; v < cantidadVisitados; v++) {
                        if (visitados[v].equals(hashNuevo)) { 
                            yaVisitado = true;
                            break;
                        }
                    }

                    if (!yaVisitado) {
                        // Evitar desbordamiento en el arreglo estático de visitados
                        if (cantidadVisitados < visitados.length) {
                            visitados[cantidadVisitados] = hashNuevo;
                            cantidadVisitados++;
                        }
                        
                        // Añadir a la cola de Java
                        cola.add(nuevoEstado);
                    }
                }
            }
        }
        
        this.proximoMovimiento = null;
    }
    @Override
    public void run() {
        try {
            while (bateria > 0) {
                tablero.esperarPorCajasObjetivo();
                calcularProximoMovimiento();

                if (this.proximoMovimiento != null) {
                    int deltaX = this.proximoMovimiento[0];
                    int deltaY = this.proximoMovimiento[1];

                    // Pasamos 'this' (el robot actual) y los deltas calculados
                    boolean movimientoExitoso = tablero.intentarMovimiento(this, deltaX, deltaY);

                    if (movimientoExitoso){
                        bateria--;
                    }
                }
                Thread.sleep(300);

            }

            System.out.println("[" + identificador + "] BATERIA AGOTADA -> Retiro del tablero");

        } catch (InterruptedException e) {
            System.out.println("Hilo del robot interrumpido.");
        }   
    }
}