package com.importaflow.algoritmos;

/** Semana 11: pila con arreglo y capacidad definida; LIFO. */
public class PilaEstatica {
    private final String[] datos;
    private int cima = -1;

    public PilaEstatica(int capacidad) {
        if (capacidad < 1)
            throw new IllegalArgumentException();
        datos = new String[capacidad];
    }

    public void push(String valor) {
        if (cima == datos.length - 1)
            throw new IllegalStateException("La pila estatica esta llena.");
        datos[++cima] = valor;
    }

    public String pop() {
        if (cima < 0)
            return null;
        String r = datos[cima];
        datos[cima--] = null;
        return r;
    }

    public String peek() {
        return cima < 0 ? null : datos[cima];
    }

    public String mostrar() {
        StringBuilder b = new StringBuilder("CIMA\n");
        for (int i = cima; i >= 0; i--)
            b.append("[ ").append(datos[i]).append(" ]\n");
        return b.append("BASE").toString();
    }

    public String[] elementos() {
        String[] r = new String[cima + 1];
        for (int i = cima; i >= 0; i--)
            r[cima - i] = datos[i];
        return r;
    }
}
