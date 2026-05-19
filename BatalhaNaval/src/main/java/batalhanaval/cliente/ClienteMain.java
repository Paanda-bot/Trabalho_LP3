package batalhanaval.cliente;
import batalhanaval.gui.JanelaLogin;
import javax.swing.*;

/** Ponto de entrada do cliente. Abre a janela de login. */
public class ClienteMain {
    public static void main(String[] args) {
        // Toda a GUI deve correr na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> new JanelaLogin().setVisible(true));
    }
}