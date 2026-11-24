package com.importaflow;
import com.importaflow.algoritmos.*;
import com.importaflow.estructuras.*;
import com.importaflow.modelo.*;
import com.importaflow.vista.LaboratorioAED;
import java.util.*;
import java.awt.Component;
import java.awt.Container;
import javax.swing.*;
/** Pruebas reproducibles sin MySQL, incluidas acciones Swing sin diálogos. */
public class PruebaFase2 {
    private static int pruebas;

    private static void ok(boolean c, String m) {
        if (!c)
            throw new AssertionError(m);
        pruebas++;
    }

    private static Envio envio(int id, int b, String prioridad) {
        return new Envio(id, 1, "IF-" + id, "Cliente", "Contenido", "Lima", b, prioridad, "PENDIENTE", "Destino",
            "900000000", "Cusco", "Calle", b, 20 + b, "", "admin", 0);
    }

    private static void click(Container c, String nombre) {
        for (Component a : c.getComponents()) {
            if (a instanceof JButton b && b.getText().equals(nombre)) {
                b.doClick(0);
                return;
            }
            if (a instanceof Container z && existe(z, nombre)) {
                click(z, nombre);
                return;
            }
        }
        throw new AssertionError("Botón no encontrado: " + nombre);
    }

    private static boolean existe(Container c, String nombre) {
        for (Component a : c.getComponents()) {
            if (a instanceof JButton b && b.getText().equals(nombre))
                return true;
            if (a instanceof Container z && existe(z, nombre))
                return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        int[][] casos = {{30, 20, 10},{10, 20, 30},{30, 10, 20},{10, 30, 20}};
        String[] nombres = {"LL", "RR", "LR", "RL"};
        for (int i = 0; i < casos.length; i++) {
            ArbolAVL a = new ArbolAVL();
            for (int x : casos[i])
                a.insertar(x);
            ok(a.valido(), nombres[i]);
            ok(a.raizVisual().id == 20, "raíz 20");
            ok(a.pasos().size() == 1 && a.pasos().get(0).texto().startsWith(nombres[i]), "rotación");
            ok(a.pasos().get(0).despues().id == 20, "captura después");
            ok(a.recorrido("Inorden").equals("[10, 20, 30]"), "inorden");
        }
        ArbolAVL a = new ArbolAVL();
        for (int x : new int[]{4, 2, 5, 1, 3})
            a.insertar(x);
        ok(a.eliminar(5) && a.valido() && !a.pasos().isEmpty(), "borrado con rotación");
        ok(!a.buscar(5), "eliminación");
        ok(!a.insertar(2), "duplicado");
        ok(!a.eliminar(999), "inexistente");
        Random rng = new Random(24);
        TreeSet<Integer> ref = new TreeSet<>();
        a = new ArbolAVL();
        boolean valido = true;
        for (int i = 0; i < 2000; i++) {
            int x = rng.nextInt(300);
            boolean ins = rng.nextBoolean();
            boolean r = ins ? a.insertar(x) : a.eliminar(x), esperado = ins ? ref.add(x) : ref.remove(x);
            valido &= r == esperado && a.valido() && a.recorrido("Inorden").equals(ref.toString());
        }
        ok(valido, "2000 operaciones AVL aleatorias contra TreeSet");
        for (String t : new String[]{"Burbuja", "Selección", "Inserción"}) {
            var data = new OrdenamientoVisual.Dato[]{new OrdenamientoVisual.Dato(1, 5), new OrdenamientoVisual
                .Dato(2, 1.5), new OrdenamientoVisual.Dato(3, 3), new OrdenamientoVisual.Dato(4, 1.5)};
            var p = OrdenamientoVisual.generar(data, t);
            var f = p.get(p.size() - 1);
            ok(Arrays.equals(Arrays.stream(f.datos()).mapToDouble(OrdenamientoVisual.Dato::valor).toArray(),
                new double[]{1.5, 1.5, 3, 5}), t);
            ok(data[0].valor() == 5 && p.get(0).datos()[0].valor() == 5, "origen intacto");
            ok(f.comparaciones() > 0 && f.movimientos() > 0, "métricas");
            ok(OrdenamientoVisual.generar(new OrdenamientoVisual.Dato[0], t).size() == 2, "vacío");
        }
        PilaEstatica ps = new PilaEstatica(3);
        ps.push("a");
        ps.push("b");
        ok(Arrays.equals(ps.elementos(), new String[]{"b", "a"}), "pila estática");
        PilaAcciones pd = new PilaAcciones();
        pd.apilar("a");
        pd.apilar("b");
        ok(Arrays.equals(pd.elementos(), ps.elementos()), "pila dinámica");
        ColaEstatica ce = new ColaEstatica(3);
        ce.enqueue(1);
        ce.enqueue(2);
        ce.dequeue();
        ce.enqueue(3);
        ce.enqueue(4);
        ok(Arrays.equals(ce.elementos(), new int[]{2, 3, 4}), "arreglo circular");
        ArbolBinarioEnvios abb = new ArbolBinarioEnvios();
        for (int id : new int[]{5, 2, 7, 3})
            abb.insertar(envio(id, id, "NORMAL"));
        ok(abb.camino(3).equals(List.of(5, 2, 3)), "ruta ABB");
        ok(abb.camino(6).equals(List.of(5, 7)), "ruta sin resultado");
        ok(ArbolExpresion.visual(2.5).der.izq.titulo.equals("5"), "expresión visual");
        List<Envio> e = List.of(envio(3, 3, "NORMAL"), envio(2, 1, "URGENTE"), envio(1, 5, "NORMAL"));
        SwingUtilities.invokeAndWait(() -> {
            LaboratorioAED lab = new LaboratorioAED(() -> e, null);
            JTabbedPane t = (JTabbedPane) lab.getComponent(0);
            ok(t.getTabCount() == 8, "8 pestañas");
            click((Container) t.getComponentAt(0), "Cargar IDs");
            click((Container) t.getComponentAt(0), "Clonar");
            click((Container) t.getComponentAt(1), "Preparar desde MySQL");
            click((Container) t.getComponentAt(1), "Siguiente paso");
            click((Container) t.getComponentAt(1), "Resultado final");
            click((Container) t.getComponentAt(2), "Cargar matriz");
            click((Container) t.getComponentAt(2), "Inversa ajustada");
            click((Container) t.getComponentAt(2), "Transponer");
            click((Container) t.getComponentAt(3), "Cargar pedidos");
            click((Container) t.getComponentAt(3), "Ordenar por bultos");
            click((Container) t.getComponentAt(4), "Cargar práctica");
            click((Container) t.getComponentAt(4), "POP · Retirar");
            click((Container) t.getComponentAt(5), "Cargar pendientes");
            click((Container) t.getComponentAt(5), "DEQUEUE · Retirar");
            click((Container) t.getComponentAt(6), "Reconstruir MySQL");
            click((Container) t.getComponentAt(6), "Inorden");
            click((Container) t.getComponentAt(6), "Paso siguiente");
            click((Container) t.getComponentAt(7), "Ejecutar caso");
            click((Container) t.getComponentAt(7), "Antes de rotar");
            click((Container) t.getComponentAt(7), "Después de rotar");
            ok(true, "acciones Swing sin base de datos");
        });
        ok(e.size() == 3 && e.get(0).estado().equals("PENDIENTE"), "fuente intacta");
        System.out.println("OK: " + pruebas
            + " comprobaciones de fase 2; incluye 2000 operaciones AVL aleatorias.");
    }
}
