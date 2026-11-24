package com.importaflow.algoritmos;

import java.util.*;

/** S17. ABB con alturas y rotaciones; cada operación conserva |FE| <= 1. */
public final class ArbolAVL {
    private static final class Nodo {
        int id, altura = 1;
        Nodo izq, der;

        Nodo(int id) {
            this.id = id;
        }
    }
    public record Paso(String texto, ArbolBinarioEnvios.NodoVisual antes, ArbolBinarioEnvios.NodoVisual intermedio,
        ArbolBinarioEnvios.NodoVisual despues) {
    }
    private Nodo raiz;
    private final List<Paso> pasos = new ArrayList<>();

    private int h(Nodo n) {
        return n == null ? 0 : n.altura;
    }

    private int fe(Nodo n) {
        return h(n.izq) - h(n.der);
    }

    private void actualizar(Nodo n) {
        n.altura = 1 + Math.max(h(n.izq), h(n.der));
    }

    private Nodo derecha(Nodo n) {
        Nodo x = n.izq;
        n.izq = x.der;
        x.der = n;
        actualizar(n);
        actualizar(x);
        return x;
    }

    private Nodo izquierda(Nodo n) {
        Nodo x = n.der;
        n.der = x.izq;
        x.izq = n;
        actualizar(n);
        actualizar(x);
        return x;
    }

    private Nodo equilibrar(Nodo n) {
        actualizar(n);
        int f = fe(n);
        if (f > 1 || f < -1) {
            var antes = visual(n);
            ArbolBinarioEnvios.NodoVisual intermedio = null;
            String tipo;
            if (f > 1) {
                if (fe(n.izq) < 0) {
                    tipo = "LR: izquierda y derecha";
                    n.izq = izquierda(n.izq);
                    actualizar(n);
                    intermedio = visual(n);
                } else
                    tipo = "LL: derecha";
                n = derecha(n);
            } else {
                if (fe(n.der) > 0) {
                    tipo = "RL: derecha e izquierda";
                    n.der = derecha(n.der);
                    actualizar(n);
                    intermedio = visual(n);
                } else
                    tipo = "RR: izquierda";
                n = izquierda(n);
            }
            pasos.add(new Paso(tipo + " (FE previo: " + f + ")", antes, intermedio, visual(n)));
        }
        return n;
    }

    public boolean insertar(int id) {
        pasos.clear();
        if (buscar(id))
            return false;
        raiz = insertar(raiz, id);
        return true;
    }

    private Nodo insertar(Nodo n, int id) {
        if (n == null)
            return new Nodo(id);
        if (id < n.id)
            n.izq = insertar(n.izq, id);
        else
            n.der = insertar(n.der, id);
        return equilibrar(n);
    }

    public boolean eliminar(int id) {
        pasos.clear();
        if (!buscar(id))
            return false;
        raiz = eliminar(raiz, id);
        return true;
    }

    private Nodo eliminar(Nodo n, int id) {
        if (n == null)
            return null;
        if (id < n.id)
            n.izq = eliminar(n.izq, id);
        else if (id > n.id)
            n.der = eliminar(n.der, id);
        else {
            if (n.izq == null)
                return n.der;
            if (n.der == null)
                return n.izq;
            Nodo s = n.der;
            while (s.izq != null)
                s = s.izq;
            n.id = s.id;
            n.der = eliminar(n.der, s.id);
        }
        return equilibrar(n);
    }

    public boolean buscar(int id) {
        Nodo n = raiz;
        while (n != null) {
            if (id == n.id)
                return true;
            n = id < n.id ? n.izq : n.der;
        }
        return false;
    }

    public List<Integer> camino(int id) {
        List<Integer> r = new ArrayList<>();
        Nodo n = raiz;
        while (n != null) {
            r.add(n.id);
            if (id == n.id)
                break;
            n = id < n.id ? n.izq : n.der;
        }
        return r;
    }

    public List<Paso> pasos() {
        return List.copyOf(pasos);
    }

    public int altura() {
        return h(raiz);
    }

    public ArbolBinarioEnvios.NodoVisual raizVisual() {
        return visual(raiz);
    }

    private ArbolBinarioEnvios.NodoVisual visual(Nodo n) {
        return n == null ? null : new ArbolBinarioEnvios.NodoVisual(n.id, "FE=" + fe(n) + " | h=" + h(n),
            visual(n.izq), visual(n.der));
    }

    public List<Integer> idsRecorrido(String tipo) {
        List<Integer> r = new ArrayList<>();
        rec(raiz, tipo, r);
        return List.copyOf(r);
    }

    public String recorrido(String tipo) {
        return idsRecorrido(tipo).toString();
    }

    private void rec(Nodo n, String t, List<Integer> r) {
        if (n == null)
            return;
        if (t.equals("Preorden"))
            r.add(n.id);
        rec(n.izq, t, r);
        if (t.equals("Inorden"))
            r.add(n.id);
        rec(n.der, t, r);
        if (t.equals("Postorden"))
            r.add(n.id);
    }

    public boolean valido() {
        return verificar(raiz, Long.MIN_VALUE, Long.MAX_VALUE) >= 0;
    }

    private int verificar(Nodo n, long min, long max) {
        if (n == null)
            return 0;
        if (n.id <= min || n.id >= max)
            return -1;
        int i = verificar(n.izq, min, n.id), d = verificar(n.der, n.id, max);
        return i < 0 || d < 0 || Math.abs(i - d) > 1 || n.altura != 1 + Math.max(i, d) ? -1 : n.altura;
    }
}
