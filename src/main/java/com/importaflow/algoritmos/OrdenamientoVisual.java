package com.importaflow.algoritmos;
import java.util.*;
/** Traza reproducible: cada estado proviene de una operación real del algoritmo. */
public final class OrdenamientoVisual {
    public record Dato(int id, double valor) {
    }
    public record Paso(Dato[] datos, int a, int b, long comparaciones, long movimientos, String explicacion) {
    }

    public static List<Paso> generar(Dato[] origen, String metodo) {
        if (origen.length > 60)
            throw new IllegalArgumentException("La demostración visual admite hasta 60 elementos.");
        Dato[] a = origen.clone();
        List<Paso> r = new ArrayList<>();
        long c = 0, m = 0;
        r.add(new Paso(a.clone(), -1, -1, c, m, "Estado inicial. Orden ascendente."));
        if (metodo.equals("Burbuja")) {
            for (int i = 0; i < a.length - 1; i++) {
                boolean cambio = false;
                for (int j = 0; j < a.length - i - 1; j++) {
                    c++;
                    boolean inv = a[j].valor() > a[j + 1].valor();
                    r.add(new Paso(a.clone(), j, j + 1, c, m, "Comparar vecinos."));
                    if (inv) {
                        Dato t = a[j];
                        a[j] = a[j + 1];
                        a[j + 1] = t;
                        m++;
                        cambio = true;
                        r.add(new Paso(a.clone(), j, j + 1, c, m, "Intercambio: el mayor avanza a la derecha."));
                    }
                }
                if (!cambio)
                    break;
            }
        } else if (metodo.equals("Selección")) {
            for (int i = 0; i < a.length - 1; i++) {
                int min = i;
                for (int j = i + 1; j < a.length; j++) {
                    c++;
                    r.add(new Paso(a.clone(), min, j, c, m, "Comparar candidato con el mínimo actual."));
                    if (a[j].valor() < a[min].valor())
                        min = j;
                }
                if (min != i) {
                    Dato t = a[i];
                    a[i] = a[min];
                    a[min] = t;
                    m++;
                }
                r.add(new Paso(a.clone(), i, min, c, m, "Colocar el mínimo en su posición definitiva."));
            }
        } else if (metodo.equals("Inserción")) {
            // Inserción mediante intercambios adyacentes: conserva todos los objetos visibles.
            for (int i = 1; i < a.length; i++) {
                int j = i;
                while (j > 0) {
                    c++;
                    r.add(new Paso(a.clone(), j - 1, j, c, m, "Comparar clave con el vecino de la izquierda."));
                    if (a[j - 1].valor() <= a[j].valor())
                        break;
                    Dato t = a[j - 1];
                    a[j - 1] = a[j];
                    a[j] = t;
                    m++;
                    r.add(new Paso(a.clone(), j - 1, j, c, m, "Desplazar la clave una posición a la izquierda."));
                    j--;
                }
            }
        } else
            throw new IllegalArgumentException("Método desconocido");
        r.add(new Paso(a.clone(), -1, -1, c, m, "Ordenamiento terminado."));
        return r;
    }
}
