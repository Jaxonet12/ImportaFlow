package com.importaflow.servicio;
import com.importaflow.modelo.Cliente;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
public final class ValidacionLogistica {

    private ValidacionLogistica() {
    }

    public static void cliente(Cliente c) {
        if (!c.documento().matches("[0-9]{8}|[0-9]{11}"))
            throw new IllegalArgumentException("DNI de 8 dígitos o RUC de 11 dígitos.");
        if (!c.telefono().isBlank())
            telefono(c.telefono());
        if (!c.correo().isBlank() && !c.correo().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Correo inválido.");
    }

    public static void telefono(String t) {
        if (!t.matches("[0-9]{9}"))
            throw new IllegalArgumentException("El teléfono debe tener 9 dígitos.");
    }

    public static void entrega(String t, String d) {
        telefono(t);
        if (d == null || d.isBlank() || d.length() > 200)
            throw new IllegalArgumentException("Ingresa la dirección de entrega (máximo 200 caracteres).");
    }

    public static BigDecimal monto(String s) {
        try {
            BigDecimal m = new BigDecimal(s.trim().replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
            if (m.signum() <= 0 || m.compareTo(new BigDecimal("99999999.99")) > 0)
                throw new ArithmeticException();
            return m;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Monto positivo con máximo dos decimales.");
        }
    }

    public static BigDecimal tarifa(String s) {
        try {
            BigDecimal m = new BigDecimal(s.trim().replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
            if (m.signum() < 0 || m.compareTo(new BigDecimal("9999999.00")) > 0)
                throw new ArithmeticException();
            return m;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Tarifa no negativa con máximo dos decimales.");
        }
    }
    /** El inicio del procesamiento se reserva a la cola de prioridad. */
    public static boolean transicionManual(String anterior, String nuevo) {
        return switch (nuevo) {
            case "EN_TRANSITO" -> "EN_PROCESO".equals(anterior);
            case "ENTREGADO" -> "EN_TRANSITO".equals(anterior);
            case "CANCELADO" -> "PENDIENTE".equals(anterior) || "EN_PROCESO".equals(anterior);
            default -> false;
        };
    }

    public static LocalDate fecha(String s) {
        return s.isBlank() ? null : LocalDate.parse(s.trim());
    }

    public static boolean enPeriodo(String f, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null)
            return true;
        if (f == null || f.length() < 10)
            return false;
        LocalDate d = LocalDate.parse(f.substring(0, 10));
        return (desde == null || !d.isBefore(desde)) && (hasta == null || !d.isAfter(hasta));
    }
}
