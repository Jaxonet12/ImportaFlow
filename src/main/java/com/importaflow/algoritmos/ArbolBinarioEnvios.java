package com.importaflow.algoritmos;

import com.importaflow.modelo.Envio;
import javax.swing.tree.DefaultMutableTreeNode;

/** Semanas 13 y 14: arbol binario creado con nodos y enlaces izquierdo/derecho.
 * Se eligio insertar por codigo numerico (propiedad de busqueda) para permitir
 * demostrar busqueda/eliminacion y recorridos. El ABB se estudia formalmente
 * en la semana 16; esta aplicacion lo introduce como caso util de arbol binario. */
public class ArbolBinarioEnvios {
    private static class Nodo {
        Envio dato;
        Nodo izq, der;

        Nodo(Envio dato) {
            this.dato = dato;
        }
    }
    /** DTO del dibujo: entrega una copia de los enlaces sin exponer los nodos internos. */
    public static final class NodoVisual {
        public final int id;
        public final String destinatario, titulo;
        public final NodoVisual izq, der;

        public NodoVisual(int id, String destinatario, NodoVisual izq, NodoVisual der) {
            this(id, "P" + id, destinatario, izq, der);
        }

        public NodoVisual(int id, String titulo, String destinatario, NodoVisual izq, NodoVisual der) {
            this.id = id;
            this.titulo = titulo;
            this.destinatario = destinatario == null ? "" : destinatario;
            this.izq = izq;
            this.der = der;
        }
    }

    /** Genera los nodos para el PanelArbol; no cambia el indice del algoritmo. */
    public NodoVisual raizVisual() {
        return copiaVisual(raiz);
    }

    private NodoVisual copiaVisual(Nodo n) {
        if (n == null)
            return null;
        return new NodoVisual(n.dato.id(), n.dato.destinatario(),
            copiaVisual(n.izq), copiaVisual(n.der));
    }

    private Nodo raiz;

    public void insertar(Envio valor) {
        raiz = insertar(raiz, valor);
    }

    private Nodo insertar(Nodo n, Envio e) {
        if (n == null)
            return new Nodo(e);
        if (e.id() < n.dato.id())
            n.izq = insertar(n.izq, e);
        else if (e.id() > n.dato.id())
            n.der = insertar(n.der, e);
        return n;
    }

    public Envio buscar(int id) {
        Nodo n = raiz;
        while (n != null) {
            if (id == n.dato.id())
                return n.dato;
            n = id < n.dato.id() ? n.izq : n.der;
        }
        return null;
    }

    public void eliminar(int id) {
        raiz = eliminar(raiz, id);
    }

    private Nodo eliminar(Nodo n, int id) {
        if (n == null)
            return null;
        if (id < n.dato.id())
            n.izq = eliminar(n.izq, id);
        else if (id > n.dato.id())
            n.der = eliminar(n.der, id);
        else {
            if (n.izq == null)
                return n.der;
            if (n.der == null)
                return n.izq;
            Nodo sucesor = n.der;
            while (sucesor.izq != null)
                sucesor = sucesor.izq;
            n.dato = sucesor.dato;
            n.der = eliminar(n.der, sucesor.dato.id());
        }
        return n;
    }

    public String preorden() {
        StringBuilder s = new StringBuilder();
        pre(raiz, s);
        return s.toString().trim();
    }

    public String inorden() {
        StringBuilder s = new StringBuilder();
        in(raiz, s);
        return s.toString().trim();
    }

    public String postorden() {
        StringBuilder s = new StringBuilder();
        post(raiz, s);
        return s.toString().trim();
    }

    private void pre(Nodo n, StringBuilder s) {
        if (n != null) {
            s.append(n.dato.id()).append(' ');
            pre(n.izq, s);
            pre(n.der, s);
        }
    }

    private void in(Nodo n, StringBuilder s) {
        if (n != null) {
            in(n.izq, s);
            s.append(n.dato.id()).append(' ');
            in(n.der, s);
        }
    }

    private void post(Nodo n, StringBuilder s) {
        if (n != null) {
            post(n.izq, s);
            post(n.der, s);
            s.append(n.dato.id()).append(' ');
        }
    }

    public DefaultMutableTreeNode modeloVisual() {
        if (raiz == null)
            return new DefaultMutableTreeNode("Sin paquetes");
        return convertir(raiz);
    }

    private DefaultMutableTreeNode convertir(Nodo n) {
        DefaultMutableTreeNode r = new DefaultMutableTreeNode("P" + n.dato.id() + " | " + n.dato.destinatario());
        if (n.izq != null)
            r.add(convertir(n.izq));
        if (n.der != null)
            r.add(convertir(n.der));
        return r;
    }

    public java.util.List<Integer> camino(int id) {
        java.util.List<Integer> r = new java.util.ArrayList<>();
        Nodo n = raiz;
        while (n != null) {
            r.add(n.dato.id());
            if (n.dato.id() == id)
                break;
            n = id < n.dato.id() ? n.izq : n.der;
        }
        return r;
    }
}
