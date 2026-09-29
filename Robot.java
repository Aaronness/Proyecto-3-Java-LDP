import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet; 
import java.util.Set;     

public class Robot implements Runnable {
    private int identificador;
    private int bateria;
    private int x;
    private int y;
    private Tablero tablero;
    private Caja objetivo;

    private int[] proximoMovimiento;

    public void setX(int x){this.x= x;}
    public void setY(int y){this.y= y;}
    public void setTablero(Tablero tablero) {this.tablero = tablero;}
    public void setCaja(Caja objetivo) { this.objetivo = objetivo;}

    public int getX() { return x; }
    public int getY() { return y; }
    public int getIdentificador() { return identificador;}
    public int getBateria() { return bateria;}
    public Caja getCaja() { return objetivo;}

    
    public Robot(int identificador, int bateria, int x, int y, Tablero tablero) { // Constructorr del Robot
        this.identificador = identificador;
        this.bateria = bateria;
        this.x = x;
        this.y = y;
        this.tablero = tablero;
    }

public void calcularProximoMovimiento() {
        char[][] estadoInicial = tablero.obtenerCopiaMatriz(); 
        Queue<EstadoBFS> cola = new LinkedList<>();
        
        Set<String> visitados = new HashSet<>(); 
        int[][] direcciones = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        EstadoBFS nodoInicial = new EstadoBFS(this.x, this.y, estadoInicial, null, null);
        cola.add(nodoInicial);
        visitados.add(nodoInicial.generarHash());

        while (!cola.isEmpty()) {
            EstadoBFS actual = cola.poll();

            // condicion victoria 
            if (actual.matrizEstado[5][5] == 'O') {
                this.proximoMovimiento = actual.primerMovimiento;
                
                // Si la ruta nos llevó al éxito y veníamos empujando una caja, la reclamamos
                if (actual.cajaEnRuta != null) {
                    actual.cajaEnRuta.setTieneRobot(true); // Bloqueamos la caja para otros
                    this.objetivo = actual.cajaEnRuta;    
                }
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

                Caja cajaFutura = actual.cajaEnRuta; 

                if (celdaDestino == '.') {
                    nuevoTablero[actual.robotX][actual.robotY] = '.';
                    nuevoTablero[nx][ny] = 'R';
                    movimientoValido = true;
                } 
                else if (celdaDestino == 'O' || celdaDestino == 'X') {
                    
                    if (celdaDestino == 'O' && cajaFutura == null) {
                        cajaFutura = tablero.buscarCaja(nx, ny);
                        
                        if (cajaFutura != null) {
                            // Si la caja ya tiene dueño y NO soy yo, esta ruta es invalida
                            if (cajaFutura.getTieneRobot() && cajaFutura != this.objetivo) {
                                continue; // Se ignora este movimiento y se busca otra ruta
                            }
                        }
                    }

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
                    
                    // Guardamos la caja que estamos manipulando en el nuevo estado simulado
                    EstadoBFS nuevoEstado = new EstadoBFS(nx, ny, nuevoTablero, primerMov, cajaFutura);
                    String hashNuevo = nuevoEstado.generarHash();
                    
                    if (!visitados.contains(hashNuevo)) {
                        if (visitados.size() < 50000) { //limite
                            visitados.add(hashNuevo);
                            cola.add(nuevoEstado);
                        }
                    }
                }
            }
        }

        if (this.objetivo != null) {
            this.objetivo.setTieneRobot(false);
            this.objetivo = null; 
        }
        
        this.proximoMovimiento = null;
    }
    @Override
    public void run() {
        try {
            int intentosFallidos = 0; 
            while (bateria > 0 && tablero.estaActiva()) {

                if(objetivo == null) tablero.esperarPorCajasObjetivo();
                calcularProximoMovimiento();

                if (!tablero.estaActiva()) {
                    break;
                }

                if (this.proximoMovimiento != null) {
                    int deltaX = this.proximoMovimiento[0];
                    int deltaY = this.proximoMovimiento[1];

                    boolean movimientoExitoso = tablero.intentarMovimiento(this, deltaX, deltaY);

                    if (movimientoExitoso){
                        bateria--;
                        intentosFallidos = 0;
                    } else {
                        intentosFallidos++;
                    }

                } else {
                    intentosFallidos++; 
                }
                if (intentosFallidos >= 25) { // Esto hace que si ya no puede encontrar una ruta valida y no tenemos transportadora termine el juego
                    tablero.registrarRobotRendido(this);
                    bateria = -1; 
                    break; // Rompemos el bucle para que el robot muera
                }
                
                Thread.sleep(300); // Simula tiempo de procesamiento 

            }
            
            if(bateria == 0) tablero.registrarRobotAgotado(this);

        } catch (InterruptedException e) {
            System.out.println("Hilo del robot interrumpido.");
        }   
    }
}
