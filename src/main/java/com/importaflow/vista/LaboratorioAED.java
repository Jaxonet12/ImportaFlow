package com.importaflow.vista;

import com.importaflow.algoritmos.*;
import com.importaflow.estructuras.*;
import com.importaflow.modelo.*;
import com.importaflow.servicio.GestorPedidos;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Laboratorio aislado del DAO: cada operación trabaja sobre estructuras en memoria. */
@SuppressWarnings("unchecked") // Los contenedores locales conservan estado de cada pestaña en sus callbacks.
public final class LaboratorioAED extends JPanel {
    private static final Color AZUL = new Color(34, 81, 130), FONDO = new Color(243, 247, 251),
        AMBAR = new Color(255, 228, 165);
    private final Supplier<List<Envio>> envios;
    private final GestorPedidos gestor;

    public LaboratorioAED(Supplier<List<Envio>> envios, GestorPedidos gestor) {
        super(new BorderLayout(6, 6));
        this.envios = envios;
        this.gestor = gestor;
        setBackground(FONDO);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Arreglos S2-3", arreglos());
        tabs.addTab("Ordenamientos", ordenamientos());
        tabs.addTab("Matrices S4", matrices());
        tabs.addTab("Listas S6-9", listas());
        tabs.addTab("Pilas S11", pilas());
        tabs.addTab("Colas S12", colas());
        tabs.addTab("ABB S13-16", arbol());
        tabs.addTab("AVL S17", avl());
        add(tabs);
        add(new JLabel("Laboratorio académico: las operaciones de práctica NO modifican MySQL."),
            BorderLayout.SOUTH);
    }

    private JPanel base(String titulo, String detalle) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.setBackground(FONDO);
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JLabel l = new JLabel(titulo);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        l.setForeground(AZUL);
        cab.add(l, BorderLayout.NORTH);
        JTextArea d = texto();
        d.setText(detalle);
        d.setOpaque(false);
        d.setRows(2);
        cab.add(d);
        p.add(cab, BorderLayout.NORTH);
        return p;
    }

    private JTextArea texto() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(new Font("SansSerif", Font.PLAIN, 13));
        a.setMargin(new Insets(8, 8, 8, 8));
        return a;
    }

    private JPanel botones() {
        return new JPanel(new GridLayout(0, 3, 5, 5));
    }

    private JButton boton(JPanel p, String nombre, Runnable accion) {
        JButton b = new JButton(nombre);
        b.setForeground(AZUL);
        b.setFocusPainted(false);
        b.addActionListener(e -> {
            try {
                accion.run();
            } catch (Cancelado ex) {
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Operación", JOptionPane.WARNING_MESSAGE);
            }
        });
        p.add(b);
        return b;
    }
    private static final class Cancelado extends RuntimeException {
    }

    private String pedir(String msg) {
        String s = JOptionPane.showInputDialog(this, msg);
        if (s == null)
            throw new Cancelado();
        if (s.isBlank())
            throw new IllegalArgumentException("Ingresa un valor.");
        return s.trim();
    }

    private int numero(String msg) {
        return Integer.parseInt(pedir(msg));
    }

    private Envio envio(int id) {
        return envios.get().stream().filter(e -> e.id() == id).findFirst().orElseThrow(()
            -> new IllegalArgumentException("No existe ese ID en los envíos cargados."));
    }

    private Pedido pedido(int id) {
        Envio e = envio(id);
        return new Pedido(e.id(), e.cliente(), e.contenido(), e.origen(), e.cantidad(), e.prioridad(), e.estado());
    }

    private Pedido[] pedidos() {
        return envios.get().stream().map(e -> new Pedido(e.id(), e.cliente(), e.contenido(), e.origen(), e
            .cantidad(), e.prioridad(), e.estado())).sorted(Comparator.comparingInt(Pedido::getId))
            .toArray(Pedido[]::new);
    }

    private String nombre(Pedido p) {
        return p == null ? "Vacío" : "P" + p.getId() + " | " + p.getCantidad() + " bultos";
    }

    private String[] etiquetas(Pedido[] p) {
        return Arrays.stream(p).map(this::nombre).toArray(String[]::new);
    }

    private String valor(Object o) {
        return o == null ? "Estructura vacía" : o.toString();
    }
    /** Visor de referencias: cada cuadro representa un elemento de la estructura real. */
    private static final class Nodos extends JPanel {
        String[] datos = {};
        String modo = "arreglo";
        int resaltado = -1, segundo = -1;
        double[] valores = null;

        Nodos() {
            setBackground(Color.WHITE);
            setToolTipText(" ");
        }

        void ver(String[] a, String m, int r) {
            datos = a;
            modo = m;
            resaltado = r;
            segundo = -1;
            valores = null;
            boolean pila = m.equals("pila");
            setPreferredSize(new Dimension(pila ? 380 : Math.max(470, 45 + a.length * 154), pila ? Math.max(185, 65
                + a.length * 46) : 180));
            revalidate();
            repaint();
        }

        void orden(OrdenamientoVisual.Paso paso) {
            String[] a = Arrays.stream(paso.datos()).map(d -> "P" + d.id() + " | " + String.format(Locale.ROOT,
                "%.2f", d.valor())).toArray(String[]::new);
            ver(a, "arreglo", paso.a());
            segundo = paso.b();
            valores = Arrays.stream(paso.datos()).mapToDouble(OrdenamientoVisual.Dato::valor).toArray();
            setPreferredSize(new Dimension(Math.max(470, 45 + a.length * 154), 260));
            revalidate();
            repaint();
            if (paso.a() >= 0)
                scrollRectToVisible(new Rectangle(16 + Math.min(paso.a(), paso.b()) * 154, 0, Math.min(300,
                    Math.abs(paso.a() - paso.b()) * 154 + 124), 240));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D d = (Graphics2D) g.create();
            d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            d.setFont(new Font("SansSerif", Font.PLAIN, 13));
            d.setColor(AZUL);
            if (datos.length == 0) {
                d.drawString("Estructura vacía. Usa los botones para cargar o agregar.", 16, 35);
                d.dispose();
                return;
            }
            boolean pila = modo.equals("pila");
            String encabezado = switch (modo) {
                case "pila" -> "CIMA ↓  |  LIFO";
                case "cola" -> "FRENTE  →  FINAL (FIFO)";
                case "prioridad" -> "FRENTE  →  urgentes primero, luego normales";
                case "circular" -> "ACTUAL  →  último vuelve al primero";
                case "doble" -> "Inicio / recorrido  ↔  enlaces en ambos sentidos";
                case "simple" -> "Inicio  →  siguiente  →  NULL";
                default -> "Posiciones del arreglo (índice desde 0)";
            };
            d.drawString(encabezado, 16, 23);
            for (int i = 0; i < datos.length; i++) {
                int x = pila ? 25 : 16 + i * 154, y = pila ? 38 + i * 46 : 55, w = pila ? 310 : 124, h = 38;
                d.setColor(i == resaltado || i == segundo ? AMBAR : new Color(225, 237, 249));
                d.fillRoundRect(x, y, w, h, 9, 9);
                d.setColor(AZUL);
                d.drawRoundRect(x, y, w, h, 9, 9);
                String t = datos[i];
                while (t.length() > 1 && d.getFontMetrics().stringWidth(t) > w - 10)
                    t = t.substring(0, t.length() - 1);
                if (!t.equals(datos[i]) && t.length() > 3)
                    t = t.substring(0, t.length() - 3) + "…";
                d.drawString(t, x + 5, y + 23);
                if (!pila) {
                    d.drawString("[" + i + "]", x + 5, y + 54);
                    if (i < datos.length - 1) {
                        d.drawString(modo.equals("doble") ? "↔" : modo.equals("arreglo") ? "" : "→", x + w + 6, y
                            + 24);
                    }
                }
            }
            if (valores != null) {
                double max = Arrays.stream(valores).max().orElse(1);
                for (int i = 0; i < valores.length; i++) {
                    int h = (int) Math.round(max > 0 ? 100 * Math.max(0, valores[i]) / max : 0);
                    d.setColor(i == resaltado || i == segundo ? new Color(205, 143, 39) : AZUL);
                    d.fillRoundRect(32 + i * 154, 235 - h, 92, Math.max(2, h), 7, 7);
                }
            }
            if (modo.equals("circular")) {
                int fin = 16 + (datos.length - 1) * 154 + 62;
                d.drawLine(fin, 96, fin, 132);
                d.drawLine(fin, 132, 78, 132);
                d.drawLine(78, 132, 78, 96);
                d.drawString("↑", 72, 108);
            }
            if (pila)
                d.drawString("BASE", 25, 43 + datos.length * 46);
            d.dispose();
        }

        @Override public String getToolTipText(MouseEvent e) {
            int i = modo.equals("pila") ? (e.getY() - 38) / 46 : (e.getX() - 16) / 154;
            return i >= 0 && i < datos.length ? datos[i] : null;
        }
    }

    private JPanel centro(Nodos n, JTextArea a) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        JScrollPane sc = new JScrollPane(n);
        p.add(sc);
        JScrollPane out = new JScrollPane(a);
        out.setPreferredSize(new Dimension(100, 110));
        p.add(out, BorderLayout.SOUTH);
        return p;
    }

    private JPanel arreglos() {
        JPanel p = base("Arreglos unidimensionales",
            "S2-3: posiciones fijas; insertar y eliminar crean un nuevo arreglo. Clonar no comparte referencias.");
        Nodos n = new Nodos();
        JTextArea msg = texto();
        p.add(centro(n, msg));
        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);
        final int[][] a = {new int[0], null};
        Consumer<String> mostrar = s -> {
            n.ver(Arrays.stream(a[0]).mapToObj(Integer::toString).toArray(String[]::new), "arreglo", -1);
            msg.setText(s + "\nActual: " + Arrays.toString(a[0]) + "\nClon: " + (a[1] == null
                ? "sin crear" : Arrays.toString(a[1])));
        };
        boton(b, "Cargar IDs",() -> {
            a[0] = envios.get().stream().mapToInt(Envio::id).toArray();
            a[1] = null;
            mostrar.accept("Recorrido de los IDs de MySQL.");
        });
        boton(b, "Insertar",() -> {
            a[0] = ArreglosAED.insertar(a[0], numero("Índice de inserción (0 a " + a[0].length + ")"),
                numero("Valor"));
            mostrar.accept("Se desplazaron los elementos hacia la derecha.");
        });
        boton(b, "Actualizar",() -> {
            a[0] = ArreglosAED.actualizar(a[0], numero("Índice"), numero("Valor nuevo"));
            mostrar.accept("Se reemplazó el valor en la posición.");
        });
        boton(b, "Eliminar",() -> {
            a[0] = ArreglosAED.eliminar(a[0], numero("Índice"));
            mostrar.accept("Se desplazaron los elementos hacia la izquierda.");
        });
        boton(b, "Clonar",() -> {
            a[1] = ArreglosAED.copiar(a[0]);
            mostrar.accept("Copia independiente.");
        });
        boton(b, "Comparar",() -> mostrar.accept(a[1] == null ? "Crea primero el clon." : "Mismo contenido: "
            + ArreglosAED.comparar(a[0], a[1]) + " | Misma referencia: " + (a[0] == a[1])));
        boton(b, "Fusionar",() -> {
            if (a[1] == null)
                throw new IllegalArgumentException("Crea primero el clon.");
            a[0] = ArreglosAED.fusionar(a[0], a[1]);
            mostrar.accept("Se concatenaron los dos arreglos.");
        });
        mostrar.accept("Carga IDs para empezar.");
        return p;
    }

    private JPanel ordenamientos() {
        JPanel p = base("Ordenamiento paso a paso",
            "S8 (complemento): usa el mismo arreglo inicial para comparar Burbuja, Selección e Inserción. Máximo 60 envíos por traza.");
        Nodos n = new Nodos();
        JTextArea msg = texto();
        p.add(centro(n, msg));
        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);
        JComboBox<String> metodo = new JComboBox<>(new String[]{"Burbuja", "Selección", "Inserción"}),
            criterio = new JComboBox<>(new String[]{"ID", "Bultos", "Peso (kg)", "Tarifa (S/)"});
        b.add(metodo);
        b.add(criterio);
        final List<OrdenamientoVisual.Paso>[] pasos = new List[]{List.of()};
        final int[] indice = {0};
        Runnable mostrar = () -> {
            if (pasos[0].isEmpty())
                return;
            var x = pasos[0].get(indice[0]);
            n.orden(x);
            msg.setText("Paso " + indice[0] + " / " + (pasos[0].size() - 1) + " | Comparaciones: " + x
                .comparaciones() + " | Intercambios: " + x.movimientos() + "\n" + x.explicacion()
                + "\nÍndices activos: " + x.a() + ", " + x.b() + ". Complejidad máxima O(n²).");
        };
        boton(b, "Preparar desde MySQL",() -> {
            String c = (String) criterio.getSelectedItem();
            var datos = envios.get().stream().sorted(Comparator.comparingInt(Envio::id).reversed()).limit(60).map(e
                -> new OrdenamientoVisual.Dato(e.id(), switch (c) {
                case "Bultos" -> e.cantidad();
                case "Peso (kg)" -> e.peso();
                case "Tarifa (S/)" -> e.tarifa().doubleValue();
                default -> e.id();
            })).toArray(OrdenamientoVisual.Dato[]::new);
            pasos[0] = OrdenamientoVisual.generar(datos,(String) metodo.getSelectedItem());
            indice[0] = 0;
            mostrar.run();
        });
        boton(b, "Anterior",() -> {
            indice[0] = Math.max(0, indice[0] - 1);
            mostrar.run();
        });
        boton(b, "Siguiente paso",() -> {
            if (!pasos[0].isEmpty())
                indice[0] = Math.min(pasos[0].size() - 1, indice[0] + 1);
            mostrar.run();
        });
        boton(b, "Resultado final",() -> {
            if (!pasos[0].isEmpty())
                indice[0] = pasos[0].size() - 1;
            mostrar.run();
        });
        boton(b, "Reiniciar pasos",() -> {
            indice[0] = 0;
            mostrar.run();
        });
        ActionListener cambio = e -> {
            pasos[0] = List.of();
            indice[0] = 0;
            n.ver(new String[0], "arreglo", -1);
            msg.setText("Cambiaste el método o criterio. Pulsa Preparar desde MySQL.");
        };
        metodo.addActionListener(cambio);
        criterio.addActionListener(cambio);
        msg.setText("Selecciona un criterio y método, luego prepara la traza.");
        return p;
    }

    private JTable tablaMatriz() {
        JTable t = new JTable();
        t.setRowHeight(45);
        t.setFont(t.getFont().deriveFont(16f));
        t.setEnabled(false);
        return t;
    }

    private void matriz(JTable t, double[][] a, String[] columnas, String[] filas) {
        Object[][] d = new Object[a.length][a[0].length + 1];
        for (int i = 0; i < a.length; i++) {
            d[i][0] = filas[i];
            for (int j = 0; j < a[i].length; j++)
                d[i][j + 1] = String.format(Locale.ROOT, "%.3f", a[i][j]);
        }
        String[] c = new String[columnas.length + 1];
        c[0] = "Fila";
        System.arraycopy(columnas, 0, c, 1, columnas.length);
        t.setModel(new DefaultTableModel(d, c));
    }

    private double[][] doubles(int[][] a) {
        return Arrays.stream(a).map(r -> Arrays.stream(r).asDoubleStream().toArray()).toArray(double[][]::new);
    }

    private JPanel matrices() {
        JPanel p = base(
            "Matrices de operaciones logísticas",
            "S4: matriz original por estado y prioridad. "
            + "Las transformaciones no modifican MySQL.");

        JTable origen = tablaMatriz();
        JTable resultado = tablaMatriz();
        JTextArea msg = texto();

        JPanel centro = new JPanel(new GridLayout(2, 1, 4, 4));
        centro.add(new JScrollPane(origen));
        centro.add(new JScrollPane(resultado));

        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.add(centro);

        JScrollPane out = new JScrollPane(msg);
        out.setPreferredSize(new Dimension(100, 90));
        cuerpo.add(out, BorderLayout.SOUTH);

        p.add(cuerpo);

        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);

        final int[][][] a = {null};

        String[] estados = {"Pendiente", "En curso", "Finalizado"};
        String[] cols = {"Normal", "Urgente", "Total"};
        String[] i = {"0", "1", "2"};

        // Cargar matriz original desde los envíos.
        Runnable cargar = () -> {
            a[0] = MatricesAED.matrizOperativa(envios.get());

            matriz(origen, doubles(a[0]), cols, estados);
            resultado.setModel(new DefaultTableModel());

            msg.setText(
                "MATRIZ ORIGINAL\n"
                + "Se organizan los envíos por estado en las filas "
                + "y por prioridad en las columnas. "
                + "La columna Total es la suma de Normal y Urgente.");
        };

        boton(b, "Cargar matriz", cargar);

        // Operaciones de transformación matricial.
        BiConsumer<String, Function<int[][], int[][]>> op =
        (label, f) -> boton(b, label,() -> {

            if (a[0] == null)
                cargar.run();

            matriz(origen, doubles(a[0]), cols, estados);

            matriz(
                resultado,
                doubles(f.apply(a[0])),
                label.equals("Transponer") ? estados : i,
                label.equals("Transponer") ? cols : i);

            String explicacion = switch (label) {

                case "Transponer" ->
                "MATRIZ TRANSPUESTA\n"
                + "Se intercambian las filas por las columnas "
                + "de la tabla 1. Cada elemento A[i][j] "
                + "pasa a la posición A[j][i] en la tabla 2.";

                case "Triangular inferior" ->
                "MATRIZ TRIANGULAR INFERIOR\n"
                + "Se conservan los valores de la diagonal "
                + "principal y los que están debajo de ella. "
                + "Los valores superiores se reemplazan por 0.";

                case "Triangular superior" ->
                "MATRIZ TRIANGULAR SUPERIOR\n"
                + "Se conservan los valores de la diagonal "
                + "principal y los que están encima de ella. "
                + "Los valores inferiores se reemplazan por 0.";

                case "Tridiagonal" ->
                "MATRIZ TRIDIAGONAL\n"
                + "Se conservan la diagonal principal y las "
                + "diagonales inmediatamente superior e inferior. "
                + "Los demás elementos se reemplazan por 0.";

                case "Simetrizar A + Aᵀ" ->
                "MATRIZ SIMÉTRICA\n"
                + "Se suma la matriz original con su transpuesta "
                + "(A + Aᵀ). Los elementos a ambos lados de "
                + "la diagonal principal quedan iguales.";

                default ->
                "El resultado matemático se muestra "
                + "en la segunda tabla.";
            };

            msg.setText(explicacion);
            msg.setCaretPosition(0);
        });

        op.accept("Transponer", MatricesAED::transponer);

        op.accept(
            "Triangular inferior",
            MatricesAED::triangularInferior);

        op.accept(
            "Triangular superior",
            MatricesAED::triangularSuperior);

        op.accept("Tridiagonal", MatricesAED::tridiagonal);

        op.accept("Simetrizar A + Aᵀ", MatricesAED::simetrizar);

        // Comprobar simetría y determinante.
        boton(b, "Simetría / determinante",() -> {

            if (a[0] == null)
                cargar.run();

            matriz(origen, doubles(a[0]), cols, estados);

            boolean simetrica = MatricesAED.esSimetrica(a[0]);
            double det = MatricesAED.determinante(a[0]);

            resultado.setModel(new DefaultTableModel());

            msg.setText(
                "SIMETRÍA Y DETERMINANTE\n"
                + "Matriz simétrica: "
                + (simetrica ? "Sí" : "No")
                + " | Determinante: " + det + "\n"
                + "Se comparan los elementos respecto a la diagonal. "
                + "El determinante es 0 porque la columna Total "
                + "depende de las otras dos columnas.");

            msg.setCaretPosition(0);
        });

        // Calcular la inversa de la matriz original.
        boton(b, "Inversa original",() -> {

            if (a[0] == null)
                cargar.run();

            matriz(origen, doubles(a[0]), cols, estados);

            double[][] r = MatricesAED.inversa(a[0]);

            if (r == null) {

                resultado.setModel(new DefaultTableModel());

                msg.setText(
                    "INVERSA DE LA MATRIZ ORIGINAL\n"
                    + "No existe inversa porque el determinante es 0. "
                    + "La columna Total es la suma de Normal y Urgente. "
                    + "Por eso la segunda tabla permanece vacía.");

            } else {

                matriz(resultado, r, i, i);

                msg.setText(
                    "INVERSA DE LA MATRIZ ORIGINAL\n"
                    + "Se aplica el método de Gauss-Jordan para "
                    + "calcular la inversa, mostrada en la tabla 2.");
            }

            msg.setCaretPosition(0);
        });

        // Construir una matriz invertible para fines académicos.
        boton(b, "Inversa ajustada",() -> {

            if (a[0] == null)
                cargar.run();

            int[][] ajustada = MatricesAED.diagonalDominante(a[0]);

            matriz(origen, doubles(ajustada), i, i);

            matriz(
                resultado,
                MatricesAED.inversa(ajustada),
                i,
                i);

            msg.setText(
                "INVERSA DE LA MATRIZ AJUSTADA\n"
                + "Tabla 1: se refuerza la diagonal principal para "
                + "obtener una matriz invertible (no representa "
                + "cantidades reales de envíos).\n"
                + "Tabla 2: se calcula su inversa mediante Gauss-Jordan. "
                + "Determinante: "
                + MatricesAED.determinante(ajustada));

            msg.setCaretPosition(0);
        });

        msg.setText(
            "Presiona Cargar matriz para obtener los datos de los "
            + "envíos. Después selecciona una operación matemática.");

        return p;
    }

    private JPanel listas() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Simple", lista(false));
        tabs.addTab("Doble", lista(true));
        tabs.addTab("Circular", circular());
        JPanel p = base("TAD y listas enlazadas",
            "TADPedidos define qué operaciones ofrece el gestor. Los nodos definen cómo se almacenan los pedidos. Las listas de prueba son independientes.");
        p.add(tabs);
        return p;
    }

    private JPanel lista(boolean doble) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        Nodos n = new Nodos();
        JTextArea msg = texto();
        p.add(centro(n, msg));
        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);
        ListaSimplePedidos[] s = {new ListaSimplePedidos()};
        ListaDoblePedidos[] d = {new ListaDoblePedidos()};
        Supplier<Pedido[]> datos = () -> doble ? d[0].elementos(false) : s[0].obtenerTodos();
        javax.swing.Timer reloj = new javax.swing.Timer(650, null);
        final Pedido[][] ruta = {new Pedido[0]};
        int[] paso = {0};
        boolean[] atras = {false};
        Consumer<String> mostrar = t -> {
            reloj.stop();
            ruta[0] = new Pedido[0];
            paso[0] = 0;
            n.ver(etiquetas(datos.get()), doble ? "doble" : "simple", -1);
            msg.setText(t + "\nNodos: " + datos.get().length);
        };
        IntFunction<Pedido> buscar = id -> doble ? d[0].buscar(id) : s[0].buscar(id);
        Runnable avanzar = () -> {
            if (paso[0] >= ruta[0].length) {
                reloj.stop();
                return;
            }
            Pedido x = ruta[0][paso[0]++];
            int i = atras[0] ? ruta[0].length - paso[0] : paso[0] - 1;
            n.resaltado = i;
            n.repaint();
            n.scrollRectToVisible(new Rectangle(16 + i * 154, 0, 124, 115));
            String recorrido = Arrays.stream(ruta[0]).limit(paso[0]).map(v -> "P" + v.getId())
                .collect(java.util.stream.Collectors.joining(" → "));
            msg.setText("Paso " + paso[0] + " / " + ruta[0].length + " | Visitando P" + x.getId() + " | Enlace: "
                + (atras[0] ? "anterior" : "siguiente") + "\nRecorrido: " + recorrido + (paso[0] == ruta[0].length
                ? " → NULL. Recorrido terminado." : ""));
            if (paso[0] == ruta[0].length)
                reloj.stop();
        };
        reloj.addActionListener(e -> avanzar.run());
        Consumer<Boolean> recorrer = r -> {
            mostrar.accept("Preparando recorrido");
            atras[0] = r;
            ruta[0] = doble ? d[0].elementos(r) : s[0].obtenerTodos();
            paso[0] = 0;
            if (ruta[0].length == 0) {
                mostrar.accept("Lista vacía: carga pedidos primero.");
                return;
            }
            avanzar.run();
            if (paso[0] < ruta[0].length)
                reloj.start();
        };
        p.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !p.isShowing())
                reloj.stop();
        });
        boton(b, "Cargar pedidos",() -> {
            s[0] = new ListaSimplePedidos();
            d[0] = new ListaDoblePedidos();
            for (Pedido x : pedidos()) {
                s[0].insertarAlFinal(x);
                d[0].insertarAlFinal(x);
            }
            mostrar.accept("Se crearon nodos enlazados a partir de los pedidos.");
        });
        boton(b, "Insertar ID",() -> {
            reloj.stop();
            int id = numero("ID real para insertar");
            if (buscar.apply(id) != null)
                throw new IllegalArgumentException("Ya existe en esta lista.");
            Pedido x = pedido(id);
            if (doble)
                d[0].insertarAlFinal(x);
            else
                s[0].insertarAlFinal(x);
            mostrar.accept("Nuevo nodo conectado al final.");
        });
        boton(b, "Eliminar ID",() -> {
            reloj.stop();
            int id = numero("ID para eliminar de la copia");
            boolean ok = doble ? d[0].eliminar(id) : s[0].eliminar(id);
            mostrar.accept(ok ? "Se reconectaron los enlaces vecinos." : "No se encontró ese ID.");
        });
        boton(b, "Buscar ID",() -> {
            reloj.stop();
            ruta[0] = new Pedido[0];
            paso[0] = 0;
            int id = numero("ID buscado");
            Pedido[] a = datos.get();
            int i = 0;
            while (i < a.length && a[i].getId() != id)
                i++;
            Pedido x = buscar.apply(id);
            n.ver(etiquetas(a), doble ? "doble" : "simple", x == null ? -1 : i);
            if (x != null)
                n.scrollRectToVisible(new Rectangle(16 + i * 154, 0, 124, 115));
            msg.setText(x == null ? "No encontrado después de recorrer la lista." : "Encontrado P" + id + " | " + x
                .getProducto() + ". Posición visual " + i + ".");
        });
        boton(b, "Recorrer adelante",() -> recorrer.accept(false));
        if (doble)
            boton(b, "Recorrer atrás",() -> recorrer.accept(true));
        else
            boton(b, "Ordenar por bultos",() -> {
                String antes = Arrays.toString(etiquetas(datos.get()));
                s[0].ordenarPorCantidad();
                mostrar.accept("Inserción ordenada de nodos. Antes: " + antes + "\nDespués: "
                    + Arrays.toString(etiquetas(datos.get())));
            });
        boton(b, "Pausar recorrido",() -> {
            reloj.stop();
            msg.append("\nRecorrido pausado. Usa Paso siguiente o inicia otro recorrido.");
        });
        boton(b, "Paso siguiente",() -> {
            reloj.stop();
            if (paso[0] < ruta[0].length && Arrays.equals(etiquetas(datos.get()), n.datos))
                avanzar.run();
            else
                msg.append("\nInicia un recorrido para avanzar.");
        });
        boton(b, "Vaciar copia",() -> {
            s[0] = new ListaSimplePedidos();
            d[0] = new ListaDoblePedidos();
            mostrar.accept("Solo memoria.");
        });
        mostrar.accept("Carga o inserta pedidos para practicar.");
        return p;
    }

    private JPanel circular() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        Nodos n = new Nodos();
        JTextArea msg = texto();
        p.add(centro(n, msg));
        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);
        ListaCircularTurnos[] c = {new ListaCircularTurnos()};
        Consumer<String> mostrar = t -> {
            n.ver(c[0].elementos(), "circular", 0);
            msg.setText(t + "\nActual: " + valor(c[0].verActual()) + " | Nodos: " + c[0].getCantidad());
        };
        boton(b, "Cargar práctica",() -> {
            c[0] = new ListaCircularTurnos();
            for (String x : new String[]{"Ana", "Luis", "Sara"})
                c[0].agregar(x);
            mostrar.accept("El último nodo vuelve al primero.");
        });
        boton(b, "Agregar encargado",() -> mostrar.accept(c[0].agregar(pedir("Nombre"))
            ? "Encargado añadido." : "Ya existe."));
        boton(b, "Eliminar encargado",() -> mostrar.accept(c[0].eliminar(pedir("Nombre"))
            ? "Nodo retirado y ciclo reconectado." : "No existe."));
        boton(b, "Buscar encargado",() -> mostrar.accept(c[0].contiene(pedir("Nombre"))
            ? "Encontrado." : "No existe."));
        boton(b, "Siguiente turno",() -> {
            c[0].siguienteTurno();
            mostrar.accept("La referencia actual avanza al siguiente; al final vuelve al inicio.");
        });
        boton(b, "Vaciar",() -> {
            c[0] = new ListaCircularTurnos();
            mostrar.accept("Ciclo vacío.");
        });
        mostrar.accept("Carga los tres encargados de práctica.");
        return p;
    }

    private JPanel pilas() {
        JPanel p = base("Pilas LIFO",
            "S11: PUSH agrega a la cima; POP retira la cima; PEEK consulta sin retirar. Estática: arreglo de 50 posiciones. Dinámica: enlaces entre nodos.");
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Estática", pila(false));
        tabs.addTab("Dinámica", pila(true));
        p.add(tabs);
        return p;
    }

    private JPanel pila(boolean dinamica) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        Nodos n = new Nodos();
        JTextArea msg = texto();
        p.add(centro(n, msg));
        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);
        PilaEstatica[] s = {new PilaEstatica(50)};
        PilaAcciones[] d = {new PilaAcciones()};
        Consumer<String> push = x -> {
            if (dinamica)
                d[0].apilar(x);
            else
                s[0].push(x);
        };
        Consumer<String> mostrar = t -> {
            String[] a = dinamica ? d[0].elementos() : s[0].elementos();
            n.ver(a, "pila", 0);
            msg.setText(t + "\nElementos: " + a.length + (dinamica ? " | Copia de práctica" : " / 50"));
        };
        boton(b, "Cargar práctica",() -> {
            s[0] = new PilaEstatica(50);
            d[0] = new PilaAcciones();
            for (String x : new String[]{"Recepción P1", "Clasificación P2", "Despacho P3"})
                push.accept(x);
            mostrar.accept("P3 fue el último en entrar, por eso está en la cima.");
        });
        boton(b, "PUSH · Apilar",() -> {
            String x = pedir("Acción para apilar");
            push.accept(x);
            mostrar.accept("PUSH: se agregó " + x + " a la cima.");
        });
        boton(b, "POP · Retirar",() -> mostrar.accept("POP: " + valor(dinamica ? d[0].desapilar() : s[0].pop())
            + ". No revierte operaciones reales."));
        boton(b, "PEEK · Consultar",() -> mostrar.accept("PEEK: " + valor(dinamica ? d[0].verCima() : s[0].peek())
            + ". Nada se elimina."));
        boton(b, "Vaciar copia",() -> {
            s[0] = new PilaEstatica(50);
            d[0] = new PilaAcciones();
            mostrar.accept("Pila vacía.");
        });
        if (dinamica)
            boton(b, "Copiar historial sesión",() -> {
                d[0] = new PilaAcciones();
                String[] a = gestor == null ? new String[0] : gestor.accionesSesion();
                for (int i = a.length - 1; i >= 0; i--)
                    d[0].apilar(a[i]);
                mostrar.accept("Historial real copiado. Las nuevas pruebas no modifican el original.");
            });
        mostrar.accept("Carga práctica o apila una acción.");
        return p;
    }

    private JPanel colas() {
        JPanel p = base("Colas FIFO y de prioridad",
            "S12: ENQUEUE agrega al final; DEQUEUE retira el frente; PEEK consulta. Las urgentes se atienden antes de las normales, con FIFO dentro de cada grupo.");
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Estática", cola(0));
        tabs.addTab("Dinámica", cola(1));
        tabs.addTab("Prioridad", cola(2));
        p.add(tabs);
        return p;
    }

    private JPanel cola(int tipo) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        Nodos n = new Nodos();
        JTextArea msg = texto();
        p.add(centro(n, msg));
        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);
        ColaEstatica[] s = {new ColaEstatica(1000)};
        ColaPedidos[] d = {new ColaPedidos()};
        ColaPrioridadPedidos[] pr = {new ColaPrioridadPedidos()};
        Consumer<Pedido> agregar = x -> {
            if (tipo == 0)
                s[0].enqueue(x.getId());
            else if (tipo == 1)
                d[0].encolar(x);
            else
                pr[0].encolar(x);
        };
        Runnable vaciar = () -> {
            s[0] = new ColaEstatica(Math.max(1000, envios.get().size() + 1));
            d[0] = new ColaPedidos();
            pr[0] = new ColaPrioridadPedidos();
        };
        Consumer<String> mostrar = t -> {
            String[] a = tipo == 0 ? Arrays.stream(s[0].elementos()).mapToObj(x -> "P" + x)
                .toArray(String[]::new) : Arrays.stream(tipo == 1 ? d[0].elementos() : pr[0].elementos()).map(x
                -> "P" + x.getId() + " | " + x.getPrioridad()).toArray(String[]::new);
            n.ver(a, tipo == 2 ? "prioridad" : "cola", 0);
            msg.setText(t + "\nElementos: " + a.length + ". Solo copia en memoria.");
        };
        boton(b, "Cargar pendientes",() -> {
            vaciar.run();
            for (Pedido x : GestorPedidos.ordenarPendientesFIFO(pedidos()))
                agregar.accept(x);
            mostrar.accept("Pendientes cargados por ID ascendente (llegada).");
        });
        boton(b, "ENQUEUE · Agregar",() -> {
            Pedido x = pedido(numero("ID de un paquete pendiente"));
            if (!"PENDIENTE".equals(x.getEstado()))
                throw new IllegalArgumentException("El paquete debe estar pendiente.");
            boolean existe = tipo == 0 ? Arrays.stream(s[0].elementos()).anyMatch(id -> id == x
                .getId()) : Arrays.stream(tipo == 1 ? d[0].elementos() : pr[0].elementos()).anyMatch(y -> y
                .getId() == x.getId());
            if (existe)
                throw new IllegalArgumentException("Ya está en la cola de práctica.");
            agregar.accept(x);
            mostrar.accept("Se agregó P" + x.getId() + " al final de su cola.");
        });
        boton(b, "DEQUEUE · Retirar",() -> mostrar.accept("Retirado: " + (tipo == 0 ? valor(s[0]
            .dequeue()) : nombre(tipo == 1 ? d[0].desencolar() : pr[0].desencolar()))));
        boton(b, "PEEK · Consultar",() -> mostrar.accept("Frente: " + (tipo == 0 ? valor(s[0]
            .peek()) : nombre(tipo == 1 ? d[0].verPrimero() : pr[0].verSiguiente())) + ". No se retira."));
        boton(b, "Vaciar copia",() -> {
            vaciar.run();
            mostrar.accept("Cola vacía.");
        });
        mostrar.accept("Carga pendientes para comparar las tres colas.");
        return p;
    }

    private JPanel visor(PanelArbol arbol) {
        JPanel p = new JPanel(new BorderLayout());
        JScrollPane sc = new JScrollPane(arbol);
        sc.getHorizontalScrollBar().setUnitIncrement(18);
        sc.getVerticalScrollBar().setUnitIncrement(18);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JLabel z = new JLabel("100%");
        Runnable fit = () -> {
            arbol.ajustarAlEspacio(sc.getViewport().getExtentSize());
            z.setText(arbol.porcentaje());
        };
        boton(b, "−",() -> {
            arbol.cambiarZoom(1 / 1.2);
            z.setText(arbol.porcentaje());
        });
        boton(b, "+",() -> {
            arbol.cambiarZoom(1.2);
            z.setText(arbol.porcentaje());
        });
        boton(b, "Ajustar", fit);
        boton(b, "100%",() -> {
            arbol.restablecerZoom();
            z.setText(arbol.porcentaje());
        });
        b.add(z);
        sc.getViewport().addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                if (arbol.esAjusteAutomatico())
                    fit.run();
            }
        });
        arbol.addPropertyChangeListener("preferredSize", e -> z.setText(arbol.porcentaje()));
        p.add(b, BorderLayout.NORTH);
        p.add(sc);
        p.putClientProperty("fit", fit);
        return p;
    }

    private void dibujar(PanelArbol v, JPanel cont, ArbolBinarioEnvios.NodoVisual n) {
        v.setArbol(n);
        SwingUtilities.invokeLater((Runnable) cont.getClientProperty("fit"));
    }

    private int contar(ArbolBinarioEnvios.NodoVisual n) {
        return n == null ? 0 : 1 + contar(n.izq) + contar(n.der);
    }

    private int altura(ArbolBinarioEnvios.NodoVisual n) {
        return n == null ? 0 : 1 + Math.max(altura(n.izq), altura(n.der));
    }

    private int hojas(ArbolBinarioEnvios.NodoVisual n) {
        return n == null ? 0 : n.izq == null && n.der == null ? 1 : hojas(n.izq) + hojas(n.der);
    }

    private int profundidad(ArbolBinarioEnvios.NodoVisual n, int id, int d) {
        if (n == null)
            return -1;
        if (n.id == id)
            return d;
        return profundidad(id < n.id ? n.izq : n.der, id, d + 1);
    }

    private String stats(ArbolBinarioEnvios.NodoVisual n) {
        return "Nodos: " + contar(n) + " | Altura (niveles): " + altura(n) + " | Hojas: " + hojas(n) + " | Raíz: "
            + (n == null ? "—" : n.id);
    }

    private void mitades(ArbolBinarioEnvios a, List<Envio> e, int l, int r) {
        if (l > r)
            return;
        int m = (l + r) >>> 1;
        a.insertar(e.get(m));
        mitades(a, e, l, m - 1);
        mitades(a, e, m + 1, r);
    }

    private JPanel arbol() {
        JPanel p = base("Árbol binario de búsqueda (ABB)",
            "S13-14 y S16: menor a la izquierda, mayor a la derecha. Verde = visitado; amarillo = nodo actual. Raíz en nivel 0.");
        PanelArbol v = new PanelArbol();
        JPanel cont = visor(v);
        JTextArea msg = texto();
        JPanel c = new JPanel(new BorderLayout());
        c.add(cont);
        JScrollPane out = new JScrollPane(msg);
        out.setPreferredSize(new Dimension(100, 110));
        c.add(out, BorderLayout.SOUTH);
        p.add(c);
        JPanel b = botones();
        p.add(b, BorderLayout.SOUTH);
        ArbolBinarioEnvios[] a = {new ArbolBinarioEnvios()};
        final List<Integer>[] ruta = new List[]{List.of()};
        int[] paso = { - 1};
        String[] tipo = {""};
        Runnable mostrarPaso = () -> {
            if (paso[0] < 0 || ruta[0].isEmpty())
                return;
            int id = ruta[0].get(paso[0]);
            v.marcar(ruta[0].subList(0, paso[0] + 1), id);
            msg.setText(tipo[0] + " | Paso " + (paso[0] + 1) + " de " + ruta[0].size() + "\nVisitados: " + ruta[0]
                .subList(0, paso[0] + 1) + "\nP" + id + " | Profundidad y nivel: " + profundidad(a[0].raizVisual(),
                id, 0) + "\n" + stats(a[0].raizVisual()));
        };
        Consumer<String> actualizar = t -> {
            ruta[0] = List.of();
            paso[0] = -1;
            dibujar(v, cont, a[0].raizVisual());
            msg.setText(t + "\n" + stats(a[0].raizVisual()) + "\nInorden: " + a[0].inorden());
        };
        boton(b, "Reconstruir MySQL",() -> {
            a[0] = new ArbolBinarioEnvios();
            List<Envio> e = new ArrayList<>(envios.get());
            e.sort(Comparator.comparingInt(Envio::id));
            mitades(a[0], e, 0, e.size() - 1);
            actualizar.accept("Inserción por mitades: equilibrio inicial, no AVL.");
        });
        boton(b, "Insertar ID",() -> {
            int id = numero("ID real del paquete");
            boolean existe = a[0].buscar(id) != null;
            a[0].insertar(envio(id));
            actualizar.accept(existe ? "El ID ya existía." : "Nodo insertado siguiendo comparaciones.");
        });
        boton(b, "Eliminar nodo",() -> {
            int id = numero("ID a eliminar solo del índice");
            boolean existe = a[0].buscar(id) != null;
            a[0].eliminar(id);
            actualizar.accept(existe
                ? "Nodo eliminado. Si tiene dos hijos se utiliza el sucesor inorden." : "ID no encontrado.");
        });
        for (String t : new String[]{"Preorden", "Inorden", "Postorden"})
            boton(b, t,() -> {
                String r = switch (t) {
                    case "Preorden" -> a[0].preorden();
                    case "Inorden" -> a[0].inorden();
                    default -> a[0].postorden();
                };
                ruta[0] = r.isBlank() ? List.of() : Arrays.stream(r.split(" ")).map(Integer::parseInt).toList();
                tipo[0] = t;
                paso[0] = 0;
                dibujar(v, cont, a[0].raizVisual());
                if (ruta[0].isEmpty())
                    msg.setText("Árbol vacío.");
                else
                    mostrarPaso.run();
            });
        boton(b, "Buscar ID",() -> {
            int id = numero("ID buscado");
            ruta[0] = a[0].camino(id);
            tipo[0] = "Buscar P" + id + " — " + (a[0].buscar(id) != null ? "encontrado" : "no encontrado");
            paso[0] = 0;
            dibujar(v, cont, a[0].raizVisual());
            if (ruta[0].isEmpty())
                msg.setText("Árbol vacío.");
            else
                mostrarPaso.run();
        });
        boton(b, "Paso anterior",() -> {
            paso[0] = Math.max(0, paso[0] - 1);
            mostrarPaso.run();
        });
        boton(b, "Paso siguiente",() -> {
            if (!ruta[0].isEmpty())
                paso[0] = Math.min(ruta[0].size() - 1, paso[0] + 1);
            mostrarPaso.run();
        });
        boton(b, "Ver recorrido completo",() -> {
            if (!ruta[0].isEmpty()) {
                paso[0] = ruta[0].size() - 1;
                mostrarPaso.run();
            }
        });
        boton(b, "Vaciar índice",() -> {
            a[0] = new ArbolBinarioEnvios();
            actualizar.accept("Solo memoria.");
        });
        boton(b, "Árbol expresión tarifa",() -> {
            double peso = Double.parseDouble(pedir("Peso (kg), tarifa ilustrativa").replace(',', '.'));
            var exp = ArbolExpresion.visual(peso);
            ruta[0] = List.of();
            paso[0] = -1;
            dibujar(v, cont, exp);
            msg.setText(ArbolExpresion.explicar(peso)
                + "\nNo cambia la tarifa real. Selecciona cualquier recorrido ABB para regresar.");
        });
        actualizar.accept("Reconstruye el índice o inserta IDs reales.");
        return p;
    }

    private JPanel avl() {
        JPanel p = base("Árbol AVL — balanceo automático",
            "FE = altura izquierda − altura derecha. Rojo: desequilibrio. Los recorridos visitan nodos; las rotaciones cambian enlaces. Todas las prácticas son en memoria.");
        PanelArbol v = new PanelArbol();
        JPanel cont = visor(v);
        JTextArea msg = texto();
        JLabel estado = new JLabel(" ");
        estado.setForeground(AZUL);
        estado.setFont(estado.getFont().deriveFont(Font.BOLD));
        JPanel c = new JPanel(new BorderLayout(4, 4));
        c.add(estado, BorderLayout.NORTH);
        c.add(cont);
        JScrollPane out = new JScrollPane(msg);
        out.setPreferredSize(new Dimension(100, 112));
        c.add(out, BorderLayout.SOUTH);
        p.add(c);
        JPanel controles = new JPanel(new GridLayout(0, 1, 3, 3));
        p.add(controles, BorderLayout.SOUTH);
        JPanel datos = new JPanel(new GridLayout(1, 5, 5, 3));
        datos.setBorder(BorderFactory.createTitledBorder("1. Origen y edición de la copia"));
        controles.add(datos);
        JPanel ejemplos = new JPanel(new GridLayout(1, 2, 5, 3));
        ejemplos.setBorder(BorderFactory
            .createTitledBorder("Ejercicios independientes (solo al pulsar Ejecutar caso)"));
        controles.add(ejemplos);
        JPanel recorridos = new JPanel(new GridLayout(2, 4, 5, 3));
        recorridos.setBorder(BorderFactory.createTitledBorder("2. Recorrido del árbol activo"));
        controles.add(recorridos);
        JPanel rotaciones = new JPanel(new GridLayout(1, 5, 5, 3));
        rotaciones.setBorder(BorderFactory
            .createTitledBorder("3. Historial de balanceo del origen activo (subárboles)"));
        controles.add(rotaciones);
        JComboBox<String> casos = new JComboBox<>(new String[]{"LL: 30, 20, 10", "RR: 10, 20, 30", "LR: 30, 10, 20",
            "RL: 10, 30, 20", "Borrado: eliminar 5"});
        record Rotacion(String operacion, ArbolAVL.Paso paso) {
        }
        // El modo de pantalla es independiente del historial. Un recorrido jamás consume rotaciones.
        class Sesion {
            ArbolAVL a = new ArbolAVL();
            String origen = "Sin datos", modo = "completo", tipo = "", contexto = "";
            List<Rotacion> rot = List.of();
            List<Integer> ruta = List.of();
            int indice = 0, fase = 0, nodo = -1;
            JButton anterior, siguiente, automatico, completo, verRot, antes, despues, pasoRot, otraRot;
            final javax.swing.Timer reloj = new javax.swing.Timer(650, e -> avanzarNodo());

            void parar() {
                reloj.stop();
            }

            void controles() {
                boolean recorriendo = modo.equals("recorrido") && !ruta.isEmpty();
                boolean rotando = modo.equals("rotacion") && !rot.isEmpty();
                anterior.setEnabled(recorriendo && nodo > 0);
                siguiente.setEnabled(recorriendo && nodo < ruta.size() - 1);
                automatico.setEnabled(recorriendo && nodo < ruta.size() - 1);
                automatico.setText(reloj.isRunning() ? "Pausar" : "Automático / pausa");
                completo.setEnabled(recorriendo);
                verRot.setEnabled(!rot.isEmpty());
                antes.setEnabled(rotando && fase > 0);
                despues.setEnabled(rotando && fase < ultimo());
                pasoRot.setEnabled(rotando && (fase < ultimo() || indice < rot.size() - 1));
                otraRot.setEnabled(rotando && indice < rot.size() - 1);
                estado.setText("Origen: " + origen + " | Vista: " + (rotando ? "rotación" : recorriendo
                    ? tipo : "árbol completo") + " | " + contar(a.raizVisual()) + " nodos");
            }

            int ultimo() {
                return rot.isEmpty() ? 0 : rot.get(indice).paso().intermedio() == null ? 1 : 2;
            }

            void limpiarRuta() {
                parar();
                ruta = List.of();
                nodo = -1;
                tipo = "";
            }

            void mostrarCompleto(String texto) {
                limpiarRuta();
                modo = "completo";
                dibujar(v, cont, a.raizVisual());
                msg.setText(texto + "\n" + stats(a.raizVisual()) + " | AVL válido: " + a.valido() + "\nInorden: "
                    + a.recorrido("Inorden") + "\nRotaciones registradas: " + rot.size()
                    + ". Usa Ver rotaciones para revisarlas.");
                msg.setCaretPosition(0);
                controles();
            }

            void nuevaOperacion(String texto, boolean mostrarRotacion) {
                limpiarRuta();
                contexto = texto;
                rot = a.pasos().stream().map(r -> new Rotacion(texto, r)).toList();
                indice = 0;
                fase = 0;
                if (mostrarRotacion && !rot.isEmpty())
                    mostrarRotacion();
                else
                    mostrarCompleto(texto + (rot.isEmpty()
                        ? " | No hubo rotación." : " | Balanceo automático realizado."));
            }

            void mostrarRotacion() {
                limpiarRuta();
                if (rot.isEmpty()) {
                    mostrarCompleto("No hay rotaciones registradas para este origen.");
                    return;
                }
                modo = "rotacion";
                var evento = rot.get(indice);
                var r = evento.paso();
                int fin = ultimo();
                var n = fase == 0 ? r.antes() : fase < fin ? r.intermedio() : r.despues();
                String caso = r.texto().substring(0, 2);
                String explicacion = switch (caso) {
                    case "LL" -> "Rotación derecha del nodo desequilibrado.";
                    case "RR" -> "Rotación izquierda del nodo desequilibrado.";
                    case "LR" -> "Izquierda sobre el hijo izquierdo; después derecha sobre el nodo desequilibrado.";
                    default -> "Derecha sobre el hijo derecho; después izquierda sobre el nodo desequilibrado.";
                };
                String etapa = fase == 0 ? "ANTES: se detecta FE fuera de [−1, +1]." : fase < fin
                    ? "INTERMEDIO: primera rotación realizada; falta la segunda." : "DESPUÉS: subárbol equilibrado. El orden inorden se conserva.";
                dibujar(v, cont, n);
                msg.setText(contexto + "\n" + evento.operacion() + " | Rotación " + (indice + 1) + " de " + rot
                    .size() + " — " + r.texto() + "\nPaso " + (fase + 1) + " de " + (fin + 1) + ": " + etapa + "\n"
                    + explicacion + "\nVista del subárbol afectado, no del AVL completo. " + stats(n));
                msg.setCaretPosition(0);
                controles();
            }

            void cargar() {
                limpiarRuta();
                a = new ArbolAVL();
                origen = "IDs de MySQL";
                List<Rotacion> todas = new ArrayList<>();
                for (Pedido x : pedidos()) {
                    a.insertar(x.getId());
                    for (var r : a.pasos())
                        todas.add(new Rotacion("Inserción del ID " + x.getId(), r));
                }
                rot = List.copyOf(todas);
                indice = 0;
                fase = 0;
                contexto = "Construcción desde MySQL por ID ascendente";
                mostrarCompleto("IDs de MySQL cargados. Selecciona un recorrido o revisa las rotaciones de estas inserciones.");
            }

            void iniciarRuta(String t, List<Integer> ids) {
                limpiarRuta();
                modo = "recorrido";
                tipo = t;
                ruta = List.copyOf(ids);
                nodo = ruta.isEmpty() ? -1 : 0;
                dibujar(v, cont, a.raizVisual());
                if (nodo < 0) {
                    mostrarCompleto("Árbol vacío. Carga IDs o ejecuta un caso.");
                    return;
                }
                mostrarNodo();
            }

            void mostrarNodo() {
                if (!modo.equals("recorrido") || nodo < 0 || ruta.isEmpty())
                    return;
                int id = ruta.get(nodo);
                v.marcar(ruta.subList(0, nodo + 1), id);
                v.enfocar(id);
                String regla = switch (tipo) {
                    case "Preorden" -> "Raíz → izquierda → derecha";
                    case "Inorden" -> "Izquierda → raíz → derecha";
                    case "Postorden" -> "Izquierda → derecha → raíz";
                    default -> "Comparar y seguir la rama correspondiente";
                };
                if (nodo == ruta.size() - 1)
                    parar();
                msg.setText(tipo + " | Paso " + (nodo + 1) + " de " + ruta.size() + " | Nodo actual: " + id + "\n"
                    + regla + "\nVisitados: " + ruta.subList(0, nodo + 1) + "\n" + (nodo == ruta.size() - 1
                    ? "Recorrido terminado. " : "Amarillo: actual; verde: visitados. ") + "El árbol no cambia.");
                msg.setCaretPosition(0);
                controles();
            }

            void avanzarNodo() {
                if (modo.equals("recorrido") && nodo < ruta.size() - 1) {
                    nodo++;
                    mostrarNodo();
                } else {
                    parar();
                    controles();
                }
            }
        }
        Sesion s = new Sesion();
        boton(datos, "Cargar IDs MySQL", s::cargar);
        boton(datos, "Insertar valor",() -> {
            int id = numero("ID positivo (práctica)");
            if (id <= 0)
                throw new IllegalArgumentException("Ingresa un ID positivo.");
            s.nuevaOperacion(s.a.insertar(id) ? "Insertado " + id : "ID duplicado.", false);
        });
        boton(datos, "Eliminar valor",() -> {
            int id = numero("ID de práctica a eliminar");
            s.nuevaOperacion(s.a.eliminar(id) ? "Eliminado " + id : "ID no encontrado.", false);
        });
        boton(datos, "Vaciar",() -> {
            s.a = new ArbolAVL();
            s.origen = "Copia vacía";
            s.nuevaOperacion("Árbol vacío.", false);
        });
        ejemplos.add(casos);
        boton(ejemplos, "Ejecutar caso",() -> {
            s.a = new ArbolAVL();
            s.origen = "Ejercicio " + casos.getSelectedItem();
            int[] x = switch (casos.getSelectedIndex()) {
                case 0 -> new int[]{30, 20, 10};
                case 1 -> new int[]{10, 20, 30};
                case 2 -> new int[]{30, 10, 20};
                case 3 -> new int[]{10, 30, 20};
                default -> new int[]{4, 2, 5, 1, 3};
            };
            for (int k : x)
                s.a.insertar(k);
            if (casos.getSelectedIndex() == 4)
                s.a.eliminar(5);
            s.nuevaOperacion("Caso " + casos.getSelectedItem(), true);
        });
        boton(datos, "Ver AVL completo",() -> s.mostrarCompleto("Árbol activo completo."));
        for (String t : new String[]{"Preorden", "Inorden", "Postorden"})
            boton(recorridos, t,() -> s.iniciarRuta(t, s.a.idsRecorrido(t)));
        boton(recorridos, "Buscar valor",() -> {
            int id = numero("ID buscado");
            s.iniciarRuta("Buscar " + id + " — " + (s.a.buscar(id) ? "Encontrado" : "No encontrado"), s.a
                .camino(id));
        });
        s.anterior = boton(recorridos, "Nodo anterior",() -> {
            s.parar();
            if (s.nodo > 0) {
                s.nodo--;
                s.mostrarNodo();
            }
        });
        s.siguiente = boton(recorridos, "Siguiente nodo",() -> {
            s.parar();
            s.avanzarNodo();
        });
        s.automatico = boton(recorridos, "Automático / pausa",() -> {
            if (s.reloj.isRunning())
                s.parar();
            else if (s.nodo < s.ruta.size() - 1)
                s.reloj.start();
            s.controles();
        });
        s.completo = boton(recorridos, "Recorrido completo",() -> {
            s.parar();
            if (!s.ruta.isEmpty()) {
                s.nodo = s.ruta.size() - 1;
                s.mostrarNodo();
            }
        });
        s.verRot = boton(rotaciones, "Ver rotaciones",() -> {
            s.indice = 0;
            s.fase = 0;
            s.mostrarRotacion();
        });
        s.antes = boton(rotaciones, "Antes de rotar",() -> {
            s.fase = 0;
            s.mostrarRotacion();
        });
        s.despues = boton(rotaciones, "Después de rotar",() -> {
            s.fase = s.ultimo();
            s.mostrarRotacion();
        });
        s.pasoRot = boton(rotaciones, "Siguiente paso",() -> {
            if (s.fase < s.ultimo())
                s.fase++;
            else if (s.indice < s.rot.size() - 1) {
                s.indice++;
                s.fase = 0;
            }
            s.mostrarRotacion();
        });
        s.otraRot = boton(rotaciones, "Siguiente rotación",() -> {
            if (s.indice < s.rot.size() - 1) {
                s.indice++;
                s.fase = 0;
            }
            s.mostrarRotacion();
        });
        p.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !p.isShowing()) {
                s.parar();
                s.controles();
            }
        });
        s.mostrarCompleto("Carga los IDs de MySQL o ejecuta un ejercicio. Los recorridos y las rotaciones tienen controles independientes.");
        return p;
    }
}
