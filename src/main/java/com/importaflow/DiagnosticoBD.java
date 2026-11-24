package com.importaflow;
import com.importaflow.config.ConexionBD;
import java.sql.*;
/** Diagnóstico de solo lectura, ejecutable desde NetBeans como archivo Java. */
public class DiagnosticoBD {

    public static void main(String[] args) throws SQLException {
        try (Connection c = ConexionBD.conectar(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT DATABASE(), VERSION()")) {
                if (r.next())
                    System.out.println("Base: " + r.getString(1) + " | Servidor: " + r.getString(2));
            }
            for (String tabla : new String[]{"usuarios", "clientes", "pedidos", "detalles_envio", "movimientos",
                "ingresos"}) {
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
                    r.next();
                    System.out.println(tabla + ": " + r.getLong(1));
                }
            }
            System.out.println("Conexión y tablas principales correctas.");
        } catch (SQLException e) {
            System.err.println("SQLState: " + e.getSQLState() + " | Código: " + e.getErrorCode());
            throw e;
        }
    }
}
