package org.yourcompany.yourproject;

/**
 * Representa un hilo individual que simula el comportamiento de un avión dentro del sistema.
 * <p>
 * Cada instancia de esta clase funciona como un hilo (implementa {@link Runnable}) que intenta
 * acceder a las pistas compartidas a través de la {@link TorreDeControl}.
 * El avión puede tener dos intenciones: ATERRIZAJE o DESPEGUE.
 * </p>
 *
 * @author Guillermo Martin, Olga Marco y Bruno Coloma.
 * @see TorreDeControl
 */
public class Avion implements Runnable {

    /**
     * Enumerado que define el tipo de maniobra que realizará el avión.
     */
    public enum Tipo {
        /** Indica que el avión desea aterrizar (tiene prioridad). */
        ATERRIZAJE,
        /** Indica que el avión desea despegar. */
        DESPEGUE
    }

    /** Identificador único del avión. */
    private int id;
    
    /** Tipo de operación que va a realizar el avión (Aterrizaje o Despegue). */
    private Tipo tipo;
    
    /** Referencia al monitor (Torre de Control) para gestionar el acceso a las pistas. */
    private TorreDeControl torreDeControl;

    /**
     * Constructor que inicializa el objeto Avión con sus propiedades y la referencia a la torre.
     *
     * @param id             Identificador único numérico para el avión.
     * @param tipo           El tipo de operación a realizar ({@code Avion.Tipo.ATERRIZAJE} o {@code Avion.Tipo.DESPEGUE}).
     * @param torreDeControl Instancia compartida de la Torre de Control que gestiona los recursos.
     */
    public Avion(int id, Tipo tipo, TorreDeControl torreDeControl) { 
        this.id = id;
        this.tipo = tipo;
        this.torreDeControl = torreDeControl;
    }

    /**
     * Método principal de ejecución del hilo.
     * <p>
     * Dependiendo del {@code tipo} de avión, solicitará permiso a la {@code torreDeControl}
     * para aterrizar o despegar. Si el hilo es interrumpido durante la espera, se captura
     * la excepción y se finaliza la ejecución de forma segura.
     * </p>
     */
    @Override
    public void run() {
        try {
            if (tipo == Tipo.ATERRIZAJE) {
                // Solicita acceso prioritario para aterrizar
                torreDeControl.solicitarAterrizaje(id);
            } else {
                // Solicita acceso para despegar (puede requerir espera si hay aterrizajes pendientes)
                torreDeControl.solicitarDespegue(id);
            }
        } catch (InterruptedException e) {
            // Restablecer el estado de interrupción y notificar el error
            Thread.currentThread().interrupt();
            System.err.println("El avión " + id + " fue interrumpido durante su operación.");
        }
    }
}
