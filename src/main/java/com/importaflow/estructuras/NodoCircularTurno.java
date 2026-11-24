package com.importaflow.estructuras;

/** Nodo que guarda a un encargado y enlaza con el siguiente turno. */
class NodoCircularTurno {
    String encargado;
    NodoCircularTurno siguiente;

    NodoCircularTurno(String encargado) {
        this.encargado = encargado;
        this.siguiente = null;
    }
}
