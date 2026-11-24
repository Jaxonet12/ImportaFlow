package com.importaflow.dao;

import com.importaflow.config.ConexionBD;
import com.importaflow.modelo.Pedido;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO: separa las consultas SQL de los algoritmos de estructuras de datos. */
public class PedidoDAO {

    public List<Pedido> listar() throws SQLException {
        String sql = "SELECT id, cliente, producto, pais_origen, cantidad, prioridad, estado "
        + "FROM pedidos ORDER BY id";
        // ArrayList solo transporta filas JDBC; la logica del proyecto usa nodos propios.
        List<Pedido> resultado = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(new Pedido(
                    rs.getInt("id"), rs.getString("cliente"), rs.getString("producto"),
                    rs.getString("pais_origen"), rs.getInt("cantidad"),
                    rs.getString("prioridad"), rs.getString("estado")));
            }
        }
        return resultado;
    }

}
