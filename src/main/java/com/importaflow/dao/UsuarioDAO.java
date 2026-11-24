package com.importaflow.dao;

import com.importaflow.config.ConexionBD;
import com.importaflow.modelo.Usuario;
import com.importaflow.seguridad.ClaveSegura;
import java.sql.*;
import java.util.*;

/** Acceso y gestion de roles. Toda consulta usa parametros JDBC. */
public class UsuarioDAO {

    public boolean inicializarAdmin() throws SQLException {
        try (Connection c = ConexionBD.conectar(); Statement st = c.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuarios")) {
            rs.next();
            if (rs.getInt(1) != 0)
                return false;
            if ("importaflow_final".equalsIgnoreCase(c.getCatalog()))
                throw new SQLException("Base final sin usuarios. Ejecuta el SQL completo de instalación antes de iniciar.");
        }
        // Solo se crea la cuenta de arranque al instalar una BD nueva.
        crearInicial("admin", "Administrador general", "ADMIN", "Importa2026!".toCharArray(), null);
        return true;
    }

    public Usuario autenticar(String username, char[] password) throws SQLException {
        String sql = "SELECT id, usuario, nombre, rol, activo, clave_hash FROM usuarios WHERE usuario = ?";
        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username == null ? "" : username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean("activo") || !ClaveSegura.verificar(password, rs
                    .getString("clave_hash")))
                    return null;
                return new Usuario(rs.getInt("id"), rs.getString("usuario"), rs.getString("nombre"), rs
                    .getString("rol"), true);
            }
        }
    }

    public List<Usuario> listar() throws SQLException {
        List<Usuario> r = new ArrayList<>();
        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement(
            "SELECT id, usuario, nombre, rol, activo FROM usuarios ORDER BY id");
            ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                r.add(new Usuario(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs
                    .getBoolean(5)));
        }
        return r;
    }

    public void crear(String usuario, String nombre, String rol, char[] password,
        String actor) throws SQLException {
        if (actor == null)
            throw new SecurityException("Se requiere administrador.");
        crearInicial(usuario, nombre, rol, password, actor);
    }

    private void crearInicial(String usuario, String nombre, String rol, char[] password,
        String actor) throws SQLException {
        if (usuario == null || !usuario.matches("[a-zA-Z0-9._-]{3,45}"))
            throw new IllegalArgumentException("El usuario debe tener entre 3 y 45 letras/numeros.");
        if (nombre == null || nombre.isBlank() || nombre.length() > 100)
            throw new IllegalArgumentException("Nombre de usuario invalido.");
        if (!List.of("ADMIN", "OPERADOR", "CONSULTA").contains(rol))
            throw new IllegalArgumentException("Rol invalido.");
        String hash = ClaveSegura.generar(password);
        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement(
            "INSERT INTO usuarios (usuario,nombre,rol,clave_hash) VALUES (?,?,?,?)")) {
            if (actor != null)
                PermisosDAO.exigir(c, actor, true);
            else
                try (Statement st = c.createStatement(); ResultSet r = st
                    .executeQuery("SELECT COUNT(*) FROM usuarios")) {
                    r.next();
                    if (r.getInt(1) != 0)
                        throw new SecurityException("La base ya tiene usuarios.");
                }
            ps.setString(1, usuario.trim());
            ps.setString(2, nombre.trim());
            ps.setString(3, rol);
            ps.setString(4, hash);
            ps.executeUpdate();
        }
    }

    public void cambiarClave(int id, char[] password, String actor) throws SQLException {
        String hash = ClaveSegura.generar(password);
        try (Connection c = ConexionBD.conectar(); PreparedStatement ps = c.prepareStatement(
            "UPDATE usuarios SET clave_hash=? WHERE id=?")) {
            PermisosDAO.exigir(c, actor, true);
            ps.setString(1, hash);
            ps.setInt(2, id);
            if (ps.executeUpdate() != 1)
                throw new IllegalArgumentException("Usuario no encontrado.");
        }
    }

    public Usuario buscarPorId(int id) throws SQLException {
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c.prepareStatement(
            "SELECT id,usuario,nombre,rol,activo FROM usuarios WHERE id=?")) {
            p.setInt(1, id);
            try (ResultSet x = p.executeQuery()) {
                return x.next() ? new Usuario(x.getInt(1), x.getString(2), x.getString(3), x.getString(4), x
                    .getBoolean(5)) : null;
            }
        }
    }

    /** Cambio personal: verifica la clave anterior y mantiene bloqueada la cuenta hasta confirmar. */
    public void cambiarClavePropia(int id, char[] actual, char[] nueva) throws SQLException {
        String hash = ClaveSegura.generar(nueva);
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c
                    .prepareStatement("SELECT clave_hash,activo FROM usuarios WHERE id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet x = p.executeQuery()) {
                        if (!x.next() || !x.getBoolean(2) || !ClaveSegura.verificar(actual, x.getString(1)))
                            throw new IllegalArgumentException("La contrasena actual es incorrecta o la cuenta esta inactiva.");
                    }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE usuarios SET clave_hash=? WHERE id=?")) {
                    p.setString(1, hash);
                    p.setInt(2, id);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void cambiarActivo(int id, boolean activo, String actor) throws SQLException {
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                PermisosDAO.exigir(c, actor, true);
                // Bloquear administradores en orden estable antes de contar los activos.
                int activos = 0;
                boolean objetivoAdminActivo = false;
                try (PreparedStatement p = c
                    .prepareStatement("SELECT id,activo FROM usuarios WHERE rol='ADMIN' ORDER BY id FOR UPDATE");
                    ResultSet x = p.executeQuery()) {
                    while (x.next())
                        if (x.getBoolean(2)) {
                            activos++;
                            if (x.getInt(1) == id)
                                objetivoAdminActivo = true;
                        }
                }
                if (!activo && objetivoAdminActivo && activos <= 1)
                    throw new IllegalArgumentException("Debe permanecer al menos un administrador activo.");
                try (PreparedStatement p = c.prepareStatement("UPDATE usuarios SET activo=? WHERE id=?")) {
                    p.setBoolean(1, activo);
                    p.setInt(2, id);
                    if (p.executeUpdate() != 1)
                        throw new IllegalArgumentException("Usuario no encontrado.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
