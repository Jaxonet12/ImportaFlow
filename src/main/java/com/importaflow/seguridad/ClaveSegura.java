package com.importaflow.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 + salt aleatorio; evita guardar contrasenas en texto plano. */
public final class ClaveSegura {
    private static final int ITERACIONES = 160000;

    private ClaveSegura() {
    }

    public static String generar(char[] clave) {
        if (clave == null || clave.length < 10)
            throw new IllegalArgumentException("La contraseña debe tener al menos 10 caracteres.");
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return "pbkdf2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
        + "$" + Base64.getEncoder().encodeToString(derivar(clave, sal, ITERACIONES));
    }

    public static boolean verificar(char[] clave, String guardado) {
        try {
            String[] datos = guardado.split("\\$");
            if (datos.length != 4 || !"pbkdf2".equals(datos[0]))
                return false;
            int iteraciones = Integer.parseInt(datos[1]);
            if (iteraciones < 10000 || iteraciones > 1000000)
                return false;
            byte[] sal = Base64.getDecoder().decode(datos[2]);
            byte[] esperado = Base64.getDecoder().decode(datos[3]);
            return MessageDigest.isEqual(esperado, derivar(clave, sal, iteraciones));
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] derivar(char[] clave, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar hash seguro", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
