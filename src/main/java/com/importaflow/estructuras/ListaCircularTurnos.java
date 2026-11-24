package com.importaflow.estructuras;

/**
 * Lista circular simple de encargados de atencion.
 * El ultimo nodo apunta al actual; nunca termina en NULL si hay nodos.
 * Los turnos son una simulacion en memoria, no se guardan en MySQL.
 */
public class ListaCircularTurnos {
    private NodoCircularTurno actual;
    private NodoCircularTurno ultimo;
    private int cantidad;

    // Inserta al final del ciclo; evita nombres repetidos.
    public boolean agregar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Ingresa el nombre del encargado.");
        }
        nombre = nombre.trim();
        if (nombre.length() > 80) {
            throw new IllegalArgumentException("El nombre no debe superar 80 caracteres.");
        }
        if (contiene(nombre))
            return false;

        NodoCircularTurno nuevo = new NodoCircularTurno(nombre);
        if (actual == null) {
            actual = ultimo = nuevo;
            nuevo.siguiente = nuevo;
            // Un solo nodo se apunta a si mismo.
        } else {
            ultimo.siguiente = nuevo;
            nuevo.siguiente = actual;
            // Cierra el ciclo.
            ultimo = nuevo;
        }
        cantidad++;
        return true;
    }

    public boolean contiene(String nombre) {
        if (actual == null || nombre == null)
            return false;
        NodoCircularTurno nodo = actual;
        do {
            if (nodo.encargado.equalsIgnoreCase(nombre.trim()))
                return true;
            nodo = nodo.siguiente;
        } while (nodo != actual);
        // Evita un bucle infinito.
        return false;
    }

    // Retira un turno ajustando el enlace anterior, el actual y/o el ultimo.
    public boolean eliminar(String nombre) {
        if (actual == null || nombre == null)
            return false;
        NodoCircularTurno anterior = ultimo;
        NodoCircularTurno nodo = actual;
        do {
            if (nodo.encargado.equalsIgnoreCase(nombre.trim())) {
                if (cantidad == 1) {
                    actual = ultimo = null;
                } else {
                    anterior.siguiente = nodo.siguiente;
                    if (nodo == actual)
                        actual = nodo.siguiente;
                    if (nodo == ultimo)
                        ultimo = anterior;
                }
                cantidad--;
                return true;
            }
            anterior = nodo;
            nodo = nodo.siguiente;
        } while (nodo != actual);
        return false;
    }

    // Rota una posicion. ultimo sigue siendo el nodo previo al actual.
    public String siguienteTurno() {
        if (actual == null)
            return null;
        actual = actual.siguiente;
        ultimo = ultimo.siguiente;
        return actual.encargado;
    }

    public String verActual() {
        return actual == null ? null : actual.encargado;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String visualizar() {
        if (actual == null)
            return "No hay encargados. Agrega uno para iniciar los turnos.";
        StringBuilder sb = new StringBuilder("TURNO ACTUAL: ")
        .append(actual.encargado).append("\n\n");
        NodoCircularTurno nodo = actual;
        do {
            sb.append("[").append(nodo.encargado).append("] -> ");
            nodo = nodo.siguiente;
        } while (nodo != actual);
        return sb.append("(vuelve a ").append(actual.encargado).append(")").toString();
    }

    public String[] elementos() {
        String[] r = new String[cantidad];
        NodoCircularTurno n = actual;
        for (int i = 0; i < cantidad; i++) {
            r[i] = n.encargado;
            n = n.siguiente;
        }
        return r;
    }
}
