package com.importaflow;
import com.importaflow.modelo.*;
import com.importaflow.servicio.ValidacionLogistica;
import com.importaflow.vista.LaboratorioAED;
import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

/** Validaciones y recorrido de listas reproducibles sin acceso a MySQL. */
public class PruebaFase3 {
    static int total;

    static void ok(boolean c, String m) {
        if (!c)
            throw new AssertionError(m);
        total++;
    }

    static void invalido(Runnable r, String m) {
        try {
            r.run();
        } catch (IllegalArgumentException e) {
            ok(true, m);
            return;
        }
        throw new AssertionError(m);
    }

    static JButton boton(Container p, String texto) {
        for (Component c : p.getComponents()) {
            if (c instanceof JButton b && b.getText().equals(texto))
                return b;
            if (c instanceof Container k) {
                JButton b = boton(k, texto);
                if (b != null)
                    return b;
            }
        }
        return null;
    }

    static JTextArea area(Container p) {
        for (Component c : p.getComponents()) {
            if (c instanceof JTextArea a)
                return a;
            if (c instanceof Container k) {
                JTextArea a = area(k);
                if (a != null)
                    return a;
            }
        }
        return null;
    }

    static JTabbedPane tabs(Container p) {
        for (Component c : p.getComponents()) {
            if (c instanceof JTabbedPane a)
                return a;
            if (c instanceof Container k) {
                JTabbedPane a = tabs(k);
                if (a != null)
                    return a;
            }
        }
        return null;
    }

    static void click(Container p, String s) {
        JButton b = boton(p, s);
        if (b == null)
            throw new AssertionError(s);
        b.doClick(0);
    }

    static Envio envio(int id) {
        return new Envio(id, 1, "IF-" + id, "Cliente", "Contenido", "Lima", id, "NORMAL", "PENDIENTE", "Destino",
            "900000000", "Cusco", "Calle 1", 1, 20, "2026-11-24 10:00", "admin", 0);
    }

    public static void main(String[] args) throws Exception {
        ValidacionLogistica.cliente(new Cliente(0, "12345678", "Cliente", "900000000", "persona@correo.test",
            "Jr. Junín"));
        ok(true, "DNI válido");
        ValidacionLogistica.cliente(new Cliente(0, "20123456789", "Empresa", "", "", ""));
        ok(true, "RUC válido");
        invalido(() -> ValidacionLogistica.cliente(new Cliente(0, "123", "Cliente", "", "", "")), "DNI inválido");
        invalido(() -> ValidacionLogistica.cliente(new Cliente(0, "12345678", "Cliente", "", "correo", "")),
            "correo inválido");
        invalido(() -> ValidacionLogistica.entrega("123", "Calle 1"), "teléfono inválido");
        invalido(() -> ValidacionLogistica.entrega("900000000", " "), "dirección obligatoria");
        ok(ValidacionLogistica.monto("19,90").equals(new BigDecimal("19.90")), "importe decimal exacto");
        invalido(() -> ValidacionLogistica.monto("1.005"), "más de dos decimales");
        invalido(() -> ValidacionLogistica.monto("0"), "monto cero");
        invalido(() -> ValidacionLogistica.monto("-5"), "monto negativo");
        LocalDate dia = LocalDate.of(2026, 11, 24);
        ok(ValidacionLogistica.enPeriodo("2026-11-24 23:59", dia, dia), "día final incluido");
        ok(!ValidacionLogistica.enPeriodo("2026-11-25 00:00", dia, dia), "día posterior excluido");
        List<Envio> origen = List.of(envio(3), envio(1), envio(2));
        SwingUtilities.invokeAndWait(() -> {
            LaboratorioAED lab = new LaboratorioAED(() -> origen, null);
            JTabbedPane t = tabs(lab);
            JTabbedPane sub = tabs((Container) t.getComponentAt(3));
            for (int i = 0; i < 2; i++) {
                Container p = (Container) sub.getComponentAt(i);
                JTextArea msg = area(p);
                click(p, "Recorrer adelante");
                ok(msg.getText().contains("vacía"), "recorrido vacío");
                click(p, "Cargar pedidos");
                click(p, "Recorrer adelante");
                ok(msg.getText().contains("Visitando P1"), "primer nodo");
                click(p, "Pausar recorrido");
                click(p, "Paso siguiente");
                ok(msg.getText().contains("P1 → P2"), "segundo nodo");
                click(p, "Paso siguiente");
                ok(msg.getText().contains("P1 → P2 → P3 → NULL"), "secuencia completa");
                if (i == 1) {
                    click(p, "Recorrer atrás");
                    ok(msg.getText().contains("Visitando P3"), "último nodo");
                    click(p, "Paso siguiente");
                    click(p, "Paso siguiente");
                    ok(msg.getText().contains("P3 → P2 → P1 → NULL"), "sentido inverso");
                }
                click(p, "Vaciar copia");
                ok(msg.getText().contains("Nodos: 0"), "vaciar");
            }
        });
        ok(origen.size() == 3, "datos de origen intactos");
        System.out.println("OK: " + total + " comprobaciones de fase 3.");
    }
}
