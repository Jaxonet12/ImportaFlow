package com.importaflow.modelo;
import java.math.BigDecimal;

/** Vista consolidada de pedido + detalle logistico persistido en MySQL. */
public record Envio(int id, int clienteId, String codigo, String cliente, String contenido,
    String origen, int cantidad, String prioridad, String estado,
    String destinatario, String telefono, String destino, String direccion,
    double peso, BigDecimal tarifa, String fecha, String registrador, BigDecimal abonado) {

    public Envio(int id, int clienteId, String codigo, String cliente, String contenido, String origen,
        int cantidad, String prioridad, String estado, String destinatario, String telefono, String destino,
        String direccion, double peso, double tarifa, String fecha, String registrador, double abonado) {
        this(id, clienteId, codigo, cliente, contenido, origen, cantidad, prioridad, estado, destinatario, telefono,
            destino, direccion, peso, BigDecimal.valueOf(tarifa), fecha, registrador, BigDecimal.valueOf(abonado));
    }

    public BigDecimal saldo() {
        return tarifa.subtract(abonado).max(BigDecimal.ZERO);
    }

    @Override public String toString() {
        return codigo + " | " + destinatario + " | " + estado;
    }
}
