package com.importaflow.estructuras;

import com.importaflow.modelo.Pedido;

/**
 * Lista doblemente enlazada implementada con nodos propios.
 * Cada nodo permite desplazarse hacia adelante o hacia atras.
 */
public class ListaDoblePedidos {
    private NodoDoblePedido primero;
    private NodoDoblePedido ultimo;
    private int cantidad;

    // Insercion O(1): guardamos una referencia al ultimo nodo.
    public void insertarAlFinal(Pedido pedido) {
        NodoDoblePedido nuevo = new NodoDoblePedido(pedido);
        if (primero == null) {
            primero = ultimo = nuevo;
        } else {
            ultimo.siguiente = nuevo;
            nuevo.anterior = ultimo;
            ultimo = nuevo;
        }
        cantidad++;
    }

    // Busqueda secuencial O(n) usando la referencia siguiente.
    public Pedido buscar(int id) {
        NodoDoblePedido actual = primero;
        while (actual != null) {
            if (actual.pedido.getId() == id)
                return actual.pedido;
            actual = actual.siguiente;
        }
        return null;
    }

    // Elimina un nodo en memoria y reconecta ambos enlaces.
    // La persistencia en MySQL se gestiona por separado desde PedidoDAO.
    public boolean eliminar(int id) {
        NodoDoblePedido actual = primero;
        while (actual != null) {
            if (actual.pedido.getId() == id) {
                if (actual.anterior == null) {
                    primero = actual.siguiente;
                } else {
                    actual.anterior.siguiente = actual.siguiente;
                }
                if (actual.siguiente == null) {
                    ultimo = actual.anterior;
                } else {
                    actual.siguiente.anterior = actual.anterior;
                }
                cantidad--;
                return true;
            }
            actual = actual.siguiente;
        }
        return false;
    }

    public int getCantidad() {
        return cantidad;
    }

    // Recorremos desde el primero siguiendo 'siguiente'.
    public String visualizarAdelante() {
        if (primero == null)
            return "La lista doble esta vacia.";
        StringBuilder sb = new StringBuilder("INICIO");
        NodoDoblePedido actual = primero;
        while (actual != null) {
            sb.append(" <-> [P").append(actual.pedido.getId())
            .append(" | ").append(actual.pedido.getProducto())
            .append(" | ").append(actual.pedido.getEstado()).append("]");
            actual = actual.siguiente;
        }
        return sb.append(" <-> FIN").toString();
    }

    // Recorremos desde el ultimo siguiendo 'anterior'.
    public String visualizarAtras() {
        if (ultimo == null)
            return "La lista doble esta vacia.";
        StringBuilder sb = new StringBuilder("FIN");
        NodoDoblePedido actual = ultimo;
        while (actual != null) {
            sb.append(" <-> [P").append(actual.pedido.getId())
            .append(" | ").append(actual.pedido.getProducto())
            .append(" | ").append(actual.pedido.getEstado()).append("]");
            actual = actual.anterior;
        }
        return sb.append(" <-> INICIO").toString();
    }

    public Pedido[] elementos(boolean reversa) {
        Pedido[] r = new Pedido[cantidad];
        int i = 0;
        for (NodoDoblePedido n = reversa ? ultimo : primero; n != null; n = reversa ? n.anterior : n.siguiente)
            r[i++] = n.pedido;
        return r;
    }
}
