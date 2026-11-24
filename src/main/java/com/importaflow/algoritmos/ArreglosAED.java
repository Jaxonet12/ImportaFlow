package com.importaflow.algoritmos;

import java.util.Arrays;

/** Semana 2 y 3. Arreglos estaticos: operaciones implementadas sin Collections.sort. */
public final class ArreglosAED {

    private ArreglosAED() {
    }
    // Insertar crea un arreglo nuevo porque los arreglos Java tienen capacidad fija.
    public static int[] insertar(int[] a, int posicion, int valor) {
        if (posicion < 0 || posicion > a.length)
            throw new IndexOutOfBoundsException("Posicion invalida");
        int[] n = new int[a.length + 1];
        for (int i = 0; i < posicion; i++)
            n[i] = a[i];
        n[posicion] = valor;
        for (int i = posicion; i < a.length; i++)
            n[i + 1] = a[i];
        return n;
    }
    // Eliminar desplaza los posteriores una posicion a la izquierda.
    public static int[] eliminar(int[] a, int posicion) {
        if (posicion < 0 || posicion >= a.length)
            throw new IndexOutOfBoundsException("Posicion invalida");
        int[] n = new int[a.length - 1];
        for (int i = 0; i < posicion; i++)
            n[i] = a[i];
        for (int i = posicion + 1; i < a.length; i++)
            n[i - 1] = a[i];
        return n;
    }

    public static int[] actualizar(int[] a, int posicion, int valor) {
        if (posicion < 0 || posicion >= a.length)
            throw new IndexOutOfBoundsException("Posicion invalida");
        int[] n = copiar(a);
        n[posicion] = valor;
        return n;
    }

    public static int[] copiar(int[] a) {
        int[] n = new int[a.length];
        for (int i = 0; i < a.length; i++)
            n[i] = a[i];
        return n;
    }

    public static boolean comparar(int[] a, int[] b) {
        if (a.length != b.length)
            return false;
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                return false;
        return true;
    }

    public static int[] fusionar(int[] a, int[] b) {
        int[] n = new int[a.length + b.length];
        for (int i = 0; i < a.length; i++)
            n[i] = a[i];
        for (int i = 0; i < b.length; i++)
            n[a.length + i] = b[i];
        return n;
    }
    // Algoritmos de ordenamiento adicionales para demostrar comparaciones e intercambios.
    public static int[] burbuja(int[] arreglo) {
        int[] a = copiar(arreglo);
        for (int i = 0; i < a.length - 1; i++) {
            boolean cambio = false;
            for (int j = 0; j < a.length - i - 1; j++)
                if (a[j] > a[j + 1]) {
                    int tmp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = tmp;
                    cambio = true;
                }
            if (!cambio)
                break;
        }
        return a;
    }

    public static int[] seleccion(int[] arreglo) {
        int[] a = copiar(arreglo);
        for (int i = 0; i < a.length; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++)
                if (a[j] < a[min])
                    min = j;
            int tmp = a[i];
            a[i] = a[min];
            a[min] = tmp;
        }
        return a;
    }

    public static int[] insercion(int[] arreglo) {
        int[] a = copiar(arreglo);
        for (int i = 1; i < a.length; i++) {
            int clave = a[i], j = i - 1;
            while (j >= 0 && a[j] > clave) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = clave;
        }
        return a;
    }
    /** Traza academica: explica cada pasada sin alterar el arreglo real.
     * Se muestra como maximo 12 pasos para que el laboratorio sea legible. */
    public static String trazaBurbuja(int[] origen) {
        int[] a = copiar(origen);
        StringBuilder s = new StringBuilder("BURBUJA: se comparan vecinos y se intercambian si estan invertidos.\n"
            + "Antes: " + mostrar(a) + "\n");
        long comparaciones = 0, intercambios = 0;
        for (int i = 0; i < a.length - 1; i++) {
            boolean cambio = false;
            for (int j = 0; j < a.length - i - 1; j++) {
                comparaciones++;
                if (a[j] > a[j + 1]) {
                    int aux = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = aux;
                    intercambios++;
                    cambio = true;
                }
            }
            if (i < 12)
                s.append("Pasada ").append(i + 1).append(": ").append(mostrar(a)).append('\n');
            if (!cambio)
                break;
        }
        return s.append("Comparaciones: ").append(comparaciones)
        .append(" | Intercambios: ").append(intercambios)
        .append("\nDespues: ").append(mostrar(a)).toString();
    }

    public static String trazaSeleccion(int[] origen) {
        int[] a = copiar(origen);
        StringBuilder s = new StringBuilder("SELECCION: buscamos el menor de la parte pendiente y lo colocamos adelante.\n"
            + "Antes: " + mostrar(a) + "\n");
        long comparaciones = 0, intercambios = 0;
        for (int i = 0; i < a.length; i++) {
            int menor = i;
            for (int j = i + 1; j < a.length; j++) {
                comparaciones++;
                if (a[j] < a[menor])
                    menor = j;
            }
            int elegido = a[menor];
            if (menor != i) {
                int aux = a[i];
                a[i] = a[menor];
                a[menor] = aux;
                intercambios++;
            }
            if (i < 12)
                s.append("Paso ").append(i + 1).append(": minimo ").append(elegido)
                .append(" -> ").append(mostrar(a)).append('\n');
        }
        return s.append("Comparaciones: ").append(comparaciones)
        .append(" | Intercambios efectivos: ").append(intercambios)
        .append("\nDespues: ").append(mostrar(a)).toString();
    }

    public static String trazaInsercion(int[] origen) {
        int[] a = copiar(origen);
        StringBuilder s = new StringBuilder("INSERCION: desplazamos los mayores para ubicar la clave.\n"
            + "Antes: " + mostrar(a) + "\n");
        long comparaciones = 0, desplazamientos = 0;
        for (int i = 1; i < a.length; i++) {
            int clave = a[i], j = i - 1;
            while (j >= 0) {
                comparaciones++;
                if (a[j] <= clave)
                    break;
                a[j + 1] = a[j];
                desplazamientos++;
                j--;
            }
            a[j + 1] = clave;
            if (i <= 12)
                s.append("Paso ").append(i).append(": insertar ").append(clave)
                .append(" -> ").append(mostrar(a)).append('\n');
        }
        return s.append("Comparaciones: ").append(comparaciones)
        .append(" | Desplazamientos: ").append(desplazamientos)
        .append("\nDespues: ").append(mostrar(a)).toString();
    }

    public static String mostrar(int[] a) {
        return Arrays.toString(a);
    }
}
