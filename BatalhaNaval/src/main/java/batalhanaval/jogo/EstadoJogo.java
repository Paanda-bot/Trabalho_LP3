package batalhanaval.jogo;

import java.io.*;
import java.util.Random;

/**
 * Representa o estado completo de uma partida de Batalha Naval.
 *
 * Esta classe é Serializable para permitir guardar e carregar o jogo em ficheiro
 * (usando ObjectOutputStream / ObjectInputStream do Java).
 *
 * Estado gerido:
 *  - Tabuleiros dos dois jogadores
 *  - Qual jogador é o actual (1 ou 2)
 *  - Quantos tiros o jogador actual ainda tem neste turno (3 por turno)
 *  - Se o jogo terminou e quem ganhou
 *  - Nome do ficheiro de save associado
 */
public class EstadoJogo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Número de tiros por turno conforme enunciado */
    public static final int TIROS_POR_TURNO = 3;

    /** Tabuleiro do Jogador 1 */
    private Tabuleiro tabuleiroJ1;

    /** Tabuleiro do Jogador 2 */
    private Tabuleiro tabuleiroJ2;

    /**
     * Fase do jogo.
     * POSICIONAMENTO → JOGO → FIM
     */
    public enum Fase { POSICIONAMENTO, JOGO, FIM }

    private Fase fase;

    /** Jogador que está a jogar actualmente (1 ou 2) */
    private int jogadorActual;

    /** Quantos tiros ainda restam ao jogador actual neste turno */
    private int tirosRestantesNoTurno;

    /** Resultado final: 0=em jogo, 1=J1 ganhou, 2=J2 ganhou, 3=empate */
    private int resultado;

    /** ID/nome deste jogo (usado para o ficheiro de save) */
    private String idJogo;

    /** Se o Jogador 1 já terminou de posicionar */
    private boolean j1Pronto;
    /** Se o Jogador 2 já terminou de posicionar */
    private boolean j2Pronto;

    /**
     * Cria um novo estado de jogo.
     *
     * @param idJogo identificador único deste jogo
     */
    public EstadoJogo(String idJogo) {
        this.idJogo = idJogo;
        this.tabuleiroJ1 = new Tabuleiro();
        this.tabuleiroJ2 = new Tabuleiro();
        this.fase = Fase.POSICIONAMENTO;
        this.resultado = 0;
        this.j1Pronto = false;
        this.j2Pronto = false;

        // Sorteia aleatoriamente quem começa
        this.jogadorActual = new Random().nextBoolean() ? 1 : 2;
        this.tirosRestantesNoTurno = TIROS_POR_TURNO;
    }

    // ─── Posicionamento ─────────────────────────────────────────────────────

    /**
     * Tenta colocar um navio no tabuleiro do jogador indicado.
     *
     * @param jogador número do jogador (1 ou 2)
     * @param navio   navio a colocar
     * @return true se colocado com sucesso
     */
    public boolean colocarNavio(int jogador, Navio navio) {
        if (fase != Fase.POSICIONAMENTO) return false;
        Tabuleiro t = getTabuleiro(jogador);
        return t != null && t.colocarNavio(navio);
    }

    /**
     * Marca o jogador como pronto (terminou de posicionar).
     * Se ambos estiverem prontos, inicia a fase de jogo.
     *
     * @param jogador número do jogador
     */
    public void marcarPronto(int jogador) {
        if (jogador == 1) j1Pronto = true;
        else if (jogador == 2) j2Pronto = true;

        // Quando ambos estão prontos, começa o jogo
        if (j1Pronto && j2Pronto) {
            fase = Fase.JOGO;
        }
    }

    // ─── Lógica de jogo ─────────────────────────────────────────────────────

    /**
     * Processa um tiro do jogador actual.
     *
     * @param jogador  jogador que dispara (deve ser o jogadorActual)
     * @param coordenada célula alvo (ex: "B4")
     * @return resultado do tiro ("AGUA", "ACERTOU:NomeNavio", "AFUNDOU:NomeNavio", "NAO_TEU_TURNO", "JA_ATIRADO")
     */
    public String processarTiro(int jogador, String coordenada) {
        if (fase != Fase.JOGO) return "JOGO_NAO_INICIADO";
        if (jogador != jogadorActual) return "NAO_TEU_TURNO";

        // O adversário recebe o tiro
        int adversario = (jogador == 1) ? 2 : 1;
        Tabuleiro tabAdversario = getTabuleiro(adversario);

        String resultado = tabAdversario.receberTiro(coordenada);

        if ("JA_ATIRADO".equals(resultado)) {
            return "JA_ATIRADO";
        }

        // Regista no tabuleiro próprio o tiro efetuado
        // (o resultado parcial antes de "AFUNDOU:" ser processado)
        String resSimplificado = resultado.startsWith("AFUNDOU") ? "AFUNDOU" :
                                  resultado.startsWith("ACERTOU") ? "ACERTOU" : "AGUA";
        getTabuleiro(jogador).registarTiroEfetuado(coordenada, resSimplificado);

        // Desconta tiro
        tirosRestantesNoTurno--;

        // Verifica fim do jogo
        if (tabAdversario.todosNaviosAfundados()) {
            fase = Fase.FIM;
            this.resultado = jogador; // este jogador ganhou
        } else if (tirosRestantesNoTurno <= 0) {
            // Turno acabou, passa a vez
            passarTurno();
        }

        return resultado;
    }

    /** Passa o turno para o outro jogador e repõe os 3 tiros */
    private void passarTurno() {
        jogadorActual = (jogadorActual == 1) ? 2 : 1;
        tirosRestantesNoTurno = TIROS_POR_TURNO;
    }

    // ─── Guardar / Carregar ─────────────────────────────────────────────────

    /**
     * Guarda o estado do jogo num ficheiro usando serialização Java.
     *
     * @param caminho caminho do ficheiro (ex: "jogo_ABC.dat")
     * @throws IOException se houver erro de I/O
     */
    public void guardarEmFicheiro(String caminho) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(caminho))) {
            oos.writeObject(this);
        }
    }

    /**
     * Carrega um estado de jogo guardado anteriormente.
     *
     * @param caminho caminho do ficheiro
     * @return EstadoJogo carregado
     * @throws IOException se o ficheiro não existir ou houver erro
     * @throws ClassNotFoundException se a classe não for reconhecida
     */
    public static EstadoJogo carregarDeFicheiro(String caminho) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(caminho))) {
            return (EstadoJogo) ois.readObject();
        }
    }

    // ─── Getters e utilitários ───────────────────────────────────────────────

    public Tabuleiro getTabuleiro(int jogador) {
        return jogador == 1 ? tabuleiroJ1 : tabuleiroJ2;
    }

    public Fase getFase() { return fase; }
    public int getJogadorActual() { return jogadorActual; }
    public int getTirosRestantesNoTurno() { return tirosRestantesNoTurno; }
    public int getResultado() { return resultado; }
    public String getIdJogo() { return idJogo; }
    public boolean isJ1Pronto() { return j1Pronto; }
    public boolean isJ2Pronto() { return j2Pronto; }

    public boolean jogoTerminou() { return fase == Fase.FIM; }

    public void setFase(Fase fase) { this.fase = fase; }
}
