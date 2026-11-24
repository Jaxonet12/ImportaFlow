package com.importaflow;

import com.importaflow.modelo.Pedido;
import com.importaflow.modelo.Usuario;
import com.importaflow.servicio.GestorPedidos;
import com.importaflow.estructuras.*;
import com.importaflow.algoritmos.ColaEstatica;
import java.util.Arrays;

/** Regresiones de la fase 1: sin acceder ni modificar MySQL. */
public class PruebaFase1 {
    private static int pruebas;

    private static void ok(boolean condicion, String descripcion) {
        if (!condicion)
            throw new AssertionError(descripcion);
        pruebas++;
        System.out.println("OK - " + descripcion);
    }

    private static Pedido pedido(int id, int bultos, String prioridad, String estado) {
        return new Pedido(id, "Cliente", "Paquete", "Peru", bultos, prioridad, estado);
    }

    public static void main(String[] args) {
        Usuario admin = new Usuario(1, "admin", "Admin", "ADMIN", true);
        Usuario op = new Usuario(2, "operador", "Operador", "OPERADOR", true);
        Usuario consulta = new Usuario(3, "consulta", "Consulta", "CONSULTA", true);
        Usuario inactivo = new Usuario(4, "inactivo", "Inactivo", "ADMIN", false);
        ok(admin.administra() && admin.puedeEditar(), "Administrador activo: edita y gestiona usuarios");
        ok(!op.administra() && op.puedeEditar(), "Operador: edita sin administrar usuarios");
        ok(!consulta.administra() && !consulta.puedeEditar(), "Consulta: no puede editar ni administrar");
        ok(!inactivo.administra() && !inactivo.puedeEditar(), "Administrador inactivo sin permisos");
        ListaSimplePedidos lista = new ListaSimplePedidos();
        lista.insertarAlFinal(pedido(1, 9, "NORMAL", "PENDIENTE"));
        lista.insertarAlFinal(pedido(2, 2, "URGENTE", "PENDIENTE"));
        lista.insertarAlFinal(pedido(3, 1, "NORMAL", "ENTREGADO"));
        lista.insertarAlFinal(pedido(4, 3, "URGENTE", "PENDIENTE"));
        lista.insertarAlFinal(pedido(5, 4, "NORMAL", "PENDIENTE"));
        lista.ordenarPorCantidad();
        Pedido[] pendientes = GestorPedidos.ordenarPendientesFIFO(lista.obtenerTodos());
        ok(Arrays.equals(Arrays.stream(pendientes).mapToInt(Pedido::getId).toArray(), new int[]{1, 2, 4, 5}),
            "FIFO independiente del orden por bultos, excluye entregados");
        ok(lista.obtenerTodos()[0].getId() == 3, "La copia FIFO no altera la lista ordenada");
        ColaEstatica estatica = new ColaEstatica(4);
        ColaPedidos dinamica = new ColaPedidos();
        ColaPrioridadPedidos prioridad = new ColaPrioridadPedidos();
        for (Pedido p : pendientes) {
            estatica.enqueue(p.getId());
            dinamica.encolar(p);
            prioridad.encolar(p);
        }
        for (int id : new int[]{1, 2, 4, 5})
            ok(estatica.dequeue() == id && dinamica.desencolar().getId() == id, "Ambas colas FIFO retiran P" + id);
        ok(estatica.dequeue() == null && dinamica.desencolar() == null, "Colas vacias sin error");
        for (int id : new int[]{2, 4, 1, 5})
            ok(prioridad.desencolar().getId() == id, "Cola prioridad retira P" + id);
        ok(prioridad.desencolar() == null, "Cola prioridad vacia");
        ok(GestorPedidos.ordenarPendientesFIFO(new Pedido[0]).length == 0, "Copia de pendientes vacia");
        System.out.println("OK: " + pruebas + " pruebas de fase 1 superadas.");
    }
}
