package com.importaflow.algoritmos;

/** Semana 12: cola FIFO circular implementada mediante arreglo fijo. */
public class ColaEstatica {
    private final int[] elementos;
    private int frente = 0, fin = 0, cantidad = 0;

    public ColaEstatica(int capacidad) {
        if (capacidad < 1)
            throw new IllegalArgumentException();
        elementos = new int[capacidad];
    }

    public void enqueue(int valor) {
        if (cantidad == elementos.length)
            throw new IllegalStateException("Cola estatica llena.");
        elementos[fin] = valor;
        fin = (fin + 1) % elementos.length;
        cantidad++;
    }

    public Integer dequeue() {
        if (cantidad == 0)
            return null;
        int r = elementos[frente];
        frente = (frente + 1) % elementos.length;
        cantidad--;
        return r;
    }

    public Integer peek() {
        return cantidad == 0 ? null : elementos[frente];
    }

    public String mostrar() {
        StringBuilder s = new StringBuilder("FRENTE  ");
        for (int i = 0; i < cantidad; i++)
            s.append("P").append(elementos[(frente + i) % elementos.length]).append("  ->  ");
        return s.append("FIN").toString();
    }

    public int[] elementos() {
        int[] r = new int[cantidad];
        for (int i = 0; i < cantidad; i++)
            r[i] = elementos[(frente + i) % elementos.length];
        return r;
    }
}
