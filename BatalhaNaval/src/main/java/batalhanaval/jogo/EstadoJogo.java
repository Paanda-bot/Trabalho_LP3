package batalhanaval.jogo;

import java.io.*;
import java.util.Random;

/**
 * ══════════════════════════════════════════════════════
 *  Estado completo de uma partida — Serializable para save/load
 * ══════════════════════════════════════════════════════
 *
 * UML:
 *   EstadoJogo
 *   ─────────────────────────────────────────────────
 *   - idJogo: String
 *   - tabuleiroJ1: Tabuleiro
 *   - tabuleiroJ2: Tabuleiro
 *   - fase: Fase
 *   - jogadorActual: int
 *   - tirosRestantesNoTurno: int
 *   - resultado: int
 *   - j1Pronto, j2Pronto: boolean
 *   ─────────────────────────────────────────────────
 *   + colocarNavio(int, Navio): boolean
 *   + marcarPronto(int): void
 *   + processarTiro(int, String): String
 *   + guardarEmFicheiro(String): void
 *   + {static} carregarDeFicheiro(String): EstadoJogo
 */
public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 2L;

    public static final int TIROS_POR_TURNO = 3;

    public enum Fase { POSICIONAMENTO, JOGO, FIM }

    private final String    idJogo;
    private       Tabuleiro tabuleiroJ1;
    private       Tabuleiro tabuleiroJ2;
    private       Fase      fase;
    private       int       jogadorActual;
    private       int       tirosRestantesNoTurno;
    private       int       resultado;   // 0=em jogo, 1=J1 ganhou, 2=J2 ganhou, 3=empate
    private       boolean   j1Pronto;
    private       boolean   j2Pronto;

    public EstadoJogo(String idJogo) {
        this.idJogo                = idJogo;
        this.tabuleiroJ1           = new Tabuleiro();
        this.tabuleiroJ2           = new Tabuleiro();
        this.fase                  = Fase.POSICIONAMENTO;
        this.resultado             = 0;
        this.j1Pronto              = false;
        this.j2Pronto              = false;
        this.jogadorActual         = new Random().nextBoolean() ? 1 : 2;
        this.tirosRestantesNoTurno = TIROS_POR_TURNO;
    }

    // ── Posicionamento ──────────────────────────────────────────────────────

    public boolean colocarNavio(int jogador, Navio navio) {
        if (fase != Fase.POSICIONAMENTO) return false;
        Tabuleiro t = getTabuleiro(jogador);
        return t != null && t.colocarNavio(navio);
    }

    public void marcarPronto(int jogador) {
        if (jogador == 1) j1Pronto = true;
        else if (jogador == 2) j2Pronto = true;
        if (j1Pronto && j2Pronto) fase = Fase.JOGO;
    }

    // ── Tiros ───────────────────────────────────────────────────────────────

    public String processarTiro(int jogador, String coordenada) {
        if (fase != Fase.JOGO)       return "JOGO_NAO_INICIADO";
        if (jogador != jogadorActual) return "NAO_TEU_TURNO";

        int adversario = (jogador == 1) ? 2 : 1;
        String resultado = getTabuleiro(adversario).receberTiro(coordenada);
        if ("JA_ATIRADO".equals(resultado)) return "JA_ATIRADO";

        String resSimp = resultado.startsWith("AFUNDOU") ? "AFUNDOU" :
                         resultado.startsWith("ACERTOU") ? "ACERTOU" : "AGUA";
        getTabuleiro(jogador).registarTiroEfetuado(coordenada, resSimp);

        tirosRestantesNoTurno--;

        if (getTabuleiro(adversario).todosNaviosAfundados()) {
            fase = Fase.FIM;
            this.resultado = jogador;
        } else if (tirosRestantesNoTurno <= 0) {
            jogadorActual = adversario;
            tirosRestantesNoTurno = TIROS_POR_TURNO;
        }
        return resultado;
    }

    // ── Save / Load ─────────────────────────────────────────────────────────

    public void guardarEmFicheiro(String caminho) throws IOException {
        new File(caminho).getParentFile().mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(caminho))) {
            oos.writeObject(this);
        }
    }

    public static EstadoJogo carregarDeFicheiro(String caminho) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(caminho))) {
            return (EstadoJogo) ois.readObject();
        }
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public Tabuleiro getTabuleiro(int jogador) { return jogador == 1 ? tabuleiroJ1 : tabuleiroJ2; }
    public Fase      getFase()                  { return fase; }
    public int       getJogadorActual()         { return jogadorActual; }
    public int       getTirosRestantesNoTurno() { return tirosRestantesNoTurno; }
    public int       getResultado()             { return resultado; }
    public String    getIdJogo()                { return idJogo; }
    public boolean   isJ1Pronto()               { return j1Pronto; }
    public boolean   isJ2Pronto()               { return j2Pronto; }
    public boolean   jogoTerminou()             { return fase == Fase.FIM; }
    public void      setFase(Fase fase)         { this.fase = fase; }
}
