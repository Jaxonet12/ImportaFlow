package com.importaflow.estructuras;

/** Pila dinamica LIFO, implementada manualmente mediante nodos. */
public class PilaAcciones {
    private NodoAccion cima;

    // PUSH: el nuevo nodo se convierte en la cima de la pila.
    public void apilar(String accion) {
        NodoAccion nuevo = new NodoAccion(accion);
        nuevo.siguiente = cima;
        cima = nuevo;
    }

    // POP: retiramos la cima y avanzamos la referencia.
    // Esta operacion NO deshace lo ocurrido en MySQL.
    public String desapilar() {
        if (cima == null)
            return null;
        String resultado = cima.accion;
        cima = cima.siguiente;
        return resultado;
    }

    // PEEK: consulta la accion mas reciente sin eliminarla.
    public String verCima() {
        return cima == null ? null : cima.accion;
    }

    public String visualizar() {
        if (cima == null)
            return "(sin acciones en esta sesion)";
        StringBuilder sb = new StringBuilder();
        NodoAccion actual = cima;
        while (actual != null) {
            sb.append("- ").append(actual.accion).append('\n');
            actual = actual.siguiente;
        }
        return sb.toString();
    }

    public String[] elementos() {
        java.util.List<String> r = new java.util.ArrayList<>();
        for (NodoAccion n = cima; n != null; n = n.siguiente)
            r.add(n.accion);
        return r.toArray(String[]::new);
    }
}
