package com.importaflow.algoritmos;

import com.importaflow.modelo.Envio;
import java.util.List;

/** Semana 4. Matrices y operaciones asociadas sin librerias externas. */
public final class MatricesAED {

    private MatricesAED() {
    }

    /** Filas: pendientes, en curso, finalizados. Columnas: normal, urgente, total. */
    public static int[][] matrizOperativa(List<Envio> envios) {
        int[][] m = new int[3][3];
        for (Envio e : envios) {
            int fila = switch (e.estado()) {
                case "PENDIENTE" -> 0;
                case "EN_PROCESO", "EN_TRANSITO" -> 1;
                default -> 2;
            };
            int columna = "URGENTE".equals(e.prioridad()) ? 1 : 0;
            m[fila][columna]++;
            m[fila][2]++;
        }
        return m;
    }

    public static int[][] transponer(int[][] m) {
        int[][] n = new int[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                n[j][i] = m[i][j];
        return n;
    }

    public static int[][] triangularInferior(int[][] m) {
        int[][] n = new int[m.length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j <= i; j++)
                n[i][j] = m[i][j];
        return n;
    }

    public static int[][] triangularSuperior(int[][] m) {
        int[][] n = new int[m.length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = i; j < m.length; j++)
                n[i][j] = m[i][j];
        return n;
    }

    public static int[][] tridiagonal(int[][] m) {
        int[][] n = new int[m.length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m.length; j++)
                if (Math.abs(i - j) <= 1)
                    n[i][j] = m[i][j];
        return n;
    }

    public static boolean esSimetrica(int[][] m) {
        if (m.length != m[0].length)
            return false;
        for (int i = 0; i < m.length; i++)
            for (int j = i + 1; j < m.length; j++)
                if (m[i][j] != m[j][i])
                    return false;
        return true;
    }
    /** Construye una matriz simetrica a partir de la matriz de operaciones. */
    public static int[][] simetrizar(int[][] m) {
        int n = m.length;
        int[][] r = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                r[i][j] = m[i][j] + m[j][i];
        return r;
    }
    /** Se refuerza la diagonal para obtener una matriz estrictamente dominante.
     * Es una transformacion matematica de los datos reales, NO otros envios. */
    public static int[][] diagonalDominante(int[][] m) {
        int n = m.length;
        int[][] r = new int[n][n];
        for (int i = 0; i < n; i++) {
            int suma = 0;
            for (int j = 0; j < n; j++) {
                r[i][j] = m[i][j];
                if (i != j)
                    suma += Math.abs(m[i][j]);
            }
            r[i][i] = suma + Math.abs(r[i][i]) + 1;
        }
        return r;
    }

    public static double determinante(int[][] m) {
        if (m.length != m[0].length)
            throw new IllegalArgumentException("La matriz debe ser cuadrada.");
        int n = m.length;
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                a[i][j] = m[i][j];
        double det = 1;
        for (int p = 0; p < n; p++) {
            int f = p;
            for (int i = p + 1; i < n; i++)
                if (Math.abs(a[i][p]) > Math.abs(a[f][p]))
                    f = i;
            if (Math.abs(a[f][p]) < 1e-12)
                return 0;
            if (f != p) {
                double[] t = a[f];
                a[f] = a[p];
                a[p] = t;
                det = -det;
            }
            double piv = a[p][p];
            det *= piv;
            for (int i = p + 1; i < n; i++) {
                double factor = a[i][p] / piv;
                for (int j = p; j < n; j++)
                    a[i][j] -= factor * a[p][j];
            }
        }
        return det;
    }
    /** Gauss-Jordan devuelve null cuando no existe matriz inversa. */
    public static double[][] inversa(int[][] m) {
        int n = m.length;
        if (n == 0 || n != m[0].length)
            return null;
        double[][] a = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++)
                a[i][j] = m[i][j];
            a[i][n + i] = 1;
        }
        for (int p = 0; p < n; p++) {
            int f = p;
            for (int i = p + 1; i < n; i++)
                if (Math.abs(a[i][p]) > Math.abs(a[f][p]))
                    f = i;
            if (Math.abs(a[f][p]) < 1e-12)
                return null;
            double[] t = a[f];
            a[f] = a[p];
            a[p] = t;
            double piv = a[p][p];
            for (int j = 0; j < 2 * n; j++)
                a[p][j] /= piv;
            for (int i = 0; i < n; i++)
                if (i != p) {
                    double factor = a[i][p];
                    for (int j = 0; j < 2 * n; j++)
                        a[i][j] -= factor * a[p][j];
                }
        }
        double[][] r = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                r[i][j] = a[i][n + j];
        return r;
    }

    public static String mostrar(int[][] m) {
        StringBuilder s = new StringBuilder();
        for (int[] fila : m) {
            for (int v : fila)
                s.append(String.format("%8d", v));
            s.append('\n');
        }
        return s.toString();
    }

    public static String mostrar(double[][] m) {
        if (m == null)
            return "No tiene inversa (determinante 0).\n";
        StringBuilder s = new StringBuilder();
        for (double[] fila : m) {
            for (double v : fila)
                s.append(String.format("%11.3f", v));
            s.append('\n');
        }
        return s.toString();
    }
}
