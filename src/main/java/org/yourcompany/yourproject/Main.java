package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Guillermo Martin, Olga Marco y Bruno Coloma.
 */

/*
Simular un aeropuerto con 2 pistas de aterrizaje/despegue (son un recurso
compartido). Hay 10 hilos "Avión" que quieren despegar y 5 hilos "Avión" que
quieren aterrizar.
1. Un avión (hilo) necesita adquirir una pista de forma exclusiva para su
operación (aterrizar o despegar).
2. Ambas operaciones (aterrizar y despegar) tardan un tiempo simulado (ej. 5
segundos).
3. Prioridad: Aterrizar siempre tiene prioridad sobre despegar. Un avión que
quiere despegar no debe hacerlo si hay un avión (aunque haya llegado
después) esperando para aterrizar.
*/

/* 
* 
* clase despegue extends Runnable
* clase aterrizaje extends Runnable
* clase torre de control (semaforo con synchronized)  -> usar semaforo de torre de control y asignar 2 con prioridad en aterrizaje
*/


public class Main {
    public static void main(String[] args) {
        // Total de aviones en la simulación
        final int TOTAL_AVIONES_ATERRIZAJE = 5;
        final int TOTAL_AVIONES_DESPEGUE = 10;
        final int TOTAL_AVIONES = TOTAL_AVIONES_ATERRIZAJE + TOTAL_AVIONES_DESPEGUE;

        TorreDeControl torreDeControl = new TorreDeControl(TOTAL_AVIONES_ATERRIZAJE, TOTAL_AVIONES);
        //TODO
        List<Thread> aviones = new ArrayList<>();
        //Lista para almacenar los hilos de aviones
        int avionIdContador = 1;
        //Contador para ir asignando IDs a los aviones

        
        for (int i = 0; i < TOTAL_AVIONES_ATERRIZAJE; i++) {// Crear 5 aviones para aterrizar
            Thread avionThread = new Thread(new Avion(avionIdContador++, Avion.Tipo.ATERRIZAJE, torreDeControl));
            aviones.add(avionThread);}

        
        for (int i = 0; i < TOTAL_AVIONES_DESPEGUE; i++) {// Crear 10 aviones para despegar
            Thread avionThread = new Thread(new Avion(avionIdContador++, Avion.Tipo.DESPEGUE, torreDeControl));
            aviones.add(avionThread);}

        
        for (Thread t : aviones) {// Iniciar todos los hilos
            t.start();}

        // Esperar a que todos los hilos terminen
        for (Thread t : aviones) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
        
        System.out.println("Ya no quedan aviones por aterrizar o despegar");
    }
}

class TorreDeControl {
    private final int TOTAL_ATERRZAJES_ESPERADOS;
    private final int TOTAL_AVIONES_SIMULACION;
    private final AtomicInteger aterrizajesCompletados = new AtomicInteger(0);
    private final AtomicInteger avionesProcesados = new AtomicInteger(0);
    //Uso atomic integer en vez de variables int normales 
    // para evitar problemas de concurrencia al incrementar 
    // los contadores desde diferentes hilos.

    private final Semaphore pistasSemaforo = new Semaphore(2); //semaforo para pistas
    private final List<String> nombresPistas = Collections.synchronizedList(new ArrayList<>(List.of("PISTA 1", "PISTA 2")));
    
    private final Map<String, Integer> estadoPistas = new ConcurrentHashMap<>(); //version de hasmap en miltihilo
    private final Object printLock = new Object();
    //Asegura que las operaciones de impresión en consola y las actualizaciones de estadoPistas
    //  sean atómicas y no se mezclen los mensajes de salida de diferentes hilos.

    public TorreDeControl(int totalAterrizajes, int totalAviones) { //constructor
        this.TOTAL_ATERRZAJES_ESPERADOS = totalAterrizajes;
        this.TOTAL_AVIONES_SIMULACION = totalAviones;
    }

    public void solicitarAterrizaje(int idAvion) throws InterruptedException {
        pistasSemaforo.acquire(); // Pedir permiso
        try {
            String pistaAsignada = nombresPistas.remove(0); // Pillar nombre
            try {
                synchronized (printLock) {
                    estadoPistas.put(pistaAsignada, idAvion); //registra el idAvion (avion concreto) esta en la pista
                    System.out.println("AVION " + idAvion + " aterrizando en " + pistaAsignada); //lo retorna
                    if (estadoPistas.size() == 2 || (avionesProcesados.get() == TOTAL_AVIONES_SIMULACION - 1 && estadoPistas.size() == 1)) {
                        //si todas estan ocupadas o es el ultimo avion....
                        imprimirEstadoPistas(); //imprime el estado de las pistas
                    }
                }
                Thread.sleep(5000);
            } finally {
                synchronized (printLock) {
                    estadoPistas.remove(pistaAsignada); //Elimina el registro del avión de la pista.
                    nombresPistas.add(pistaAsignada); // Devuelve la pista a la lista de disponibles.
                    avionesProcesados.incrementAndGet();
                    if (aterrizajesCompletados.incrementAndGet() == TOTAL_ATERRZAJES_ESPERADOS) {
                        printLock.notifyAll(); //si el aterrizaje ha sido completado (el hilo se ha consumido)...
                    }
                    System.out.println("AVION " + idAvion + " aterrizado."); //salta el sout de aterrizado
                }
            }
        } finally {
            pistasSemaforo.release(); // Liberar permiso
        }
    }

    public void solicitarDespegue(int idAvion) throws InterruptedException {
        synchronized (printLock) { //el bloque syncronyched asegura que...
            while (aterrizajesCompletados.get() < TOTAL_ATERRZAJES_ESPERADOS) {
                printLock.wait(); //los aviones que quieren despegar esperan a que todos los aterrizajes se completen (los del metodo anterior)
            }
        }

        pistasSemaforo.acquire(); // Pedir permiso
        try {
            String pistaAsignada = nombresPistas.remove(0); // Pillar nombre
            try {
                synchronized(printLock) {
                    estadoPistas.put(pistaAsignada, idAvion);
                    System.out.println("AVION " + idAvion + " despegando en " + pistaAsignada);
                    if (estadoPistas.size() == 2 || (avionesProcesados.get() == TOTAL_AVIONES_SIMULACION - 1 && estadoPistas.size() == 1)) {
                        imprimirEstadoPistas();
                    }
                }
                Thread.sleep(5000);
            } finally {
                synchronized(printLock) {
                    estadoPistas.remove(pistaAsignada);
                    nombresPistas.add(pistaAsignada); // Devolver nombre
                    avionesProcesados.incrementAndGet();
                    System.out.println("AVION " + idAvion + " ha despegado (volado).");
                }
            }
        } finally {
            pistasSemaforo.release(); // Liberar permiso
        }
    }

    private void imprimirEstadoPistas() {
        int ocupadas = estadoPistas.size();
        String avionesEnPistaStr = "ninguno";
        if (ocupadas > 0) {
            List<String> descripciones = new ArrayList<>();
            List<String> pistasOrdenadas = new ArrayList<>(estadoPistas.keySet());
            Collections.sort(pistasOrdenadas);
            for(String pista : pistasOrdenadas) {
                descripciones.add("AVION" + estadoPistas.get(pista) + " en " + pista);
            }
            avionesEnPistaStr = String.join(" y ", descripciones);
        }
        System.out.println("--> Pistas ocupadas: " + ocupadas + " por " + avionesEnPistaStr);
        System.out.println("---------------------------------------------------------------------");
    }
}