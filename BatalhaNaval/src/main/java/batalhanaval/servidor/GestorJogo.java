package batalhanaval.servidor;

import batalhanaval.jogo.EstadoJogo;
import batalhanaval.jogo.Navio;
import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;
import batalhanaval.util.UtilNavio;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor de um jogo entre dois jogadores.
 *
 * Esta classe implementa Runnable para correr numa Thread dedicada.
 * Cada instância gere exactamente um jogo (dois jogadores).
 *
 * Responsabilidades:
 *  - Receber mensagens dos dois clientes (cada um numa sub-thread de leitura)
 *  - Validar posicionamentos de navios
 *  - Gerir turnos e tiros
 *  - Comunicar resultados a ambos os jogadores
 *  - Guardar/carregar o estado do jogo
 *  - Tratar desconexões e guardar automaticamente
 *
 * Modelo de concorrência:
 *  - A Thread principal (run) coordena o estado do jogo
 *  - Duas sub-threads lêem os sockets de cada jogador (LeitorCliente)
 *  - As mensagens recebidas são processadas com synchronized para evitar race conditions
 */
public class GestorJogo implements Runnable {

    /** Identificador único deste jogo (usado também para o ficheiro de save) */
    private final String idJogo;

    /** Sockets dos dois jogadores */
    private final Socket socketJ1;
    private final Socket socketJ2;

    /** Streams de saída para cada jogador */
    private PrintWriter outJ1;
    private PrintWriter outJ2;

    /** Estado actual do jogo */
    private EstadoJogo estado;

    /** Pasta onde são guardados os ficheiros de save */
    private static final String PASTA_SAVES = "saves/";

    /** Mapa de jogos activos do servidor (referência ao mapa global) */
    private final Map<String, GestorJogo> jogosActivos;

    /** Flags de ligação activa */
    private volatile boolean j1Ligado = true;
    private volatile boolean j2Ligado = true;

    /**
     * Cria um novo GestorJogo para dois clientes.
     *
     * @param socketJ1      socket do Jogador 1
     * @param socketJ2      socket do Jogador 2
     * @param jogosActivos  referência ao mapa global de jogos
     */
    public GestorJogo(Socket socketJ1, Socket socketJ2, Map<String, GestorJogo> jogosActivos) {
        this.socketJ1 = socketJ1;
        this.socketJ2 = socketJ2;
        this.jogosActivos = jogosActivos;
        this.idJogo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Garante que a pasta de saves existe
        new File(PASTA_SAVES).mkdirs();
    }

    /** @return o ID único deste jogo */
    public String getIdJogo() { return idJogo; }

    @Override
    public void run() {
        System.out.println("[JOGO " + idJogo + "] A iniciar...");

        try {
            // Inicializa os streams de comunicação
            outJ1 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketJ1.getOutputStream())), true);
            outJ2 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketJ2.getOutputStream())), true);

            // Cria o estado inicial do jogo
            estado = new EstadoJogo(idJogo);

            // Informa os clientes do seu número de jogador e do ID do jogo
            enviarMensagem(1, TipoMensagem.LIGADO, "1:" + idJogo);
            enviarMensagem(2, TipoMensagem.LIGADO, "2:" + idJogo);

            // Pede aos dois jogadores para posicionarem os navios
            enviarMensagem(1, TipoMensagem.INICIAR_POSICIONAMENTO, "Posicione os seus navios.");
            enviarMensagem(2, TipoMensagem.INICIAR_POSICIONAMENTO, "Posicione os seus navios.");

            // Inicia as threads de leitura de cada cliente
            Thread leitorJ1 = new Thread(new LeitorCliente(1, socketJ1), "LeitorJ1-" + idJogo);
            Thread leitorJ2 = new Thread(new LeitorCliente(2, socketJ2), "LeitorJ2-" + idJogo);
            leitorJ1.setDaemon(true);
            leitorJ2.setDaemon(true);
            leitorJ1.start();
            leitorJ2.start();

            // Aguarda o fim do jogo (ambas as threads de leitura terminam)
            leitorJ1.join();
            leitorJ2.join();

        } catch (IOException | InterruptedException e) {
            System.err.println("[JOGO " + idJogo + "] Erro: " + e.getMessage());
            guardarAutomaticamente(); // Guarda em caso de falha
        } finally {
            fecharLigacoes();
            jogosActivos.remove(idJogo);
            System.out.println("[JOGO " + idJogo + "] Terminado.");
        }
    }

    // ─── Processamento de mensagens ─────────────────────────────────────────

    /**
     * Processa uma mensagem recebida de um jogador.
     * Método synchronized: apenas uma mensagem é processada de cada vez,
     * evitando race conditions no estado do jogo.
     *
     * @param jogador número do jogador (1 ou 2)
     * @param msg     mensagem recebida
     */
    private synchronized void processarMensagem(int jogador, Mensagem msg) {
        if (msg == null) return;

        System.out.println("[JOGO " + idJogo + "] J" + jogador + " → " + msg.serializar());

        switch (msg.getTipo()) {

            case POSICIONAR_NAVIO:
                processarPosicionamento(jogador, msg.getDados());
                break;

            case PRONTO:
                processarPronto(jogador);
                break;

            case TIRO:
                processarTiro(jogador, msg.getDados());
                break;

            case GUARDAR:
                guardarJogo(jogador);
                break;

            case CARREGAR:
                carregarJogo(jogador, msg.getDados());
                break;

            case SAIR:
                tratarDesconexao(jogador);
                break;

            default:
                enviarMensagem(jogador, TipoMensagem.ERRO, "Comando desconhecido: " + msg.getTipo());
        }
    }

    // ─── Posicionamento ─────────────────────────────────────────────────────

    /**
     * Trata o posicionamento de um navio.
     * Dados esperados: "TIPO_NAVIO:INICIO:ORIENTACAO"  ex: "FRAGATA:C3:H"
     */
    private void processarPosicionamento(int jogador, String dados) {
        if (estado.getFase() != EstadoJogo.Fase.POSICIONAMENTO) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Não é fase de posicionamento.");
            return;
        }

        Navio navio = UtilNavio.deserializarPosicionamento(dados);
        if (navio == null) {
            enviarMensagem(jogador, TipoMensagem.NAVIO_INVALIDO, "Formato inválido: " + dados);
            return;
        }

        boolean ok = estado.colocarNavio(jogador, navio);
        if (ok) {
            enviarMensagem(jogador, TipoMensagem.NAVIO_POSICIONADO,
                    navio.getTipo().getNome() + " colocado em " + navio.getCelulas());
        } else {
            enviarMensagem(jogador, TipoMensagem.NAVIO_INVALIDO,
                    "Posição inválida para " + navio.getTipo().getNome());
        }
    }

    /**
     * Trata a confirmação de que um jogador terminou de posicionar.
     * Quando ambos confirmam, o jogo começa.
     */
    private void processarPronto(int jogador) {
        if (estado.getFase() != EstadoJogo.Fase.POSICIONAMENTO) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Não é fase de posicionamento.");
            return;
        }

        // Verifica se o tabuleiro está completo
        if (!estado.getTabuleiro(jogador).estaCompleto()) {
            enviarMensagem(jogador, TipoMensagem.ERRO,
                    "Ainda não colocou todos os navios. Verifique os tipos necessários.");
            return;
        }

        estado.marcarPronto(jogador);
        enviarMensagem(jogador, TipoMensagem.AGUARDAR, "A aguardar o adversário...");

        // Se ambos estiverem prontos, começa o jogo
        if (estado.getFase() == EstadoJogo.Fase.JOGO) {
            iniciarFaseJogo();
        }
    }

    /** Informa os dois jogadores de quem começa e envia o estado inicial */
    private void iniciarFaseJogo() {
        int actual = estado.getJogadorActual();
        int outro = (actual == 1) ? 2 : 1;

        enviarMensagem(actual, TipoMensagem.TEU_TURNO,
                "O jogo começa! Tens " + EstadoJogo.TIROS_POR_TURNO + " tiros.");
        enviarMensagem(outro, TipoMensagem.TURNO_ADVERSARIO,
                "O jogo começa! É o turno do Jogador " + actual + ".");

        // Envia os tabuleiros a cada jogador
        enviarTabuleiros(1);
        enviarTabuleiros(2);
    }

    // ─── Tiros ──────────────────────────────────────────────────────────────

    /**
     * Processa um tiro de um jogador.
     *
     * @param jogador   jogador que atirou
     * @param coordenada célula alvo (ex: "B4")
     */
    private void processarTiro(int jogador, String coordenada) {
        if (estado.getFase() != EstadoJogo.Fase.JOGO) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "O jogo ainda não começou.");
            return;
        }

        String resultado = estado.processarTiro(jogador, coordenada);

        // Constrói mensagem de resultado
        String resultadoMsg = resultado + ":" + coordenada;

        // Envia resultado a ambos os jogadores
        enviarMensagem(jogador, TipoMensagem.RESULTADO_TIRO, resultadoMsg);
        int adversario = (jogador == 1) ? 2 : 1;
        enviarMensagem(adversario, TipoMensagem.TIRO_ADVERSARIO, resultadoMsg);

        // Actualiza tabuleiros
        enviarTabuleiros(1);
        enviarTabuleiros(2);

        // Verifica fim do jogo
        if (estado.jogoTerminou()) {
            anunciarFimJogo();
        } else if (!"NAO_TEU_TURNO".equals(resultado) && !"JA_ATIRADO".equals(resultado)) {
            // Informa quantos tiros restam ou anuncia mudança de turno
            int tirosRestantes = estado.getTirosRestantesNoTurno();
            if (estado.getJogadorActual() == jogador) {
                // Ainda é o mesmo turno
                enviarMensagem(jogador, TipoMensagem.TIROS_RESTANTES,
                        "Tiros restantes: " + tirosRestantes);
            } else {
                // Turno passou
                int novoActual = estado.getJogadorActual();
                int novoOutro = (novoActual == 1) ? 2 : 1;
                enviarMensagem(novoActual, TipoMensagem.TEU_TURNO,
                        "É o teu turno! Tens " + EstadoJogo.TIROS_POR_TURNO + " tiros.");
                enviarMensagem(novoOutro, TipoMensagem.TURNO_ADVERSARIO,
                        "Turno do Jogador " + novoActual + ".");
            }
        }
    }

    /** Anuncia o resultado final do jogo a ambos os jogadores */
    private void anunciarFimJogo() {
        int vencedor = estado.getResultado();
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

    // ─── Guardar / Carregar ─────────────────────────────────────────────────

    /**
     * Guarda o estado do jogo a pedido de um jogador.
     */
    private void guardarJogo(int jogador) {
        String caminho = PASTA_SAVES + "jogo_" + idJogo + ".dat";
        try {
            estado.guardarEmFicheiro(caminho);
            enviarMensagem(1, TipoMensagem.JOGO_GUARDADO, caminho);
            enviarMensagem(2, TipoMensagem.JOGO_GUARDADO, caminho);
            System.out.println("[JOGO " + idJogo + "] Guardado em " + caminho);
        } catch (IOException e) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Erro ao guardar: " + e.getMessage());
        }
    }

    /**
     * Guarda o estado automaticamente (em caso de falha de ligação).
     */
    private void guardarAutomaticamente() {
        if (estado != null) {
            String caminho = PASTA_SAVES + "jogo_" + idJogo + "_auto.dat";
            try {
                estado.guardarEmFicheiro(caminho);
                System.out.println("[JOGO " + idJogo + "] Guardado automaticamente em " + caminho);
            } catch (IOException ex) {
                System.err.println("[JOGO " + idJogo + "] Erro ao guardar auto: " + ex.getMessage());
            }
        }
    }

    /**
     * Carrega um jogo guardado.
     * Dados esperados: caminho do ficheiro de save.
     */
    private void carregarJogo(int jogador, String caminho) {
        try {
            estado = EstadoJogo.carregarDeFicheiro(caminho);
            enviarMensagem(1, TipoMensagem.JOGO_CARREGADO, "Jogo carregado: " + caminho);
            enviarMensagem(2, TipoMensagem.JOGO_CARREGADO, "Jogo carregado: " + caminho);
            enviarTabuleiros(1);
            enviarTabuleiros(2);
            // Reinicia o turno
            iniciarFaseJogo();
        } catch (IOException | ClassNotFoundException e) {
            enviarMensagem(jogador, TipoMensagem.ERRO, "Erro ao carregar: " + e.getMessage());
        }
    }

    // ─── Envio de mensagens ──────────────────────────────────────────────────

    /**
     * Envia os dois tabuleiros a um jogador (o seu e o do adversário).
     *
     * @param jogador número do jogador destinatário
     */
    private void enviarTabuleiros(int jogador) {
        String proprio = estado.getTabuleiro(jogador).toStringProprioTabuleiro();
        String adversario = estado.getTabuleiro(jogador).toStringTabuleirAdversario();
        // Envia concatenado, separado por "||"
        enviarMensagem(jogador, TipoMensagem.ESTADO_TABULEIRO,
                proprio.replace("\n", "\\n") + "||" + adversario.replace("\n", "\\n"));
    }

    /**
     * Envia uma mensagem a um jogador.
     *
     * @param jogador número do jogador (1 ou 2)
     * @param tipo    tipo da mensagem
     * @param dados   dados da mensagem
     */
    private void enviarMensagem(int jogador, TipoMensagem tipo, String dados) {
        PrintWriter out = (jogador == 1) ? outJ1 : outJ2;
        if (out != null) {
            Mensagem msg = new Mensagem(tipo, dados);
            out.println(msg.serializar());
            System.out.println("[JOGO " + idJogo + "] → J" + jogador + ": " + msg.serializar());
        }
    }

    // ─── Desconexão ─────────────────────────────────────────────────────────

    /**
     * Trata a desconexão de um jogador.
     * Guarda o estado e informa o adversário.
     */
    private void tratarDesconexao(int jogador) {
        System.out.println("[JOGO " + idJogo + "] Jogador " + jogador + " desligou-se.");
        guardarAutomaticamente();
        int adversario = (jogador == 1) ? 2 : 1;
        enviarMensagem(adversario, TipoMensagem.ADVERSARIO_DESLIGADO,
                "O adversário desligou-se. Jogo guardado automaticamente.");
    }

    /** Fecha as ligações de ambos os sockets */
    private void fecharLigacoes() {
        try { if (socketJ1 != null) socketJ1.close(); } catch (IOException ignored) {}
        try { if (socketJ2 != null) socketJ2.close(); } catch (IOException ignored) {}
    }

    // ─── Thread de leitura do cliente ───────────────────────────────────────

    /**
     * Classe interna que corre numa Thread dedicada para ler mensagens de um cliente.
     *
     * Porque precisamos de threads aqui:
     *  - O servidor gere DOIS sockets em simultâneo (J1 e J2)
     *  - Se usássemos um loop sequencial, bloqueávamos à espera de um jogador
     *    enquanto o outro ficava sem resposta
     *  - Com uma Thread por cliente, ambos podem enviar/receber em paralelo
     */
    private class LeitorCliente implements Runnable {

        private final int jogador;
        private final Socket socket;

        LeitorCliente(int jogador, Socket socket) {
            this.jogador = jogador;
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {

                String linha;
                // Lê mensagens até o cliente se desligar
                while ((linha = in.readLine()) != null) {
                    Mensagem msg = Mensagem.deserializar(linha);
                    if (msg != null) {
                        processarMensagem(jogador, msg);
                    }
                    // Se o jogo terminou, não precisa de continuar a ler
                    if (estado != null && estado.jogoTerminou()) break;
                }
            } catch (IOException e) {
                // Cliente desligou-se abruptamente
                if (estado != null && !estado.jogoTerminou()) {
                    tratarDesconexao(jogador);
                }
            }
            System.out.println("[JOGO " + idJogo + "] Leitura de J" + jogador + " terminada.");
        }
    }
}
