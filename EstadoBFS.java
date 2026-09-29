import java.util.Arrays;

public class EstadoBFS {
    public int robotX;
    public int robotY;
    public char[][] matrizEstado;
    public int[] primerMovimiento;
    public Caja cajaEnRuta;

    public EstadoBFS(int robotX, int robotY, char[][] matrizEstado, int[] primerMovimiento, Caja cajaEnRuta) {
        this.robotX = robotX;
        this.robotY = robotY;
        this.matrizEstado = matrizEstado;
        this.primerMovimiento = primerMovimiento;
        this.cajaEnRuta = cajaEnRuta;
    }

    public char[][] clonarTablero() {
        char[][] copia = new char[6][];
        for (int i = 0; i < 6; i++) {
            copia[i] = this.matrizEstado[i].clone();
        }
        return copia;
    }

    public String generarHash() {
        return robotX + "," + robotY + "-" + Arrays.deepToString(matrizEstado);
    }
}