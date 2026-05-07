package batalhanaval.launcher;

import batalhanaval.servidor.ServidorMain;
import batalhanaval.cliente.InterfaceCLI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * ╔══════════════════════════════════════════════════════════════════╗ ║
 * LauncherIDE — Ponto de entrada ÚNICO para correr dentro do IDE ║ ║ ║ ║ Ao
 * carregar F6 no NetBeans, abre um menu gráfico que permite: ║ ║ [1] Jogar
 * (abre 2 janelas de cliente + servidor automático) ║ ║ [2] Correr só o
 * servidor ║ ║ [3] Correr um cliente (para ligar a servidor externo) ║ ║ ║ ║
 * Não precisas de abrir terminais separados! ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Como funciona internamente: → ServidorMain corre numa Thread daemon da mesma
 * JVM → Cada cliente corre numa Thread própria, com um JFrame Swing que
 * redireciona System.in/out para uma área de texto → As threads partilham a
 * mesma JVM mas têm I/O isolado via PipedInputStream / PipedOutputStream
 */
public class LauncherIDE {

    static final int PORTA = ServidorMain.PORTA_PADRAO;

    public static void main(String[] args) throws Exception {
        // Look & feel do sistema (para parecer nativo)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(LauncherIDE::mostrarMenuLauncher);
    }

    // ── Menu principal ────────────────────────────────────────────────────────
    private static void mostrarMenuLauncher() {
        JFrame frame = new JFrame("⚓ Batalha Naval — Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(480, 380);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBackground(new Color(15, 30, 50));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Título
        JLabel titulo = new JLabel("<html><center>"
                + "<span style='font-size:22pt; color:#4fc3f7; font-weight:bold'>⚓ BATALHA NAVAL</span><br>"
                + "<span style='font-size:10pt; color:#90a4ae'>Laboratórios de Programação — TP3</span>"
                + "</center></html>", JLabel.CENTER);
        panel.add(titulo, BorderLayout.NORTH);

        // Botões
        JPanel btnPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        btnPanel.setBackground(new Color(15, 30, 50));

        JButton btnJogar = criarBotao("🎮  Jogar Agora  (Servidor + 2 Clientes)",
                new Color(0, 100, 60), new Color(0, 150, 90));
        JButton btnServidor = criarBotao("🖥  Correr Apenas o Servidor",
                new Color(40, 60, 100), new Color(60, 90, 150));
        JButton btnCliente = criarBotao("👤  Ligar como Cliente",
                new Color(80, 40, 100), new Color(120, 60, 150));

        btnJogar.addActionListener(e -> {
            frame.dispose();
            iniciarJogoCompleto();
        });
        btnServidor.addActionListener(e -> {
            frame.dispose();
            iniciarServidorSomente();
        });
        btnCliente.addActionListener(e -> {
            String host = JOptionPane.showInputDialog(frame,
                    "IP do servidor:", "localhost");
            if (host != null) {
                frame.dispose();
                abrirJanelaCliente("localhost".equals(host.trim()) ? "localhost" : host.trim(),
                        PORTA, 1);
            }
        });

        btnPanel.add(btnJogar);
        btnPanel.add(btnServidor);
        btnPanel.add(btnCliente);

        // Rodapé
        JLabel rodape = new JLabel(
                "<html><center><span style='font-size:8pt; color:#546e7a'>"
                + "Porta: " + PORTA + "  │  Netbeans Maven  │  Java 11+"
                + "</span></center></html>", JLabel.CENTER);

        panel.add(btnPanel, BorderLayout.CENTER);
        panel.add(rodape, BorderLayout.SOUTH);
        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    // ── Modo "Jogar Agora" ────────────────────────────────────────────────────
    private static void iniciarJogoCompleto() {
        // 1. Servidor em thread daemon
        Thread tServidor = new Thread(() -> {
            try {
                ServidorMain.main(new String[]{String.valueOf(PORTA)});
            } catch (Exception e) {
                System.err.println("Servidor: " + e.getMessage());
            }
        }, "Servidor");
        tServidor.setDaemon(true);
        tServidor.start();

        // 2. Aguarda 600ms para o servidor subir
        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {
        }

        // 3. Abre dois JFrames de cliente
        abrirJanelaCliente("localhost", PORTA, 1);
        abrirJanelaCliente("localhost", PORTA, 2);
    }

    // ── Modo "Só Servidor" ────────────────────────────────────────────────────
    // ✅ SUBSTITUI POR ESTE:
    private static void iniciarServidorSomente() {
        JFrame frame = criarJanelaConsola("⚓ Batalha Naval — Servidor", new Color(0, 20, 40));
        redireccionarOutputParaJanela(frame, () -> {
            try {
                ServidorMain.main(new String[]{String.valueOf(PORTA)});
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        });
    }

    // ── Janela de Cliente ─────────────────────────────────────────────────────
    /**
     * Abre uma janela Swing com uma área de texto que simula um terminal. A
     * InterfaceCLI lê do campo de input (JTextField) e escreve no JTextArea.
     */
    static void abrirJanelaCliente(String host, int porta, int numJogador) {
        // Componentes
        JFrame frame = new JFrame("⚓ Batalha Naval — Jogador " + numJogador);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setLocationRelativeTo(null);
        if (numJogador == 2) {
            frame.setLocation(frame.getX() + 30, frame.getY() + 30);
        }

        // Área de texto (output do jogo)
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(new Color(12, 20, 35));
        textArea.setForeground(new Color(200, 220, 255));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        textArea.setMargin(new Insets(8, 12, 8, 12));
        textArea.setLineWrap(false);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // Campo de input
        JTextField inputField = new JTextField();
        inputField.setBackground(new Color(20, 35, 55));
        inputField.setForeground(new Color(100, 220, 120));
        inputField.setFont(new Font("Monospaced", Font.BOLD, 14));
        inputField.setCaretColor(Color.GREEN);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 80, 120), 2),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JLabel lblInput = new JLabel(" ▶ ");
        lblInput.setForeground(new Color(100, 220, 120));
        lblInput.setFont(new Font("Monospaced", Font.BOLD, 14));

        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        inputPanel.setBackground(new Color(12, 20, 35));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        inputPanel.add(lblInput, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
        inputField.requestFocusInWindow();

        // Pipes: conectam o cliente (que usa System.in/out) à janela Swing
        PipedOutputStream pipeOut = new PipedOutputStream();
        PipedInputStream pipeIn = new PipedInputStream();
        PipedOutputStream pipeInSrc = new PipedOutputStream();
        try {
            pipeIn.connect(pipeInSrc);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // PrintStream que escreve na JTextArea
        PrintStream ps;
        try {
            PipedInputStream readOut = new PipedInputStream(pipeOut);
            ps = new PrintStream(pipeOut, true, "UTF-8");
            Thread leitorOutput = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(readOut, "UTF-8"))) {
                    String linha;
                    while ((linha = br.readLine()) != null) {
                        // Remove sequências ANSI para o JTextArea (não suporta ANSI nativo)
                        String limpa = linha.replaceAll("\u001B\\[[;\\d]*[mHJ]", "");
                        final String l = limpa;
                        SwingUtilities.invokeLater(() -> {
                            textArea.append(l + "\n");
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        });
                    }
                } catch (IOException e) {
                    /* terminal fechado */ }
            }, "OutputLeitor-J" + numJogador);
            leitorOutput.setDaemon(true);
            leitorOutput.start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Input do utilizador → pipe que o Scanner do cliente lê
        final PipedOutputStream finalPipeInSrc = pipeInSrc;
        inputField.addActionListener(e -> {
            String texto = inputField.getText();
            inputField.setText("");
            // Mostra no textArea o que o utilizador escreveu
            SwingUtilities.invokeLater(() -> textArea.append(" ▶ " + texto + "\n"));
            try {
                finalPipeInSrc.write((texto + "\n").getBytes("UTF-8"));
                finalPipeInSrc.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        final PrintStream finalPs = ps;
        final PipedInputStream finalPipeIn = pipeIn;
        final int fNum = numJogador;

        // Thread do cliente com I/O redirecionado
        Thread tCliente = new Thread(() -> {
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            InputStream oldIn = System.in;
            try {
                System.setOut(finalPs);
                System.setErr(finalPs);
                System.setIn(finalPipeIn);
                new InterfaceCLI(host, porta, fNum).iniciar();
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
                System.setIn(oldIn);
                SwingUtilities.invokeLater(()
                        -> JOptionPane.showMessageDialog(frame, "Jogo terminado!", "Batalha Naval",
                                JOptionPane.INFORMATION_MESSAGE));
            }
        }, "Cliente-J" + numJogador);
        tCliente.setDaemon(true);
        tCliente.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static JButton criarBotao(String texto, Color bg, Color hover) {
        JButton btn = new JButton("<html><b>" + texto + "</b></html>");
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hover);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    
  

    private static void redireccionarOutput(JFrame frame, Runnable task) {
        JTextArea area = (JTextArea) ((JScrollPane) frame.getContentPane().getComponent(0)).getViewport().getView();
        PrintStream ps;
        try {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            ps = new PrintStream(pos, true, "UTF-8");
            Thread t = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(pis, "UTF-8"))) {
                    String l;
                    while ((l = br.readLine()) != null) {
                        final String linha = l.replaceAll("\u001B\\[[;\\d]*[mHJ]", "");
                        SwingUtilities.invokeLater(() -> {
                            area.append(linha + "\n");
                            area.setCaretPosition(area.getDocument().getLength());
                        });
                    }
                } catch (IOException ignored) {
                }
            });
            t.setDaemon(true);
            t.start();
            System.setOut(ps);
            System.setErr(ps);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(task, "ServidorOutput").start();
    }

    // Mapa para guardar referência ao JTextArea sem casts inseguros
    private static final java.util.Map<JFrame, JTextArea> frameAreas = new java.util.HashMap<>();

    private static JFrame criarJanelaConsola(String titulo, Color bg) {
        JFrame f = new JFrame(titulo);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setSize(700, 500);
        f.setLocationRelativeTo(null);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(bg);
        area.setForeground(new Color(180, 210, 255));
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setMargin(new Insets(8, 12, 8, 12));

        f.add(new JScrollPane(area));
        f.setVisible(true);

        frameAreas.put(f, area); // guarda referência directa — sem cast depois
        return f;
    }

    private static void redireccionarOutputParaJanela(JFrame frame, Runnable task) {
        JTextArea area = frameAreas.get(frame); // sem cast, sem navegação na árvore Swing
        if (area == null) {
            new Thread(task).start();
            return;
        }

        try {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            PrintStream ps = new PrintStream(pos, true, "UTF-8");

            Thread leitor = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(pis, "UTF-8"))) {
                    String l;
                    while ((l = br.readLine()) != null) {
                        final String linha = l.replaceAll("\u001B\\[[;\\d]*[mHJ]", "");
                        SwingUtilities.invokeLater(() -> {
                            area.append(linha + "\n");
                            area.setCaretPosition(area.getDocument().getLength());
                        });
                    }
                } catch (IOException ignored) {
                }
            }, "OutputLeitor-Servidor");
            leitor.setDaemon(true);
            leitor.start();

            System.setOut(ps);
            System.setErr(ps);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread t = new Thread(task, "ServidorOutput");
        t.setDaemon(true);
        t.start();
    }

}
