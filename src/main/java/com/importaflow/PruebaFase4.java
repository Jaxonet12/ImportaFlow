package com.importaflow;
import com.importaflow.modelo.*;
import com.importaflow.dao.SistemaDAO;
import com.importaflow.seguridad.ClaveSegura;
import com.importaflow.config.ConexionBD;
import com.importaflow.vista.VentanaSistema;
import java.util.*;
import java.nio.file.*;
import java.awt.*;
import javax.swing.*;
import java.util.regex.*;

/** Prueba de arranque gráfico con datos simulados; no inicia sesión ni conecta a MySQL. */
public class PruebaFase4 {
    static int total;

    static void ok(boolean cond, String m) {
        if (!cond)
            throw new AssertionError(m);
        total++;
        System.out.println("OK - " + m);
    }

    static JButton boton(Container c, String t) {
        for (Component x : c.getComponents()) {
            if (x instanceof JButton b && b.getText().equals(t))
                return b;
            if (x instanceof Container y) {
                JButton b = boton(y, t);
                if (b != null)
                    return b;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String sql = Files.readString(Path.of("database/IMPORTAFLOW_31_INSTALACION.sql"));
        Matcher m = Pattern.compile("pbkdf2\\$160000\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+").matcher(sql);
        for (String p : new String[]{"Importa2026!", "Operador2026!", "Consulta2026!"}) {
            ok(m.find(), "hash de cuenta incluido");
            ok(ClaveSegura.verificar(p.toCharArray(), m.group()), "hash válido para cuenta de práctica");
        }
        ok(!m.find(), "exactamente tres cuentas de práctica");
        ok(!sql.toUpperCase(Locale.ROOT).contains("DROP DATABASE"), "sin borrado de bases");
        ok(sql.contains("USE importaflow_final;"), "base independiente");
        Path f = Files.createTempFile("if-config-", ".properties");
        String old = System.getProperty("importaflow.config");
        try {
            Files.writeString(f, "db.user=prueba_local\n");
            System.setProperty("importaflow.config", f.toString());
            if (System.getenv("IMPORTAFLOW_DB_USER") == null)
                ok(ConexionBD.configuracion().getProperty("db.user").equals("prueba_local"),
                    "config local recargable");
            Files.writeString(f, "db.user=segunda_prueba\n");
            if (System.getenv("IMPORTAFLOW_DB_USER") == null)
                ok(ConexionBD.configuracion().getProperty("db.user").equals("segunda_prueba"),
                    "cambio sin reiniciar JVM");
        } finally {
            if (old == null)
                System.clearProperty("importaflow.config");
            else
                System.setProperty("importaflow.config", old);
            Files.deleteIfExists(f);
        }
        if (!GraphicsEnvironment.isHeadless()) {
            var cl = new Cliente(1, "99000001", "Cliente de prueba", "900000001", "cliente@correo.invalid",
                "Av. Junín 202");
            var e = new Envio(1, 1, "IF-00000001", cl.nombre(), "Libros", "Lima", 3, "URGENTE", "PENDIENTE",
                "Destinatario", "900001001", "Cusco", "Jr. Lima 2", 2, 40, "2026-11-24 10:00", "operador", 0);
            SwingUtilities.invokeAndWait(() -> {
                for (String rol : new String[]{"ADMIN", "OPERADOR", "CONSULTA"})
                    for (boolean vacia : new boolean[]{false, true}) {
                        Usuario u = new Usuario(1, "prueba", "Prueba", rol, true);
                        VentanaSistema v = null;
                        try {
                            var datos = new VentanaSistema.DatosIniciales(vacia
                                ? java.util.List.of() : java.util.List.of(cl), vacia
                                ? java.util.List.of() : java.util.List.of(e), java.util.List.of(),
                                java.util.List.of(u), java.util.List.of(), Map.of());
                            v = new VentanaSistema(u, null, datos);
                            v.setVisible(true);
                            ok(v.isShowing(), "ventana principal visible " + rol + " / " + (vacia
                                ? "vacía" : "con datos"));
                            for (String menu : new String[]{"Panel principal", "Clientes", "Recepcion de paquetes",
                                "Gestion y seguimiento", "Ingresos y cobros", "Reportes",
                                "Algoritmos y estructuras"}) {
                                JButton b = boton(v, menu);
                                ok(b != null, "menú " + menu);
                                b.doClick(0);
                            }
                            ok((boton(v, "Usuarios y roles") != null) == u.administra(),
                                "visibilidad de gestión usuarios");
                            ok(boton(v, "Guardar cliente").isEnabled() == u.puedeEditar(), "permiso de edición");
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        } finally {
                            if (v != null)
                                v.dispose();
                        }
                    }
            });
        } else
            System.out.println("Sin pantalla: se omiten comprobaciones JFrame. Usa escritorio o Xvfb.");
        System.out.println("OK: " + total + " comprobaciones de fase 4.");
    }
}
