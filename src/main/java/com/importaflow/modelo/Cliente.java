package com.importaflow.modelo;

/** Cliente remitente; su documento se valida para evitar duplicados. */
public record Cliente(int id, String documento, String nombre, String telefono,
    String correo, String direccion) {

    @Override public String toString() {
        return nombre + " (" + documento + ")";
    }
}
