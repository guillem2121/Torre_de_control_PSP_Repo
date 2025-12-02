package org.yourcompany.yourproject;

import java.util.concurrent.Semaphore;

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

        TorreDeControl torreDeControl = new TorreDeControl();
        for (int i = 0; i < 5; i++) {
            new Thread(new Aterrizaje(torreDeControl)).start();
        }
        for (int i = 0; i < 10; i++) {
            new Thread(new Despegue(torreDeControl)).start();
        }
    }
}

class Aterrizaje implements Runnable {
    private TorreDeControl torreDeControl;

    public Aterrizaje(TorreDeControl torreDeControl) {
        this.torreDeControl = torreDeControl;
    }

    @Override
    public void run() {
        // Lógica para aterrizar un avión
        try {
            torreDeControl.aterrizar();
            //System.out.println("Avión aterrizado");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Despegue implements Runnable {
    private TorreDeControl torreDeControl;

    public Despegue(TorreDeControl torreDeControl) {
        this.torreDeControl = torreDeControl;
    }

    @Override
    public void run() {
        // Lógica para despegar un avión 
        try {
            torreDeControl.despegar();
            //System.out.println("Avión despegado");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class TorreDeControl {
    private Semaphore pista = new Semaphore(2);
    private int avionesAterrizandoEsperando = 0;

   public void aterrizar() throws InterruptedException {
        // PASO 1: Avisar que llego (Rápido y exclusivo)
        synchronized (this) {
            avionesAterrizandoEsperando++;
        }

        // PASO 2: La acción (Lenta y paralela)
        // Esto está FUERA del synchronized para que 2 aviones puedan hacerlo a la vez
        pista.acquire(); 
        try {
            System.out.println("Avión ATERRIZANDO... (Pistas ocupadas: " + (2 - pista.availablePermits()) + ")");
            Thread.sleep(5000); // Simular tiempo
            System.out.println("Avión ATERRIZADO.");
        } finally {
            pista.release(); // Soltamos la pista antes de avisar a la torre
            
            // PASO 3: Avisar que me voy (Rápido y exclusivo)
            synchronized (this) {
                avionesAterrizandoEsperando--;
                notifyAll(); // <--- ¡CRUCIAL! Despierta a los despegues dormidos
            }
        }
    }

    public void despegar() throws InterruptedException {
        // PASO 1: Pedir permiso al "gorila" (Rápido y exclusivo)
        synchronized (this) {
            // Mientras haya alguien queriendo aterrizar, yo espero.
            while (avionesAterrizandoEsperando > 0) {
                wait(); // Suelta el candado y se duerme hasta que alguien haga notifyAll
            }
        }

        // PASO 2: La acción (Lenta y paralela)
        // Si pasé el while, significa que tengo permiso para intentar coger pista
        pista.acquire();
        try {
            System.out.println("Avión DESPEGANDO... (Pistas ocupadas: " + (2 - pista.availablePermits()) + ")");
            Thread.sleep(5000); 
            System.out.println("Avión DESPEGADO.");
        } finally {
            pista.release();
        }
    }
}
