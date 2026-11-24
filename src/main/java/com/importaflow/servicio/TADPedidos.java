package com.importaflow.servicio;

import com.importaflow.modelo.Pedido;
import java.sql.SQLException;

/** TAD: contrato de operaciones, independientemente de la implementacion. */
public interface TADPedidos {
    // Contrato vigente: los cambios de expedientes se gestionan por SistemaDAO.
    Pedido atenderSiguiente(String usuario) throws SQLException;

    Pedido[] listar();

    Pedido[] pendientesFIFO();

    String verCola();
}
