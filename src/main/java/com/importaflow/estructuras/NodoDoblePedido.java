package com.importaflow.estructuras;

import com.importaflow.modelo.Pedido;

/** Nodo doble: conoce al nodo anterior y al siguiente. */
class NodoDoblePedido {
    Pedido pedido;
    NodoDoblePedido anterior;
    NodoDoblePedido siguiente;

    NodoDoblePedido(Pedido pedido) {
        this.pedido = pedido;
        this.anterior = null;
        this.siguiente = null;
    }
}
