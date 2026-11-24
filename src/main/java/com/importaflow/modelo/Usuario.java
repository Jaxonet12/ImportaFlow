package com.importaflow.modelo;

/** Nunca transportamos ni mostramos hashes de contrasenas en la interfaz. */
public record Usuario(int id, String usuario, String nombre, String rol, boolean activo) {

    public boolean administra() {
        return activo && "ADMIN".equals(rol);
    }

    public boolean puedeEditar() {
        return activo && (administra() || "OPERADOR".equals(rol));
    }
}
