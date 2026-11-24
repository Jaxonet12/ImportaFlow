package com.importaflow.modelo;

/** Objeto que representa un pedido internacional. */
public class Pedido {
    private final int id;
    private final String cliente;
    private final String producto;
    private final String paisOrigen;
    private final int cantidad;
    private final String prioridad;
    private final String estado;

    // El constructor agrupa los datos de cada pedido en un solo objeto.
    public Pedido(int id, String cliente, String producto, String paisOrigen,
        int cantidad, String prioridad, String estado) {
        this.id = id;
        this.cliente = cliente;
        this.producto = producto;
        this.paisOrigen = paisOrigen;
        this.cantidad = cantidad;
        this.prioridad = prioridad;
        this.estado = estado;
    }

    // Getters: proporcionan lectura controlada de atributos encapsulados.
    public int getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public String getProducto() {
        return producto;
    }

    public String getPaisOrigen() {
        return paisOrigen;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public String getEstado() {
        return estado;
    }
}
