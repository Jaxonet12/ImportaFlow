package com.importaflow;

import com.importaflow.algoritmos.*;
import com.importaflow.estructuras.*;
import com.importaflow.modelo.*;
import com.importaflow.seguridad.ClaveSegura;
import java.util.*;

/** Pruebas automaticas sin MySQL para estructuras de las semanas 2 a 14. */
public class PruebaTemario {
    private static int pruebas = 0;

    private static void verificar(boolean ok, String causa) {
        pruebas++;
        if (!ok)
            throw new AssertionError(causa);
    }

    private static Pedido pedido(int id, int cantidad) {
        return new Pedido(id, "Cliente", "Producto", "Peru", cantidad, "NORMAL", "PENDIENTE");
    }

    private static Envio envio(int id) {
        return new Envio(id, 0, "IF-" + id, "Cliente", "Contenido", "Peru", 1, "NORMAL", "PENDIENTE",
            "Destinatario", "", "Lima", "", 1, 10, "", "test", 0);
    }

    public static void main(String[] args) {
        int[] a = {5, 2, 8};
        verificar(Arrays.equals(ArreglosAED.insertar(a, 1, 7), new int[]{5, 7, 2, 8}), "S2 insertar");
        verificar(Arrays.equals(ArreglosAED.eliminar(a, 1), new int[]{5, 8}), "S2 eliminar");
        verificar(Arrays.equals(ArreglosAED.actualizar(a, 0, 9), new int[]{9, 2, 8}), "S2 actualizar");
        int[] b = ArreglosAED.copiar(a);
        verificar(a != b && ArreglosAED.comparar(a, b), "S3 copia y comparacion");
        verificar(Arrays.equals(ArreglosAED.fusionar(a, b), new int[]{5, 2, 8, 5, 2, 8}), "S3 fusion");
        for (int[] ordenado : new int[][]{ArreglosAED.burbuja(a), ArreglosAED.seleccion(a),
            ArreglosAED.insercion(a)})
            verificar(Arrays.equals(ordenado, new int[]{2, 5, 8}), "ordenar");
        verificar(ArreglosAED.trazaBurbuja(a).contains("Despues: [2, 5, 8]"), "traza burbuja");
        verificar(ArreglosAED.trazaSeleccion(a).contains("Despues: [2, 5, 8]"), "traza seleccion");
        verificar(ArreglosAED.trazaInsercion(a).contains("Despues: [2, 5, 8]"), "traza insercion");
        int[][] m = {{2, 1, 0},{1, 3, 4},{0, 4, 5}};
        verificar(MatricesAED.esSimetrica(m), "S4 simetria");
        verificar(MatricesAED.triangularInferior(m)[0][1] == 0, "S4 triangular inferior");
        verificar(MatricesAED.triangularSuperior(m)[2][0] == 0, "S4 triangular superior");
        verificar(MatricesAED.tridiagonal(m)[0][2] == 0, "S4 tridiagonal");
        verificar(MatricesAED.transponer(m)[1][2] == 4, "S4 transponer");
        verificar(Math.abs(MatricesAED.determinante(m) + 7) < 1e-9, "S4 determinante");
        verificar(MatricesAED.inversa(m) != null, "S4 inversa");
        verificar(MatricesAED.inversa(new int[][]{{1, 2},{2, 4}}) == null, "S4 matriz singular");
        verificar(MatricesAED.esSimetrica(MatricesAED.simetrizar(m)), "S4 simetrizar");
        verificar(MatricesAED.inversa(MatricesAED.diagonalDominante(new int[][]{{1, 2, 3},{1, 2, 3},{3, 4,
            5}})) != null, "S4 inversa ajustada");
        ListaSimplePedidos s = new ListaSimplePedidos();
        s.insertarAlFinal(pedido(1, 5));
        s.insertarAlFinal(pedido(2, 2));
        s.insertarAlFinal(pedido(3, 3));
        verificar(s.buscar(2) != null, "S7 busqueda simple");
        s.ordenarPorCantidad();
        verificar(s.obtenerTodos()[0].getId() == 2, "S8 ordenar nodos");
        verificar(s.visualizar().contains("bultos: 2"), "S8 cantidad visible en lista");
        verificar(s.eliminar(2) && s.getCantidad() == 2, "S7 eliminar nodo");
        ListaDoblePedidos doble = new ListaDoblePedidos();
        doble.insertarAlFinal(pedido(1, 1));
        doble.insertarAlFinal(pedido(2, 2));
        verificar(doble.visualizarAtras().indexOf("P2") < doble.visualizarAtras().indexOf("P1"),
            "S9 doble reversa");
        verificar(doble.eliminar(1) && doble.buscar(1) == null, "S9 doble eliminar");
        ListaCircularTurnos turnos = new ListaCircularTurnos();
        turnos.agregar("Ana");
        turnos.agregar("Luis");
        verificar("Luis".equals(turnos.siguienteTurno()), "S9 rotacion");
        verificar("Ana".equals(turnos.siguienteTurno()), "S9 circularidad");
        PilaEstatica pila = new PilaEstatica(2);
        pila.push("A");
        pila.push("B");
        verificar("B".equals(pila.pop()) && "A".equals(pila.peek()), "S11 pila estatica");
        PilaAcciones dinamica = new PilaAcciones();
        dinamica.apilar("A");
        dinamica.apilar("B");
        verificar("B".equals(dinamica.desapilar()), "S11 pila dinamica");
        ColaEstatica cola = new ColaEstatica(2);
        cola.enqueue(1);
        cola.enqueue(2);
        verificar(cola.dequeue() == 1, "S12 cola estatica");
        cola.enqueue(3);
        verificar(cola.dequeue() == 2 && cola.dequeue() == 3, "S12 cola estatica circular");
        ColaPedidos colaD = new ColaPedidos();
        colaD.encolar(pedido(1, 1));
        colaD.encolar(pedido(2, 1));
        verificar(colaD.desencolar().getId() == 1, "S12 cola dinamica");
        ColaPrioridadPedidos prio = new ColaPrioridadPedidos();
        prio.encolar(pedido(1, 1));
        prio.encolar(new Pedido(2, "Cliente", "Prod", "Peru", 1, "URGENTE", "PENDIENTE"));
        verificar(prio.desencolar().getId() == 2, "S12 prioridad");
        ArbolBinarioEnvios arbol = new ArbolBinarioEnvios();
        for (int v : new int[]{5, 3, 7, 2, 4, 6, 8})
            arbol.insertar(envio(v));
        verificar(arbol.buscar(4) != null, "S14 buscar");
        verificar(arbol.preorden().equals("5 3 2 4 7 6 8"), "S13 preorden");
        verificar(arbol.inorden().equals("2 3 4 5 6 7 8"), "S13 inorden");
        verificar(arbol.postorden().equals("2 4 3 6 8 7 5"), "S13 postorden");
        arbol.eliminar(5);
        verificar(arbol.buscar(5) == null && arbol.buscar(6) != null, "S14 eliminar raiz");
        verificar(ArbolExpresion.explicar(2).contains("20.00"), "S14 arbol expresion");
        char[] pass = "ContrasenaTest2026!".toCharArray();
        String hash = ClaveSegura.generar(pass);
        verificar(ClaveSegura.verificar(pass, hash) && !ClaveSegura.verificar("incorrecta".toCharArray(), hash),
            "PBKDF2");
        System.out.println("OK: " + pruebas + " pruebas de algoritmos y seguridad superadas.");
    }
}
