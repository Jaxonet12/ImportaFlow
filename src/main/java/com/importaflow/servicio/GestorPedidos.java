package com.importaflow.servicio;

import com.importaflow.dao.PedidoDAO;
import com.importaflow.dao.SistemaDAO;
import java.util.Arrays;
import java.util.Comparator;
import com.importaflow.estructuras.ColaPrioridadPedidos;
import com.importaflow.estructuras.ListaSimplePedidos;
import com.importaflow.estructuras.ListaDoblePedidos;
import com.importaflow.estructuras.ListaCircularTurnos;
import com.importaflow.estructuras.PilaAcciones;
import com.importaflow.modelo.Pedido;
import java.sql.SQLException;

/** Integra MySQL y las estructuras dinamicas definidas por el curso. */
public class GestorPedidos implements TADPedidos {
    private final PedidoDAO dao = new PedidoDAO();
    private final SistemaDAO sistema = new SistemaDAO();
    private final PilaAcciones historial = new PilaAcciones();
    private ListaSimplePedidos lista = new ListaSimplePedidos();
    private ListaDoblePedidos listaDoble = new ListaDoblePedidos();
    private final ListaCircularTurnos turnos = new ListaCircularTurnos();
    private ColaPrioridadPedidos cola = new ColaPrioridadPedidos();

    public GestorPedidos() throws SQLException {
        recargar();
    }

    // Sincronizacion: recuperamos los pedidos persistidos y reconstruimos nodos y colas.
    // Los nodos son estructuras en memoria; MySQL conserva los datos al cerrar el programa.
    public final void recargar() throws SQLException {
        ListaSimplePedidos nuevaLista = new ListaSimplePedidos();
        ListaDoblePedidos nuevaDoble = new ListaDoblePedidos();
        ColaPrioridadPedidos nuevaCola = new ColaPrioridadPedidos();
        for (Pedido pedido : dao.listar()) {
            nuevaLista.insertarAlFinal(pedido);
            nuevaDoble.insertarAlFinal(pedido);
            if ("PENDIENTE".equals(pedido.getEstado())) {
                nuevaCola.encolar(pedido);
            }
        }
        lista = nuevaLista;
        listaDoble = nuevaDoble;
        cola = nuevaCola;
    }

    @Override
    public Pedido atenderSiguiente(String usuario) throws SQLException {
        // La cola se reconstruye dentro de la transaccion con filas bloqueadas.
        // El DAO confirma estado e historial juntos, antes de modificar la memoria.
        Pedido siguiente = sistema.atenderSiguiente(usuario);
        if (siguiente != null)
            historial.apilar("Clasificado " + SistemaDAO.codigo(siguiente.getId()));
        return siguiente;
    }

    /** Copia independiente del orden actual de la lista enlazada. ID creciente = llegada. */
    public static Pedido[] ordenarPendientesFIFO(Pedido[] pedidos) {
        return Arrays.stream(pedidos).filter(x -> "PENDIENTE".equals(x.getEstado()))
        .sorted(Comparator.comparingInt(Pedido::getId)).toArray(Pedido[]::new);
    }

    @Override
    public Pedido[] pendientesFIFO() {
        return ordenarPendientesFIFO(listar());
    }

    @Override
    public Pedido[] listar() {
        return lista.obtenerTodos();
    }

    public int getCantidad() {
        return lista.getCantidad();
    }

    public int getPendientes() {
        return cola.getCantidad();
    }

    public int getUrgentes() {
        return cola.getUrgentes();
    }

    public String verLista() {
        return lista.visualizar();
    }
    /** Ordenamiento de la lista por cantidad: solo nodos en memoria. */
    public String ordenarListaSimplePorCantidad() {
        lista.ordenarPorCantidad();
        return lista.visualizar();
    }
    // Los pedidos se consultan en ambos sentidos sin modificar su orden en MySQL.
    public String verListaDobleAdelante() {
        return listaDoble.visualizarAdelante();
    }

    public String verListaDobleAtras() {
        return listaDoble.visualizarAtras();
    }

    public Pedido buscarEnListaDoble(int id) {
        return listaDoble.buscar(id);
    }
    // La lista circular administra encargados durante la sesion actual.
    public boolean agregarEncargado(String nombre) {
        return turnos.agregar(nombre);
    }

    public boolean eliminarEncargado(String nombre) {
        return turnos.eliminar(nombre);
    }

    public String avanzarTurno() {
        return turnos.siguienteTurno();
    }

    public String verEncargadoActual() {
        return turnos.verActual();
    }

    public String verListaCircular() {
        return turnos.visualizar();
    }

    public String verCola() {
        return cola.visualizar();
    }

    public String verPila() {
        return historial.visualizar();
    }
    /** Registra operaciones del modulo logistico en la pila dinamica de la sesion. */
    public void registrarAccion(String accion) {
        historial.apilar(accion);
    }

    public String retirarUltimaAccion() {
        return historial.desapilar();
    }

    public String[] accionesSesion() {
        return historial.elementos();
    }
}
