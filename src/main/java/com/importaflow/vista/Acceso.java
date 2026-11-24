package com.importaflow.vista;

import com.importaflow.dao.UsuarioDAO;
import com.importaflow.modelo.Usuario;
import com.importaflow.servicio.GestorPedidos;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.sql.SQLException;

/** Ventana visible inmediatamente; conexión y autenticación fuera del EDT. */
public class Acceso extends JFrame {
    private final JTextField txtUsuario = new JTextField("admin", 18);
    private final JPasswordField txtClave = new JPasswordField(18);
    private final JLabel mensaje = new JLabel("Comprobando conexión con MySQL…");
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final JButton entrar = new JButton("INGRESAR AL SISTEMA"),
        reintentar = new JButton("Reintentar conexión");
    private final JLabel aviso = new JLabel(" ");
    private boolean ocupado;
    private record Sesion(Usuario usuario, GestorPedidos gestor, VentanaSistema.DatosIniciales datos) {
    }

    public Acceso() {
        setTitle("ImportaFlow 3.2 | Inicio de sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(550, 535);
        setMinimumSize(new Dimension(490, 480));
        setLocationRelativeTo(null);
        JPanel fondo = new JPanel(new BorderLayout());
        fondo.setBackground(new Color(242, 246, 250));
        JPanel banner = new JPanel(new GridLayout(0, 1, 2, 2));
        banner.setBackground(new Color(24, 43, 69));
        banner.setBorder(BorderFactory.createEmptyBorder(27, 34, 26, 34));
        JLabel l = new JLabel("IMPORTAFLOW");
        l.setFont(new Font("SansSerif", Font.BOLD, 28));
        l.setForeground(Color.WHITE);
        JLabel s = new JLabel("Plataforma de gestión de envíos y paquetería");
        s.setForeground(new Color(195, 210, 226));
        banner.add(l);
        banner.add(s);
        fondo.add(banner, BorderLayout.NORTH);
        JPanel datos = new JPanel(new GridLayout(0, 1, 5, 6));
        datos.setOpaque(false);
        datos.setBorder(BorderFactory.createEmptyBorder(22, 38, 18, 38));
        datos.add(new JLabel("Usuario"));
        datos.add(txtUsuario);
        datos.add(new JLabel("Contraseña"));
        datos.add(txtClave);
        char oculto = txtClave.getEchoChar();
        JCheckBox visible = new JCheckBox("Mostrar contraseña");
        visible.setOpaque(false);
        visible.addActionListener(e -> txtClave.setEchoChar(visible.isSelected() ? (char) 0 : oculto));
        datos.add(visible);
        entrar.setFont(new Font("SansSerif", Font.BOLD, 14));
        entrar.setFocusPainted(false);
        datos.add(entrar);
        datos.add(reintentar);
        mensaje.setForeground(new Color(31, 48, 67));
        datos.add(mensaje);
        aviso.setForeground(new Color(137, 75, 9));
        datos.add(aviso);
        fondo.add(datos, BorderLayout.CENTER);
        setContentPane(fondo);
        entrar.addActionListener(e -> acceder());
        txtClave.addActionListener(e -> acceder());
        reintentar.addActionListener(e -> comprobar());
        entrar.setEnabled(false);
        reintentar.setEnabled(false);
        SwingUtilities.invokeLater(this::comprobar);
    }

    private void estado(boolean trabajando, boolean listo, String texto) {
        ocupado = trabajando;
        entrar.setEnabled(!trabajando && listo);
        reintentar.setEnabled(!trabajando);
        mensaje.setText(texto);
    }

    private void comprobar() {
        if (ocupado)
            return;
        estado(true, false, "Comprobando conexión con MySQL…");
        new SwingWorker<Boolean, Void>() {

            protected Boolean doInBackground() throws Exception {
                return usuarios.inicializarAdmin();
            }

            protected void done() {
                try {
                    boolean nueva = get();
                    if (nueva)
                        aviso.setText("Instalación inicial: admin / Importa2026!");
                    estado(false, true, "MySQL conectado. Puedes ingresar.");
                } catch (Exception e) {
                    estado(false, false, "Sin conexión. Revisa la configuración y reintenta.");
                    diagnostico(e);
                }
            }
        }.execute();
    }

    private void acceder() {
        if (ocupado || !entrar.isEnabled())
            return;
        char[] clave = txtClave.getPassword();
        String nombre = txtUsuario.getText();
        txtClave.setText("");
        estado(true, false, "Validando acceso y cargando módulos…");
        new SwingWorker<Sesion, Void>() {

            protected Sesion doInBackground() throws Exception {
                try {
                    Usuario u = usuarios.autenticar(nombre, clave);
                    if (u == null)
                        return null;
                    GestorPedidos g = new GestorPedidos();
                    return new Sesion(u, g, VentanaSistema.cargarDatos(u));
                } finally {
                    Arrays.fill(clave, '\0');
                }
            }

            protected void done() {
                try {
                    Sesion s = get();
                    if (s == null) {
                        estado(false, true, "Usuario o contraseña incorrectos, o cuenta inactiva.");
                        return;
                    }
                    new VentanaSistema(s.usuario(), s.gestor(), s.datos()).setVisible(true);
                    dispose();
                } catch (Exception e) {
                    estado(false, true, "No se pudo ingresar. Consulta el diagnóstico.");
                    diagnostico(e);
                }
            }
        }.execute();
    }

    private void diagnostico(Exception e) {
        Throwable causa = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
        causa.printStackTrace(System.err);
        String detalle = causa.getMessage();
        if (detalle == null)
            detalle = causa.getClass().getSimpleName();
        String sql = causa instanceof SQLException x ? "\nSQLState: " + x.getSQLState() + " | Código: " + x
            .getErrorCode() : "";
        JTextArea a = new JTextArea("No se pudo completar el inicio.\n\n" + detalle + sql
            + "\n\nComprueba que MySQL esté iniciado, que exista la base seleccionada y que db.local.properties o db.properties tenga los datos correctos.\nSi editas db.properties desde NetBeans, ejecuta Clean and Build.\nConsulta Output para ver el detalle técnico.", 10, 45);
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(a), "Diagnóstico de ImportaFlow",
            JOptionPane.ERROR_MESSAGE);
    }
}
