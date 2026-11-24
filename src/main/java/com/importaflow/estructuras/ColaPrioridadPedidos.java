package com.importaflow.estructuras;

import com.importaflow.modelo.Pedido;

/** Dos colas FIFO: primero urgentes; entre iguales se respeta la llegada. */
public class ColaPrioridadPedidos {
    private final ColaPedidos urgentes = new ColaPedidos();
    private final ColaPedidos normales = new ColaPedidos();

    public void encolar(Pedido pedido) {
        if ("URGENTE".equals(pedido.getPrioridad())) {
            urgentes.encolar(pedido);
        } else {
            normales.encolar(pedido);
        }
    }

    public Pedido verSiguiente() {
        return urgentes.estaVacia() ? normales.verPrimero() : urgentes.verPrimero();
    }

    public Pedido desencolar() {
        return urgentes.estaVacia() ? normales.desencolar() : urgentes.desencolar();
    }

    public int getCantidad() {
        return urgentes.getCantidad() + normales.getCantidad();
    }

    public int getUrgentes() {
        return urgentes.getCantidad();
    }

    public String visualizar() {
        return "=== URGENTES (primero) ===\n" + urgentes.visualizar()
        + "\n=== NORMALES ===\n" + normales.visualizar();
    }

    public Pedido[] elementos() {
        Pedido[] a = urgentes.elementos(), b = normales.elementos(), r = new Pedido[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
