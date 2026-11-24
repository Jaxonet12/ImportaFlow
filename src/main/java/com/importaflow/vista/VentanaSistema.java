package com.importaflow.vista;

import com.importaflow.dao.SistemaDAO;
import com.importaflow.dao.UsuarioDAO;
import com.importaflow.modelo.*;
import com.importaflow.servicio.GestorPedidos;
import com.importaflow.servicio.ValidacionLogistica;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

/**
 * Sistema empresarial de paqueteria.
 * Los datos permanentes viven en MySQL y los algoritmos AED se aplican a
 * instantaneas en memoria. Los ejercicios del laboratorio NO alteran MySQL.
 */
public class VentanaSistema extends JFrame {
    private static final Color NAVY = new Color(24, 42, 67), NAVY2 = new Color(37, 68, 105);
    private static final Color AZUL = new Color(40, 110, 181), FONDO = new Color(243, 247, 251);
    private static final Color TEXTO = new Color(31, 48, 67), MUTED = new Color(99, 115, 130);
    private final Usuario sesion;
    private final GestorPedidos gestor;
    private final SistemaDAO dao = new SistemaDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final CardLayout navegacion = new CardLayout();
    private final JPanel paginas = new JPanel(navegacion);
    private final Map<String, JButton> menu = new LinkedHashMap<>();
    private final JLabel nombrePagina = new JLabel("Panel principal");
    private final JPanel metrics = new JPanel(new GridLayout(2, 4, 8, 8));
    private final DefaultTableModel mClientes = modelo("ID", "Documento", "Nombre", "Telefono", "Correo",
        "Direccion");
    private final DefaultTableModel mEnvios = modelo("ID", "Codigo", "Remitente", "Destinatario", "Destino",
        "Contenido", "Prioridad", "Estado", "Tarifa", "Saldo");
    private final DefaultTableModel mIngresos = modelo("Fecha", "Codigo", "Monto", "Metodo", "Usuario");
    private final DefaultTableModel mMovimientos = modelo("Fecha", "Codigo", "Anterior", "Nuevo", "Observacion",
        "Usuario");
    private final DefaultTableModel mUsuarios = modelo("ID", "Usuario", "Nombre", "Rol", "Activo");
    private final JTable tClientes = tabla(mClientes), tEnvios = tabla(mEnvios), tIngresos = tabla(mIngresos),
    tMovimientos = tabla(mMovimientos), tUsuarios = tabla(mUsuarios);
    private final JComboBox<Cliente> cbRemitente = new JComboBox<>();
    private final JComboBox<Envio> cbPagoEnvio = new JComboBox<>();
    private final JTextField cDocumento = new JTextField(), cNombre = new JTextField(),
        cTelefono = new JTextField(),
    cCorreo = new JTextField(), cDireccion = new JTextField();
    private final JTextField eContenido = new JTextField(), eOrigen = new JTextField("Lima, Peru"),
        eCantidad = new JTextField("1"),
    eDestinatario = new JTextField(), eTelefono = new JTextField(), eDestino = new JTextField(),
    eDireccion = new JTextField(), ePeso = new JTextField("1.00"), eTarifa = new JTextField("20.00");
    private final JComboBox<String> ePrioridad = new JComboBox<>(new String[]{"NORMAL", "URGENTE"});
    private final JLabel lblRegistro = new JLabel("REGISTRO DE NUEVO PAQUETE");
    private final JTextField buscarEnvio = new JTextField(20), montoPago = new JTextField(10),
        uUsuario = new JTextField(), uNombre = new JTextField();
    private final JComboBox<String> metodoPago = new JComboBox<>(new String[]{"EFECTIVO", "TARJETA",
        "TRANSFERENCIA", "YAPE", "PLIN"});
    private final JComboBox<String> rolUsuario = new JComboBox<>(new String[]{"OPERADOR", "CONSULTA", "ADMIN"});
    private final JPasswordField claveUsuario = new JPasswordField();
    private final DefaultTableModel mRecientes = modelo("Fecha", "Codigo", "Estado", "Observacion");
    private final JTable tRecientes = tabla(mRecientes);
    private final DefaultTableModel mReporte = modelo("Categoria", "Indicador", "Resultado");
    private final JTable tReporte = tabla(mReporte);
    private final JLabel totalReportado = new JLabel(" ");
    private final JTextField buscarCliente = new JTextField(16), buscarRemitente = new JTextField(),
        desdeReporte = new JTextField(10), hastaReporte = new JTextField(10);
    private final JComboBox<String> filtroEstado = new JComboBox<>(new String[]{"Todos", "PENDIENTE", "EN_PROCESO",
        "EN_TRANSITO", "ENTREGADO", "CANCELADO"}), filtroPrioridad = new JComboBox<>(new String[]{"Todas", "NORMAL",
        "URGENTE"});
    private final JPanel distribucion = new JPanel(new GridLayout(1, 5, 6, 2));
    private final JLabel progresoEnvio = new JLabel("Selecciona un paquete para ver su recorrido."),
        resumenPago = new JLabel("Selecciona un envío."), resumenInicio = new JLabel(" ");
    private final JLabel avisoPago = new JLabel("Registra abonos relacionados con un envio y su saldo.");
    private final JLabel estadoFooter = new JLabel("Datos en MySQL | Algoritmos AED en Java");
    private List<Envio> envios = new ArrayList<>();
    private List<Cliente> clientes = new ArrayList<>();
    private int clienteEditando = 0, envioEditando = 0;
    private JButton btnGuardarCliente, btnEliminarCliente, btnGuardarEnvio, btnAtender, btnAvanzar, btnCancelar,
    btnIngreso, btnCrearUsuario, btnCambiarEstadoUsuario, btnClaveUsuario, btnEditarEnvio;

    /** Instantánea de arranque: se consulta fuera del hilo gráfico. */
    public record DatosIniciales(List<Cliente> clientes, List<Envio> envios,
        List<SistemaDAO.Ingreso> ingresos, List<Usuario> usuarios,
        List<SistemaDAO.Movimiento> movimientos, Map<String, BigDecimal> porMetodo) {
    }

    public static DatosIniciales cargarDatos(Usuario sesion) throws Exception {
        SistemaDAO dao = new SistemaDAO();
        UsuarioDAO usuarios = new UsuarioDAO();
        Usuario actual = usuarios.buscarPorId(sesion.id());
        if (actual == null || !actual.activo() || !actual.rol().equals(sesion.rol()))
            throw new IllegalStateException("La cuenta ya no tiene autorización.");
        return new DatosIniciales(dao.clientes(), dao.envios(), dao.ingresos(),
            sesion.administra() ? usuarios.listar() : List.of(), dao.movimientos(0), dao.ingresosPorMetodo());
    }

    public VentanaSistema(Usuario sesion, GestorPedidos gestor, DatosIniciales datos) throws Exception {
        this.sesion = sesion;
        this.gestor = gestor;
        setTitle("ImportaFlow 3.2 | Plataforma de paqueteria - " + sesion.nombre());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1350, 820);
        setMinimumSize(new Dimension(1060, 660));
        setLocationRelativeTo(null);
        construir();
        aplicarDatos(datos);
        renderizarReporte(null, null, datos.porMetodo());
        mostrar("INICIO", "Panel principal");
    }

    private static DefaultTableModel modelo(String... cols) {
        return new DefaultTableModel(cols, 0) {

            @Override public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private static JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m) {

            @Override public String getToolTipText(MouseEvent e) {
                int fila = rowAtPoint(e.getPoint()), columna = columnAtPoint(e.getPoint());
                if (fila < 0 || columna < 0)
                    return null;
                Object valor = getValueAt(fila, columna);
                return valor == null ? null : String.valueOf(valor);
            }
        };
        t.setRowHeight(28);
        t.setAutoCreateRowSorter(true);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setGridColor(new Color(229, 234, 240));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(225, 233, 242));
        t.getTableHeader().setForeground(TEXTO);
        t.setSelectionBackground(new Color(207, 227, 248));
        t.setSelectionForeground(TEXTO);
        return t;
    }

    private static JTextArea area() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(new Font("Monospaced", Font.PLAIN, 13));
        a.setForeground(TEXTO);
        a.setBackground(Color.WHITE);
        a.setMargin(new Insets(16, 16, 16, 16));
        return a;
    }

    private JLabel titulo(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("SansSerif", Font.BOLD, 18));
        l.setForeground(TEXTO);
        return l;
    }

    private JLabel pista(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(MUTED);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }

    private JButton boton(String txt) {
        JButton b = new JButton(txt) {

            @Override protected void paintComponent(Graphics g) {
                Graphics2D gg = (Graphics2D) g.create();
                gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gg.setColor(getModel().isPressed() ? new Color(183, 207, 235) : getModel().isRollover()
                    ? new Color(207, 225, 244) : new Color(228, 238, 249));
                gg.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                gg.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(new Color(19, 47, 80));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return b;
    }

    private JButton primario(String texto) {
        JButton b = boton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        return b;
    }

    private JPanel ficha() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(224, 233, 242)),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return p;
    }

    private JPanel cuerpo() {
        JPanel p = new JPanel(new BorderLayout(12, 14));
        p.setBackground(FONDO);
        p.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        return p;
    }

    private JPanel botones(JButton... bs) {
        JPanel p = bs.length > 4 ? new JPanel(new GridLayout(0, 4, 8, 8))
        : new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        for (JButton b : bs)
            p.add(b);
        return p;
    }

    private JPanel campo(String etiqueta, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = pista(etiqueta);
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    private JPanel formulario(int columnas) {
        JPanel p = new JPanel(new GridLayout(0, columnas, 13, 12));
        p.setOpaque(false);
        return p;
    }

    private JScrollPane scrollear(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(new Color(224, 233, 242)));
        return s;
    }

    private void construir() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(FONDO);
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setPreferredSize(new Dimension(253, 0));
        lateral.setBackground(NAVY);
        JLabel marca = new JLabel("  IMPORTAFLOW  3.2");
        marca.setFont(new Font("SansSerif", Font.BOLD, 20));
        marca.setForeground(Color.WHITE);
        marca.setBorder(BorderFactory.createEmptyBorder(28, 10, 28, 0));
        lateral.add(marca, BorderLayout.NORTH);
        JPanel enlaces = new JPanel(new GridLayout(0, 1, 0, 7));
        enlaces.setBackground(NAVY);
        enlaces.setBorder(BorderFactory.createEmptyBorder(6, 13, 6, 13));
        agregarMenu(enlaces, "INICIO", "Panel principal");
        agregarMenu(enlaces, "CLIENTES", "Clientes");
        agregarMenu(enlaces, "RECEPCION", "Recepcion de paquetes");
        agregarMenu(enlaces, "ENVIOS", "Gestion y seguimiento");
        agregarMenu(enlaces, "INGRESOS", "Ingresos y cobros");
        agregarMenu(enlaces, "REPORTES", "Reportes");
        agregarMenu(enlaces, "AED", "Algoritmos y estructuras");
        if (sesion.administra())
            agregarMenu(enlaces, "USUARIOS", "Usuarios y roles");
        JPanel alineador = new JPanel(new BorderLayout());
        alineador.setOpaque(false);
        alineador.add(enlaces, BorderLayout.NORTH);
        lateral.add(alineador, BorderLayout.CENTER);
        JPanel cuenta = new JPanel(new GridLayout(0, 1, 4, 8));
        cuenta.setBackground(NAVY2);
        cuenta.setBorder(BorderFactory.createEmptyBorder(17, 16, 18, 16));
        JLabel usuario = new JLabel(sesion.nombre());
        usuario.setForeground(Color.WHITE);
        JLabel rol = new JLabel(sesion.rol());
        rol.setForeground(new Color(192, 207, 224));
        JButton salir = new JButton("Cerrar sesion");
        salir.setForeground(Color.BLACK);
        salir.addActionListener(e -> {
            dispose();
            try {
                new Acceso().setVisible(true);
            } catch (Exception ex) {
                error(ex);
            }
        });
        JButton miClave = boton("Cambiar mi clave");
        miClave.addActionListener(e -> ejecutar(() -> cambiarMiClave()));
        cuenta.add(usuario);
        cuenta.add(rol);
        cuenta.add(miClave);
        cuenta.add(salir);
        lateral.add(cuenta, BorderLayout.SOUTH);
        root.add(lateral, BorderLayout.WEST);
        JPanel derecha = new JPanel(new BorderLayout());
        derecha.setBackground(FONDO);
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(17, 24, 17, 24));
        nombrePagina.setForeground(TEXTO);
        nombrePagina.setFont(new Font("SansSerif", Font.BOLD, 21));
        top.add(nombrePagina, BorderLayout.WEST);
        JButton reload = boton("Actualizar datos");
        reload.addActionListener(e -> ejecutar(() -> refrescar()));
        top.add(reload, BorderLayout.EAST);
        derecha.add(top, BorderLayout.NORTH);
        paginas.add(crearInicio(), "INICIO");
        paginas.add(crearClientes(), "CLIENTES");
        paginas.add(crearRecepcion(), "RECEPCION");
        paginas.add(crearEnvios(), "ENVIOS");
        paginas.add(crearIngresos(), "INGRESOS");
        paginas.add(crearReportes(), "REPORTES");
        paginas.add(crearAED(), "AED");
        if (sesion.administra())
            paginas.add(crearUsuarios(), "USUARIOS");
        derecha.add(paginas, BorderLayout.CENTER);
        estadoFooter.setForeground(MUTED);
        estadoFooter.setBorder(BorderFactory.createEmptyBorder(7, 24, 9, 12));
        derecha.add(estadoFooter, BorderLayout.SOUTH);
        root.add(derecha, BorderLayout.CENTER);
        setContentPane(root);
    }
    // Los botones del menu se dibujan manualmente para evitar que el tema
    // visual de Windows deje texto claro sobre botones blancos.
    private void agregarMenu(JPanel p, String id, String txt) {
        JButton b = new JButton(txt) {

            @Override protected void paintComponent(Graphics g) {
                Graphics2D gg = (Graphics2D) g.create();
                gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean activo = Boolean.TRUE.equals(getClientProperty("activo"));
                Color fondo = activo ? new Color(45, 111, 184)
                : getModel().isRollover() ? new Color(38, 65, 100) : NAVY;
                gg.setColor(fondo);
                gg.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (activo) {
                    gg.setColor(new Color(139, 202, 255));
                    gg.fillRoundRect(0, 7, 4, Math.max(0, getHeight() - 14), 3, 3);
                }
                gg.dispose();
                super.paintComponent(g);
            }
        };
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 9));
        b.setPreferredSize(new Dimension(215, 44));
        b.addActionListener(e -> mostrar(id, txt));
        menu.put(id, b);
        p.add(b);
    }

    private void mostrar(String id, String titulo) {
        if (!menu.containsKey(id))
            return;
        navegacion.show(paginas, id);
        nombrePagina.setText(titulo);
        for (Map.Entry<String, JButton> entry : menu.entrySet()) {
            entry.getValue().putClientProperty("activo", entry.getKey().equals(id));
            entry.getValue().setForeground(Color.WHITE);
            entry.getValue().repaint();
        }
    }

    private JPanel crearInicio() {
        JPanel p = cuerpo();
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        JPanel encabezado = new JPanel(new GridLayout(0, 1, 0, 5));
        encabezado.setOpaque(false);
        encabezado.add(titulo("Resumen de operaciones"));
        encabezado.add(pista("Seguimiento de paqueteria, clientes y cobros en tiempo real"));
        cab.add(encabezado, BorderLayout.NORTH);
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(100, 200));
        cab.add(metrics, BorderLayout.CENTER);
        resumenInicio.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        JPanel resumen = new JPanel(new BorderLayout(4, 4));
        resumen.setOpaque(false);
        resumen.add(resumenInicio, BorderLayout.NORTH);
        distribucion.setOpaque(false);
        resumen.add(distribucion, BorderLayout.SOUTH);
        cab.add(resumen, BorderLayout.SOUTH);
        p.add(cab, BorderLayout.NORTH);
        JPanel reciente = ficha();
        reciente.add(titulo("Ultimos movimientos"), BorderLayout.NORTH);
        reciente.add(scrollear(tRecientes), BorderLayout.CENTER);
        p.add(reciente, BorderLayout.CENTER);
        JPanel abajo = ficha();
        abajo.add(pista("Flujo: registrar cliente  >  recibir paquete  >  cola de prioridades  >  en transito  >  entregar."
            + "  |  En el modulo AED puedes explicar cada estructura con datos del sistema."), BorderLayout.CENTER);
        p.add(abajo, BorderLayout.SOUTH);
        return p;
    }

    private JPanel crearClientes() {
        JPanel p = cuerpo();
        JPanel caja = ficha();
        JPanel form = formulario(3);
        form.add(campo("Documento / DNI / RUC", cDocumento));
        form.add(campo("Nombre completo o razon social", cNombre));
        form.add(campo("Telefono", cTelefono));
        form.add(campo("Correo", cCorreo));
        form.add(campo("Direccion", cDireccion));
        caja.add(form, BorderLayout.CENTER);
        btnGuardarCliente = primario("Guardar cliente");
        btnEliminarCliente = boton("Eliminar seleccionado");
        JButton limpiar = boton("Nuevo / limpiar");
        btnGuardarCliente.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            Cliente datos = new Cliente(clienteEditando, txt(cDocumento), txt(cNombre), txt(cTelefono),
                txt(cCorreo), txt(cDireccion));
            consultar(() -> {
                dao.guardarCliente(datos, sesion.usuario());
                return null;
            });
            limpiarCliente();
            refrescar();
            mensaje("Cliente guardado correctamente.");
        }));
        btnEliminarCliente.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            int id = idTabla(tClientes, mClientes);
            if (!confirmar("¿Eliminar este cliente? Solo se permite si no tiene envíos asociados."))
                return;
            consultar(() -> {
                dao.eliminarCliente(id, sesion.usuario());
                return null;
            });
            limpiarCliente();
            refrescar();
        }));
        limpiar.addActionListener(e -> limpiarCliente());
        caja.add(botones(btnGuardarCliente, btnEliminarCliente, limpiar), BorderLayout.SOUTH);
        p.add(caja, BorderLayout.NORTH);
        JPanel tabla = ficha();
        JPanel cab = new JPanel(new BorderLayout(8, 4));
        cab.setOpaque(false);
        cab.add(titulo("Directorio de clientes"), BorderLayout.WEST);
        cab.add(campo("Buscar por nombre o documento", buscarCliente), BorderLayout.EAST);
        tabla.add(cab, BorderLayout.NORTH);
        observar(buscarCliente,() -> {
            String q = txt(buscarCliente);
            ((TableRowSorter<?>) tClientes.getRowSorter()).setRowFilter(q.isBlank() ? null : RowFilter
                .regexFilter("(?i)" + Pattern.quote(q), 1, 2));
        });
        JButton verEnvios = boton("Ver envíos del cliente");
        verEnvios.addActionListener(e -> ejecutar(() -> {
            int id = idTabla(tClientes, mClientes);
            DefaultTableModel m = modelo("Código", "Destino", "Estado", "Prioridad");
            for (Envio v : envios)
                if (v.clienteId() == id)
                    m.addRow(new Object[]{v.codigo(), v.destino(), v.estado(), v.prioridad()});
            JScrollPane sc = scrollear(tabla(m));
            sc.setPreferredSize(new Dimension(620, 280));
            JOptionPane.showMessageDialog(this, sc, "Envíos asociados (" + m.getRowCount() + ")",
                JOptionPane.INFORMATION_MESSAGE);
        }));
        tabla.add(botones(verEnvios), BorderLayout.SOUTH);
        tabla.add(scrollear(tClientes), BorderLayout.CENTER);
        p.add(tabla, BorderLayout.CENTER);
        // Priorizamos nombre, correo y direccion para evitar cortes innecesarios.
        int[] anchos = {52, 125, 275, 125, 260, 230};
        for (int i = 0; i < anchos.length; i++)
            tClientes.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        tClientes.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || tClientes.getSelectedRow() < 0)
                return;
            int fila = tClientes.convertRowIndexToModel(tClientes.getSelectedRow());
            clienteEditando = (Integer) mClientes.getValueAt(fila, 0);
            cDocumento.setText(valor(mClientes, fila, 1));
            cNombre.setText(valor(mClientes, fila, 2));
            cTelefono.setText(valor(mClientes, fila, 3));
            cCorreo.setText(valor(mClientes, fila, 4));
            cDireccion.setText(valor(mClientes, fila, 5));
        });
        return p;
    }

    private void limpiarCliente() {
        clienteEditando = 0;
        tClientes.clearSelection();
        for (JTextField t : new JTextField[]{cDocumento, cNombre, cTelefono, cCorreo, cDireccion})
            t.setText("");
    }

    // Cada seccion usa dos columnas y campos de altura fija, evitando el
    // formulario anterior de tres columnas con casillas demasiado altas.
    private JPanel seccionRecepcion(String numero, String tituloSeccion, String ayuda, JPanel campos) {
        JPanel seccion = new JPanel(new BorderLayout(0, 12));
        seccion.setBackground(new Color(249, 251, 254));
        seccion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(223, 232, 242)),
            BorderFactory.createEmptyBorder(14, 16, 16, 16)));
        JPanel encabezado = new JPanel(new GridLayout(0, 1, 0, 3));
        encabezado.setOpaque(false);
        JLabel nombre = new JLabel(numero + "   " + tituloSeccion);
        nombre.setFont(new Font("SansSerif", Font.BOLD, 14));
        nombre.setForeground(TEXTO);
        encabezado.add(nombre);
        encabezado.add(pista(ayuda));
        seccion.add(encabezado, BorderLayout.NORTH);
        seccion.add(campos, BorderLayout.CENTER);
        seccion.setAlignmentX(Component.LEFT_ALIGNMENT);
        // BoxLayout conserva la altura natural aunque sobre espacio vertical.
        seccion.setMaximumSize(new Dimension(Integer.MAX_VALUE, seccion.getPreferredSize().height));
        return seccion;
    }

    private JPanel rejillaRecepcion() {
        JPanel p = new JPanel(new GridLayout(0, 2, 16, 12));
        p.setOpaque(false);
        return p;
    }

    private JPanel crearRecepcion() {
        JPanel p = cuerpo();
        JPanel form = ficha();
        JPanel arriba = new JPanel(new BorderLayout(0, 6));
        arriba.setOpaque(false);
        lblRegistro.setFont(new Font("SansSerif", Font.BOLD, 21));
        lblRegistro.setForeground(TEXTO);
        arriba.add(lblRegistro, BorderLayout.NORTH);
        arriba.add(pista("Completa la guia del envio. El sistema generara un codigo unico y su primer movimiento."),
            BorderLayout.SOUTH);
        form.add(arriba, BorderLayout.NORTH);

        // Alturas coherentes en los JTextField y JComboBox de recepcion.
        for (JTextField campo : new JTextField[]{eContenido, eOrigen, eCantidad, eDestinatario,
        eTelefono, eDestino, eDireccion, ePeso, eTarifa}) {
            campo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            campo.setForeground(TEXTO);
            campo.setBackground(Color.WHITE);
            campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(195, 208, 222)),
                BorderFactory.createEmptyBorder(8, 11, 8, 11)));
            campo.setPreferredSize(new Dimension(145, 38));
        }
        for (JComboBox<?> combo : new JComboBox<?>[]{cbRemitente, ePrioridad}) {
            combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            combo.setForeground(TEXTO);
            combo.setBackground(Color.WHITE);
            combo.setPreferredSize(new Dimension(145, 38));
        }

        JPanel datos = new JPanel();
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        datos.setOpaque(false);
        JPanel origen = rejillaRecepcion();
        JPanel selector = new JPanel(new BorderLayout(0, 4));
        selector.setOpaque(false);
        buscarRemitente.setToolTipText("Filtra por nombre o documento");
        buscarRemitente.setPreferredSize(new Dimension(145, 28));
        selector.add(buscarRemitente, BorderLayout.NORTH);
        selector.add(cbRemitente, BorderLayout.CENTER);
        origen.add(campo("Remitente (buscar y seleccionar)", selector));
        observar(buscarRemitente,() -> filtrarRemitentes());
        origen.add(campo("Contenido del paquete", eContenido));
        origen.add(campo("Ciudad / pais de origen", eOrigen));
        origen.add(campo("Cantidad de bultos", eCantidad));
        datos.add(seccionRecepcion("01", "Datos del paquete",
            "Identifica quien envia el paquete y las caracteristicas de su contenido.", origen));
        datos.add(Box.createVerticalStrut(12));

        JPanel destino = rejillaRecepcion();
        destino.add(campo("Nombre del destinatario", eDestinatario));
        destino.add(campo("Telefono del destinatario", eTelefono));
        destino.add(campo("Ciudad / pais de destino", eDestino));
        destino.add(campo("Direccion de entrega", eDireccion));
        datos.add(seccionRecepcion("02", "Datos de entrega",
            "Indica a quien y adonde se entregara el envio.", destino));
        datos.add(Box.createVerticalStrut(12));

        JPanel servicio = rejillaRecepcion();
        servicio.add(campo("Peso total (kg)", ePeso));
        servicio.add(campo("Tarifa del servicio (S/)", eTarifa));
        servicio.add(campo("Prioridad de atencion", ePrioridad));
        servicio.add(campo("Orden de atencion",
            pista("URGENTE primero; entre iguales, por llegada.")));
        datos.add(seccionRecepcion("03", "Condiciones del servicio",
            "El peso, precio y prioridad ayudan al control y la gestion de colas.", servicio));

        JScrollPane scroll = scrollear(datos);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        form.add(scroll, BorderLayout.CENTER);

        btnGuardarEnvio = primario("Registrar recepcion");
        JButton nuevo = boton("Limpiar formulario");
        JButton irClientes = boton("Ir a clientes");
        irClientes.addActionListener(e -> mostrar("CLIENTES", "Clientes"));
        btnGuardarEnvio.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            Cliente cl = (Cliente) cbRemitente.getSelectedItem();
            int editar = envioEditando;
            String contenido = txt(eContenido), ciudadOrigen = txt(eOrigen), prioridad = String.valueOf(ePrioridad
                .getSelectedItem()), destinatario = txt(eDestinatario), telefono = txt(eTelefono),
                ciudadDestino = txt(eDestino), direccion = txt(eDireccion);
            int cantidad = entero(eCantidad);
            double peso = decimal(ePeso);
            BigDecimal tarifa = ValidacionLogistica.tarifa(txt(eTarifa));
            int id = consultar(() -> {
                if (editar == 0)
                    return dao.recibir(cl, contenido, ciudadOrigen, cantidad, prioridad, destinatario, telefono,
                        ciudadDestino, direccion, peso, tarifa, sesion.usuario());
                dao.actualizarEnvio(editar, cl, contenido, ciudadOrigen, cantidad, prioridad, destinatario,
                    telefono, ciudadDestino, direccion, peso, tarifa, sesion.usuario());
                return editar;
            });
            gestor.registrarAccion((editar == 0 ? "Recepción " : "Editado ") + SistemaDAO.codigo(id));
            limpiarEnvio();
            refrescar();
            for (Envio v : envios)
                if (v.id() == id) {
                    mostrarFicha(v);
                    break;
                }
        }));
        nuevo.addActionListener(e -> limpiarEnvio());
        form.add(botones(btnGuardarEnvio, nuevo, irClientes), BorderLayout.SOUTH);
        p.add(form, BorderLayout.CENTER);
        JPanel nota = ficha();
        nota.add(pista("Los cobros se registran en Ingresos y el estado se cambia desde Gestion y seguimiento."));
        p.add(nota, BorderLayout.SOUTH);
        return p;
    }

    private void limpiarEnvio() {
        envioEditando = 0;
        lblRegistro.setText("REGISTRO DE NUEVO PAQUETE");
        for (JTextField t : new JTextField[]{eContenido, eDestinatario, eTelefono, eDestino, eDireccion})
            t.setText("");
        eOrigen.setText("Lima, Peru");
        eCantidad.setText("1");
        ePeso.setText("1.00");
        eTarifa.setText("20.00");
        ePrioridad.setSelectedIndex(0);
        buscarRemitente.setText("");
        if (cbRemitente.getItemCount() > 0)
            cbRemitente.setSelectedIndex(0);
    }

    private void cargarEnvioParaEditar() {
        Envio e = seleccionarEnvio();
        if (!"PENDIENTE".equals(e.estado()))
            throw new IllegalArgumentException("Solo edita paquetes pendientes.");
        buscarRemitente.setText("");
        cbRemitente.setSelectedIndex(-1);
        envioEditando = e.id();
        lblRegistro.setText("MODIFICAR " + e.codigo());
        for (int i = 0; i < cbRemitente.getItemCount(); i++)
            if (cbRemitente.getItemAt(i).id() == e.clienteId())
                cbRemitente.setSelectedIndex(i);
        eContenido.setText(e.contenido());
        eOrigen.setText(e.origen());
        eCantidad.setText(Integer.toString(e.cantidad()));
        eDestinatario.setText(e.destinatario());
        eTelefono.setText(e.telefono());
        eDestino.setText(e.destino());
        eDireccion.setText(e.direccion());
        ePeso.setText(Double.toString(e.peso()));
        eTarifa.setText(e.tarifa().toPlainString());
        ePrioridad.setSelectedItem(e.prioridad());
        mostrar("RECEPCION", "Recepcion de paquetes");
    }

    private JPanel crearEnvios() {
        JPanel p = cuerpo();
        JPanel caja = ficha();
        JPanel top = new JPanel(new BorderLayout(12, 8));
        top.setOpaque(false);
        top.add(titulo("Expedientes de paquetes"), BorderLayout.WEST);
        JPanel filtros = formulario(3);
        filtros.add(campo("Buscar código, cliente o destino", buscarEnvio));
        filtros.add(campo("Estado", filtroEstado));
        filtros.add(campo("Prioridad", filtroPrioridad));
        top.add(filtros, BorderLayout.SOUTH);
        filtroEstado.addActionListener(e -> filtrarEnvios());
        filtroPrioridad.addActionListener(e -> filtrarEnvios());
        caja.add(top, BorderLayout.NORTH);
        caja.add(scrollear(tEnvios), BorderLayout.CENTER);
        btnAtender = primario("Atender siguiente (prioridad)");
        btnAvanzar = boton("Avanzar seleccionado");
        btnCancelar = boton("Cancelar envio");
        btnEditarEnvio = boton("Editar pendiente");
        JButton ver = boton("Ver ficha / seguimiento");
        btnAtender.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            Pedido atendido = consultar(() -> gestor.atenderSiguiente(sesion.usuario()));
            if (atendido == null) {
                refrescar();
                mensaje("No hay paquetes pendientes.");
                return;
            }
            refrescar();
            mensaje("Siguiente por prioridad: " + SistemaDAO.codigo(atendido.getId()));
        }));
        btnAvanzar.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            Envio sel = seleccionarEnvio();
            String proximo = switch (sel.estado()) {
                case "PENDIENTE"
                    -> throw new IllegalArgumentException("Usa Atender siguiente (prioridad) para iniciar un paquete pendiente.");
                case "EN_PROCESO" -> "EN_TRANSITO";
                case "EN_TRANSITO" -> "ENTREGADO";
                default -> null;
            };
            if (proximo == null)
                throw new IllegalArgumentException("El paquete no admite otro avance.");
            if (!confirmar("Cambiar " + sel.codigo() + " a " + proximo + "?"))
                return;
            consultar(() -> {
                dao.cambiarEstado(sel.id(), proximo, "Actualizacion manual de seguimiento", sesion.usuario());
                return null;
            });
            gestor.registrarAccion("Estado " + sel.codigo() + " -> " + proximo);
            refrescar();
        }));
        btnCancelar.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            Envio sel = seleccionarEnvio();
            String motivo = JOptionPane.showInputDialog(this, "Motivo para cancelar " + sel.codigo() + ":");
            if (motivo == null)
                return;
            if (motivo.isBlank() || motivo.length() > 250)
                throw new IllegalArgumentException("Escribe un motivo entre 1 y 250 caracteres.");
            consultar(() -> {
                dao.cambiarEstado(sel.id(), "CANCELADO", motivo, sesion.usuario());
                return null;
            });
            gestor.registrarAccion("Cancelado " + sel.codigo());
            refrescar();
        }));
        btnEditarEnvio.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            cargarEnvioParaEditar();
        }));
        ver.addActionListener(e -> ejecutar(() -> mostrarFicha(seleccionarEnvio())));
        caja.add(botones(btnAtender, btnAvanzar, btnCancelar, btnEditarEnvio, ver), BorderLayout.SOUTH);
        p.add(caja, BorderLayout.CENTER);
        JPanel historial = ficha();
        historial.setPreferredSize(new Dimension(100, 225));
        JPanel cabHist = new JPanel(new GridLayout(0, 1, 0, 4));
        cabHist.setOpaque(false);
        cabHist.add(titulo("Historial de seguimiento"));
        cabHist.add(progresoEnvio);
        historial.add(cabHist, BorderLayout.NORTH);
        historial.add(scrollear(tMovimientos), BorderLayout.CENTER);
        p.add(historial, BorderLayout.SOUTH);
        tEnvios.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;
            try {
                if (tEnvios.getSelectedRow() >= 0) {
                    cargarMovimientos(idTabla(tEnvios, mEnvios));
                    actualizarSeguimiento();
                } else {
                    mMovimientos.setRowCount(0);
                    progresoEnvio.setText("Selecciona un paquete para ver su recorrido.");
                }
            } catch (Exception ex) {
                estadoFooter.setText("Seguimiento: " + ex.getMessage());
            }
        });
        observar(buscarEnvio,() -> filtrarEnvios());
        return p;
    }

    private void observar(JTextField campo, Runnable cambio) {
        campo.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                cambio.run();
            }

            public void removeUpdate(DocumentEvent e) {
                cambio.run();
            }

            public void changedUpdate(DocumentEvent e) {
                cambio.run();
            }
        });
    }

    private void filtrarRemitentes() {
        Cliente anterior = (Cliente) cbRemitente.getSelectedItem();
        String q = txt(buscarRemitente).toLowerCase(Locale.ROOT);
        cbRemitente.removeAllItems();
        for (Cliente c : clientes)
            if (c.toString().toLowerCase(Locale.ROOT).contains(q))
                cbRemitente.addItem(c);
        if (anterior != null)
            for (int i = 0; i < cbRemitente.getItemCount(); i++)
                if (cbRemitente.getItemAt(i).id() == anterior.id())
                    cbRemitente.setSelectedIndex(i);
    }

    private void filtrarEnvios() {
        List<RowFilter<Object, Object>> f = new ArrayList<>();
        String q = txt(buscarEnvio);
        if (!q.isBlank())
            f.add(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        if (filtroEstado.getSelectedIndex() > 0)
            f.add(RowFilter.regexFilter("^" + filtroEstado.getSelectedItem() + "$", 7));
        if (filtroPrioridad.getSelectedIndex() > 0)
            f.add(RowFilter.regexFilter("^" + filtroPrioridad.getSelectedItem() + "$", 6));
        ((TableRowSorter<?>) tEnvios.getRowSorter()).setRowFilter(f.isEmpty() ? null : RowFilter.andFilter(f));
    }

    private void actualizarSeguimiento() {
        Envio e = seleccionarEnvio();
        String[] estados = {"PENDIENTE", "EN_PROCESO", "EN_TRANSITO", "ENTREGADO"};
        String[] nombres = {"Recepción", "En proceso", "En tránsito", "Entregado"};
        int actual = Arrays.asList(estados).indexOf(e.estado());
        StringBuilder h = new StringBuilder("<html>");
        if (actual < 0) {
            h.append("<b style='color:#a34e37'>CANCELADO</b>");
        } else
            for (int i = 0; i < 4; i++) {
                if (i > 0)
                    h.append(" &nbsp;→&nbsp; ");
                h.append("<span style='color:").append(i < actual ? "#267553" : i == actual ? "#235d91" : "#8492a0")
                    .append("'>").append(i < actual ? "✓ " : i == actual ? "● " : "○ ").append(nombres[i])
                    .append("</span>");
            }
        progresoEnvio.setText(h.append("</html>").toString());
    }

    private void mostrarFicha(Envio e) throws Exception {
        StringBuilder s = new StringBuilder("CODIGO: " + e.codigo() + "\nESTADO: " + e.estado() + "\n\n"
            + "Remitente: " + e.cliente() + "\nDestinatario: " + e.destinatario() + "\nTelefono: " + e.telefono()
            + "\nOrigen: " + e.origen() + "\nDestino: " + e.destino() + "\nDireccion: " + e.direccion()
            + "\nContenido: " + e.contenido() + "\nBultos: " + e.cantidad() + "\nPeso: " + e.peso()
            + " kg\nPrioridad: " + e.prioridad() + "\nTarifa S/ " + dinero(e.tarifa())
            + "\nAbonado S/ " + dinero(e.abonado()) + "\nSaldo S/ " + dinero(e.saldo()) + "\n\nMOVIMIENTOS:\n");
        for (SistemaDAO.Movimiento m : consultar(() -> dao.movimientos(e.id())))
            s.append(m.fecha()).append("  ").append(m.nuevo())
            .append(" - ").append(m.observacion()).append(" (" + m.usuario() + ")\n");
        JTextArea a = area();
        a.setText(s.toString());
        a.setCaretPosition(0);
        JScrollPane sc = scrollear(a);
        sc.setPreferredSize(new Dimension(620, 440));
        JOptionPane.showMessageDialog(this, sc, "Ficha de paquete " + e.codigo(), JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel crearIngresos() {
        JPanel p = cuerpo();
        JPanel caja = ficha();
        JPanel fila = formulario(3);
        fila.add(campo("Envio", cbPagoEnvio));
        fila.add(campo("Monto a cobrar (S/)", montoPago));
        fila.add(campo("Medio de pago", metodoPago));
        caja.add(fila, BorderLayout.CENTER);
        btnIngreso = primario("Registrar ingreso");
        btnIngreso.addActionListener(e -> ejecutar(() -> {
            exigirEdicion();
            Envio sel = (Envio) cbPagoEnvio.getSelectedItem();
            if (sel == null)
                throw new IllegalArgumentException("Selecciona un envio.");
            BigDecimal abono = ValidacionLogistica.monto(txt(montoPago));
            String medio = String.valueOf(metodoPago.getSelectedItem());
            consultar(() -> {
                dao.registrarIngreso(sel.id(), abono, medio, sesion.usuario());
                return null;
            });
            gestor.registrarAccion("Cobro " + sel.codigo());
            montoPago.setText("");
            refrescar();
            mensaje("Ingreso registrado.");
        }));
        JButton saldo = boton("Completar monto con saldo"), hist = boton("Historial del envío");
        saldo.setEnabled(sesion.puedeEditar());
        saldo.addActionListener(e -> {
            Envio v = (Envio) cbPagoEnvio.getSelectedItem();
            if (v != null)
                montoPago.setText(v.saldo().setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString());
        });
        cbPagoEnvio.addActionListener(e -> {
            Envio v = (Envio) cbPagoEnvio.getSelectedItem();
            resumenPago.setText(v == null ? "No hay envíos seleccionados." : "Tarifa S/ " + dinero(v.tarifa())
                + "  |  Abonado S/ " + dinero(v.abonado()) + "  |  Saldo S/ " + dinero(v.saldo()));
        });
        hist.addActionListener(e -> ejecutar(() -> {
            Envio v = (Envio) cbPagoEnvio.getSelectedItem();
            String codigo = v == null ? "" : v.codigo();
            String elegido = JOptionPane.showInputDialog(this, "Código de envío para consultar cobros", codigo);
            if (elegido == null)
                return;
            DefaultTableModel m = modelo("Fecha", "Código", "Monto", "Método", "Usuario");
            for (SistemaDAO.Ingreso i : consultar(() -> dao.ingresosPorCodigo(elegido.trim())))
                m.addRow(new Object[]{i.fecha(), i.codigo(), dinero(i.monto()), i.metodo(), i.usuario()});
            JScrollPane sc = scrollear(tabla(m));
            sc.setPreferredSize(new Dimension(620, 280));
            JOptionPane.showMessageDialog(this, sc, "Historial de cobros", JOptionPane.INFORMATION_MESSAGE);
        }));
        JPanel acciones = new JPanel(new BorderLayout(0, 6));
        acciones.setOpaque(false);
        acciones.add(resumenPago, BorderLayout.NORTH);
        acciones.add(botones(btnIngreso, saldo, hist), BorderLayout.SOUTH);
        caja.add(acciones, BorderLayout.SOUTH);
        p.add(caja, BorderLayout.NORTH);
        JPanel centro = ficha();
        centro.add(titulo("Registro de cobros"), BorderLayout.NORTH);
        centro.add(scrollear(tIngresos), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);
        JPanel pie = ficha();
        pie.add(avisoPago, BorderLayout.CENTER);
        p.add(pie, BorderLayout.SOUTH);
        return p;
    }

    private JPanel crearReportes() {
        JPanel p = cuerpo();
        JPanel caja = ficha();
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.add(titulo("Resumen operacional y financiero"), BorderLayout.WEST);
        JButton exportar = primario("Exportar resumen CSV");
        exportar.addActionListener(e -> ejecutar(() -> exportarReporte()));
        cab.add(exportar, BorderLayout.EAST);
        JPanel filtros = formulario(4);
        filtros.add(campo("Desde (AAAA-MM-DD)", desdeReporte));
        filtros.add(campo("Hasta (AAAA-MM-DD)", hastaReporte));
        JButton aplicar = boton("Aplicar fechas"), limpiar = boton("Todo el historial");
        aplicar.addActionListener(e -> ejecutar(() -> actualizarReporte()));
        limpiar.addActionListener(e -> ejecutar(() -> {
            desdeReporte.setText("");
            hastaReporte.setText("");
            actualizarReporte();
        }));
        filtros.add(aplicar);
        filtros.add(limpiar);
        cab.add(filtros, BorderLayout.SOUTH);
        caja.add(cab, BorderLayout.NORTH);
        caja.add(scrollear(tReporte), BorderLayout.CENTER);
        p.add(caja, BorderLayout.CENTER);
        JPanel nota = ficha();
        nota.add(totalReportado, BorderLayout.CENTER);
        p.add(nota, BorderLayout.SOUTH);
        return p;
    }

    private void actualizarReporte() throws Exception {
        LocalDate desde = ValidacionLogistica.fecha(txt(desdeReporte)), hasta = ValidacionLogistica
            .fecha(txt(hastaReporte));
        if (desde != null && hasta != null && desde.isAfter(hasta))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final.");
        renderizarReporte(desde, hasta, consultar(() -> dao.ingresosPorMetodo(desde, hasta)));
    }

    private void renderizarReporte(LocalDate desde, LocalDate hasta, Map<String, BigDecimal> ingresos) {
        List<Envio> filtrados = envios.stream().filter(e -> ValidacionLogistica.enPeriodo(e.fecha(), desde, hasta))
            .toList();
        mReporte.setRowCount(0);
        Map<String, Integer> estados = new LinkedHashMap<>(), prioridades = new LinkedHashMap<>();
        for (Envio e : filtrados) {
            estados.merge(e.estado(), 1, Integer::sum);
            prioridades.merge(e.prioridad(), 1, Integer::sum);
        }
        mReporte.addRow(new Object[]{"Periodo",(desde == null ? "Inicio" : desde.toString()) + " a "
            + (hasta == null ? "Hoy / sin límite" : hasta.toString()), "Por fecha de recepción / cobro"});
        mReporte.addRow(new Object[]{"General", "Paquetes recibidos", filtrados.size()});
        mReporte.addRow(new Object[]{"General", "Clientes registrados (global)", clientes.size()});
        for (var it : estados.entrySet())
            mReporte.addRow(new Object[]{"Estado actual", it.getKey(), it.getValue()});
        for (var it : prioridades.entrySet())
            mReporte.addRow(new Object[]{"Prioridad", it.getKey(), it.getValue()});
        BigDecimal total = BigDecimal.ZERO;
        for (var it : ingresos.entrySet()) {
            mReporte.addRow(new Object[]{"Ingresos S/", it.getKey(), dinero(it.getValue())});
            total = total.add(it.getValue());
        }
        BigDecimal saldo = filtrados.stream().filter(e -> !"CANCELADO".equals(e.estado())).map(Envio::saldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        mReporte.addRow(new Object[]{"Ingresos S/", "Cobros del periodo", dinero(total)});
        mReporte.addRow(new Object[]{"Saldo S/", "Saldo actual de envíos del periodo", dinero(saldo)});
        totalReportado.setText("Cobrado en periodo: S/ " + dinero(total) + " | Saldo actual: S/ " + dinero(saldo));
    }

    private void exportarReporte() throws Exception {
        actualizarReporte();
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new java.io.File("importaflow_reporte.csv"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        java.io.File destino = selector.getSelectedFile();
        if (destino.exists() && !confirmar("El archivo ya existe. Deseas reemplazarlo?"))
            return;
        StringBuilder csv = new StringBuilder("\ufeffCategoria;Indicador;Resultado\r\n");
        for (int fila = 0; fila < mReporte.getRowCount(); fila++) {
            for (int col = 0; col < 3; col++) {
                if (col > 0)
                    csv.append(';');
                String v = String.valueOf(mReporte.getValueAt(fila, col)).replace("\"", "\"\"");
                csv.append('"').append(v).append('"');
            }
            csv.append("\r\n");
        }
        java.nio.file.Files.writeString(destino.toPath(), csv.toString(), java.nio.charset.StandardCharsets.UTF_8);
        mensaje("Reporte exportado: " + destino.getAbsolutePath());
    }

    private JPanel crearUsuarios() {
        JPanel p = cuerpo();
        JPanel caja = ficha();
        JPanel f = formulario(4);
        f.add(campo("Usuario (login)", uUsuario));
        f.add(campo("Nombre", uNombre));
        f.add(campo("Rol de acceso", rolUsuario));
        f.add(campo("Contrasena (minimo 10)", claveUsuario));
        caja.add(f, BorderLayout.CENTER);
        btnCrearUsuario = primario("Crear usuario");
        btnCambiarEstadoUsuario = boton("Activar / Desactivar");
        btnClaveUsuario = boton("Cambiar contrasena seleccionada");
        btnCrearUsuario.addActionListener(e -> ejecutar(() -> {
            exigirAdmin();
            String login = txt(uUsuario), nombre = txt(uNombre), rol = String.valueOf(rolUsuario.getSelectedItem());
            char[] pwd = claveUsuario.getPassword();
            try {
                consultar(() -> {
                    usuarios.crear(login, nombre, rol, pwd, sesion.usuario());
                    return null;
                });
            } finally {
                Arrays.fill(pwd, '\0');
            }
            uUsuario.setText("");
            uNombre.setText("");
            claveUsuario.setText("");
            refrescar();
            mensaje("Usuario creado.");
        }));
        btnCambiarEstadoUsuario.addActionListener(e -> ejecutar(() -> {
            exigirAdmin();
            int id = idTabla(tUsuarios, mUsuarios);
            if (id == sesion.id())
                throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
            int filaUsuario = tUsuarios.convertRowIndexToModel(tUsuarios.getSelectedRow());
            boolean activo = "SI".equals(mUsuarios.getValueAt(filaUsuario, 4));
            if (!confirmar((activo ? "Desactivar" : "Activar") + " usuario seleccionado?"))
                return;
            consultar(() -> {
                usuarios.cambiarActivo(id, !activo, sesion.usuario());
                return null;
            });
            refrescar();
        }));
        btnClaveUsuario.addActionListener(e -> ejecutar(() -> {
            exigirAdmin();
            int id = idTabla(tUsuarios, mUsuarios);
            if (id == sesion.id()) {
                cambiarMiClave();
                return;
            }
            JPasswordField clave = new JPasswordField(18);
            int ok = JOptionPane.showConfirmDialog(this, clave, "Nueva contrasena (minimo 10 caracteres)",
                JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION)
                return;
            char[] pwd = clave.getPassword();
            try {
                consultar(() -> {
                    usuarios.cambiarClave(id, pwd, sesion.usuario());
                    return null;
                });
            } finally {
                Arrays.fill(pwd, '\0');
            }
            mensaje("Contrasena actualizada.");
        }));
        caja.add(botones(btnCrearUsuario, btnCambiarEstadoUsuario, btnClaveUsuario), BorderLayout.SOUTH);
        p.add(caja, BorderLayout.NORTH);
        JPanel centro = ficha();
        centro.add(titulo("Cuentas y permisos"), BorderLayout.NORTH);
        centro.add(scrollear(tUsuarios), BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);
        JPanel pie = ficha();
        pie.add(pista("ADMIN gestiona cuentas; OPERADOR registra y procesa; CONSULTA solo visualiza y ejecuta demostraciones AED."));
        p.add(pie, BorderLayout.SOUTH);
        return p;
    }

    // La interfaz comprueba la cuenta vigente, no solo el rol guardado al iniciar sesion.
    private Usuario cuentaVigente() throws Exception {
        Usuario actual = consultar(() -> usuarios.buscarPorId(sesion.id()));
        if (actual == null || !actual.activo() || !actual.rol().equals(sesion.rol()))
            throw new IllegalStateException("Tu cuenta ya no tiene esta autorizacion. Cierra sesion y vuelve a ingresar.");
        return actual;
    }

    private void exigirEdicion() throws Exception {
        if (!cuentaVigente().puedeEditar())
            throw new IllegalStateException("Tu rol solo permite consultar datos.");
    }

    private void exigirAdmin() throws Exception {
        if (!cuentaVigente().administra())
            throw new IllegalStateException("Solo el administrador gestiona cuentas.");
    }

    private void cambiarMiClave() throws Exception {
        cuentaVigente();
        JPasswordField actual = new JPasswordField(20), nueva = new JPasswordField(20),
            repetir = new JPasswordField(20);
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Contrasena actual"));
        panel.add(actual);
        panel.add(new JLabel("Nueva contrasena (minimo 10 caracteres)"));
        panel.add(nueva);
        panel.add(new JLabel("Repite la nueva contrasena"));
        panel.add(repetir);
        if (JOptionPane.showConfirmDialog(this, panel, "Cambiar mi contrasena",
            JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        char[] a = actual.getPassword(), n = nueva.getPassword(), r = repetir.getPassword();
        try {
            if (!Arrays.equals(n, r))
                throw new IllegalArgumentException("Las nuevas contrasenas no coinciden.");
            consultar(() -> {
                usuarios.cambiarClavePropia(sesion.id(), a, n);
                return null;
            });
            mensaje("Contrasena actualizada. Utilizala en el proximo acceso.");
        } finally {
            Arrays.fill(a, '\0');
            Arrays.fill(n, '\0');
            Arrays.fill(r, '\0');
            actual.setText("");
            nueva.setText("");
            repetir.setText("");
        }
    }

    // ---------- UTILIDADES DE UI Y REFRESCO ----------
    @FunctionalInterface private interface Trabajo {
        void run() throws Exception;
    }
    /** Ejecuta consultas en un worker; la ventana modal impide acciones simultáneas y permite repintar. */
    private < T > T consultar(java.util.concurrent.Callable<T> tarea) throws Exception {
        if (!SwingUtilities.isEventDispatchThread())
            return tarea.call();
        JDialog espera = new JDialog(this, "ImportaFlow — MySQL", true);
        espera.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        panel.add(new JLabel("Consultando base de datos…"), BorderLayout.NORTH);
        JProgressBar barra = new JProgressBar();
        barra.setIndeterminate(true);
        panel.add(barra);
        espera.setContentPane(panel);
        espera.pack();
        espera.setLocationRelativeTo(this);
        SwingWorker<T, Void> worker = new SwingWorker<>() {

            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            protected void done() {
                espera.dispose();
            }
        };
        worker.execute();
        espera.setVisible(true);
        try {
            return worker.get();
        } catch (java.util.concurrent.ExecutionException ex) {
            if (ex.getCause() instanceof Exception e)
                throw e;
            throw new IllegalStateException(ex.getCause());
        } finally {
            espera.dispose();
        }
    }

    private void ejecutar(Trabajo t) {
        try {
            t.run();
        } catch (Exception e) {
            error(e);
        }
    }

    private void error(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Operacion no completada", JOptionPane.ERROR_MESSAGE);
    }

    private void mensaje(String s) {
        JOptionPane.showMessageDialog(this, s, "ImportaFlow", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean confirmar(String s) {
        return JOptionPane.showConfirmDialog(this, s, "Confirmar accion",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private static String txt(JTextField t) {
        return t.getText().trim();
    }

    private static int entero(JTextField t) {
        return Integer.parseInt(txt(t));
    }

    private static double decimal(JTextField t) {
        return Double.parseDouble(txt(t).replace(',', '.'));
    }

    private static String dinero(BigDecimal d) {
        return d.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String valor(DefaultTableModel m, int fila, int col) {
        return String.valueOf(m.getValueAt(fila, col));
    }

    private static int idTabla(JTable t, DefaultTableModel m) {
        if (t.getSelectedRow() < 0)
            throw new IllegalArgumentException("Selecciona una fila de la tabla.");
        return (Integer) m.getValueAt(t.convertRowIndexToModel(t.getSelectedRow()), 0);
    }

    private Envio seleccionarEnvio() {
        int id = idTabla(tEnvios, mEnvios);
        return envios.stream().filter(e -> e.id() == id).findFirst().orElseThrow(()
            -> new IllegalArgumentException("Paquete no encontrado."));
    }

    private void cargarMovimientos(int id) {
        mMovimientos.setRowCount(0);
        new SwingWorker<List<SistemaDAO.Movimiento>, Void>() {

            protected List<SistemaDAO.Movimiento> doInBackground() throws Exception {
                return dao.movimientos(id);
            }

            protected void done() {
                try {
                    if (tEnvios.getSelectedRow() < 0 || idTabla(tEnvios, mEnvios) != id)
                        return;
                    mMovimientos.setRowCount(0);
                    for (SistemaDAO.Movimiento m : get())
                        mMovimientos.addRow(new Object[]{m.fecha(), m.codigo(), m.anterior(), m.nuevo(), m
                            .observacion(), m.usuario()});
                } catch (Exception e) {
                    estadoFooter.setText("No se pudo consultar seguimiento: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void refrescar() throws Exception {
        DatosIniciales datos = consultar(() -> {
            DatosIniciales d = cargarDatos(sesion);
            gestor.recargar();
            return d;
        });
        aplicarDatos(datos);
        try {
            actualizarReporte();
        } catch (java.time.DateTimeException | IllegalArgumentException ex) {
            totalReportado.setText("Revisa las fechas del reporte y pulsa Aplicar.");
        }
    }

    private void aplicarDatos(DatosIniciales datos) {
        clientes = datos.clientes();
        envios = datos.envios();
        mClientes.setRowCount(0);
        for (Cliente c : clientes)
            mClientes.addRow(new Object[]{c.id(), c.documento(), c.nombre(), c.telefono(), c.correo(), c
                .direccion()});
        filtrarRemitentes();
        mEnvios.setRowCount(0);
        Envio pagoAntes = (Envio) cbPagoEnvio.getSelectedItem();
        cbPagoEnvio.removeAllItems();
        for (Envio e : envios) {
            mEnvios.addRow(new Object[]{e.id(), e.codigo(), e.cliente(), e.destinatario(), e.destino(), e
                .contenido(), e.prioridad(),
            e.estado(), dinero(e.tarifa()), dinero(e.saldo())});
            if (!"CANCELADO".equals(e.estado()) && e.saldo().signum() > 0)
                cbPagoEnvio.addItem(e);
        }
        if (pagoAntes != null)
            for (int i = 0; i < cbPagoEnvio.getItemCount(); i++)
                if (cbPagoEnvio.getItemAt(i).id() == pagoAntes.id())
                    cbPagoEnvio.setSelectedIndex(i);
        mIngresos.setRowCount(0);
        BigDecimal cobrado = BigDecimal.ZERO;
        for (SistemaDAO.Ingreso i : datos.ingresos()) {
            mIngresos.addRow(new Object[]{i.fecha(), i.codigo(), dinero(i.monto()), i.metodo(), i.usuario()});
            cobrado = cobrado.add(i.monto());
        }
        avisoPago.setText("Suma de todos los ingresos registrados: S/ " + dinero(cobrado)
            + "  |  El saldo se calcula por cada paquete.");
        if (sesion.administra()) {
            mUsuarios.setRowCount(0);
            for (Usuario u : datos.usuarios())
                mUsuarios.addRow(new Object[]{u.id(), u.usuario(), u.nombre(), u.rol(), u.activo() ? "SI" : "NO"});
        }
        mRecientes.setRowCount(0);
        for (SistemaDAO.Movimiento m : datos.movimientos()) {
            if (mRecientes.getRowCount() >= 15)
                break;
            mRecientes.addRow(new Object[]{m.fecha(), m.codigo(), m.nuevo(), m.observacion()});
        }
        if (tEnvios.getSelectedRow() < 0)
            mMovimientos.setRowCount(0);
        metricas();
        configurarPermisos();
        estadoFooter.setText("Sesion: " + sesion.usuario() + " [" + sesion.rol() + "]   |   MySQL conectado   |   "
            + envios.size() + " envios cargados");
    }

    private void metricas() {
        int pendientes = 0, urgentes = 0, entregados = 0, proceso = 0, transito = 0, cancelados = 0;
        for (Envio e : envios) {
            if ("PENDIENTE".equals(e.estado()))
                pendientes++;
            if ("URGENTE".equals(e.prioridad()) && !"ENTREGADO".equals(e.estado()) && !"CANCELADO".equals(e
                .estado()))
                urgentes++;
            if ("ENTREGADO".equals(e.estado()))
                entregados++;
            if ("EN_PROCESO".equals(e.estado()))
                proceso++;
            if ("EN_TRANSITO".equals(e.estado()))
                transito++;
            if ("CANCELADO".equals(e.estado()))
                cancelados++;
        }
        metrics.removeAll();
        metrics.add(kpi("Total de envios", envios.size(), "Registros de paqueteria"));
        metrics.add(kpi("Pendientes", pendientes, "Por clasificar"));
        metrics.add(kpi("Urgentes", urgentes, "Atencion prioritaria"));
        metrics.add(kpi("En proceso", proceso, "Clasificación"));
        metrics.add(kpi("En tránsito", transito, "Distribución"));
        metrics.add(kpi("Entregados", entregados, "Finalizados"));
        metrics.add(kpi("Cancelados", cancelados, "Fuera del flujo"));
        metrics.add(kpi("Clientes", clientes.size(), "Directorio"));
        resumenInicio.setText("Recaudado S/ " + dinero(envios.stream().map(Envio::abonado).reduce(BigDecimal.ZERO,
            BigDecimal::add)) + "  |  Saldo por cobrar S/ " + dinero(envios.stream().filter(e -> !e.estado()
            .equals("CANCELADO")).map(Envio::saldo).reduce(BigDecimal.ZERO, BigDecimal::add)));
        distribucion.removeAll();
        String[] etiquetas = {"Pendientes", "En proceso", "En tránsito", "Entregados", "Cancelados"};
        int[] valores = {pendientes, proceso, transito, entregados, cancelados};
        Color[] colores = {new Color(190, 135, 37), AZUL, new Color(107, 101, 176), new Color(43, 139, 98), MUTED};
        for (int i = 0; i < 5; i++) {
            JProgressBar barra = new JProgressBar(0, Math.max(1, envios.size()));
            barra.setValue(valores[i]);
            barra.setStringPainted(true);
            barra.setString(etiquetas[i] + ": " + valores[i]);
            barra.setForeground(colores[i]);
            distribucion.add(barra);
        }
        distribucion.revalidate();
        distribucion.repaint();
        metrics.revalidate();
        metrics.repaint();
    }

    private JPanel kpi(String label, int valor, String note) {
        JPanel p = ficha();
        p.setPreferredSize(new Dimension(100, 105));
        JLabel a = pista(label);
        JLabel num = new JLabel(Integer.toString(valor));
        num.setFont(new Font("SansSerif", Font.BOLD, 25));
        num.setForeground(AZUL);
        JPanel c = new JPanel(new GridLayout(0, 1));
        c.setOpaque(false);
        c.add(a);
        c.add(num);
        c.add(pista(note));
        p.add(c);
        return p;
    }

    private void configurarPermisos() {
        boolean editar = sesion.puedeEditar();
        for (JButton b : new JButton[]{btnGuardarCliente, btnEliminarCliente, btnGuardarEnvio, btnAtender,
            btnAvanzar, btnCancelar, btnIngreso, btnEditarEnvio})
            if (b != null)
                b.setEnabled(editar);
    }

    // Laboratorio independiente de las operaciones persistentes.
    private JPanel crearAED() {
        return new LaboratorioAED(() -> envios, gestor);
    }
}
