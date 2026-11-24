package com.importaflow.estructuras;

import com.importaflow.modelo.Pedido;

/** Nodo basico: dato + referencia al siguiente nodo. */
class NodoPedido {
    Pedido pedido;
    NodoPedido siguiente;

    NodoPedido(Pedido pedido) {
        this.pedido = pedido;
        this.siguiente = null;
    }
}
