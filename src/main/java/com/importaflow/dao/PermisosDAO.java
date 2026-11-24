package com.importaflow.dao;
import java.sql.*;
/** Control de actor para operaciones persistentes. No sustituye permisos de MySQL. */
final class PermisosDAO {

    static void exigir(Connection c, String actor, boolean admin) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT rol,activo FROM usuarios WHERE usuario=?")) {
            p.setString(1, actor);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !r.getBoolean(2) || !permitido(r.getString(1), admin))
                    throw new SecurityException("Cuenta inactiva o sin permiso para esta operación.");
            }
        }
    }

    private static boolean permitido(String rol, boolean admin) {
        return "ADMIN".equals(rol) || (!admin && "OPERADOR".equals(rol));
    }
}
