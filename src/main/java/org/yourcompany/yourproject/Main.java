package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clase principal que inicializa y ejecuta la simulación del aeropuerto.
 * <p>
 * Simula un escenario con recursos compartidos (pistas) y multihilo (aviones),
 * gestionando prioridades entre aterrizajes y despegues.
 * </p>
 *
 * @author Guillermo Martin
 * @author Olga Marco
 * @author Bruno Coloma
 * @version 1.0
 */
public class Main {

    /**
     * Método de entrada principal de la aplicación.
     * <p>
     * Configura la torre de control, crea los hilos de los aviones (tanto de aterrizaje
     * como de despegue), los inicia y espera a su finalización.
     * </p>
     *
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        // Total de aviones en la simulación
        final int TOTAL_AVIONES_ATERRIZAJE = 5;
        final int TOTAL_AVIONES_DESPEGUE = 10;
        final int TOTAL_AVIONES = TOTAL_AVIONES_ATERRIZAJE + TOTAL_AVIONES_DESPEGUE;

        // Instancia del monitor (Torre de Control)
        TorreDeControl torreDeControl = new TorreDeControl(TOTAL_AVIONES_ATERRIZAJE, TOTAL_AVIONES);

        List<Thread> aviones = new ArrayList<>();
        int avionIdContador = 1;

        // Crear 5 aviones para aterrizar
        for (int i = 0; i < TOTAL_AVIONES_ATERRIZAJE; i++) {
            Thread avionThread = new Thread(new Avion(avionIdContador++, Avion.Tipo.ATERRIZAJE, torreDeControl));
            aviones.add(avionThread);
        }

        // Crear 10 aviones para despegar
        for (int i = 0; i < TOTAL_AVIONES_DESPEGUE; i++) {
            Thread avionThread = new Thread(new Avion(avionIdContador++, Avion.Tipo.DESPEGUE, torreDeControl));
            aviones.add(avionThread);
        }

        // Iniciar todos los hilos
        System.out.println("--- INICIANDO SIMULACIÓN DE AEROPUERTO ---");
        for (Thread t : aviones) {
            t.start();
        }

        // Esperar a que todos los hilos terminen (join)
        for (Thread t : aviones) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Hilo principal interrumpido.");
            }
        }

        System.out.println("--- FIN DE LA SIMULACIÓN: Ya no quedan aviones por aterrizar o despegar ---");
    }
}

/**
 * Representa un Avión que actúa como un hilo independiente.
 * Cada avión tiene una tarea específica: ATERRIZAR o DESPEGAR.
 */
class Avion implements Runnable {
    /**
     * Enumerado para definir el tipo de operación del avión.
     */
    public enum Tipo {
        ATERRIZAJE, DESPEGUE
    }

    private final int id;
    private final Tipo tipo;
    private final TorreDeControl torre;

    /**
     * Constructor del Avión.
     *
     * @param id Identificador único del avión.
     * @param tipo Tipo de operación (ATERRIZAJE o DESPEGUE).
     * @param torre Referencia a la torre de control compartida.
     */
    public Avion(int id, Tipo tipo, TorreDeControl torre) {
        this.id = id;
        this.tipo = tipo;
        this.torre = torre;
    }

    /**
     * Lógica de ejecución del hilo.
     * Intenta realizar la operación asignada comunicándose con la Torre de Control.
     */
    @Override
    public void run() {
        try {
            if (tipo == Tipo.ATERRIZAJE) {
                torre.solicitarAterrizaje(id);
            } else {
                torre.solicitarDespegue(id);
            }
        } catch (InterruptedException e) {
            System.err.println("Avión " + id + " fue interrumpido durante su operación.");
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Monitor que gestiona el acceso a las pistas de aterrizaje y despegue.
 * <p>
 * Implementa la sincronización necesaria para manejar 2 pistas y priorizar
 * los aterrizajes sobre los despegues. Utiliza un {@link Semaphore} para limitar
 * el acceso a las pistas y {@code wait()/notifyAll()} para la gestión de prioridades.
 * </p>
 */
class TorreDeControl {
    
    /** Número total de aterrizajes que se deben completar antes de permitir despegues masivos. */
    private final int TOTAL_ATERRIZAJES_ESPERADOS;
    
    /** Número total de aviones en la simulación para control de estado. */
    private final int TOTAL_AVIONES_SIMULACION;

    /** Contador atómico de aterrizajes completados exitosamente. */
    private final AtomicInteger aterrizajesCompletados = new AtomicInteger(0);
    
    /** Contador atómico de aviones que han finalizado su operación. */
    private final AtomicInteger avionesProcesados = new AtomicInteger(0);

    /** Semáforo que controla el acceso a las 2 pistas disponibles. */
    private final Semaphore pistasSemaforo = new Semaphore(2);

    /** Lista thread-safe que contiene los nombres de las pistas disponibles. */
    private final List<String> nombresPistas = Collections.synchronizedList(new ArrayList<>(List.of("PISTA 1", "PISTA 2")));

    /** Mapa concurrente para rastrear qué avión está en qué pista (para visualización). */
    private final Map<String, Integer> estadoPistas = new ConcurrentHashMap<>();

    /** Objeto monitor utilizado para sincronizar la impresión por consola y la lógica de espera de prioridad. */
    private final Object printLock = new Object();

    /**
     * Constructor de la Torre de Control.
     *
     * @param totalAterrizajes Número de aviones que van a aterrizar.
     * @param totalAviones Número total de aviones (aterrizaje + despegue).
     */
    public TorreDeControl(int totalAterrizajes, int totalAviones) {
        this.TOTAL_ATERRIZAJES_ESPERADOS = totalAterrizajes;
        this.TOTAL_AVIONES_SIMULACION = totalAviones;
    }

    /**
     * Gestiona la solicitud de un avión para aterrizar.
     * <p>
     * El aterrizaje tiene prioridad. El método adquiere una pista, simula el tiempo
     * de aterrizaje y luego libera la pista. Si es el último aterrizaje esperado,
     * notifica a los hilos de despegue que están esperando.
     * </p>
     *
     * @param idAvion Identificador del avión que solicita aterrizar.
     * @throws InterruptedException Si el hilo es interrumpido mientras espera el semáforo o durante el sleep.
     */
    public void solicitarAterrizaje(int idAvion) throws InterruptedException {
        pistasSemaforo.acquire(); // Pedir permiso de pista
        String pistaAsignada = "";
        try {
            pistaAsignada = nombresPistas.remove(0); // Obtener nombre de pista disponible

            synchronized (printLock) {
                estadoPistas.put(pistaAsignada, idAvion);
                System.out.println("AVION " + idAvion + " aterrizando en " + pistaAsignada);
                // Imprimir estado si las pistas están llenas o es el último avión
                if (estadoPistas.size() == 2 || (avionesProcesados.get() == TOTAL_AVIONES_SIMULACION - 1 && estadoPistas.size() == 1)) {
                    imprimirEstadoPistas();
                }
            }
            
            // Simulación del tiempo de operación
            Thread.sleep(5000); 

        } finally {
            synchronized (printLock) {
                if (!pistaAsignada.isEmpty()) {
                    estadoPistas.remove(pistaAsignada); // Liberar del mapa
                    nombresPistas.add(pistaAsignada);   // Devolver a la lista de disponibles
                }
                
                avionesProcesados.incrementAndGet();
                
                // Lógica de Prioridad: Si se han completado todos los aterrizajes, despertar a los despegues
                if (aterrizajesCompletados.incrementAndGet() == TOTAL_ATERRIZAJES_ESPERADOS) {
                    printLock.notifyAll(); 
                }
                System.out.println("AVION " + idAvion + " aterrizado.");
            }
            pistasSemaforo.release(); // Liberar permiso del semáforo
        }
    }

    /**
     * Gestiona la solicitud de un avión para despegar.
     * <p>
     * <b>Restricción de Prioridad:</b> Los aviones que desean despegar deben esperar
     * (wait) hasta que todos los aterrizajes previstos se hayan completado.
     * Una vez despiertos, compiten por las pistas disponibles.
     * </p>
     *
     * @param idAvion Identificador del avión que solicita despegar.
     * @throws InterruptedException Si el hilo es interrumpido durante la espera (wait) o el sleep.
     */
    public void solicitarDespegue(int idAvion) throws InterruptedException {
        // Bloqueo de prioridad: Esperar si aún quedan aterrizajes pendientes
        synchronized (printLock) {
            while (aterrizajesCompletados.get() < TOTAL_ATERRIZAJES_ESPERADOS) {
                // System.out.println("AVION " + idAvion + " esperando para despegar (prioridad aterrizaje)...");
                printLock.wait(); // El hilo se duerme aquí hasta que notifyAll sea llamado en solicitarAterrizaje
            }
        }

        pistasSemaforo.acquire(); // Pedir permiso de pista
        String pistaAsignada = "";
        try {
            pistaAsignada = nombresPistas.remove(0);

            synchronized(printLock) {
                estadoPistas.put(pistaAsignada, idAvion);
                System.out.println("AVION " + idAvion + " despegando en " + pistaAsignada);
                if (estadoPistas.size() == 2 || (avionesProcesados.get() == TOTAL_AVIONES_SIMULACION - 1 && estadoPistas.size() == 1)) {
                    imprimirEstadoPistas();
                }
            }
            
            // Simulación del tiempo de operación
            Thread.sleep(5000);
            
        } finally {
            synchronized(printLock) {
                if (!pistaAsignada.isEmpty()) {
                    estadoPistas.remove(pistaAsignada);
                    nombresPistas.add(pistaAsignada);
                }
                avionesProcesados.incrementAndGet();
                System.out.println("AVION " + idAvion + " ha despegado (volado).");
            }
            pistasSemaforo.release(); // Liberar permiso
        }
    }

    /**
     * Método auxiliar privado para imprimir el estado actual de las pistas.
     * Muestra qué aviones están ocupando qué pistas en un momento dado.
     */
    private void imprimirEstadoPistas() {
        int ocupadas = estadoPistas.size();
        String avionesEnPistaStr = "ninguno";
        if (ocupadas > 0) {
            List<String> descripciones = new ArrayList<>();
            List<String> pistasOrdenadas = new ArrayList<>(estadoPistas.keySet());
            Collections.sort(pistasOrdenadas); // Ordenar alfabéticamente para salida consistente
            for(String pista : pistasOrdenadas) {
                descripciones.add("AVION " + estadoPistas.get(pista) + " en " + pista);
            }
            avionesEnPistaStr = String.join(" y ", descripciones);
        }
        System.out.println("--> Pistas ocupadas: " + ocupadas + " por " + avionesEnPistaStr);
        System.out.println("---------------------------------------------------------------------");
    }
}
