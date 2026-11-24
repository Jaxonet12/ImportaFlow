package com.importaflow;
import com.importaflow.vista.Acceso;
import javax.swing.*;
/** Punto de entrada de ImportaFlow 3.2. */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            try {
                new Acceso().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                JOptionPane.showMessageDialog(null, "Error al abrir ImportaFlow: " + e.getMessage(),
                    "Error de inicio", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
