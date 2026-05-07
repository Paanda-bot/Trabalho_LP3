package batalhanaval.servidor;

import batalhanaval.jogo.EstadoJogo;
import batalhanaval.jogo.Navio;
import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;
import batalhanaval.util.UtilNavio;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════
 *  GestorJogo — árbitro de uma partida entre dois jogadores
 * ══════════════════════════════════════════════════════════
 *
 * UML:
 *   GestorJogo implements Runnable
 *   ─────────────────────────────────────────────────────────
 *   - idJogo: String
 *   - socketJ1, socketJ2: Socket
 *   - outJ1, outJ2: PrintWriter
 *   - estado: EstadoJogo
 *   - j1Ligado, j2Ligado: volatile boolean
 *   ─────────────────────────────────────────────────────────
 *   + run(): void                 ← Thread principal do jogo
 *   - processarMensagem(int, Mensagem): void   ← synchronized
 *   - processarPosicionamento(int, String): void
 *   - processarTiro(int, String): void
 *   - guardarJogo(int): void
 *   - carregarJogo(int, String): void
 *   - enviarMensagem(int, TipoMensagem, String): void
 *   - enviarTabuleiros(int): void
 *
 *   [inner class] LeitorCliente implements Runnable
 *   ─────────────────────────────────────────────────────────
 *   Lê mensagens de um cliente numa Thread dedicada
 *   e chama processarMensagem() para cada uma.
 *
 * Por que synchronized em processarMensagem?
 *   → Dois LeitorCliente podem chamar este método em simultâneo
 *     (um por cada jogador). O synchronized garante que só um
 *     processa de cada vez, evitando race conditions no EstadoJogo.
 */
public class GestorJogo implements Runnable {

    private static final String PASTA_SAVES = "saves/";

    private final String idJogo;
    private final Socket socketJ1;
    private final Socket socketJ2;
    private PrintWriter  outJ1;
    private PrintWriter  outJ2;
    private EstadoJogo   estado;
    private volatile boolean j1Ligado = true;
    private volatile boolean j2Ligado = true;

    public GestorJogo(Socket socketJ1, Socket socketJ2) {
        this.socketJ1 = socketJ1;
        this.socketJ2 = socketJ2;
        this.idJogo   = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        new File(PASTA_SAVES).mkdirs();
    }

    public String getIdJogo() { return idJogo; }

    @Override
    public void run() {
        log(ServidorMain.GREEN, "Jogo " + idJogo + " a iniciar...");
        try {
            outJ1 = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socketJ1.getOutputStream())), true);
            outJ2 = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socketJ2.getOutputStream())), true);

            estado = new EstadoJogo(idJogo);

            enviarMensagem(1, TipoMensagem.LIGADO, "1:" + idJogo);
            enviarMensagem(2, TipoMensagem.LIGADO, "2:" + idJogo);
            enviarMensagem(1, TipoMensagem.INICIAR_POSICIONAMENTO, "Posicione os seus navios.");
            enviarMensagem(2, TipoMensagem.INICIAR_POSICIONAMENTO, "Posicione os seus navios.");

            Thread lj1 = new Thread(new LeitorCliente(1, socketJ1), "Leitor-J1-" + idJogo);
            Thread lj2 = new Thread(new LeitorCliente(2, socketJ2), "Leitor-J2-" + idJogo);
            lj1.setDaemon(true);
            lj2.setDaemon(true);
            lj1.start();
            lj2.start();
            lj1.join();
            lj2.join();

        } catch (IOException | InterruptedException e) {
            log(ServidorMain.RED, "Erro no jogo " + idJogo + ": " + e.getMessage());
            guardarAutomaticamente();
        } finally {
            fecharLigacoes();
            ServidorMain.jogosActivos.remove(idJogo);
            log(ServidorMain.DIM, "Jogo " + idJogo + " terminado.");
        }
    }

    // ── Processamento de mensagens ───────────────────────────────────────────

    private synchronized void processarMensagem(int jogador, Mensagem msg) {
        if (msg == null) return;
        log(ServidorMain.DIM, "J" + jogador + " → " + msg.serializar());

        switch (msg.getTipo()) {
            case POSICIONAR_NAVIO: processarPosicionamento(jogador, msg.getDados()); break;
            case PRONTO:           processarPronto(jogador);                          break;
            case TIRO:             processarTiro(jogador, msg.getDados());            break;
            case GUARDAR:          guardarJogo(jogador);                              break;
            case CARREGAR:         carregarJogo(jogador, msg.getDados());             break;
            case SAIR:             tratarDesconexao(jogador);                         break;
            default: enviarMensagem(jogador, TipoMensagem.ERRO, "Comando desconhecido.");
        }
    }

    // ── Posicionamento ───────────────────────────────────────────────────────

    private void processarPosicionamento(int jogador, String dados) {
        if (estado.getFase() != EstadoJogo.Fase.POSICIONAMENTO) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Não é fase de posicionamento."); return;
        }
        Navio navio = UtilNavio.deserializarPosicionamento(dados);
        if (navio == null) {
            enviarMensagem(jogador, TipoMensagem.NAVIO_INVALIDO, "Formato inválido: " + dados); return;
        }
        if (estado.colocarNavio(jogador, navio)) {
            enviarMensagem(jogador, TipoMensagem.NAVIO_POSICIONADO,
                    navio.getTipo().getNome() + " em " + navio.getCelulas());
        } else {
            enviarMensagem(jogador, TipoMensagem.NAVIO_INVALIDO,
                    "Posição inválida para " + navio.getTipo().getNome());
        }
    }

    private void processarPronto(int jogador) {
        if (estado.getFase() != EstadoJogo.Fase.POSICIONAMENTO) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Não é fase de posicionamento."); return;
        }
        if (!estado.getTabuleiro(jogador).estaCompleto()) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Ainda faltam navios por posicionar."); return;
        }
        estado.marcarPronto(jogador);
        enviarMensagem(jogador, TipoMensagem.AGUARDAR, "A aguardar o adversário...");
        if (estado.getFase() == EstadoJogo.Fase.JOGO) iniciarFaseJogo();
    }

    private void iniciarFaseJogo() {
        int actual = estado.getJogadorActual();
        int outro  = (actual == 1) ? 2 : 1;
        log(ServidorMain.GREEN, "Jogo " + idJogo + " INICIADO! Começa J" + actual);
        enviarMensagem(actual, TipoMensagem.TEU_TURNO,
                "O jogo começa! Tens " + EstadoJogo.TIROS_POR_TURNO + " tiros.");
        enviarMensagem(outro,  TipoMensagem.TURNO_ADVERSARIO,
                "O jogo começa! Aguarda a tua vez (J" + actual + " começa).");
        enviarTabuleiros(1);
        enviarTabuleiros(2);
    }

    // ── Tiros ────────────────────────────────────────────────────────────────

    private void processarTiro(int jogador, String coordenada) {
        if (estado.getFase() != EstadoJogo.Fase.JOGO) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "O jogo ainda não começou."); return;
        }
        String resultado = estado.processarTiro(jogador, coordenada);
        if ("NAO_TEU_TURNO".equals(resultado)) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Não é o teu turno."); return;
        }
        if ("JA_ATIRADO".equals(resultado)) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Já atiraste para " + coordenada + "."); return;
        }

        String resMsg = resultado + ":" + coordenada;
        int adversario = (jogador == 1) ? 2 : 1;

        enviarMensagem(jogador,    TipoMensagem.RESULTADO_TIRO,  resMsg);
        enviarMensagem(adversario, TipoMensagem.TIRO_ADVERSARIO, resMsg);
        enviarTabuleiros(1);
        enviarTabuleiros(2);

        if (estado.jogoTerminou()) {
            anunciarFimJogo();
        } else if (estado.getJogadorActual() == jogador) {
            // Ainda é o mesmo turno
            enviarMensagem(jogador, TipoMensagem.TIROS_RESTANTES,
                    "" + estado.getTirosRestantesNoTurno());
        } else {
            // Turno passou
            int novoActual = estado.getJogadorActual();
            int novoOutro  = (novoActual == 1) ? 2 : 1;
            enviarMensagem(novoActual, TipoMensagem.TEU_TURNO,
                    "É o teu turno! Tens " + EstadoJogo.TIROS_POR_TURNO + " tiros.");
            enviarMensagem(novoOutro,  TipoMensagem.TURNO_ADVERSARIO,
                    "Aguarda. É o turno do Jogador " + novoActual + ".");
        }
    }

    private void anunciarFimJogo() {
        int vencedor = estado.getResultado();
        log(ServidorMain.YELLOW, "Jogo " + idJogo + " TERMINADO! Vencedor: J" + vencedor);
        if (vencedor == 1) {
            enviarMensagem(1, TipoMensagem.FIM_JOGO, "VITORIA");
            enviarMensagem(2, TipoMensagem.FIM_JOGO, "DERROTA");
        } else if (vencedor == 2) {
            enviarMensagem(1, TipoMensagem.FIM_JOGO, "DERROTA");
            enviarMensagem(2, TipoMensagem.FIM_JOGO, "VITORIA");
        } else {
            enviarMensagem(1, TipoMensagem.FIM_JOGO, "EMPATE");
            enviarMensagem(2, TipoMensagem.FIM_JOGO, "EMPATE");
        }
    }

    // ── Save / Load ──────────────────────────────────────────────────────────

    private void guardarJogo(int jogador) {
        String caminho = PASTA_SAVES + "jogo_" + idJogo + ".dat";
        try {
            estado.guardarEmFicheiro(caminho);
            enviarMensagem(1, TipoMensagem.JOGO_GUARDADO, caminho);
            enviarMensagem(2, TipoMensagem.JOGO_GUARDADO, caminho);
            log(ServidorMain.GREEN, "Jogo " + idJogo + " guardado → " + caminho);
        } catch (IOException e) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Erro ao guardar: " + e.getMessage());
        }
    }

    private void guardarAutomaticamente() {
        if (estado == null) return;
        String caminho = PASTA_SAVES + "jogo_" + idJogo + "_auto.dat";
        try {
            estado.guardarEmFicheiro(caminho);
            log(ServidorMain.YELLOW, "Auto-save → " + caminho);
        } catch (IOException e) {
            log(ServidorMain.RED, "Erro auto-save: " + e.getMessage());
        }
    }

    private void carregarJogo(int jogador, String caminho) {
        try {
            estado = EstadoJogo.carregarDeFicheiro(caminho);
            enviarMensagem(1, TipoMensagem.JOGO_CARREGADO, "Jogo carregado de: " + caminho);
            enviarMensagem(2, TipoMensagem.JOGO_CARREGADO, "Jogo carregado de: " + caminho);
            enviarTabuleiros(1);
            enviarTabuleiros(2);
            iniciarFaseJogo();
        } catch (IOException | ClassNotFoundException e) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Erro ao carregar: " + e.getMessage());
        }
    }

    // ── Comunicação ──────────────────────────────────────────────────────────

    private void enviarTabuleiros(int jogador) {
        String proprio    = estado.getTabuleiro(jogador).toStringProprioTabuleiro();
        String adversario = estado.getTabuleiro(jogador).toStringTabuleirAdversario();
        enviarMensagem(jogador, TipoMensagem.ESTADO_TABULEIRO,
                proprio.replace("\n","\\n") + "||" + adversario.replace("\n","\\n"));
    }

    private void enviarMensagem(int jogador, TipoMensagem tipo, String dados) {
        PrintWriter out = (jogador == 1) ? outJ1 : outJ2;
        if (out != null) {
            out.println(new Mensagem(tipo, dados).serializar());
        }
    }

    private void tratarDesconexao(int jogador) {
        log(ServidorMain.RED, "J" + jogador + " desligou-se do jogo " + idJogo);
        guardarAutomaticamente();
        int adversario = (jogador == 1) ? 2 : 1;
        enviarMensagem(adversario, TipoMensagem.ADVERSARIO_DESLIGADO,
                "O adversário desligou-se. Estado guardado automaticamente.");
    }

    private void fecharLigacoes() {
        try { if (socketJ1 != null) socketJ1.close(); } catch (IOException ignored) {}
        try { if (socketJ2 != null) socketJ2.close(); } catch (IOException ignored) {}
    }

    private void log(String cor, String msg) {
        ServidorMain.log(cor, "[" + idJogo + "] " + msg);
    }

    // ── Thread de leitura do cliente ─────────────────────────────────────────

    /**
     * LeitorCliente — corre numa Thread dedicada por cliente.
     *
     * Por que precisamos de uma thread por cliente?
     * → ServerSocket.accept() é bloqueante, assim como BufferedReader.readLine().
     *   Se lermos ambos os clientes sequencialmente, um ficaria bloqueado enquanto
     *   o outro esperava. Com uma thread por cliente, ambos lêem em paralelo.
     * → O processamento das mensagens é synchronized para garantir
     *   exclusão mútua no estado do jogo.
     */
    private class LeitorCliente implements Runnable {
        private final int    jogador;
        private final Socket socket;

        LeitorCliente(int jogador, Socket socket) {
            this.jogador = jogador;
            this.socket  = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
                String linha;
                while ((linha = in.readLine()) != null) {
                    processarMensagem(jogador, Mensagem.deserializar(linha));
                    if (estado != null && estado.jogoTerminou()) break;
                }
            } catch (IOException e) {
                if (estado != null && !estado.jogoTerminou()) tratarDesconexao(jogador);
            }
        }
    }
}
