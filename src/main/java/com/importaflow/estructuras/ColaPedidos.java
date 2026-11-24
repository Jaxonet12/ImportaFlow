package com.importaflow.estructuras;

import com.importaflow.modelo.Pedido;

/** Cola dinamica FIFO mediante nodos: primero en entrar, primero en salir. */
public class ColaPedidos {
    private NodoPedido primero;
    private NodoPedido ultimo;
    private int cantidad;

    // ENQUEUE: agregamos al final en tiempo constante.
    public void encolar(Pedido pedido) {
        NodoPedido nuevo = new NodoPedido(pedido);
        if (ultimo == null) {
            primero = nuevo;
            ultimo = nuevo;
        } else {
            ultimo.siguiente = nuevo;
            ultimo = nuevo;
        }
        cantidad++;
    }

    // DEQUEUE: quitamos el nodo del frente, sin recorrer la cola.
    public Pedido desencolar() {
        if (primero == null)
            return null;
        Pedido resultado = primero.pedido;
        primero = primero.siguiente;
        if (primero == null)
            ultimo = null;
        cantidad--;
        return resultado;
    }

    // PEEK: muestra el primero sin desencolarlo.
    public Pedido verPrimero() {
        return primero == null ? null : primero.pedido;
    }

    public int getCantidad() {
        return cantidad;
    }

    public boolean estaVacia() {
        return primero == null;
    }

    public String visualizar() {
        if (estaVacia())
            return "(sin pedidos)";
        StringBuilder sb = new StringBuilder();
        NodoPedido actual = primero;
        while (actual != null) {
            sb.append("P").append(actual.pedido.getId())
            .append(" - ").append(actual.pedido.getProducto()).append('\n');
            actual = actual.siguiente;
        }
        return sb.toString();
    }

    public Pedido[] elementos() {
        Pedido[] r = new Pedido[cantidad];
        int i = 0;
        for (NodoPedido n = primero; n != null; n = n.siguiente)
            r[i++] = n.pedido;
        return r;
    }
}
