package org.yourcompany.yourproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        // Total de aviones en la simulación
        final int TOTAL_AVIONES_ATERRIZAJE = 5;
        final int TOTAL_AVIONES_DESPEGUE = 10;
        final int TOTAL_AVIONES = TOTAL_AVIONES_ATERRIZAJE + TOTAL_AVIONES_DESPEGUE;

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
        for (Thread t : aviones) {
            t.start();
        }

        // Esperar a que todos los hilos terminen
        for (Thread t : aviones) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
        
        System.out.println("Ya no quedan aviones en la simulación.");
    }
}

class TorreDeControl {
    private final int TOTAL_ATERRZAJES_ESPERADOS;
    private final int TOTAL_AVIONES_SIMULACION;
    private final AtomicInteger aterrizajesCompletados = new AtomicInteger(0);
    private final AtomicInteger avionesProcesados = new AtomicInteger(0);

    private final BlockingQueue<String> pistasDisponibles;
    private final Map<String, Integer> estadoPistas = new ConcurrentHashMap<>();
    private final Object printLock = new Object();

    public TorreDeControl(int totalAterrizajes, int totalAviones) {
        this.TOTAL_ATERRZAJES_ESPERADOS = totalAterrizajes;
        this.TOTAL_AVIONES_SIMULACION = totalAviones;
        pistasDisponibles = new ArrayBlockingQueue<>(2);
        pistasDisponibles.add("PISTA 1");
        pistasDisponibles.add("PISTA 2");
    }

    public void solicitarAterrizaje(int idAvion) throws InterruptedException {
        String pistaAsignada = pistasDisponibles.take();
        
        try {
            synchronized (printLock) {
                estadoPistas.put(pistaAsignada, idAvion);
                System.out.println("AVION " + idAvion + " aterrizando en " + pistaAsignada);
                
                // Lógica de impresión condicional
                if (estadoPistas.size() == 2 || (avionesProcesados.get() == TOTAL_AVIONES_SIMULACION - 1 && estadoPistas.size() == 1)) {
                    imprimirEstadoPistas();
                }
            }

            Thread.sleep(5000);

        } finally {
            synchronized (printLock) {
                estadoPistas.remove(pistaAsignada);
                pistasDisponibles.put(pistaAsignada);
                avionesProcesados.incrementAndGet();
                
                if (aterrizajesCompletados.incrementAndGet() == TOTAL_ATERRZAJES_ESPERADOS) {
                    printLock.notifyAll();
                }
                System.out.println("AVION " + idAvion + " aterrizado.");
            }
        }
    }

    public void solicitarDespegue(int idAvion) throws InterruptedException {
        synchronized (printLock) {
            while (aterrizajesCompletados.get() < TOTAL_ATERRZAJES_ESPERADOS) {
                printLock.wait();
            }
        }

        String pistaAsignada = pistasDisponibles.take();
        
        try {
            synchronized(printLock) {
                estadoPistas.put(pistaAsignada, idAvion);
                System.out.println("AVION " + idAvion + " despegando en " + pistaAsignada);
                
                // Lógica de impresión condicional
                if (estadoPistas.size() == 2 || (avionesProcesados.get() == TOTAL_AVIONES_SIMULACION - 1 && estadoPistas.size() == 1)) {
                    imprimirEstadoPistas();
                }
            }

            Thread.sleep(5000);

        } finally {
            synchronized(printLock) {
                estadoPistas.remove(pistaAsignada);
                pistasDisponibles.put(pistaAsignada);
                avionesProcesados.incrementAndGet();
                System.out.println("AVION " + idAvion + " ha despegado (volado).");
            }
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

        System.out.println("-> Pistas ocupadas: " + ocupadas + " por " + avionesEnPistaStr);
        System.out.println("---------------------------------------------------------------------");
    }
}