package batalhanaval.gui;

import batalhanaval.cliente.LigacaoServidor;
import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;
import batalhanaval.util.UtilNavio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Janela principal do jogo.
 *
 * Layout:
 *   [Tabuleiro próprio] [Tabuleiro adversário]
 *   [Log de eventos                ] [Guardar]
 *
 * - Tabuleiro próprio: mostra os teus navios e os tiros que recebeste
 * - Tabuleiro adversário: clica para disparar (só no teu turno)
 * - Tiros restantes no turno: 3 por turno
 */
public class JanelaJogo extends JFrame
        implements LigacaoServidor.RecebidaCallback {

    // ── Cores ──────────────────────────────────────────────────────────────
    static final Color COR_AGUA     = new Color(25,  55, 100);
    static final Color COR_BORDA    = new Color(45,  85, 140);
    static final Color COR_NAVIO    = new Color(0,  190, 110);
    static final Color COR_ACERTOU  = new Color(255, 180,   0);
    static final Color COR_AFUNDADO = new Color(220,  50,  50);
    static final Color COR_FALHA    = new Color(100, 130, 180);
    static final Color COR_HOVER    = new Color(60,  160, 255, 160);
    static final Color COR_FUNDO = new Color(10, 25, 50);
    static final Color COR_TEXTO = new Color(200, 220, 255);
    static final int CELULA_PX   = 48;
    static final int GRID        = 10;
    static final int OFFSET      = 28;

    // ── Estado ─────────────────────────────────────────────────────────────
    private final LigacaoServidor ligacao;
    private final int numeroJogador;

    private boolean meuTurno       = false;
    private int     tirosRestantes = 0;

    /** Resultados dos tiros que fizemos no adversário: "B4" → "AGUA"|"ACERTOU:Fragata"|"AFUNDOU:Fragata" */
    private final Map<String, String> resultadosAdv  = new HashMap<>();

    /** Tiros que recebemos no nosso tabuleiro */
    private final Map<String, String> resultadosProp = new HashMap<>();

    /** Células dos nossos navios (vêm do posicionamento guardado localmente) */
    private final Set<String> celulasNossas = new HashSet<>();

    private String celulaHoverAdv = null;

    // ── Componentes ────────────────────────────────────────────────────────
    private PainelTabuleiro tabProprio;
    private PainelTabuleiro tabAdversario;
    private JLabel labelEstado;
    private JLabel labelTiros;
    private JTextArea logArea;

    // ──────────────────────────────────────────────────────────────────────

    public JanelaJogo(LigacaoServidor ligacao, int numeroJogador) {
        super("Batalha Naval — Jogador " + numeroJogador);
        this.ligacao        = ligacao;
        this.numeroJogador  = numeroJogador;

        construirInterface();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Permite à JanelaPosicionar passar as células dos nossos navios
     * para as mostrarmos no tabuleiro próprio durante o jogo.
     */
    public void setCelulasNossas(Set<String> celulas) {
        celulasNossas.addAll(celulas);
    }

    // ── Construção ─────────────────────────────────────────────────────────

    private void construirInterface() {
        getContentPane().setBackground(COR_FUNDO);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Topo: estado do turno
        labelEstado = new JLabel("A aguardar início do jogo...", JLabel.CENTER);
        labelEstado.setFont(new Font("SansSerif", Font.BOLD, 15));
        labelEstado.setForeground(new Color(100, 180, 255));
        add(labelEstado, BorderLayout.NORTH);

        // Centro: dois tabuleiros lado a lado
        JPanel centro = new JPanel(new GridLayout(1, 2, 16, 0));
        centro.setBackground(COR_FUNDO);
        tabProprio    = new PainelTabuleiro(false, "O meu tabuleiro");
        tabAdversario = new PainelTabuleiro(true,  "Tabuleiro do adversário  [clica para disparar]");
        centro.add(tabProprio);
        centro.add(tabAdversario);
        add(centro, BorderLayout.CENTER);

        // Sul: log + controlo
        add(construirRodape(), BorderLayout.SOUTH);
    }

    private JPanel construirRodape() {
        JPanel p = new JPanel(new BorderLayout(8, 4));
        p.setBackground(COR_FUNDO);

        // Área de log
        logArea = new JTextArea(6, 55);
        logArea.setBackground(new Color(18, 35, 60));
        logArea.setForeground(new Color(160, 210, 160));
        logArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Painel direito: tiros + botão guardar
        JPanel direito = new JPanel(new GridLayout(3, 1, 0, 8));
        direito.setBackground(COR_FUNDO);
        direito.setPreferredSize(new Dimension(160, 0));
        direito.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));

        labelTiros = new JLabel("", JLabel.CENTER);
        labelTiros.setFont(new Font("SansSerif", Font.BOLD, 14));
        labelTiros.setForeground(new Color(255, 200, 80));

        JButton btnGuardar = botao("💾  Guardar Jogo");
        btnGuardar.addActionListener(e -> ligacao.enviar(TipoMensagem.GUARDAR));

        direito.add(labelTiros);
        direito.add(btnGuardar);
        direito.add(new JLabel(""));
        p.add(direito, BorderLayout.EAST);
        return p;
    }

    // ── Painel de tabuleiro (reutilizado para próprio e adversário) ─────────

    private class PainelTabuleiro extends JPanel {

        /** true = tabuleiro adversário (clicável no nosso turno) */
        final boolean interactivo;
        final String  titulo;

        PainelTabuleiro(boolean interactivo, String titulo) {
            this.interactivo = interactivo;
            this.titulo      = titulo;
            int sz = OFFSET + CELULA_PX * GRID + 2;
            setPreferredSize(new Dimension(sz, sz + 22)); // +22 para o título
            setBackground(COR_FUNDO);

            if (interactivo) {
                addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        // Só mostra hover se for o nosso turno
                        celulaHoverAdv = meuTurno
                                ? pixelParaCelula(e.getX(), e.getY()) : null;
                        repaint();
                    }
                });
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (meuTurno && celulaHoverAdv != null)
                            disparar(celulaHoverAdv);
                    }
                    public void mouseExited(MouseEvent e) {
                        celulaHoverAdv = null;
                        repaint();
                    }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int yBase = 20; // espaço para o título

            // Título
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(COR_TEXTO);
            g2.drawString(titulo, 4, 14);

            // Labels de colunas (1-10)
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            for (int c = 0; c < GRID; c++) {
                int x = OFFSET + c * CELULA_PX + CELULA_PX / 2 - 5;
                g2.drawString(String.valueOf(c + 1), x, yBase + 16);
            }
            // Labels de linhas (A-J)
            for (int l = 0; l < GRID; l++) {
                int y = yBase + OFFSET + l * CELULA_PX + CELULA_PX / 2 + 5;
                g2.drawString(String.valueOf((char) ('A' + l)), 5, y);
            }

            // Células
            Map<String, String> resultados = interactivo ? resultadosAdv : resultadosProp;
            for (int l = 0; l < GRID; l++) {
                for (int c = 0; c < GRID; c++) {
                    String chave = "" + (char) ('A' + l) + (c + 1);
                    int x   = OFFSET + c * CELULA_PX;
                    int y   = yBase + OFFSET + l * CELULA_PX;
                    String res = resultados.get(chave);

                    // Cor de fundo
                    if (res == null) {
                        // No tabuleiro próprio mostramos os nossos navios
                        if (!interactivo && celulasNossas.contains(chave))
                            g2.setColor(COR_NAVIO);
                        else
                            g2.setColor(COR_AGUA);
                    } else if (res.startsWith("AFUNDOU")) {
                        g2.setColor(COR_AFUNDADO);
                    } else if (res.startsWith("ACERTOU")) {
                        g2.setColor(COR_ACERTOU);
                    } else { // AGUA
                        g2.setColor(COR_FALHA);
                    }
                    g2.fillRect(x, y, CELULA_PX, CELULA_PX);

                    // Ícone de resultado
                    if (res != null) {
                        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                        g2.setColor(Color.WHITE);
                        String icon = res.startsWith("AFUNDOU") ? "✕"
                                    : res.startsWith("ACERTOU") ? "●" : "·";
                        g2.drawString(icon,
                                x + CELULA_PX / 2 - 7,
                                y + CELULA_PX / 2 + 8);
                    }

                    // Borda
                    g2.setColor(COR_BORDA);
                    g2.drawRect(x, y, CELULA_PX, CELULA_PX);
                }
            }

            // Highlight hover (só no tabuleiro adversário e no nosso turno)
            if (interactivo && meuTurno && celulaHoverAdv != null) {
                Object[] p = UtilNavio.parseCelula(celulaHoverAdv);
                if (p != null) {
                    int col = (int) p[1] - 1;
                    int lin = (char) p[0] - 'A';
                    g2.setColor(COR_HOVER);
                    g2.fillRect(
                            OFFSET + col * CELULA_PX + 2,
                            yBase + OFFSET + lin * CELULA_PX + 2,
                            CELULA_PX - 4, CELULA_PX - 4);
                }
            }

            // Cursor muda para mira quando é o nosso turno
            setCursor(Cursor.getPredefinedCursor(
                    interactivo && meuTurno
                            ? Cursor.CROSSHAIR_CURSOR
                            : Cursor.DEFAULT_CURSOR));
        }

        private String pixelParaCelula(int px, int py) {
            int yBase = 20;
            int c = (px - OFFSET) / CELULA_PX;
            int l = (py - yBase - OFFSET) / CELULA_PX;
            if (c < 0 || c >= GRID || l < 0 || l >= GRID) return null;
            return "" + (char) ('A' + l) + (c + 1);
        }
    }

    // ── Lógica de disparo ──────────────────────────────────────────────────

    private void disparar(String chave) {
        if (resultadosAdv.containsKey(chave)) {
            log("⚠ Já disparaste em " + chave + "!");
            return;
        }
        Object[] p = UtilNavio.parseCelula(chave);
        if (p == null) return;
        // Envia: TIRO:Linha:Coluna  ex: TIRO:B:4
        ligacao.enviar(TipoMensagem.TIRO,
                String.valueOf((char) p[0]),
                String.valueOf((int)  p[1]));
        celulaHoverAdv = null;
    }

    // ── Callback do servidor ───────────────────────────────────────────────

    @Override
    public void onMensagemRecebida(Mensagem msg) {
        SwingUtilities.invokeLater(() -> tratarMensagem(msg));
    }

    private void tratarMensagem(Mensagem msg) {
    switch (msg.getTipo()) {

        case TEU_TURNO:
            tirosRestantes = Integer.parseInt(msg.getDado(0));
            meuTurno = true;
            labelEstado.setForeground(new Color(80, 220, 80));
            labelEstado.setText("O teu turno! Clica no tabuleiro adversário para disparar.");
            actualizarTiros();
            log("--- O teu turno (" + tirosRestantes + " tiros) ---");
            break;

        case TURNO_ADVERSARIO:
            meuTurno = false;
            labelEstado.setForeground(new Color(200, 150, 60));
            labelEstado.setText("Turno do adversário — aguarda...");
            labelTiros.setText("");
            log("--- Turno do adversário ---");
            break;

        case RESULTADO_TIRO:
            String resultado = msg.getDado(0);
            String chave     = msg.getDado(1);
            if (meuTurno) {
                resultadosAdv.put(chave, resultado);
                tirosRestantes--;
                actualizarTiros();
                if (tirosRestantes <= 0) meuTurno = false;
            } else {
                resultadosProp.put(chave, resultado);
            }
            log("[" + chave + "] -> " + resultado);
            tabProprio.repaint();
            tabAdversario.repaint();
            break;

        case TIRO_INVALIDO:
            log("Tiro invalido: " + msg.getDado(0));
            break;

        case INFO:
            log(msg.getDado(0));
            labelEstado.setForeground(COR_TEXTO);
            labelEstado.setText(msg.getDado(0));
            break;

        case JOGO_GUARDADO:
            log("Jogo guardado com sucesso.");
            break;

        case FIM_JOGO:
            meuTurno = false;
            boolean venceu = "VITORIA".equals(msg.getDado(0));
            labelEstado.setForeground(venceu
                    ? new Color(80, 230, 80) : new Color(230, 80, 80));
            labelEstado.setText(venceu
                    ? "VITORIA! Parabens!" : "Derrota. Boa sorte da proxima!");
            log("=== FIM DE JOGO: " + (venceu ? "VITORIA" : "DERROTA") + " ===");
            JOptionPane.showMessageDialog(this,
                    venceu ? "Parabens! Venceste!" : "Perdeste. Tenta de novo!",
                    "Fim do Jogo",
                    venceu ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            break;

        case DESLIGAR:
            meuTurno = false;
            log("Ligacao perdida: " + msg.getDado(0));
            labelEstado.setForeground(new Color(255, 100, 100));
            labelEstado.setText("Ligacao perdida com o servidor.");
            break;

        default:
            break;
    }
}

    // ── Helpers ────────────────────────────────────────────────────────────

    private void actualizarTiros() {
        labelTiros.setText(tirosRestantes > 0
                ? "Tiros: " + tirosRestantes + " restantes"
                : "");
    }

    private void log(String texto) {
        logArea.append(texto + "\n");
        // Auto-scroll para o fim
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private JButton botao(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(new Color(50, 85, 140));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        return b;
    }
}