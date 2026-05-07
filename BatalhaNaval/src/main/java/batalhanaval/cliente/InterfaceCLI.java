package batalhanaval.cliente;

import batalhanaval.jogo.Tabuleiro;
import batalhanaval.jogo.TipoNavio;
import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;
import batalhanaval.util.UtilNavio;

import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║   InterfaceCLI — Interface de linha de comandos do cliente   ║
 * ║   Rica em ANSI, responsiva por threads, com UX completa      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * UML:
 *   InterfaceCLI implements LigacaoServidor.MensagemCallback
 *   ──────────────────────────────────────────────────────────────
 *   - host, porta: String/int
 *   - ligacao: LigacaoServidor
 *   - scanner: Scanner
 *   - fase: Fase  (volatile — lida por duas threads)
 *   - tabProprio, tabAdversario: Tabuleiro
 *   - numJogador, tirosRestantes: int
 *   - ultimaMensagem: String
 *   ──────────────────────────────────────────────────────────────
 *   + iniciar(): void
 *   + onMensagemRecebida(Mensagem): void   ← callback (thread do socket)
 *   - fasePosicionamento(): void
 *   - faseJogo(): void
 *   - renderUI(): void                     ← re-desenha a consola completa
 *
 * Sincronização:
 *   → 'fase' é volatile: a thread do socket escreve, a thread principal lê
 *   → O Object 'lock' serve de monitor para wait()/notifyAll():
 *       thread principal faz lock.wait() quando não é seu turno
 *       thread do socket faz lock.notifyAll() quando a fase muda
 *   → Os tabuleiros são actualizados só pela thread do socket (sem concorrência)
 */
public class InterfaceCLI implements LigacaoServidor.MensagemCallback {

    // ── Cores ANSI ────────────────────────────────────────────────────────────
    static final String RESET  = "\u001B[0m";
    static final String BOLD   = "\u001B[1m";
    static final String DIM    = "\u001B[2m";
    static final String BLUE   = "\u001B[34m";
    static final String CYAN   = "\u001B[36m";
    static final String GREEN  = "\u001B[32m";
    static final String RED    = "\u001B[31m";
    static final String YELLOW = "\u001B[33m";
    static final String WHITE  = "\u001B[37m";
    static final String MAGENTA= "\u001B[35m";
    static final String BG_CYAN= "\u001B[46m";

    // ── Estado ────────────────────────────────────────────────────────────────
    private final String  host;
    private final int     porta;
    private final int     idLauncher;       // 0 = standalone, 1/2 = dentro do LauncherIDE
    private LigacaoServidor ligacao;
    private final Scanner scanner = new Scanner(System.in);

    private enum Fase { AGUARDAR_LIGACAO, POSICIONAMENTO, AGUARDAR_ADV, JOGO_MEU_TURNO,
                        JOGO_TURNO_ADV, FIM, ERRO }
    private volatile Fase   fase       = Fase.AGUARDAR_LIGACAO;
    private final Object     lock       = new Object(); // monitor para wait/notify

    private int              numJogador  = 0;
    private String           idJogo      = "";
    private int              tirosRestantes = 0;
    private volatile String  ultimaMensagem = "";
    private volatile String  tabuleirosTexto = "";  // recebido do servidor
    private final Tabuleiro  tabLocal    = new Tabuleiro(); // usado para render local rico

    // Contagem local dos navios ainda por posicionar
    private final Map<TipoNavio, Integer> naviosPorPosicionar = new LinkedHashMap<>();

    public InterfaceCLI(String host, int porta, int idLauncher) {
        this.host       = host;
        this.porta      = porta;
        this.idLauncher = idLauncher;
        for (TipoNavio t : TipoNavio.values()) naviosPorPosicionar.put(t, t.getQuantidade());
    }

    // ── Entrada principal ─────────────────────────────────────────────────────

    public void iniciar() {
        limparEcra();
        bannerBoas();
        try {
            print(CYAN, "▶ A ligar a " + host + ":" + porta + "...\n");
            ligacao = new LigacaoServidor(host, porta, this);
            print(GREEN, "✔ Ligado! A aguardar início do jogo...\n");
            aguardarFase(Fase.POSICIONAMENTO);
            fasePosicionamento();
            aguardarFase(Fase.JOGO_MEU_TURNO, Fase.JOGO_TURNO_ADV);
            faseJogo();
        } catch (Exception e) {
            print(RED, "\n✖ Erro: " + e.getMessage() + "\n");
        }
    }

    // ── Fase de Posicionamento ────────────────────────────────────────────────

    private void fasePosicionamento() {
        limparEcra();
        tituloPosicionamento();
        mostrarNaviosPorPosicionar();
        mostrarTabuleiroProprio();

        for (TipoNavio tipo : TipoNavio.values()) {
            int qtd = naviosPorPosicionar.get(tipo);
            for (int i = 0; i < qtd; i++) {
                boolean ok = false;
                while (!ok) {
                    ok = pedirPosicaoNavio(tipo, i + 1, qtd);
                }
            }
        }

        print(GREEN, "\n✔ Todos os navios posicionados! A aguardar o adversário...\n");
        ligacao.enviar(new Mensagem(TipoMensagem.PRONTO));
        setFase(Fase.AGUARDAR_ADV);
        print(DIM, "  (aguarda que o adversário termine de posicionar os seus navios)\n");
        aguardarFase(Fase.JOGO_MEU_TURNO, Fase.JOGO_TURNO_ADV);
    }

    private boolean pedirPosicaoNavio(TipoNavio tipo, int num, int total) {
        System.out.println();
        print(BOLD + YELLOW, "  ┌─ Posicionar: " + tipo.getNome()
                + " (" + tipo.getTamanho() + " casas)"
                + (total > 1 ? " [" + num + "/" + total + "]" : "") + "\n");
        print(WHITE, "  │  Início (ex: A1, B3, C10): ");
        String celula = lerLinha().trim().toUpperCase();
        if (!Tabuleiro.celulaValida(celula)) {
            print(RED, "  └✖ Célula inválida. Use letra A-J + número 1-10 (ex: A1, B10)\n"); return false;
        }
        print(WHITE, "  │  Orientação [H]orizontal / [V]ertical: ");
        String ori = lerLinha().trim().toUpperCase();
        if (!ori.equals("H") && !ori.equals("V")) {
            print(RED, "  └✖ Orientação inválida. Use H ou V\n"); return false;
        }

        // Validação local antes de enviar
        var celulas = UtilNavio.calcularCelulas(celula, ori.charAt(0), tipo.getTamanho());
        if (celulas == null) {
            print(RED, "  └✖ O navio ultrapassa os limites do tabuleiro!\n"); return false;
        }
        var navio = new batalhanaval.jogo.Navio(tipo, celulas);
        if (!tabLocal.colocarNavio(navio)) {
            print(RED, "  └✖ Posição ocupada ou inválida!\n"); return false;
        }

        // Envia ao servidor (servidor também valida)
        ligacao.enviar(new Mensagem(TipoMensagem.POSICIONAR_NAVIO,
                UtilNavio.serializarPosicionamento(tipo, celula, ori.charAt(0))));

        // Aguarda confirmação do servidor (com timeout)
        try {
            synchronized (lock) { lock.wait(3000); }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        if (fase == Fase.ERRO) {
            tabLocal.getNavios(); // o navio já não está no local → desfaz
            // Re-cria o tabuleiro local sem o último navio (simplificação)
            print(RED, "  └✖ Servidor recusou: " + ultimaMensagem + "\n");
            return false;
        }
        print(GREEN, "  └✔ " + tipo.getNome() + " posicionado em " + celulas + "\n");
        mostrarTabuleiroProprio();
        return true;
    }

    // ── Fase de Jogo ──────────────────────────────────────────────────────────

    private void faseJogo() {
        while (fase != Fase.FIM && fase != Fase.ERRO) {
            renderUI();
            if (fase == Fase.JOGO_MEU_TURNO) {
                executarTiro();
            } else if (fase == Fase.JOGO_TURNO_ADV) {
                print(DIM, "\n  ⏳ Aguarda o tiro do adversário...\n");
                aguardarFase(Fase.JOGO_MEU_TURNO, Fase.FIM);
            }
        }
        renderFimJogo();
    }

    private void executarTiro() {
        print(BOLD + CYAN, "\n  ╔════════════════════════════════════╗\n");
        print(BOLD + CYAN, "  ║  É O TEU TURNO!  Tiros: " + tirosRestantes + "           ║\n");
        print(BOLD + CYAN, "  ╚════════════════════════════════════╝\n");
        print(WHITE, "  Coordenada (ex: B4) ou [S]air: ");
        String input = lerLinha().trim().toUpperCase();

        if (input.equals("S") || input.equals("SAIR")) {
            ligacao.enviar(new Mensagem(TipoMensagem.SAIR));
            setFase(Fase.FIM);
            return;
        }
        if (input.equalsIgnoreCase("GUARDAR")) {
            ligacao.enviar(new Mensagem(TipoMensagem.GUARDAR));
            return;
        }
        if (!Tabuleiro.celulaValida(input)) {
            print(RED, "  ✖ Coordenada inválida! Use letra A-J + número 1-10\n"); return;
        }
        ligacao.enviar(new Mensagem(TipoMensagem.TIRO, input));
        // Aguarda resultado (TEU_TURNO ou TURNO_ADVERSARIO ou FIM_JOGO)
        aguardarFase(Fase.JOGO_MEU_TURNO, Fase.JOGO_TURNO_ADV, Fase.FIM);
    }

    // ── Callback do socket (corre na thread LeitorServidor) ───────────────────

    @Override
    public void onMensagemRecebida(Mensagem msg) {
        switch (msg.getTipo()) {

            case LIGADO:
                String[] p = msg.getDados().split(":");
                numJogador = Integer.parseInt(p[0]);
                idJogo = p.length > 1 ? p[1] : "";
                break;

            case INICIAR_POSICIONAMENTO:
                setFase(Fase.POSICIONAMENTO);
                break;

            case NAVIO_POSICIONADO:
                ultimaMensagem = msg.getDados();
                notificarLock();
                break;

            case NAVIO_INVALIDO:
                ultimaMensagem = msg.getDados();
                setFase(Fase.ERRO);
                notificarLock();
                // Volta ao posicionamento após erro
                setFase(Fase.POSICIONAMENTO);
                break;

            case TEU_TURNO:
                tirosRestantes = batalhanaval.jogo.EstadoJogo.TIROS_POR_TURNO;
                ultimaMensagem = msg.getDados();
                setFase(Fase.JOGO_MEU_TURNO);
                break;

            case TURNO_ADVERSARIO:
                ultimaMensagem = msg.getDados();
                setFase(Fase.JOGO_TURNO_ADV);
                break;

            case TIROS_RESTANTES:
                try { tirosRestantes = Integer.parseInt(msg.getDados().trim()); } catch (NumberFormatException ignored) {}
                setFase(Fase.JOGO_MEU_TURNO);
                break;

            case RESULTADO_TIRO:
                mostrarResultadoTiro(msg.getDados(), true);
                break;

            case TIRO_ADVERSARIO:
                mostrarResultadoTiro(msg.getDados(), false);
                break;

            case ESTADO_TABULEIRO:
                tabuleirosTexto = msg.getDados();
                break;

            case FIM_JOGO:
                ultimaMensagem = msg.getDados();
                setFase(Fase.FIM);
                break;

            case JOGO_GUARDADO:
                print(GREEN, "\n  ✔ Jogo guardado em: " + msg.getDados() + "\n");
                break;

            case JOGO_CARREGADO:
                print(GREEN, "\n  ✔ Jogo carregado: " + msg.getDados() + "\n");
                break;

            case ADVERSARIO_DESLIGADO:
                print(RED, "\n  ✖ " + msg.getDados() + "\n");
                setFase(Fase.FIM);
                break;

            case ERRO:
                print(RED, "\n  ✖ Servidor: " + msg.getDados() + "\n");
                notificarLock();
                break;

            default:
                break;
        }
    }

    // ── Renderização ──────────────────────────────────────────────────────────

    private void renderUI() {
        limparEcra();
        // Cabeçalho
        System.out.println(BOLD + CYAN
            + "  ╔══════════════════════════════════════════════════════════╗");
        System.out.printf( "  ║  ⚓  BATALHA NAVAL  │  Jogador %d  │  Jogo: %-14s║%n",
                numJogador, idJogo);
        System.out.println(
            "  ╚══════════════════════════════════════════════════════════╝" + RESET);

        // Tabuleiros lado a lado (renderizados localmente com ANSI)
        System.out.println(tabLocal.renderDuplo());

        // Última mensagem do servidor
        if (!ultimaMensagem.isEmpty()) {
            print(YELLOW, "  → " + ultimaMensagem + "\n");
        }
        System.out.println();
    }

    private void mostrarResultadoTiro(String dados, boolean meuTiro) {
        // dados = "AGUA:B4" ou "ACERTOU:Fragata:B4" ou "AFUNDOU:Fragata:B4"
        String[] p = dados.split(":");
        String tipo = p[0];
        String coord = p[p.length - 1];
        String nomeNavio = (p.length > 2) ? p[1] : "";

        if (meuTiro) {
            switch (tipo) {
                case "AGUA":
                    print(BLUE,   "  💧 ÁGUA em " + coord + "!\n");
                    break;
                case "ACERTOU":
                    print(RED,    "  🎯 ACERTOU! " + nomeNavio + " em " + coord + "!\n");
                    break;
                case "AFUNDOU":
                    print(BOLD + RED + BG_CYAN,
                          "  💥 AFUNDOU o " + nomeNavio + " em " + coord + "!\n");
                    break;
            }
            // Actualiza tabuleiro adversário local
            tabLocal.registarTiroEfetuado(coord, tipo);
        } else {
            switch (tipo) {
                case "AGUA":
                    print(DIM,    "  💧 Adversário atirou " + coord + " → ÁGUA\n");
                    break;
                case "ACERTOU":
                    print(YELLOW, "  ⚠  Adversário acertou no teu " + nomeNavio + " em " + coord + "!\n");
                    break;
                case "AFUNDOU":
                    print(BOLD + RED, "  ☠  Adversário afundou o teu " + nomeNavio + " em " + coord + "!\n");
                    break;
            }
        }
        ultimaMensagem = (meuTiro ? "O teu tiro: " : "Tiro adversário: ")
                       + tipo + (nomeNavio.isEmpty() ? "" : " " + nomeNavio) + " em " + coord;
    }

    private void renderFimJogo() {
        limparEcra();
        boolean vitoria = "VITORIA".equals(ultimaMensagem);
        boolean empate  = "EMPATE".equals(ultimaMensagem);

        if (vitoria) {
            System.out.println(BOLD + GREEN);
            System.out.println("  ╔══════════════════════════════════════════╗");
            System.out.println("  ║          🏆  VITÓRIA!  PARABÉNS!  🏆      ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
        } else if (empate) {
            System.out.println(BOLD + YELLOW);
            System.out.println("  ╔══════════════════════════════════════════╗");
            System.out.println("  ║              🤝  EMPATE!                 ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
        } else {
            System.out.println(BOLD + RED);
            System.out.println("  ╔══════════════════════════════════════════╗");
            System.out.println("  ║              💀  DERROTA...              ║");
            System.out.println("  ╚══════════════════════════════════════════╝");
        }
        System.out.println(RESET);
        System.out.println(tabLocal.renderDuplo());
        System.out.println(DIM + "  Obrigado por jogar! Prima ENTER para sair." + RESET);
        lerLinha();
        ligacao.fechar();
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

    private void bannerBoas() {
        System.out.println(BOLD + BLUE);
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║     ⚓  BATALHA NAVAL  ⚓                     ║");
        System.out.println("  ║     Laboratórios de Programação — TP3        ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    private void tituloPosicionamento() {
        System.out.println(BOLD + MAGENTA
            + "  ╔══════════════════════════════════════════════╗\n"
            + "  ║   🚢  FASE DE POSICIONAMENTO DOS NAVIOS  🚢   ║\n"
            + "  ╚══════════════════════════════════════════════╝"
            + RESET + "\n");
    }

    private void mostrarNaviosPorPosicionar() {
        System.out.println(BOLD + WHITE + "  Navios a posicionar:" + RESET);
        for (TipoNavio t : TipoNavio.values()) {
            System.out.printf("    %s%-15s%s × %d  (%d casas)%n",
                GREEN, t.getNome(), RESET, t.getQuantidade(), t.getTamanho());
        }
        System.out.println();
    }

    private void mostrarTabuleiroProprio() {
        System.out.println(tabLocal.renderProprioTabuleiro());
    }

    private static void limparEcra() {
        // Funciona na maioria dos terminais (NetBeans Output tab respeita sequências ANSI)
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    static void print(String cor, String msg) {
        System.out.print(cor + msg + RESET);
    }

    private String lerLinha() {
        try { return scanner.nextLine(); }
        catch (Exception e) { return ""; }
    }

    // ── Sincronização por wait/notify ────────────────────────────────────────

    /**
     * Bloqueia a thread principal até a fase ser uma das esperadas.
     * A thread do socket chama setFase() → notifyAll() para acordar.
     */
    private void aguardarFase(Fase... fasesEsperadas) {
        synchronized (lock) {
            while (!faseIgualA(fasesEsperadas)) {
                try { lock.wait(200); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private boolean faseIgualA(Fase[] fases) {
        for (Fase f : fases) { if (fase == f) return true; }
        return false;
    }

    private void setFase(Fase novaFase) {
        synchronized (lock) { fase = novaFase; lock.notifyAll(); }
    }

    private void notificarLock() {
        synchronized (lock) { lock.notifyAll(); }
    }
}
