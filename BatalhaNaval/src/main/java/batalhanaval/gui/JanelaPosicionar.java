package batalhanaval.gui;

import batalhanaval.cliente.LigacaoServidor;
import batalhanaval.jogo.TipoNavio;
import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;
import batalhanaval.util.UtilNavio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Janela de posicionamento de navios.
 *
 * Como funciona:
 *   1. Selecciona o tipo de navio na lista a direita
 *   2. Move o rato sobre o tabuleiro — preview azul mostra onde vai ficar
 *   3. Preview vermelho = posicao invalida (fora do limite ou sobreposicao)
 *   4. Setas <- -> = orientacao horizontal  |  setas cima/baixo = vertical
 *   5. Clica para confirmar — servidor valida e responde
 *   6. Quando todos os navios estiverem colocados, o jogo comeca automaticamente
 */
public class JanelaPosicionar extends JFrame
        implements LigacaoServidor.RecebidaCallback {

    // ── Constantes visuais (partilhadas com JanelaJogo) ────────────────────
    static final Color COR_FUNDO       = new Color(10,  25,  50);
    static final Color COR_CELULA      = new Color(25,  55, 100);
    static final Color COR_BORDA       = new Color(45,  85, 140);
    static final Color COR_PREVIEW_OK  = new Color(60, 160, 255, 180);
    static final Color COR_PREVIEW_ERR = new Color(255, 70,  70, 180);
    static final Color COR_NAVIO       = new Color(0,  190, 110);
    static final Color COR_TEXTO       = new Color(200, 220, 255);

    static final int CELULA_PX  = 48;  // tamanho de cada celula em pixels
    static final int GRID       = 10;  // tabuleiro 10x10
    static final int OFFSET_TAB = 28;  // espaco para as labels A-J e 1-10

    // ── Estado do jogo ─────────────────────────────────────────────────────
    // NAO e final — setLigacao() precisa de alterar depois da construcao
    private LigacaoServidor ligacao;
    private int numeroJogador = 0;

    // Navios que ainda faltam colocar (tipo -> quantidade restante)
    private final Map<TipoNavio, Integer> porColocar         = new LinkedHashMap<>();
    // Celulas ja ocupadas por navios confirmados pelo servidor
    private final Set<String>             celulasConfirmadas = new HashSet<>();

    private char      orientacao        = 'H'; // H=horizontal, V=vertical
    private String    celulaHover       = null; // celula sob o cursor do rato
    private TipoNavio navioSeleccionado = null; // navio actualmente seleccionado

    // ── Componentes da interface ───────────────────────────────────────────
    private PainelTabuleiro          painelTab;
    private DefaultListModel<String> modeloLista;
    private JList<String>            listaNavios;
    private JLabel                   labelInfo;
    private JLabel                   labelOrient;

    // ── Construtor ─────────────────────────────────────────────────────────

    /**
     * @param ligacao pode ser null — chamar setLigacao() antes de interagir
     */
    public JanelaPosicionar(LigacaoServidor ligacao) {
        super("Batalha Naval — Posicionar Navios");
        this.ligacao = ligacao;
        inicializarNavios();
        construirInterface();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    /** Chamado pela JanelaLogin apos criar e ligar o LigacaoServidor */
    public void setLigacao(LigacaoServidor ligacao) {
        this.ligacao = ligacao;
    }

    // ── Inicializacao ──────────────────────────────────────────────────────

    private void inicializarNavios() {
        // Preenche o mapa com todos os tipos e as suas quantidades
        for (TipoNavio t : TipoNavio.values())
            porColocar.put(t, t.getQuantidade());
        navioSeleccionado = TipoNavio.TORPEDEIRO;
    }

    // ── Construcao da interface ────────────────────────────────────────────

    private void construirInterface() {
        getContentPane().setBackground(COR_FUNDO);
        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel titulo = new JLabel("Posiciona os teus navios", JLabel.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        titulo.setForeground(new Color(100, 180, 255));
        add(titulo, BorderLayout.NORTH);

        painelTab = new PainelTabuleiro();
        add(painelTab, BorderLayout.CENTER);
        add(construirLateral(), BorderLayout.EAST);
        add(construirRodape(),  BorderLayout.SOUTH);

        registarTeclas();
    }

    private JPanel construirLateral() {
        JPanel p = new JPanel(new BorderLayout(6, 10));
        p.setPreferredSize(new Dimension(200, 0));
        p.setBackground(new Color(18, 38, 65));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 85, 140)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel t = new JLabel("Navios por colocar:", JLabel.CENTER);
        t.setFont(new Font("SansSerif", Font.BOLD, 13));
        t.setForeground(COR_TEXTO);
        p.add(t, BorderLayout.NORTH);

        // Lista com os navios disponiveis
        modeloLista = new DefaultListModel<>();
        actualizarLista();
        listaNavios = new JList<>(modeloLista);
        listaNavios.setBackground(new Color(22, 45, 80));
        listaNavios.setForeground(COR_TEXTO);
        listaNavios.setFont(new Font("SansSerif", Font.PLAIN, 13));
        listaNavios.setSelectionBackground(new Color(0, 100, 190));
        listaNavios.setSelectionForeground(Color.WHITE);
        listaNavios.setFixedCellHeight(34);
        if (!modeloLista.isEmpty()) listaNavios.setSelectedIndex(0);
        listaNavios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) seleccionarDaLista();
        });
        p.add(new JScrollPane(listaNavios), BorderLayout.CENTER);

        JButton btnOrient = botao("Mudar Orientacao");
        btnOrient.addActionListener(e -> toggleOrientacao());
        p.add(btnOrient, BorderLayout.SOUTH);
        return p;
    }

    private JPanel construirRodape() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 3));
        p.setBackground(COR_FUNDO);

        labelInfo = new JLabel("Selecciona um navio e clica no tabuleiro", JLabel.CENTER);
        labelInfo.setFont(new Font("SansSerif", Font.ITALIC, 13));
        labelInfo.setForeground(COR_TEXTO);

        labelOrient = new JLabel("Orientacao: HORIZONTAL  (setas <- -> = H | cima/baixo = V)", JLabel.CENTER);
        labelOrient.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelOrient.setForeground(new Color(140, 200, 140));

        p.add(labelInfo);
        p.add(labelOrient);
        return p;
    }

    /** Regista teclas de seta para mudar orientacao sem clicar no botao */
    private void registarTeclas() {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,  0), "setH");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "setH");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,    0), "setV");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,  0), "setV");
        root.getActionMap().put("setH",
                new AbstractAction() { public void actionPerformed(ActionEvent e) { setOrientacao('H'); } });
        root.getActionMap().put("setV",
                new AbstractAction() { public void actionPerformed(ActionEvent e) { setOrientacao('V'); } });
    }

    // ── Painel do tabuleiro (desenhado manualmente com Graphics2D) ──────────

    private class PainelTabuleiro extends JPanel {

        PainelTabuleiro() {
            int sz = OFFSET_TAB + CELULA_PX * GRID + 2;
            setPreferredSize(new Dimension(sz, sz));
            setBackground(COR_FUNDO);
            setFocusable(true);

            // Actualiza a celula hover conforme o rato se move
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    celulaHover = pixelParaCelula(e.getX(), e.getY());
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Recalcula a celula NO MOMENTO do clique para evitar desfasamento
                    String celula = pixelParaCelula(e.getX(), e.getY());
                    if (celula != null) {
                        celulaHover = celula;
                        enviarPosicionamento(celula);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    celulaHover = null;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Labels de colunas: 1 a 10
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(COR_TEXTO);
            for (int c = 0; c < GRID; c++) {
                int x = OFFSET_TAB + c * CELULA_PX + CELULA_PX / 2 - 5;
                g2.drawString(String.valueOf(c + 1), x, 18);
            }
            // Labels de linhas: A a J
            for (int l = 0; l < GRID; l++) {
                int y = OFFSET_TAB + l * CELULA_PX + CELULA_PX / 2 + 5;
                g2.drawString(String.valueOf((char)('A' + l)), 7, y);
            }

            // Desenha as 100 celulas
            for (int l = 0; l < GRID; l++) {
                for (int c = 0; c < GRID; c++) {
                    String chave = "" + (char)('A' + l) + (c + 1);
                    int x = OFFSET_TAB + c * CELULA_PX;
                    int y = OFFSET_TAB + l * CELULA_PX;
                    // Verde = navio ja colocado, azul = celula livre
                    g2.setColor(celulasConfirmadas.contains(chave) ? COR_NAVIO : COR_CELULA);
                    g2.fillRect(x, y, CELULA_PX, CELULA_PX);
                    g2.setColor(COR_BORDA);
                    g2.drawRect(x, y, CELULA_PX, CELULA_PX);
                }
            }

            // Preview do navio sob o rato
            if (celulaHover != null && navioSeleccionado != null) {
                Set<String> prev = calcularPreview(celulaHover);
                boolean valido = !prev.isEmpty()
                        && prev.stream().noneMatch(celulasConfirmadas::contains);
                // Azul = posicao valida, Vermelho = invalida
                g2.setColor(valido ? COR_PREVIEW_OK : COR_PREVIEW_ERR);
                for (String ch : prev) {
                    Object[] p = UtilNavio.parseCelula(ch);
                    if (p == null) continue;
                    int col = (int) p[1] - 1;
                    int lin = (char) p[0] - 'A';
                    g2.fillRoundRect(
                            OFFSET_TAB + col * CELULA_PX + 3,
                            OFFSET_TAB + lin * CELULA_PX + 3,
                            CELULA_PX - 6, CELULA_PX - 6, 8, 8);
                }
            }
        }

        /** Converte coordenadas de pixeis em chave de celula (ex: "B4") */
        private String pixelParaCelula(int px, int py) {
            int c = (px - OFFSET_TAB) / CELULA_PX;
            int l = (py - OFFSET_TAB) / CELULA_PX;
            if (c < 0 || c >= GRID || l < 0 || l >= GRID) return null;
            return "" + (char)('A' + l) + (c + 1);
        }
    }

    // ── Logica de posicionamento ───────────────────────────────────────────

    /**
     * Valida a posicao localmente e envia ao servidor para validacao final.
     */
    private void enviarPosicionamento(String chave) {
        if (navioSeleccionado == null) {
            setInfo("Selecciona primeiro um navio na lista!", false);
            return;
        }
        Object[] comp = UtilNavio.parseCelula(chave);
        if (comp == null) {
            setInfo("Celula invalida: " + chave, false);
            return;
        }
        Set<String> prev = calcularPreview(chave);
        if (prev.isEmpty()) {
            setInfo("Navio fora dos limites do tabuleiro!", false);
            return;
        }
        if (prev.stream().anyMatch(celulasConfirmadas::contains)) {
            setInfo("Ja existe um navio nessa posicao!", false);
            return;
        }
        // Envia mensagem ao servidor: POSICIONAR_NAVIO:TIPO:Linha:Coluna:Orientacao
        ligacao.enviar(TipoMensagem.POSICIONAR_NAVIO,
                navioSeleccionado.name(),
                String.valueOf((char) comp[0]),
                String.valueOf((int)  comp[1]),
                String.valueOf(orientacao));
    }

    private Set<String> calcularPreview(String chave) {
        if (navioSeleccionado == null) return Collections.emptySet();
        Object[] c = UtilNavio.parseCelula(chave);
        if (c == null) return Collections.emptySet();
        return UtilNavio.calcularCelulas(
                (char) c[0], (int) c[1],
                navioSeleccionado.getTamanho(), orientacao);
    }

    // ── Callback de mensagens do servidor ─────────────────────────────────

    @Override
    public void onMensagemRecebida(Mensagem msg) {
        // Todas as actualizacoes de GUI devem correr na Event Dispatch Thread
        SwingUtilities.invokeLater(() -> tratarMensagem(msg));
    }

    private void tratarMensagem(Mensagem msg) {
        switch (msg.getTipo()) {

            case LIGADO:
                // Servidor confirma ligacao e informa o numero do jogador (1 ou 2)
                numeroJogador = Integer.parseInt(msg.getDado(0));
                setTitle("Batalha Naval — Jogador " + numeroJogador + " — Posicionar");
                break;

            case JOGO_RECUPERADO:
                // Havia um jogo guardado — posiciona os navios novamente
                setInfo("Jogo recuperado! Posiciona os navios novamente.", true);
                break;

            case INICIAR_POSICIONAMENTO:
                setInfo("Posiciona os teus navios! Usa as setas para mudar orientacao.", true);
                break;

            case NAVIO_POSICIONADO:
                // Servidor confirmou o navio — marca as celulas no tabuleiro visual
                if (celulaHover != null)
                    celulasConfirmadas.addAll(calcularPreview(celulaHover));
                // Decrementa a quantidade deste tipo na lista
                if (navioSeleccionado != null) {
                    int resto = porColocar.getOrDefault(navioSeleccionado, 0) - 1;
                    if (resto <= 0) porColocar.remove(navioSeleccionado);
                    else            porColocar.put(navioSeleccionado, resto);
                }
                actualizarLista();
                painelTab.repaint();
                setInfo("OK: " + msg.getDado(0), true);
                break;

            case ERRO_POSICIONAMENTO:
                setInfo("ERRO: " + msg.getDado(0), false);
                break;

            case INFO:
                setInfo(msg.getDado(0), true);
                break;

            // Posicionamento terminado — transicao automatica para o jogo
            case TEU_TURNO:
            case TURNO_ADVERSARIO:
            case POSICIONAMENTO_COMPLETO:
                abrirJanelaJogo(msg);
                break;

            default:
                break;
        }
    }

    /**
     * Cria a JanelaJogo, transfere o callback e reencaminha a primeira mensagem.
     */
    private void abrirJanelaJogo(Mensagem primeiraMsg) {
        JanelaJogo jogo = new JanelaJogo(ligacao, numeroJogador);
        jogo.setCelulasNossas(celulasConfirmadas); // passa os nossos navios para o tabuleiro proprio
        ligacao.setCallback(jogo);                 // redirige mensagens futuras para a nova janela
        dispose();                                 // fecha esta janela
        jogo.setVisible(true);
        jogo.onMensagemRecebida(primeiraMsg);      // nao perde a primeira mensagem (TEU_TURNO etc.)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void seleccionarDaLista() {
        int idx = listaNavios.getSelectedIndex();
        if (idx < 0) return;
        List<TipoNavio> disponiveis = new ArrayList<>(porColocar.keySet());
        if (idx < disponiveis.size()) navioSeleccionado = disponiveis.get(idx);
        painelTab.repaint();
    }

    private void actualizarLista() {
        int sel = (listaNavios != null) ? listaNavios.getSelectedIndex() : 0;
        modeloLista.clear();
        for (Map.Entry<TipoNavio, Integer> e : porColocar.entrySet()) {
            modeloLista.addElement(String.format("%-14s x%d  [%d casa%s]",
                    e.getKey().getNome(),
                    e.getValue(),
                    e.getKey().getTamanho(),
                    e.getKey().getTamanho() > 1 ? "s" : ""));
        }
        if (!modeloLista.isEmpty() && listaNavios != null) {
            listaNavios.setSelectedIndex(Math.min(sel, modeloLista.size() - 1));
            seleccionarDaLista();
        }
    }

    private void toggleOrientacao() {
        setOrientacao(orientacao == 'H' ? 'V' : 'H');
    }

    private void setOrientacao(char o) {
        orientacao = o;
        labelOrient.setText(o == 'H'
            ? "Orientacao: HORIZONTAL  (setas <- -> = H | cima/baixo = V)"
            : "Orientacao: VERTICAL    (setas cima/baixo = V | <- -> = H)");
        painelTab.repaint();
    }

    private void setInfo(String texto, boolean ok) {
        labelInfo.setText(texto);
        labelInfo.setForeground(ok ? new Color(120, 230, 120) : new Color(255, 110, 110));
    }

    private JButton botao(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(new Color(0, 100, 185));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        return b;
    }
}
