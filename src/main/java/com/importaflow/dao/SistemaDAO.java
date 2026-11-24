package com.importaflow.dao;

import com.importaflow.config.ConexionBD;
import com.importaflow.modelo.Cliente;
import com.importaflow.modelo.Envio;
import com.importaflow.modelo.Pedido;
import com.importaflow.estructuras.ColaPrioridadPedidos;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.importaflow.servicio.ValidacionLogistica;
import java.util.*;

/** Persistencia del sistema logistico. Las mutaciones relacionadas son transacciones. */
public class SistemaDAO {
    public record Movimiento(String fecha, String codigo, String anterior, String nuevo,
        String observacion, String usuario) {
    }
    public record Ingreso(String fecha, String codigo, BigDecimal monto, String metodo, String usuario) {
    }

    public List<Cliente> clientes() throws SQLException {
        List<Cliente> r = new ArrayList<>();
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c.prepareStatement(
            "SELECT id,documento,nombre,telefono,correo,direccion FROM clientes ORDER BY nombre");
            ResultSet x = p.executeQuery()) {
            while (x.next())
                r.add(new Cliente(x.getInt(1), x.getString(2), x.getString(3),
                    x.getString(4), x.getString(5), x.getString(6)));
        }
        return r;
    }

    public void guardarCliente(Cliente a, String usuario) throws SQLException {
        if (a.documento() == null || a.documento().isBlank() || a.documento().length() > 20
            || a.nombre() == null || a.nombre().isBlank() || a.nombre().length() > 120)
            throw new IllegalArgumentException("Documento (max.20) y nombre (max.120) son obligatorios.");
        ValidacionLogistica.cliente(a);
        if (a.telefono().length() > 30 || a.correo().length() > 120 || a.direccion().length() > 200)
            throw new IllegalArgumentException("Uno de los datos del cliente es demasiado largo.");
        String sql = a.id() == 0
            ? "INSERT INTO clientes (documento,nombre,telefono,correo,direccion) VALUES(?,?,?,?,?)"
        : "UPDATE clientes SET documento=?,nombre=?,telefono=?,correo=?,direccion=? WHERE id=?";
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            PermisosDAO.exigir(c, usuario, false);
            p.setString(1, a.documento().trim());
            p.setString(2, a.nombre().trim());
            p.setString(3, a.telefono().trim());
            p.setString(4, a.correo().trim());
            p.setString(5, a.direccion().trim());
            if (a.id() != 0)
                p.setInt(6, a.id());
            if (p.executeUpdate() != 1)
                throw new SQLException("No se guardo el cliente.");
        }
    }

    public void eliminarCliente(int id, String usuario) throws SQLException {
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                PermisosDAO.exigir(c, usuario, false);
                try (PreparedStatement p = c.prepareStatement("SELECT id FROM clientes WHERE id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet x = p.executeQuery()) {
                        if (!x.next())
                            throw new IllegalArgumentException("Cliente no encontrado.");
                    }
                }
                try (PreparedStatement p = c
                    .prepareStatement("SELECT pedido_id FROM detalles_envio WHERE cliente_id=? LIMIT 1 FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet x = p.executeQuery()) {
                        if (x.next())
                            throw new IllegalArgumentException("No se elimina un cliente con historial de envíos.");
                    }
                }
                try (PreparedStatement p = c.prepareStatement("DELETE FROM clientes WHERE id=?")) {
                    p.setInt(1, id);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Los abonos se agregan para que se vea el saldo en el listado de envios. */
    private static final String CONSULTA_ENVIOS = """
            SELECT p.id,COALESCE(d.cliente_id,0) cliente_id,
              COALESCE(d.codigo,CONCAT('IF-',LPAD(p.id,8,'0'))) codigo,
              p.cliente,p.producto,p.pais_origen,p.cantidad,p.prioridad,p.estado,
              COALESCE(d.destinatario,p.cliente) destinatario,
              COALESCE(d.telefono_destinatario,'') telefono,
              COALESCE(d.ciudad_destino,'POR COMPLETAR') destino,
              COALESCE(d.direccion_destino,'') direccion,
              COALESCE(d.peso_kg,1) peso,COALESCE(d.tarifa,0) tarifa,
              DATE_FORMAT(p.fecha_creacion,'%Y-%m-%d %H:%i') fecha,
              COALESCE(d.usuario_registro,'LEGADO') registrador,
              COALESCE((SELECT SUM(i.monto) FROM ingresos i WHERE i.pedido_id=p.id),0) abonado
            FROM pedidos p LEFT JOIN detalles_envio d ON d.pedido_id=p.id
            ORDER BY p.id DESC
            """;

    public List<Envio> envios() throws SQLException {
        List<Envio> r = new ArrayList<>();
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c.prepareStatement(CONSULTA_ENVIOS);
            ResultSet x = p.executeQuery()) {
            while (x.next())
                r.add(new Envio(x.getInt("id"), x.getInt("cliente_id"),
                    x.getString("codigo"), x.getString("cliente"), x.getString("producto"),
                    x.getString("pais_origen"), x.getInt("cantidad"), x.getString("prioridad"),
                    x.getString("estado"), x.getString("destinatario"), x.getString("telefono"),
                    x.getString("destino"), x.getString("direccion"), x.getDouble("peso"),
                    x.getBigDecimal("tarifa"), x.getString("fecha"), x.getString("registrador"),
                    x.getBigDecimal("abonado")));
        }
        return r;
    }

    public int recibir(Cliente cliente, String contenido, String origen, int cantidad, String prioridad,
        String destinatario, String telefono, String destino, String direccion,
        double peso, BigDecimal tarifa, String usuario) throws SQLException {
        if (cliente == null || cliente.id() <= 0)
            throw new IllegalArgumentException("Primero registra y selecciona un cliente.");
        if (vacio(contenido, 120) || vacio(origen, 80) || vacio(destinatario, 120) || vacio(destino, 120))
            throw new IllegalArgumentException("Completa contenido, origen, destinatario y destino; revisa su longitud.");
        if (cantidad <= 0 || cantidad > 99999 || !Double.isFinite(peso) || peso <= 0 || peso > 999999
            || tarifa == null || tarifa.signum() < 0 || tarifa.compareTo(new BigDecimal("9999999")) > 0 || tarifa
            .scale() > 2)
            throw new IllegalArgumentException("Cantidad, peso o tarifa invalidos.");
        ValidacionLogistica.entrega(telefono, direccion);
        if (telefono.length() > 30 || direccion.length() > 200)
            throw new IllegalArgumentException("Telefono o direccion muy largos.");
        if (!List.of("NORMAL", "URGENTE").contains(prioridad))
            throw new IllegalArgumentException("Prioridad invalida.");
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                PermisosDAO.exigir(c, usuario, false);
                int id;
                try (PreparedStatement p = c
                    .prepareStatement("INSERT INTO pedidos(cliente,producto,pais_origen,cantidad,prioridad) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, cliente.nombre());
                    p.setString(2, contenido.trim());
                    p.setString(3, origen.trim());
                    p.setInt(4, cantidad);
                    p.setString(5, prioridad);
                    p.executeUpdate();
                    try (ResultSet k = p.getGeneratedKeys()) {
                        if (!k.next())
                            throw new SQLException("MySQL no devolvio el ID");
                        id = k.getInt(1);
                    }
                }
                try (PreparedStatement p = c
                    .prepareStatement("INSERT INTO detalles_envio(pedido_id,codigo,cliente_id,destinatario,telefono_destinatario,ciudad_destino,direccion_destino,peso_kg,tarifa,usuario_registro) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                    p.setInt(1, id);
                    p.setString(2, codigo(id));
                    p.setInt(3, cliente.id());
                    p.setString(4, destinatario.trim());
                    p.setString(5, telefono.trim());
                    p.setString(6, destino.trim());
                    p.setString(7, direccion.trim());
                    p.setDouble(8, peso);
                    p.setBigDecimal(9, tarifa);
                    p.setString(10, usuario);
                    p.executeUpdate();
                }
                guardarMovimiento(c, id, null, "PENDIENTE", "Recepcion de paquete", usuario);
                c.commit();
                return id;
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se)
                    throw se;
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private boolean vacio(String s, int max) {
        return s == null || s.isBlank() || s.length() > max;
    }

    public static String codigo(int id) {
        return String.format("IF-%08d", id);
    }

    /** Actualizacion de datos permitida solo antes del procesamiento. */
    public void actualizarEnvio(int id, Cliente cliente, String contenido, String origen, int cantidad,
        String prioridad, String destinatario, String telefono, String destino, String direccion, double peso,
        BigDecimal tarifa, String usuario) throws SQLException {
        if (cliente == null || cliente.id() <= 0 || vacio(contenido, 120) || vacio(origen, 80)
            || vacio(destinatario, 120) || vacio(destino, 120)
            || cantidad <= 0 || cantidad > 99999 || peso <= 0 || peso > 999999 || !Double.isFinite(peso)
            || tarifa == null || tarifa.signum() < 0 || tarifa.compareTo(new BigDecimal("9999999")) > 0 || tarifa
            .scale() > 2
            || telefono.length() > 30 || direccion.length() > 200 || !List.of("NORMAL", "URGENTE")
            .contains(prioridad))
            throw new IllegalArgumentException("Revisa datos obligatorios, cantidades y longitudes.");
        ValidacionLogistica.entrega(telefono, direccion);
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                PermisosDAO.exigir(c, usuario, false);
                try (PreparedStatement p = c
                    .prepareStatement("UPDATE pedidos SET cliente=?,producto=?,pais_origen=?,cantidad=?,prioridad=? WHERE id=? AND estado='PENDIENTE'")) {
                    p.setString(1, cliente.nombre());
                    p.setString(2, contenido.trim());
                    p.setString(3, origen.trim());
                    p.setInt(4, cantidad);
                    p.setString(5, prioridad);
                    p.setInt(6, id);
                    if (p.executeUpdate() != 1)
                        throw new IllegalArgumentException("Solo puedes editar un paquete pendiente.");
                }
                try (PreparedStatement p = c
                    .prepareStatement("UPDATE detalles_envio SET cliente_id=?,destinatario=?,telefono_destinatario=?,ciudad_destino=?,direccion_destino=?,peso_kg=?,tarifa=? WHERE pedido_id=? AND ? >= (SELECT COALESCE(SUM(i.monto),0) FROM ingresos i WHERE i.pedido_id=?)")) {
                    p.setInt(1, cliente.id());
                    p.setString(2, destinatario.trim());
                    p.setString(3, telefono.trim());
                    p.setString(4, destino.trim());
                    p.setString(5, direccion.trim());
                    p.setDouble(6, peso);
                    p.setBigDecimal(7, tarifa);
                    p.setInt(8, id);
                    p.setBigDecimal(9, tarifa);
                    p.setInt(10, id);
                    if (p.executeUpdate() != 1)
                        throw new IllegalArgumentException("No se pudo guardar el detalle del paquete (verifica el saldo abonado).");
                }
                guardarMovimiento(c, id, "PENDIENTE", "PENDIENTE", "Datos del envío modificados", usuario);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se)
                    throw se;
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Seleccion FIFO por prioridad usando nodos propios, con estado e historial atomicos. */
    public Pedido atenderSiguiente(String usuario) throws SQLException {
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                PermisosDAO.exigir(c, usuario, false);
                ColaPrioridadPedidos cola = new ColaPrioridadPedidos();
                // Lectura bloqueante: dos operadores no pueden atender el mismo paquete.
                String sql = "SELECT id,cliente,producto,pais_origen,cantidad,prioridad,estado "
                + "FROM pedidos WHERE estado='PENDIENTE' ORDER BY id FOR UPDATE";
                try (PreparedStatement p = c.prepareStatement(sql); ResultSet x = p.executeQuery()) {
                    while (x.next())
                        cola.encolar(new Pedido(x.getInt(1), x.getString(2),
                            x.getString(3), x.getString(4), x.getInt(5), x.getString(6), x.getString(7)));
                }
                Pedido siguiente = cola.desencolar();
                if (siguiente != null) {
                    try (PreparedStatement p = c.prepareStatement(
                        "UPDATE pedidos SET estado='EN_PROCESO' WHERE id=? AND estado='PENDIENTE'")) {
                        p.setInt(1, siguiente.getId());
                        if (p.executeUpdate() != 1)
                            throw new SQLException("El paquete ya no esta pendiente.");
                    }
                    guardarMovimiento(c, siguiente.getId(), "PENDIENTE", "EN_PROCESO",
                        "Atendido desde cola de prioridad", usuario);
                }
                c.commit();
                return siguiente;
            } catch (SQLException | RuntimeException ex) {
                try {
                    c.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }
                throw ex;
            }
        }
    }

    public void cambiarEstado(int id, String nuevo, String observacion, String usuario) throws SQLException {
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                PermisosDAO.exigir(c, usuario, false);
                String anterior;
                try (PreparedStatement p = c.prepareStatement("SELECT estado FROM pedidos WHERE id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet x = p.executeQuery()) {
                        if (!x.next())
                            throw new IllegalArgumentException("Paquete no encontrado.");
                        anterior = x.getString(1);
                    }
                }
                boolean valido = ValidacionLogistica.transicionManual(anterior, nuevo);
                if ("EN_PROCESO".equals(nuevo))
                    throw new IllegalArgumentException("Usa Atender siguiente (prioridad) para iniciar un envío.");
                if (!valido)
                    throw new IllegalArgumentException("Transicion no valida: " + anterior + " -> " + nuevo);
                // Sin modulo de devoluciones, no debemos cancelar un envio que ya tiene cobros.
                if ("CANCELADO".equals(nuevo)) {
                    try (PreparedStatement p = c
                        .prepareStatement("SELECT COALESCE(SUM(monto),0) FROM ingresos WHERE pedido_id=?")) {
                        p.setInt(1, id);
                        try (ResultSet x = p.executeQuery()) {
                            x.next();
                            if (x.getBigDecimal(1).signum() > 0)
                                throw new IllegalArgumentException("No se puede cancelar un paquete con abonos: primero se requiere una devolucion.");
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE pedidos SET estado=? WHERE id=?")) {
                    p.setString(1, nuevo);
                    p.setInt(2, id);
                    p.executeUpdate();
                }
                guardarMovimiento(c, id, anterior, nuevo, observacion, usuario);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se)
                    throw se;
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void guardarMovimiento(Connection c, int id, String anterior, String nuevo, String observacion,
        String usuario) throws SQLException {
        try (PreparedStatement p = c
            .prepareStatement("INSERT INTO movimientos(pedido_id,estado_anterior,estado_nuevo,observacion,usuario) VALUES(?,?,?,?,?)")) {
            p.setInt(1, id);
            p.setString(2, anterior);
            p.setString(3, nuevo);
            p.setString(4, observacion == null ? "" : observacion.trim());
            p.setString(5, usuario);
            p.executeUpdate();
        }
    }

    public List<Movimiento> movimientos(int id) throws SQLException {
        List<Movimiento> r = new ArrayList<>();
        String sql = "SELECT DATE_FORMAT(m.fecha,'%Y-%m-%d %H:%i'),COALESCE(d.codigo,CONCAT('IF-',LPAD(m.pedido_id,8,'0'))),COALESCE(m.estado_anterior,'—'),m.estado_nuevo,m.observacion,m.usuario FROM movimientos m LEFT JOIN detalles_envio d ON d.pedido_id=m.pedido_id WHERE (?=0 OR m.pedido_id=?) ORDER BY m.id DESC";
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.setInt(2, id);
            try (ResultSet x = p.executeQuery()) {
                while (x.next())
                    r.add(new Movimiento(x.getString(1), x.getString(2), x.getString(3), x.getString(4), x
                        .getString(5), x.getString(6)));
            }
        }
        return r;
    }

    /** Abonos parciales estan permitidos, pero nunca exceden la tarifa ni se admiten en cancelados. */
    public void registrarIngreso(int id, BigDecimal monto, String metodo, String usuario) throws SQLException {
        if (monto == null || monto.signum() <= 0 || monto.scale() > 2 || !List.of("EFECTIVO", "TARJETA",
            "TRANSFERENCIA", "YAPE", "PLIN").contains(metodo))
            throw new IllegalArgumentException("Monto o metodo de pago invalidos.");
        try (Connection c = ConexionBD.conectar()) {
            c.setAutoCommit(false);
            try {
                PermisosDAO.exigir(c, usuario, false);
                BigDecimal tarifa, abonado;
                String estado;
                try (PreparedStatement p = c
                    .prepareStatement("SELECT p.estado,d.tarifa,(SELECT COALESCE(SUM(i.monto),0) FROM ingresos i WHERE i.pedido_id=p.id) FROM pedidos p JOIN detalles_envio d ON d.pedido_id=p.id WHERE p.id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet x = p.executeQuery()) {
                        if (!x.next())
                            throw new IllegalArgumentException("Paquete sin detalle.");
                        estado = x.getString(1);
                        tarifa = x.getBigDecimal(2);
                        abonado = x.getBigDecimal(3);
                    }
                }
                if ("CANCELADO".equals(estado))
                    throw new IllegalArgumentException("No se registra un pago a paquetes cancelados.");
                if (monto.compareTo(tarifa.subtract(abonado)) > 0)
                    throw new IllegalArgumentException("El abono supera el saldo S/ " + tarifa.subtract(abonado)
                        .toPlainString());
                try (PreparedStatement p = c
                    .prepareStatement("INSERT INTO ingresos(pedido_id,monto,metodo,usuario) VALUES(?,?,?,?)")) {
                    p.setInt(1, id);
                    p.setBigDecimal(2, monto);
                    p.setString(3, metodo);
                    p.setString(4, usuario);
                    p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException se)
                    throw se;
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Resumen global: no limita el historial a los ultimos 500 registros. */
    public Map<String, BigDecimal> ingresosPorMetodo() throws SQLException {
        return ingresosPorMetodo(null, null);
    }

    public Map<String, BigDecimal> ingresosPorMetodo(LocalDate desde, LocalDate hasta) throws SQLException {
        Map<String, BigDecimal> r = new LinkedHashMap<>();
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c.prepareStatement(
            "SELECT metodo,COALESCE(SUM(monto),0) FROM ingresos WHERE (? IS NULL OR fecha>=?) AND (? IS NULL OR fecha<DATE_ADD(?,INTERVAL 1 DAY)) GROUP BY metodo ORDER BY metodo")) {
            for (int i = 1; i <= 4; i++) {
                LocalDate fecha = i <= 2 ? desde : hasta;
                if (fecha == null)
                    p.setNull(i, Types.DATE);
                else
                    p.setDate(i, java.sql.Date.valueOf(fecha));
            }
            try (ResultSet x = p.executeQuery()) {
                while (x.next())
                    r.put(x.getString(1), x.getBigDecimal(2));
            }
        }
        return r;
    }

    public List<Ingreso> ingresosPorCodigo(String codigo) throws SQLException {
        List<Ingreso> r = new ArrayList<>();
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c
            .prepareStatement("SELECT DATE_FORMAT(i.fecha,'%Y-%m-%d %H:%i'),d.codigo,i.monto,i.metodo,i.usuario FROM ingresos i JOIN detalles_envio d ON d.pedido_id=i.pedido_id WHERE d.codigo=? ORDER BY i.id DESC")) {
            p.setString(1, codigo);
            try (ResultSet x = p.executeQuery()) {
                while (x.next())
                    r.add(new Ingreso(x.getString(1), x.getString(2), x.getBigDecimal(3), x.getString(4), x
                        .getString(5)));
            }
        }
        return r;
    }

    public List<Ingreso> ingresos() throws SQLException {
        List<Ingreso> r = new ArrayList<>();
        try (Connection c = ConexionBD.conectar(); PreparedStatement p = c.prepareStatement(
            "SELECT DATE_FORMAT(i.fecha,'%Y-%m-%d %H:%i'),d.codigo,i.monto,i.metodo,i.usuario FROM ingresos i JOIN detalles_envio d ON d.pedido_id=i.pedido_id ORDER BY i.id DESC"); ResultSet x = p.executeQuery()) {
            while (x.next())
                r.add(new Ingreso(x.getString(1), x.getString(2), x.getBigDecimal(3), x.getString(4), x
                    .getString(5)));
        }
        return r;
    }
}
