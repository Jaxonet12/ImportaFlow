package com.importaflow;
import com.importaflow.algoritmos.ArbolAVL;
import com.importaflow.modelo.Envio;
import com.importaflow.servicio.ValidacionLogistica;
import com.importaflow.vista.LaboratorioAED;
import java.math.BigDecimal;
import java.awt.*;
import javax.swing.*;
import java.util.List;
import java.nio.file.*;
/** Regresión de la auditoría, sin acceso a datos persistentes. */
public class PruebaFinal31 {
    private static int n;

    static void ok(boolean b, String nombre) {
        if (!b)
            throw new AssertionError(nombre);
        n++;
        System.out.println("OK - " + nombre);
    }

    static < T extends Component > T buscar(Container c, Class<T> tipo) {
        for (Component x : c.getComponents()) {
            if (tipo.isInstance(x))
                return tipo.cast(x);
            if (x instanceof Container k) {
                T y = buscar(k, tipo);
                if (y != null)
                    return y;
            }
        }
        return null;
    }

    static JButton boton(Container c, String texto) {
        for (Component x : c.getComponents()) {
            if (x instanceof JButton b && b.getText().equals(texto))
                return b;
            if (x instanceof Container k) {
                JButton y = boton(k, texto);
                if (y != null)
                    return y;
            }
        }
        return null;
    }

    static boolean contiene(Container c, String texto) {
        for (Component x : c.getComponents()) {
            if (x instanceof JTextArea a && a.getText().contains(texto))
                return true;
            if (x instanceof Container k && contiene(k, texto))
                return true;
        }
        return false;
    }

    static void click(Container c, String texto) {
        JButton b = boton(c, texto);
        if (b == null)
            throw new AssertionError("Falta botón " + texto);
        b.doClick(0);
    }

    public static void main(String[] args) throws Exception {
        int[][] casos = {{30, 20, 10},{10, 20, 30},{30, 10, 20},{10, 30, 20}};
        for (int i = 0; i < 4; i++) {
            ArbolAVL a = new ArbolAVL();
            for (int x : casos[i])
                a.insertar(x);
            var p = a.pasos().get(0);
            ok(p.antes().id == casos[i][0] && p.antes().destinatario.contains(i == 0 || i == 2 ? "FE=2" : "FE=-2"),
                "captura desequilibrada real caso " + i);
            ok((p.intermedio() != null) == (i >= 2), "paso intermedio solo en dobles " + i);
            if (i == 2)
                ok(p.intermedio().izq.id == 20 && p.intermedio().izq.izq.id == 10, "LR primera rotación izquierda");
            if (i == 3)
                ok(p.intermedio().der.id == 20 && p.intermedio().der.der.id == 30, "RL primera rotación derecha");
            ok(p.despues().id == 20 && a.valido(), "equilibrio final " + i);
        }
        ok(!ValidacionLogistica.transicionManual("PENDIENTE", "EN_PROCESO"), "no se salta prioridad");
        ok(ValidacionLogistica.transicionManual("EN_PROCESO", "EN_TRANSITO"), "tránsito permitido");
        ok(ValidacionLogistica.transicionManual("EN_TRANSITO", "ENTREGADO"), "entrega permitida");
        ok(!ValidacionLogistica.transicionManual("ENTREGADO", "CANCELADO"), "no cancela entregado");
        ok(ValidacionLogistica.transicionManual("PENDIENTE", "CANCELADO"), "cancelación pendiente permitida");
        Envio e = new Envio(1, 1, "IF-1", "Cliente", "Libros", "Lima", 1, "NORMAL", "PENDIENTE", "Destinatario",
            "900000000", "Cusco", "Calle", 1, new BigDecimal("0.30"), "", "admin", new BigDecimal("0.10"));
        ok(e.saldo().equals(new BigDecimal("0.20")), "saldo decimal exacto");
        ok(ValidacionLogistica.tarifa("0").equals(new BigDecimal("0.00")), "tarifa gratuita válida");
        ok(ValidacionLogistica.tarifa("32,10").equals(new BigDecimal("32.10")), "coma decimal");
        for (String valor : new String[]{"1.001", "-1", "NaN"}) {
            boolean error = false;
            try {
                ValidacionLogistica.tarifa(valor);
            } catch (IllegalArgumentException ex) {
                error = true;
            }
            ok(error, "tarifa inválida rechazada: " + valor);
        }
        String sql = Files.readString(Path.of("database/IMPORTAFLOW_31_INSTALACION.sql"));
        ok(sql.contains("EXIT HANDLER FOR SQLEXCEPTION") && sql.contains("ROLLBACK;") && sql.contains("RESIGNAL;"),
            "protección de carga SQL (inspección estática)");
        SwingUtilities.invokeAndWait(() -> {
            LaboratorioAED lab = new LaboratorioAED(() -> List.of(e), null);
            JTabbedPane tabs = (JTabbedPane) lab.getComponent(0);
            Container avl = (Container) tabs.getComponentAt(7);
            JComboBox<?> combo = buscar(avl, JComboBox.class);
            for (int i = 0; i < 4; i++) {
                combo.setSelectedIndex(i);
                click(avl, "Ejecutar caso");
                ok(contiene(avl, "ANTES:"), "UI caso " + i + " inicia antes");
                click(avl, "Siguiente paso");
                if (i >= 2) {
                    ok(contiene(avl, "INTERMEDIO:"), "UI rotación doble intermedia " + i);
                    click(avl, "Siguiente paso");
                }
                ok(contiene(avl, "DESPUÉS:"), "UI caso " + i + " termina después");
                click(avl, "Ver AVL completo");
                ok(contiene(avl, "AVL válido: true"), "UI equilibrio " + i);
            }
        });
        System.out.println("OK: " + n + " comprobaciones finales 3.1.");
    }
}
