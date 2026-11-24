package com.importaflow.algoritmos;

/** Semana 14: arbol de expresiones utilizado para calcular una tarifa ejemplo. */
public final class ArbolExpresion {
    private static class Nodo {
        String dato;
        Nodo izq, der;

        Nodo(String dato, Nodo izq, Nodo der) {
            this.dato = dato;
            this.izq = izq;
            this.der = der;
        }
    }

    private ArbolExpresion() {
    }

    public static String explicar(double peso) {
        if (!Double.isFinite(peso) || peso <= 0)
            throw new IllegalArgumentException("Peso invalido.");
        // Expresion: 10 + (5 * peso). El nodo raiz es el operador suma.
        Nodo raiz = new Nodo("+", new Nodo("10", null, null),
            new Nodo("*", new Nodo("5", null, null), new Nodo(Double.toString(peso), null, null)));
        StringBuilder pre = new StringBuilder(), in = new StringBuilder(), post = new StringBuilder();
        pre(raiz, pre);
        in(raiz, in);
        post(raiz, post);
        return "Arbol de expresion: tarifa ilustrativa = 10 + (5 x peso)\n"
        + "Preorden: " + pre + "\nInorden: " + in + "\nPostorden: " + post
        + "\nEvaluacion recursiva: S/ " + String.format("%.2f", evaluar(raiz));
    }

    private static double evaluar(Nodo n) {
        return switch (n.dato) {
            case "+" -> evaluar(n.izq) + evaluar(n.der);
            case "*" -> evaluar(n.izq) * evaluar(n.der);
            default -> Double.parseDouble(n.dato);
        };
    }

    private static void pre(Nodo n, StringBuilder b) {
        if (n == null)
            return;
        b.append(n.dato).append(' ');
        pre(n.izq, b);
        pre(n.der, b);
    }

    private static void in(Nodo n, StringBuilder b) {
        if (n == null)
            return;
        in(n.izq, b);
        b.append(n.dato).append(' ');
        in(n.der, b);
    }

    private static void post(Nodo n, StringBuilder b) {
        if (n == null)
            return;
        post(n.izq, b);
        post(n.der, b);
        b.append(n.dato).append(' ');
    }

    public static ArbolBinarioEnvios.NodoVisual visual(double peso) {
        explicar(peso);
        return new ArbolBinarioEnvios.NodoVisual(1, "+", "Suma", new ArbolBinarioEnvios.NodoVisual(2, "10", "Base",
            null, null), new ArbolBinarioEnvios.NodoVisual(3, "×", "Multiplicación", new ArbolBinarioEnvios
            .NodoVisual(4, "5", "Por kg", null, null), new ArbolBinarioEnvios.NodoVisual(5, Double.toString(peso),
            "Peso", null, null)));
    }
}
