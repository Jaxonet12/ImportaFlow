package com.importaflow.estructuras;

import com.importaflow.modelo.Pedido;

/** Lista simple implementada manualmente, sin java.util.LinkedList. */
public class ListaSimplePedidos {
    private NodoPedido inicio;
    private int cantidad;

    // Agregar al final: enlazamos un nuevo nodo al ultimo nodo conocido.
    public void insertarAlFinal(Pedido pedido) {
        NodoPedido nuevo = new NodoPedido(pedido);
        if (inicio == null) {
            inicio = nuevo;
        } else {
            NodoPedido actual = inicio;
            while (actual.siguiente != null) {
                actual = actual.siguiente;
            }
            actual.siguiente = nuevo;
        }
        cantidad++;
    }

    // Busqueda lineal: comprobamos cada nodo siguiendo la referencia siguiente.
    public Pedido buscar(int id) {
        NodoPedido actual = inicio;
        while (actual != null) {
            if (actual.pedido.getId() == id) {
                return actual.pedido;
            }
            actual = actual.siguiente;
        }
        return null;
    }

    // Sustituimos el objeto del nodo sin cambiar las referencias.
    public boolean actualizar(Pedido pedido) {
        NodoPedido actual = inicio;
        while (actual != null) {
            if (actual.pedido.getId() == pedido.getId()) {
                actual.pedido = pedido;
                return true;
            }
            actual = actual.siguiente;
        }
        return false;
    }

    // Eliminar: reconectamos el nodo anterior con el siguiente nodo.
    public boolean eliminar(int id) {
        if (inicio == null)
            return false;
        if (inicio.pedido.getId() == id) {
            inicio = inicio.siguiente;
            cantidad--;
            return true;
        }
        NodoPedido anterior = inicio;
        while (anterior.siguiente != null) {
            if (anterior.siguiente.pedido.getId() == id) {
                anterior.siguiente = anterior.siguiente.siguiente;
                cantidad--;
                return true;
            }
            anterior = anterior.siguiente;
        }
        return false;
    }

    // Proporcionamos un arreglo para poder poblar una JTable.
    // El almacenamiento interno de la lista SIGUE siendo mediante nodos.
    public Pedido[] obtenerTodos() {
        Pedido[] todos = new Pedido[cantidad];
        NodoPedido actual = inicio;
        int indice = 0;
        while (actual != null) {
            todos[indice++] = actual.pedido;
            actual = actual.siguiente;
        }
        return todos;
    }

    /** Semana 8: insercion ordenada en lista simplemente enlazada.
     * Ordenamos por cantidad de bultos; no se consulta Collections.sort.
     * Se reubican NODOS mediante referencias, no se reordena la base MySQL. */
    public void ordenarPorCantidad() {
        NodoPedido ordenada = null;
        NodoPedido actual = inicio;
        while (actual != null) {
            NodoPedido siguienteOriginal = actual.siguiente;
            if (ordenada == null || actual.pedido.getCantidad() < ordenada.pedido.getCantidad()) {
                actual.siguiente = ordenada;
                ordenada = actual;
            } else {
                NodoPedido cursor = ordenada;
                while (cursor.siguiente != null
                    && cursor.siguiente.pedido.getCantidad() <= actual.pedido.getCantidad()) {
                    cursor = cursor.siguiente;
                }
                actual.siguiente = cursor.siguiente;
                cursor.siguiente = actual;
            }
            actual = siguienteOriginal;
        }
        inicio = ordenada;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String visualizar() {
        if (inicio == null)
            return "La lista enlazada esta vacia.";
        StringBuilder sb = new StringBuilder("INICIO");
        NodoPedido actual = inicio;
        while (actual != null) {
            sb.append(" -> [P").append(actual.pedido.getId())
            .append(" | ").append(actual.pedido.getProducto())
            .append(" | bultos: ").append(actual.pedido.getCantidad()).append("]");
            actual = actual.siguiente;
        }
        return sb.append(" -> NULL").toString();
    }
}
