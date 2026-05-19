package batalhanaval.gui;

import batalhanaval.cliente.LigacaoServidor;
import javax.swing.*;
import java.awt.*;

/**
 * Janela de login — introduz IP e porta do servidor.
 *
 * Fluxo:
 *   1. Utilizador preenche IP e Porta
 *   2. Clica "Ligar ao Servidor" (ou pressiona Enter)
 *   3. Cria JanelaPosicionar, LigacaoServidor, e liga ao servidor em background
 *   4. Se ligar com sucesso abre JanelaPosicionar e fecha esta janela
 *   5. Se falhar mostra mensagem de erro a vermelho
 */
public class JanelaLogin extends JFrame {

    // Campos de texto para IP e porta
    private final JTextField campoIP    = new JTextField("localhost", 18);
    private final JTextField campoPorta = new JTextField("12345", 18);

    // Label para mostrar erros ou estado da ligacao
    private final JLabel labelErro = new JLabel(" ");

    public JanelaLogin() {
        super("Batalha Naval — Ligar");
        construirInterface();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setMinimumSize(new Dimension(420, 320));
        setLocationRelativeTo(null); // centra no ecra
    }

    private void construirInterface() {
        Color fundo  = new Color(10, 25, 50);
        Color painel = new Color(20, 45, 80);
        Color texto  = new Color(200, 220, 255);
        Color acento = new Color(0, 120, 220);

        getContentPane().setBackground(fundo);
        setLayout(new BorderLayout(0, 0));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // ── Titulo ──────────────────────────────────────────────────────────
        JLabel titulo = new JLabel("BATALHA NAVAL", JLabel.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        titulo.setForeground(new Color(80, 160, 255));
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(titulo, BorderLayout.NORTH);

        // ── Formulario ──────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(painel);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 85, 140)),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 6, 8, 6);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Label e campo IP
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        JLabel lIP = new JLabel("IP do Servidor:");
        lIP.setFont(new Font("SansSerif", Font.BOLD, 14));
        lIP.setForeground(texto);
        form.add(lIP, g);

        g.gridx = 1; g.weightx = 1;
        estilizarCampo(campoIP);
        form.add(campoIP, g);

        // Label e campo Porta
        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        JLabel lPorta = new JLabel("Porta:");
        lPorta.setFont(new Font("SansSerif", Font.BOLD, 14));
        lPorta.setForeground(texto);
        form.add(lPorta, g);

        g.gridx = 1; g.weightx = 1;
        estilizarCampo(campoPorta);
        form.add(campoPorta, g);

        add(form, BorderLayout.CENTER);

        // ── Rodape: label de erro + botao ───────────────────────────────────
        JPanel rodape = new JPanel(new BorderLayout(0, 8));
        rodape.setBackground(fundo);
        rodape.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        labelErro.setHorizontalAlignment(JLabel.CENTER);
        labelErro.setFont(new Font("SansSerif", Font.ITALIC, 13));
        labelErro.setForeground(new Color(255, 100, 100));
        rodape.add(labelErro, BorderLayout.NORTH);

        JButton btnLigar = new JButton("Ligar ao Servidor");
        btnLigar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnLigar.setBackground(acento);
        btnLigar.setForeground(Color.WHITE);
        btnLigar.setFocusPainted(false);
        btnLigar.setOpaque(true);
        btnLigar.setBorderPainted(false);
        btnLigar.setPreferredSize(new Dimension(0, 46));
        btnLigar.addActionListener(e -> tentarLigar());

        // Pressionar Enter activa o botao
        getRootPane().setDefaultButton(btnLigar);

        rodape.add(btnLigar, BorderLayout.SOUTH);
        add(rodape, BorderLayout.SOUTH);
    }

    /** Aplica estilo escuro ao campo de texto */
    private void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setBackground(new Color(12, 30, 60));
        campo.setForeground(new Color(200, 220, 255));
        campo.setCaretColor(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 85, 140)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        campo.setPreferredSize(new Dimension(200, 38));
    }

    /**
     * Valida os campos e tenta ligar ao servidor em background.
     * A ligacao e feita numa thread separada para nao bloquear a GUI.
     */
    private void tentarLigar() {
        String ip    = campoIP.getText().trim();
        String porta = campoPorta.getText().trim();

        // Validacao basica dos campos
        if (ip.isEmpty()) {
            labelErro.setText("Introduz o IP do servidor.");
            return;
        }
        if (porta.isEmpty()) {
            labelErro.setText("Introduz a porta.");
            return;
        }

        int portaNum;
        try {
            portaNum = Integer.parseInt(porta);
        } catch (NumberFormatException ex) {
            labelErro.setText("Porta invalida — deve ser um numero.");
            return;
        }

        // Feedback visual enquanto liga
        labelErro.setForeground(new Color(120, 200, 120));
        labelErro.setText("A ligar...");

        // Liga em background para nao congelar a janela
        new Thread(() -> {
            try {
                // 1. Cria a janela de posicionamento SEM ligacao ainda
                JanelaPosicionar janela = new JanelaPosicionar(null);

                // 2. Cria a ligacao usando a janela como callback de mensagens
                LigacaoServidor lig = new LigacaoServidor(janela);

                // 3. Passa a ligacao a janela (precisa para enviar mensagens ao servidor)
                janela.setLigacao(lig);

                // 4. Liga ao servidor (pode lancar IOException se falhar)
                lig.ligar(ip, portaNum);

                // 5. Ligacao bem-sucedida — mostra a janela de posicionamento
                SwingUtilities.invokeLater(() -> {
                    dispose();           // fecha janela de login
                    janela.setVisible(true);
                });

            } catch (Exception ex) {
                // Falhou — mostra o erro na GUI
                SwingUtilities.invokeLater(() -> {
                    labelErro.setForeground(new Color(255, 100, 100));
                    labelErro.setText("Erro: " + ex.getMessage());
                });
            }
        }, "thread-ligacao").start();
    }
}
