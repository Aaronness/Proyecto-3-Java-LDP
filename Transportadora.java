import java.util.Random;

public class Transportadora implements Runnable {
    private String identificador;
    private Tablero tablero;

    public Transportadora(String identificador, Tablero tablero) {
        this.identificador = "Productor-" + identificador;
        this.tablero = tablero;
    }

    public void run(){
        Random randomizador = new Random();
        try {
            while(tablero.estaActiva()){
                int probabilidad = randomizador.nextInt(2);
                Caja cajaNueva;

                if(probabilidad == 0){
                    cajaNueva = new Caja(true);
                } else {
                    cajaNueva = new Caja(false);
                }
                tablero.insertarCaja(cajaNueva, identificador);
                Thread.sleep(1000);
            }
        }catch (InterruptedException e) {
            // Es buena práctica restaurar el estado de interrupción del hilo
            Thread.currentThread().interrupt();
        }
        
    }
}