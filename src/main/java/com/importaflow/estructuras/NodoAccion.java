package com.importaflow.estructuras;

/** Nodo para una pila de acciones: dato y enlace a la siguiente accion. */
class NodoAccion {
    String accion;
    NodoAccion siguiente;

    NodoAccion(String accion) {
        this.accion = accion;
        this.siguiente = null;
    }
}
