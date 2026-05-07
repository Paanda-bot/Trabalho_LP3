package batalhanaval.cliente;

import batalhanaval.jogo.TipoNavio;
import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;
import batalhanaval.util.UtilNavio;

import java.util.Scanner;

/**
 * Interface de linha de comandos (CLI) para o cliente da Batalha Naval.
 *
 * Esta classe:
 *  1. Implementa LigacaoServidor.MensagemCallback para receber mensagens do servidor
 *  2. Gere o estado local da interface (que fase está o cliente)
 *  3. Apresenta os menus e lê input do utilizador
 *  4. Envia mensagens ao servidor via LigacaoServidor
 *
 * O fluxo é:
 *   LIGAR → POSICIONAMENTO → AGUARDAR → JOGO → FIM
 */
public class InterfaceCLI implements LigacaoServidor.MensagemCallback {

    /** Ligação ao servidor */
    private final LigacaoServidor ligacao;

    /** Scanner para ler input do utilizador */
    private final Scanner scanner = new Scanner(System.in);

    /** Número deste jogador (1 ou 2), atribuído pelo servidor */
    private int numeroJogador = 0;

    /** ID do jogo actual */
    private String idJogo = "";

    /** Flag: é o meu turno? */
    private volatile boolean meuTurno = false;

    /** Fase actual da interface */
    private volatile Fase faseActual = Fase.POSICIONAMENTO;

    /** Último estado dos tabuleiros recebido */
    private volatile String ultimoEstadoTabuleiros = "";

    private enum Fase { POSICIONAMENTO, AGUARDAR, JOGO, FIM }

    /**
     * Tipos de navios ainda por colocar (controla o fluxo de posicionamento)
     * Índice: mesmo que TipoNavio.values()
     */
    private int[] naviosRestantes;

    public InterfaceCLI(LigacaoServidor ligacao) {
        this.ligacao = ligacao;
        // Inicializa os navios restantes conforme TipoNavio
        TipoNavio[] tipos = TipoNavio.values();
        naviosRestantes = new int[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            naviosRestantes[i] = tipos[i].getQuantidade();
        }
    }

    // ─── Callback do servidor ────────────────────────────────────────────────

    /**
     * Chamado pela thread de leitura quando chega uma mensagem do servidor.
     * Nota: este método corre na thread do LeitorServidor, não na thread principal!
     * Por isso usamos synchronized / volatile para partilha de dados.
     */
    @Override
    public synchronized void onMensagemRecebida(Mensagem msg) {
        switch (msg.getTipo()) {

            case LIGADO:
                // Formato: "1:IDXXX" ou "2:IDXXX"
                String[] partes = msg.getDados().split(":");
                numeroJogador = Integer.parseInt(partes[0]);
                idJogo = partes.length > 1 ? partes[1] : "";
                System.out.println("\n✔ Ligado como Jogador " + numeroJogador + " | Jogo: " + idJogo);
                break;

            case INICIAR_POSICIONAMENTO:
                faseActual = Fase.POSICIONAMENTO;
                System.out.println("\n══ FASE DE POSICIONAMENTO ══");
                System.out.println(msg.getDados());
                mostrarNaviosRestantes();
                notifyAll(); // Acorda a thread principal se estiver à espera
                break;

            case NAVIO_POSICIONADO:
                System.out.println("✔ " + msg.getDados());
                mostrarNaviosRestantes();
                notifyAll();
                break;

            case NAVIO_INVALIDO:
                System.out.println("✖ Posição inválida: " + msg.getDados());
                notifyAll();
                break;

            case AGUARDAR:
                faseActual = Fase.AGUARDAR;
                System.out.println("⏳ " + msg.getDados());
                notifyAll();
                break;

            case ESTADO_TABULEIRO:
                // Guarda o estado e imprime (substitui \n de volta)
                ultimoEstadoTabuleiros = msg.getDados().replace("\\n", "\n");
                String[] tabs = ultimoEstadoTabuleiros.split("\\|\\|");
                if (tabs.length >= 2) {
                    System.out.println(tabs[0]);
                    System.out.println(tabs[1]);
                }
                break;

            case TEU_TURNO:
                faseActual = Fase.JOGO;
                meuTurno = true;
                System.out.println("\n🎯 " + msg.getDados());
                notifyAll();
                break;

            case TURNO_ADVERSARIO:
                faseActual = Fase.JOGO;
                meuTurno = false;
                System.out.println("\n⏳ " + msg.getDados());
                break;

            case RESULTADO_TIRO:
                System.out.println("📣 Resultado do teu tiro: " + formatarResultado(msg.getDados()));
                break;

            case TIRO_ADVERSARIO:
                System.out.println("💥 Tiro do adversário: " + formatarResultado(msg.getDados()));
                break;

            case TIROS_RESTANTES:
                System.out.println("🔫 " + msg.getDados());
                notifyAll();
                break;

            case FIM_JOGO:
                faseActual = Fase.FIM;
                System.out.println("\n╔══════════════════════════════╗");
                System.out.println("║  FIM DO JOGO: " + msg.getDados() + "          ║");
                System.out.println("╚══════════════════════════════╝");
                notifyAll();
                break;

            case JOGO_GUARDADO:
                System.out.println("💾 Jogo guardado: " + msg.getDados());
                break;

            case JOGO_CARREGADO:
                System.out.println("📂 Jogo carregado: " + msg.getDados());
                faseActual = Fase.JOGO;
                notifyAll();
                break;

            case ADVERSARIO_DESLIGADO:
                System.out.println("⚠ " + msg.getDados());
                faseActual = Fase.FIM;
                notifyAll();
                break;

            case ERRO:
                System.out.println("⚠ Erro: " + msg.getDados());
                notifyAll();
                break;

            default:
                System.out.println("[MSG] " + msg.serializar());
        }
    }

    @Override
    public void onDesconexao() {
        System.out.println("\n[CLIENTE] Ligação ao servidor perdida.");
        faseActual = Fase.FIM;
    }

    // ─── Loop principal do cliente ───────────────────────────────────────────

    /**
     * Loop principal de interacção com o utilizador.
     * Corre na thread principal.
     */
    public void iniciar() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      BATALHA NAVAL - CLIENTE         ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Aguarda confirmação de ligação
        aguardarFase(Fase.POSICIONAMENTO, 30000);

        // Fase de posicionamento
        if (faseActual == Fase.POSICIONAMENTO) {
            fasePosicionamento();
        }

        // Loop do jogo
        while (faseActual != Fase.FIM) {
            if (faseActual == Fase.JOGO && meuTurno) {
                faseTiro();
            } else {
                // Aguarda o turno ou mensagem do servidor (até 2 minutos)
                pausar(500);
            }
        }

        System.out.println("\nObrigado por jogar! A fechar...");
        ligacao.desligar();
    }

    // ─── Fase de Posicionamento ──────────────────────────────────────────────

    /**
     * Gere a fase de posicionamento dos navios.
     * Pede ao utilizador para colocar cada tipo de navio.
     */
    private void fasePosicionamento() {
        TipoNavio[] tipos = TipoNavio.values();

        for (int i = 0; i < tipos.length; i++) {
            TipoNavio tipo = tipos[i];
            int quantidade = tipo.getQuantidade();

            for (int j = 0; j < quantidade; j++) {
                boolean colocado = false;
                while (!colocado) {
                    System.out.println("\n─── Posicionar " + tipo.getNome() +
                            " (tamanho " + tipo.getTamanho() + ") [" + (j+1) + "/" + quantidade + "] ───");
                    System.out.print("Célula inicial (ex: A1): ");
                    String celula = scanner.nextLine().trim().toUpperCase();

                    System.out.print("Orientação H(horizontal) ou V(vertical): ");
                    String orientStr = scanner.nextLine().trim().toUpperCase();

                    if (orientStr.isEmpty() || (!orientStr.startsWith("H") && !orientStr.startsWith("V"))) {
                        System.out.println("Orientação inválida. Use H ou V.");
                        continue;
                    }

                    char orientacao = orientStr.charAt(0);

                    // Pré-valida localmente para dar feedback imediato
                    if (UtilNavio.calcularCelulas(celula, orientacao, tipo.getTamanho()) == null) {
                        System.out.println("Posição fora do tabuleiro. Tente novamente.");
                        continue;
                    }

                    // Envia ao servidor para validação definitiva
                    String dados = UtilNavio.serializarPosicionamento(tipo, celula, orientacao);
                    ligacao.enviarMensagem(TipoMensagem.POSICIONAR_NAVIO, dados);

                    // Aguarda resposta do servidor (NAVIO_POSICIONADO ou NAVIO_INVALIDO)
                    // (simplificado: pequena pausa)
                    pausar(500);
                    colocado = true; // Se NAVIO_INVALIDO, o callback já avisou e repetiremos
                }
            }
        }

        // Confirma que terminou o posicionamento
        System.out.println("\n✔ Todos os navios colocados! A confirmar...");
        ligacao.enviarMensagem(TipoMensagem.PRONTO);
        faseActual = Fase.AGUARDAR;

        // Aguarda o adversário
        System.out.println("⏳ À espera que o adversário posicione os navios...");
        aguardarFase(Fase.JOGO, 120000);
    }

    // ─── Fase de Tiro ────────────────────────────────────────────────────────

    /**
     * Gere os tiros do jogador quando é o seu turno.
     */
    private void faseTiro() {
        System.out.println("\n─── O TEU TURNO ───");
        System.out.println("Comandos: <célula> para atirar (ex: B3) | GUARDAR | SAIR");
        System.out.print("> ");

        String input = scanner.nextLine().trim().toUpperCase();

        if (input.equals("GUARDAR")) {
            ligacao.enviarMensagem(TipoMensagem.GUARDAR);
            return;
        }

        if (input.equals("SAIR")) {
            ligacao.enviarMensagem(TipoMensagem.SAIR);
            faseActual = Fase.FIM;
            return;
        }

        // Valida coordenada localmente
        if (!batalhanaval.jogo.Tabuleiro.celulaValida(input)) {
            System.out.println("Coordenada inválida. Use formato A1–J10.");
            return;
        }

        // Envia tiro ao servidor
        ligacao.enviarMensagem(TipoMensagem.TIRO, input);
        meuTurno = false; // Aguarda resposta (será re-activado por TEU_TURNO se ainda tiver tiros)
    }

    // ─── Utilitários ────────────────────────────────────────────────────────

    /** Formata o resultado de um tiro para apresentação */
    private String formatarResultado(String dados) {
        if (dados.startsWith("AGUA")) return "💧 Água em " + dados.split(":")[1];
        if (dados.startsWith("ACERTOU")) return "🔥 Acertou em " + dados.split(":")[1] + " (" + dados.split(":")[2] + ")";
        if (dados.startsWith("AFUNDOU")) return "💣 Afundou " + dados.split(":")[1] + " (" + dados.split(":")[2] + ")";
        return dados;
    }

    /** Mostra quantos navios de cada tipo ainda faltam colocar */
    private void mostrarNaviosRestantes() {
        System.out.println("\nNavios para colocar:");
        for (TipoNavio tipo : TipoNavio.values()) {
            System.out.println("  " + tipo.getNome() + " (tam " + tipo.getTamanho() + "): " + tipo.getQuantidade() + "x");
        }
    }

    /** Aguarda até a fase ser a esperada, com timeout */
    private synchronized void aguardarFase(Fase fase, long timeoutMs) {
        long inicio = System.currentTimeMillis();
        while (faseActual != fase && faseActual != Fase.FIM) {
            long restante = timeoutMs - (System.currentTimeMillis() - inicio);
            if (restante <= 0) break;
            try { wait(restante); } catch (InterruptedException e) { break; }
        }
    }

    /** Pausa a thread corrente por ms milissegundos */
    private void pausar(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
