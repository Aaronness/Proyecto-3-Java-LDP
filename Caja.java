public class Caja {
    private int x;
    private int y;
    private boolean esObjetivo;
    private boolean tieneRobot;

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean getObjetivo() { return esObjetivo; }

    public void setX(int x){this.x= x;}
    public void setY(int y){this.y= y;}
    public void setTieneRobot(boolean tieneRobot){this.tieneRobot= tieneRobot;}

    Caja(boolean esObjetivo){
        this.x = -1;
        this.y = -1;
        this.esObjetivo = esObjetivo;
        this.tieneRobot = false;
    }

    public void moverA(int nuevoX, int nuevoY) { setX(nuevoX); setY(nuevoY); }

    public boolean chocaCon(Caja otraCaja) {
        if (otraCaja == null) {
            return false;
        }
        return this.x == otraCaja.x && this.y == otraCaja.y;
    }

    public char obtenerSimbolo() {
        return this.esObjetivo ? 'O' : 'X';
    }

}
