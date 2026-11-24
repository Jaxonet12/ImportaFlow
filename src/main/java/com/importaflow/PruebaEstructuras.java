package com.importaflow;

import com.importaflow.estructuras.ColaPedidos;
import com.importaflow.estructuras.ColaPrioridadPedidos;
import com.importaflow.estructuras.ListaSimplePedidos;
import com.importaflow.estructuras.ListaDoblePedidos;
import com.importaflow.estructuras.ListaCircularTurnos;
import com.importaflow.estructuras.PilaAcciones;
import com.importaflow.modelo.Pedido;

/**
 * Pruebas basicas de los algoritmos SIN requerir MySQL.
 * Ejecutar este archivo con clic derecho -> Ejecutar archivo.
 */
public class PruebaEstructuras {

    private static void comprobar(boolean ok, String descripcion) {
        if (!ok)
            throw new IllegalStateException("Fallo: " + descripcion);
        System.out.println("OK - " + descripcion);
    }

    public static void main(String[] args) {
        Pedido normal1 = new Pedido(1, "Ana", "Mouse", "China", 2, "NORMAL", "PENDIENTE");
        Pedido normal2 = new Pedido(2, "Luis", "Laptop", "EE.UU.", 1, "NORMAL", "PENDIENTE");
        Pedido urgente = new Pedido(3, "Sara", "Celular", "Japon", 1, "URGENTE", "PENDIENTE");

        ListaSimplePedidos lista = new ListaSimplePedidos();
        lista.insertarAlFinal(normal1);
        lista.insertarAlFinal(normal2);
        comprobar(lista.getCantidad() == 2, "insercion en lista simple");
        comprobar(lista.buscar(2) == normal2, "busqueda por ID");
        comprobar(lista.eliminar(1) && lista.buscar(1) == null, "eliminacion de nodo");

        // La lista doble permite recorrer los pedidos en ambos sentidos.
        ListaDoblePedidos doble = new ListaDoblePedidos();
        doble.insertarAlFinal(normal1);
        doble.insertarAlFinal(normal2);
        doble.insertarAlFinal(urgente);
        comprobar(doble.buscar(2) == normal2, "busqueda en lista doble");
        comprobar(doble.visualizarAtras().indexOf("P3") < doble.visualizarAtras().indexOf("P2"),
            "recorrido inverso de lista doble");
        comprobar(doble.eliminar(2) && doble.buscar(2) == null && doble.getCantidad() == 2,
            "eliminacion en lista doble reconectando anterior y siguiente");

        // La lista circular retorna al primer encargado tras el ultimo.
        ListaCircularTurnos turnos = new ListaCircularTurnos();
        comprobar(turnos.agregar("Ana") && turnos.agregar("Luis") && turnos.agregar("Sara"),
            "insercion en lista circular");
        comprobar("Luis".equals(turnos.siguienteTurno()), "siguiente turno circular");
        comprobar("Sara".equals(turnos.siguienteTurno()), "segundo turno circular");
        comprobar("Ana".equals(turnos.siguienteTurno()), "regreso al inicio del ciclo");
        comprobar(turnos.eliminar("Luis") && turnos.getCantidad() == 2,
            "eliminacion en lista circular");

        ColaPedidos cola = new ColaPedidos();
        cola.encolar(normal1);
        cola.encolar(normal2);
        comprobar(cola.desencolar() == normal1, "cola FIFO (dequeue)");
        comprobar(cola.verPrimero() == normal2, "consulta de frente");

        ColaPrioridadPedidos prioridades = new ColaPrioridadPedidos();
        prioridades.encolar(normal1);
        prioridades.encolar(normal2);
        prioridades.encolar(urgente);
        comprobar(prioridades.desencolar() == urgente, "urgente primero");
        comprobar(prioridades.desencolar() == normal1, "FIFO dentro de prioridad normal");

        PilaAcciones pila = new PilaAcciones();
        pila.apilar("primera");
        pila.apilar("segunda");
        comprobar("segunda".equals(pila.desapilar()), "pila LIFO (pop)");
        comprobar("primera".equals(pila.verCima()), "consulta de cima");

        System.out.println("TODAS LAS PRUEBAS SUPERADAS.");
    }
}
