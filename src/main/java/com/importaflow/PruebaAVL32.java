package com.importaflow;

import com.importaflow.algoritmos.ArbolAVL;
import com.importaflow.algoritmos.ArbolBinarioEnvios;
import com.importaflow.modelo.Envio;
import com.importaflow.vista.LaboratorioAED;
import com.importaflow.vista.PanelArbol;
import java.awt.Container;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import javax.swing.*;

/** Comprueba que las acciones del recorrido y del historial nunca se mezclan. */
public final class PruebaAVL32 {
    private static int n;

    private static void ok(boolean b, String m) {
        if (!b)
            throw new AssertionError(m);
        n++;
        System.out.println("OK - " + m);
    }

    private static void click(Container c, String t) {
        PruebaFinal31.click(c, t);
    }

    private static boolean contiene(Container c, String t) {
        return PruebaFinal31.contiene(c, t);
    }

    private static JButton boton(Container c, String t) {
        return PruebaFinal31.boton(c, t);
    }

    private static Object campo(PanelArbol v, String nombre) {
        try {
            var f = PanelArbol.class.getDeclaredField(nombre);
            f.setAccessible(true);
            return f.get(v);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static int contar(ArbolBinarioEnvios.NodoVisual v) {
        return v == null ? 0 : 1 + contar(v.izq) + contar(v.der);
    }

    private static Envio envio(int id) {
        return new Envio(id, 1, "IF-" + id, "Cliente", "Libros", "Lima", 1, "NORMAL", "PENDIENTE", "Destino",
            "900000000", "Cusco", "Calle", 1, 20, "", "admin", 0);
    }

    public static void main(String[] args) throws Exception {
        // IDs distintos de los ejercicios LL/RR/LR/RL para detectar cualquier fuga de estado.
        List<Envio> registros = IntStream.rangeClosed(101, 120).mapToObj(PruebaAVL32::envio).toList();
        ArbolAVL esperado = new ArbolAVL();
        registros.forEach(e -> esperado.insertar(e.id()));
        Container[] panel = new Container[1];
        SwingUtilities.invokeAndWait(() -> {
            var lab = new LaboratorioAED(() -> registros, null);
            var tabs = (JTabbedPane) lab.getComponent(0);
            var avl = (Container) tabs.getComponentAt(7);
            panel[0] = avl;
            var v = PruebaFinal31.buscar(avl, PanelArbol.class);
            var combo = PruebaFinal31.buscar(avl, JComboBox.class);
            combo.setSelectedIndex(2);
            click(avl, "Ejecutar caso");
            click(avl, "Siguiente paso");
            ok(contiene(avl, "INTERMEDIO:"), "ejercicio LR independiente");
            click(avl, "Cargar IDs MySQL");
            ok(contiene(avl, "AVL válido: true"), "carga abre AVL completo válido");
            ok(contar((ArbolBinarioEnvios.NodoVisual) campo(v, "raiz")) == 20, "veinte nodos en el dibujo");
            ok(!boton(avl, "Siguiente paso").isEnabled() && !boton(avl, "Siguiente rotación").isEnabled(),
                "historial no invade vista completa");
            combo.setSelectedIndex(0);
            // Elegir un ejercicio no ejecuta ni cambia el árbol.
            for (String t : List.of("Preorden", "Inorden", "Postorden")) {
                List<Integer> ruta = esperado.idsRecorrido(t);
                click(avl, t);
                ok(contiene(avl, t + " | Paso 1 de 20") && campo(v, "resaltado").equals(ruta.get(0)), t
                    + ": primer ID real resaltado");
                ok(!boton(avl, "Siguiente paso").isEnabled() && !boton(avl, "Siguiente rotación").isEnabled(), t
                    + ": controles de rotación desactivados");
                click(avl, "Siguiente nodo");
                ok(campo(v, "resaltado").equals(ruta.get(1)) && contiene(avl, "Paso 2 de 20"), t
                    + ": siguiente nodo real");
                click(avl, "Nodo anterior");
                ok(campo(v, "resaltado").equals(ruta.get(0)), t + ": retroceso");
                click(avl, "Recorrido completo");
                ok(contiene(avl, "Visitados: " + ruta) && ((Set<?>) campo(v, "visitados")).size() == 20, t
                    + ": secuencia completa correcta");
                ok(contar((ArbolBinarioEnvios.NodoVisual) campo(v, "raiz")) == 20, t + ": conserva árbol completo");
            }
            click(avl, "Ver rotaciones");
            ok(contiene(avl, "Inserción del ID 103") && contiene(avl, "ANTES:"),
                "rotación real de la primera inserción que desequilibra");
            click(avl, "Siguiente paso");
            ok(contiene(avl, "DESPUÉS:"), "rotación MySQL después");
            click(avl, "Siguiente rotación");
            ok(contiene(avl, "Rotación 2 de") && contiene(avl, "Inserción del ID 105"),
                "siguiente rotación pertenece a MySQL");
            click(avl, "Inorden");
            ok(contiene(avl, "Paso 1 de 20") && !boton(avl, "Siguiente rotación").isEnabled(),
                "vuelve al recorrido real desde una rotación");
            click(avl, "Automático / pausa");
        });
        Thread.sleep(800);
        SwingUtilities.invokeAndWait(() -> {
            var avl = panel[0];
            ok(!contiene(avl, "Paso 1 de 20"), "temporizador avanza el recorrido");
            click(avl, "Pausar");
            click(avl, "Ver AVL completo");
            ok(contiene(avl, "AVL válido: true"), "regresa al árbol completo");
            click(avl, "Vaciar");
            click(avl, "Preorden");
            ok(contiene(avl, "Árbol vacío") && !boton(avl, "Ver rotaciones").isEnabled(),
                "vaciar limpia recorrido e historial");
        });
        ok(registros.size() == 20 && registros.stream().allMatch(e -> e.estado().equals("PENDIENTE")),
            "datos de origen intactos");
        System.out.println("OK: " + n + " comprobaciones AVL 3.2.");
    }
}
