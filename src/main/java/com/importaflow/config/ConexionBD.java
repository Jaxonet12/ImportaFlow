package com.importaflow.config;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Configuración recargable; los secretos locales no se publican en Git. */
public final class ConexionBD {

    private ConexionBD() {
    }

    public static Properties configuracion() throws SQLException {
        Properties p = new Properties();
        for (String recurso : new String[]{"/db.defaults.properties", "/db.properties"}) {
            try (InputStream in = ConexionBD.class.getResourceAsStream(recurso)) {
                if (in != null)
                    p.load(in);
            } catch (IOException ex) {
                throw new SQLException("No se pudo leer " + recurso, ex);
            }
        }
        String ruta = System.getProperty("importaflow.config", "db.local.properties");
        try {
            Path local = Path.of(ruta);
            if (Files.exists(local))
                try (InputStream in = Files.newInputStream(local)) {
                    p.load(in);
                }
        } catch (IOException | RuntimeException ex) {
            throw new SQLException("No se pudo leer la configuración local.", ex);
        }
        String[] claves = {"db.url", "db.user", "db.password"};
        String[] variables = {"IMPORTAFLOW_DB_URL", "IMPORTAFLOW_DB_USER", "IMPORTAFLOW_DB_PASSWORD"};
        for (int i = 0; i < claves.length; i++) {
            String valor = System.getenv(variables[i]);
            if (valor != null)
                p.setProperty(claves[i], valor);
        }
        return p;
    }

    public static Connection conectar() throws SQLException {
        Properties p = configuracion();
        String url = p.getProperty("db.url", "").trim();
        if (!url.startsWith("jdbc:mysql://"))
            throw new SQLException("Configura db.url con una URL JDBC de MySQL.");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("No se encontró el controlador MySQL. Recarga las dependencias Maven.", ex);
        }
        Properties credenciales = new Properties();
        credenciales.setProperty("user", p.getProperty("db.user", "root").trim());
        credenciales.setProperty("password", p.getProperty("db.password", ""));
        credenciales.setProperty("connectTimeout", "5000");
        credenciales.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(url, credenciales);
    }
}
