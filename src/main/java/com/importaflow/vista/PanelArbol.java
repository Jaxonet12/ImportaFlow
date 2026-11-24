package com.importaflow.vista;

import com.importaflow.algoritmos.ArbolBinarioEnvios;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Dibuja un arbol binario verdadero con ramas y nodos, en vez del JTree de carpetas.
 * El dibujo NO modifica los pedidos: recibe una copia de los nodos para visualizarlos.
 * La escala permite hacer zoom y ajustar arboles grandes al espacio disponible.
 */
public final class PanelArbol extends JPanel {
    private static final int PASO_X = 136;
    // Evita que dos nodos hermanos se superpongan.
    private static final int PASO_Y = 112;
    private static final int ANCHO_NODO = 124;
    private static final int ALTO_NODO = 55;
    private static final int MARGEN = 24;
    private static final double ZOOM_MIN = 0.08;
    private static final double ZOOM_MAX = 2.50;
    private final Map<ArbolBinarioEnvios.NodoVisual, Point2D.Double> posiciones = new IdentityHashMap<>();
    private ArbolBinarioEnvios.NodoVisual raiz;
    private int unidades, niveles;
    private double zoom = 1.0;
    private int resaltado = -1;
    private java.util.Set<Integer> visitados = java.util.Set.of();
    private boolean ajusteAutomatico = true;

    public PanelArbol() {
        setBackground(Color.WHITE);
        setOpaque(true);
        // El tooltip enseña nombre completo sin agrandar cada nodo.
        setToolTipText(" ");
    }

    /** Actualiza solamente el dibujo; no altera el arbol real ni la base de datos. */
    public void setArbol(ArbolBinarioEnvios.NodoVisual nuevaRaiz) {
        raiz = nuevaRaiz;
        resaltado = -1;
        visitados = java.util.Set.of();
        posiciones.clear();
        unidades = Math.max(1, calcularUnidades(raiz));
        niveles = Math.max(1, calcularAltura(raiz));
        if (raiz != null)
            asignarCoordenadas(raiz, 0, 0);
        actualizarDimension();
    }

    /** Cada hijo ausente reserva un hueco conceptual; mantiene la rama al lado correcto. */
    private int calcularUnidades(ArbolBinarioEnvios.NodoVisual n) {
        if (n == null)
            return 0;
        if (n.izq == null && n.der == null)
            return 1;
        return Math.max(1, calcularUnidades(n.izq)) + Math.max(1, calcularUnidades(n.der));
    }

    private int calcularAltura(ArbolBinarioEnvios.NodoVisual n) {
        if (n == null)
            return 0;
        return 1 + Math.max(calcularAltura(n.izq), calcularAltura(n.der));
    }

    /** La posicion de cada padre queda entre las posiciones de sus hijos. */
    private double asignarCoordenadas(ArbolBinarioEnvios.NodoVisual n, int inicio, int nivel) {
        if (n == null)
            return 0;
        double x;
        if (n.izq == null && n.der == null) {
            x = (inicio + 0.5) * PASO_X;
        } else {
            int unidadesIzq = Math.max(1, calcularUnidades(n.izq));
            double xIzq = n.izq == null ? (inicio + 0.5) * PASO_X
            : asignarCoordenadas(n.izq, inicio, nivel + 1);
            double xDer = n.der == null ? (inicio + unidadesIzq + 0.5) * PASO_X
            : asignarCoordenadas(n.der, inicio + unidadesIzq, nivel + 1);
            x = (xIzq + xDer) / 2.0;
        }
        posiciones.put(n, new Point2D.Double(x, ALTO_NODO / 2.0 + nivel * PASO_Y));
        return x;
    }

    public void cambiarZoom(double factor) {
        setZoom(zoom * factor);
        ajusteAutomatico = false;
    }

    public void setZoom(double valor) {
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, valor));
        actualizarDimension();
    }

    public double getZoom() {
        return zoom;
    }

    public String porcentaje() {
        return Math.round(zoom * 100) + "%";
    }

    public boolean esAjusteAutomatico() {
        return ajusteAutomatico;
    }

    /** Calcula el zoom que permite mostrar todas las ramas sin cortarlas. */
    public void ajustarAlEspacio(Dimension espacio) {
        ajusteAutomatico = true;
        if (espacio.width <= 0 || espacio.height <= 0)
            return;
        double escalaX = (double)(espacio.width - 15) / anchoBase();
        double escalaY = (double)(espacio.height - 15) / altoBase();
        setZoom(Math.min(1.0, Math.min(escalaX, escalaY)));
    }

    public void restablecerZoom() {
        ajusteAutomatico = false;
        setZoom(1.0);
    }

    public void resaltar(int id) {
        resaltado = id;
        repaint();
    }

    public void quitarResaltado() {
        resaltado = -1;
        visitados = java.util.Set.of();
        repaint();
    }

    public void marcar(java.util.List<Integer> ids, int actual) {
        visitados = new java.util.HashSet<>(ids);
        resaltado = actual;
        repaint();
    }

    /** Desplaza la ventana hasta el nodo actual sin modificar el zoom. */
    public void enfocar(int id) {
        for (var entrada : posiciones.entrySet()) {
            if (entrada.getKey().id != id)
                continue;
            var p = entrada.getValue();
            int x = (int) Math.floor(desplazarX() + (MARGEN + p.x - ANCHO_NODO / 2.0) * zoom);
            int y = (int) Math.floor(desplazarY() + (MARGEN + p.y - ALTO_NODO / 2.0) * zoom);
            scrollRectToVisible(new Rectangle(x, y,(int) Math.ceil(ANCHO_NODO * zoom)
                + 1,(int) Math.ceil(ALTO_NODO * zoom) + 1));
            break;
        }
    }

    private int anchoBase() {
        return unidades * PASO_X + MARGEN * 2;
    }

    private int altoBase() {
        return niveles * PASO_Y + MARGEN * 2;
    }

    private void actualizarDimension() {
        setPreferredSize(new Dimension((int) Math.ceil(anchoBase() * zoom),
            (int) Math.ceil(altoBase() * zoom)));
        revalidate();
        repaint();
    }

    private int desplazarX() {
        return Math.max(0,(getWidth() - (int) Math.ceil(anchoBase() * zoom)) / 2);
    }

    private int desplazarY() {
        return Math.max(0,(getHeight() - (int) Math.ceil(altoBase() * zoom)) / 2);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(desplazarX() + MARGEN * zoom, desplazarY() + MARGEN * zoom);
        g2.scale(zoom, zoom);
        if (raiz == null) {
            g2.setColor(new Color(105, 119, 137));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g2.drawString("Sin nodos. Pulsa Reconstruir desde MySQL.", 10, 48);
        } else {
            dibujarRamas(g2, raiz);
            dibujarNodos(g2, raiz, true);
        }
        g2.dispose();
    }

    /** Primero dibujamos las lineas para que los nodos queden por encima de ellas. */
    private void dibujarRamas(Graphics2D g, ArbolBinarioEnvios.NodoVisual n) {
        if (n == null)
            return;
        Point2D.Double p = posiciones.get(n);
        g.setColor(new Color(145, 161, 181));
        g.setStroke(new BasicStroke(2.3f));
        if (n.izq != null) {
            Point2D.Double h = posiciones.get(n.izq);
            g.drawLine((int) p.x,(int)(p.y + ALTO_NODO / 2.0),
                (int) h.x,(int)(h.y - ALTO_NODO / 2.0));
        }
        if (n.der != null) {
            Point2D.Double h = posiciones.get(n.der);
            g.drawLine((int) p.x,(int)(p.y + ALTO_NODO / 2.0),
                (int) h.x,(int)(h.y - ALTO_NODO / 2.0));
        }
        dibujarRamas(g, n.izq);
        dibujarRamas(g, n.der);
    }

    private void dibujarNodos(Graphics2D g, ArbolBinarioEnvios.NodoVisual n, boolean esRaiz) {
        if (n == null)
            return;
        Point2D.Double p = posiciones.get(n);
        int x = (int) p.x - ANCHO_NODO / 2, y = (int) p.y - ALTO_NODO / 2;
        boolean seleccionado = n.id == resaltado;
        boolean desequilibrado = n.destinatario != null && (n.destinatario.startsWith("FE=2 ") || n.destinatario
            .startsWith("FE=-2 "));
        g.setColor(seleccionado ? new Color(253, 235, 178)
            : desequilibrado ? new Color(168, 65, 49) : visitados.contains(n.id) ? new Color(29, 126, 108) : esRaiz
            ? new Color(27, 70, 119) : new Color(33, 86, 140));
        g.fillRoundRect(x, y, ANCHO_NODO, ALTO_NODO, 14, 14);
        g.setColor(seleccionado ? new Color(195, 130, 24) : new Color(103, 151, 210));
        g.setStroke(new BasicStroke(seleccionado ? 3.5f : 1.7f));
        g.drawRoundRect(x, y, ANCHO_NODO, ALTO_NODO, 14, 14);
        g.setColor(seleccionado ? new Color(47, 55, 66) : Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        String id = n.titulo;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(id,(int) p.x - fm.stringWidth(id) / 2, y + 23);
        // Destinatarios largos se consultan completos dejando el puntero encima del nodo.
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String nombre = ajustarTexto(g.getFontMetrics(), n.destinatario, ANCHO_NODO - 13);
        fm = g.getFontMetrics();
        g.drawString(nombre,(int) p.x - fm.stringWidth(nombre) / 2, y + 40);
        dibujarNodos(g, n.izq, false);
        dibujarNodos(g, n.der, false);
    }

    private String ajustarTexto(FontMetrics metrics, String texto, int maxAncho) {
        if (texto == null)
            return "";
        if (metrics.stringWidth(texto) <= maxAncho)
            return texto;
        String sufijo = "...";
        while (!texto.isEmpty() && metrics.stringWidth(texto + sufijo) > maxAncho)
            texto = texto.substring(0, texto.length() - 1);
        return texto + sufijo;
    }

    @Override public String getToolTipText(MouseEvent e) {
        double x = (e.getX() - desplazarX()) / zoom - MARGEN;
        double y = (e.getY() - desplazarY()) / zoom - MARGEN;
        for (Map.Entry<ArbolBinarioEnvios.NodoVisual, Point2D.Double> entrada : posiciones.entrySet()) {
            Point2D.Double p = entrada.getValue();
            if (Math.abs(p.x - x) <= ANCHO_NODO / 2.0 && Math.abs(p.y - y) <= ALTO_NODO / 2.0) {
                ArbolBinarioEnvios.NodoVisual n = entrada.getKey();
                return n.titulo + " | " + n.destinatario;
            }
        }
        return null;
    }
}
