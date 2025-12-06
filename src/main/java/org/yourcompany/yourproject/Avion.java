package org.yourcompany.yourproject;

public class Avion implements Runnable {

    public enum Tipo {
        ATERRIZAJE,
        DESPEGUE
    }

    private int id;
    private Tipo tipo;
    private TorreDeControl torreDeControl;

    public Avion(int id, Tipo tipo, TorreDeControl torreDeControl) {
        this.id = id;
        this.tipo = tipo;
        this.torreDeControl = torreDeControl;
    }

    @Override
    public void run() {
        try {
            if (tipo == Tipo.ATERRIZAJE) {
                torreDeControl.solicitarAterrizaje(id);
            } else {
                torreDeControl.solicitarDespegue(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("El avión " + id + " fue interrumpido.");
        }
    }
}
