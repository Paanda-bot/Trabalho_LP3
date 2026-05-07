package batalhanaval.jogo;

import java.io.Serializable;
import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  Tabuleiro 10×10 de Batalha Naval                        ║
 * ║  Linhas: A–J  |  Colunas: 1–10                          ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * UML:
 *   Tabuleiro
 *   ─────────────────────────────────────
 *   - navios: List<Navio>
 *   - tirosRecebidos: Map<String,Boolean>   (true=acerto, false=agua)
 *   - tirosEfetuados: Map<String,String>    (resultado)
 *   ─────────────────────────────────────
 *   + colocarNavio(Navio): boolean
 *   + receberTiro(String): String
 *   + registarTiroEfetuado(String,String): void
 *   + estaCompleto(): boolean
 *   + todosNaviosAfundados(): boolean
 *   + renderProprioTabuleiro(): String        ← com cores ANSI
 *   + renderTabuleirAdversario(): String      ← com cores ANSI
 *   + {static} celulaValida(String): boolean
 */
public class Tabuleiro implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final char[] LINHAS  = {'A','B','C','D','E','F','G','H','I','J'};
    public static final int    TAMANHO = 10;

    // ── Cores ANSI ───────────────────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String BLUE   = "\u001B[34m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String WHITE  = "\u001B[37m";
    private static final String DIM    = "\u001B[2m";
    private static final String BG_BLUE  = "\u001B[44m";
    private static final String BG_RED   = "\u001B[41m";
    private static final String BG_GREEN = "\u001B[42m";
    private static final String BG_GRAY  = "\u001B[100m";

    private final List<Navio>          navios          = new ArrayList<>();
    private final Map<String, Boolean> tirosRecebidos  = new LinkedHashMap<>();
    private final Map<String, String>  tirosEfetuados  = new LinkedHashMap<>();

    // ── Posicionamento ───────────────────────────────────────────────────────

    public boolean colocarNavio(Navio navio) {
        long count = navios.stream().filter(n -> n.getTipo() == navio.getTipo()).count();
        if (count >= navio.getTipo().getQuantidade()) return false;
        for (String c : navio.getCelulas()) {
            if (!celulaValida(c)) return false;
            if (celulasOcupadas().contains(c)) return false;
        }
        navios.add(navio);
        return true;
    }

    private Set<String> celulasOcupadas() {
        Set<String> s = new HashSet<>();
        for (Navio n : navios) s.addAll(n.getCelulas());
        return s;
    }

    public static boolean celulaValida(String celula) {
        if (celula == null || celula.length() < 2 || celula.length() > 3) return false;
        char l = Character.toUpperCase(celula.charAt(0));
        if (l < 'A' || l > 'J') return false;
        try { int c = Integer.parseInt(celula.substring(1)); return c >= 1 && c <= 10; }
        catch (NumberFormatException e) { return false; }
    }

    public boolean estaCompleto() {
        for (TipoNavio t : TipoNavio.values()) {
            long c = navios.stream().filter(n -> n.getTipo() == t).count();
            if (c < t.getQuantidade()) return false;
        }
        return true;
    }

    // ── Tiros ────────────────────────────────────────────────────────────────

    public String receberTiro(String celula) {
        String c = celula.toUpperCase();
        if (tirosRecebidos.containsKey(c)) return "JA_ATIRADO";
        for (Navio navio : navios) {
            if (navio.ocupaCelula(c)) {
                boolean afundou = navio.receberTiro(c);
                tirosRecebidos.put(c, true);
                return (afundou ? "AFUNDOU" : "ACERTOU") + ":" + navio.getTipo().getNome();
            }
        }
        tirosRecebidos.put(c, false);
        return "AGUA";
    }

    public void registarTiroEfetuado(String celula, String resultado) {
        tirosEfetuados.put(celula.toUpperCase(), resultado);
    }

    public boolean todosNaviosAfundados() {
        return !navios.isEmpty() && navios.stream().allMatch(Navio::estaAfundado);
    }

    // ── Renderização com ANSI ────────────────────────────────────────────────

    /**
     * Renderiza o tabuleiro PRÓPRIO (navios visíveis, tiros recebidos).
     *
     * Símbolos:
     *   ~  = água livre          (azul)
     *   ■  = navio intacto       (verde)
     *   ✕  = navio atingido      (vermelho)
     *   ●  = água atingida       (amarelo)
     */
    public String renderProprioTabuleiro() {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(CYAN)
          .append("  ╔══════════════════════════════════════════╗\n")
          .append("  ║        O MEU TABULEIRO                   ║\n")
          .append("  ╚══════════════════════════════════════════╝\n")
          .append(RESET);
        sb.append(cabecalhoColunas());

        for (int i = 0; i < TAMANHO; i++) {
            sb.append(BOLD).append(WHITE)
              .append(String.format("  %c ", LINHAS[i])).append(RESET);
            sb.append(DIM).append("│").append(RESET);
            for (int j = 0; j < TAMANHO; j++) {
                String cel = "" + LINHAS[i] + (j + 1);
                sb.append(" ").append(simboloProprioTabuleiro(cel)).append(" ");
            }
            sb.append(DIM).append("│").append(RESET);
            sb.append("\n");
        }
        sb.append(linhaRodape());
        sb.append(legendaProprioTabuleiro());
        return sb.toString();
    }

    /**
     * Renderiza o tabuleiro DO ADVERSÁRIO (só tiros efetuados visíveis).
     */
    public String renderTabuleiradversario() {
        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(YELLOW)
          .append("  ╔══════════════════════════════════════════╗\n")
          .append("  ║        TABULEIRO DO ADVERSÁRIO           ║\n")
          .append("  ╚══════════════════════════════════════════╝\n")
          .append(RESET);
        sb.append(cabecalhoColunas());

        for (int i = 0; i < TAMANHO; i++) {
            sb.append(BOLD).append(WHITE)
              .append(String.format("  %c ", LINHAS[i])).append(RESET);
            sb.append(DIM).append("│").append(RESET);
            for (int j = 0; j < TAMANHO; j++) {
                String cel = "" + LINHAS[i] + (j + 1);
                sb.append(" ").append(simboloTabuleiradversario(cel)).append(" ");
            }
            sb.append(DIM).append("│").append(RESET);
            sb.append("\n");
        }
        sb.append(linhaRodape());
        sb.append(legendaTabuleiradversario());
        return sb.toString();
    }

    /** Renderiza os dois tabuleiros lado a lado */
    public String renderDuplo() {
        String[] prop = renderProprioTabuleiro().split("\n");
        String[] adv  = renderTabuleiradversario().split("\n");
        int maxL = Math.max(prop.length, adv.length);
        // Largura fixa de cada tabuleiro (sem cores)
        int largura = 46;
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < maxL; i++) {
            String lp = (i < prop.length) ? prop[i] : "";
            String la = (i < adv.length)  ? adv[i]  : "";
            sb.append(padAnsi(lp, largura)).append("   ").append(la).append("\n");
        }
        return sb.toString();
    }

    // ── Helpers de renderização ──────────────────────────────────────────────

    private String simboloProprioTabuleiro(String cel) {
        // Navio atingido?
        for (Navio n : navios) {
            if (n.getCelulasAtingidas().contains(cel)) {
                return n.estaAfundado()
                    ? BG_RED   + BOLD + "✕" + RESET  // Afundado
                    : RED      + BOLD + "✕" + RESET;  // Atingido
            }
            if (n.ocupaCelula(cel)) {
                return GREEN + "■" + RESET;  // Navio intacto
            }
        }
        // Água atingida (miss)?
        if (tirosRecebidos.containsKey(cel) && !tirosRecebidos.get(cel)) {
            return YELLOW + "●" + RESET;
        }
        return BLUE + "~" + RESET;  // Água livre
    }

    private String simboloTabuleiradversario(String cel) {
        String r = tirosEfetuados.get(cel);
        if (r == null) return DIM + BLUE + "~" + RESET;
        if (r.startsWith("AGUA"))    return YELLOW + "●" + RESET;
        if (r.startsWith("AFUNDOU")) return BG_RED + BOLD + "✕" + RESET;
        return RED + "✕" + RESET;  // ACERTOU
    }

    private String cabecalhoColunas() {
        StringBuilder sb = new StringBuilder();
        sb.append(DIM).append("     ");
        for (int i = 1; i <= 10; i++) sb.append(String.format("%-3d", i));
        sb.append(RESET).append("\n");
        sb.append(DIM).append("    ╔");
        for (int i = 0; i < TAMANHO; i++) sb.append("───");
        sb.append("╗").append(RESET).append("\n");
        return sb.toString();
    }

    private String linhaRodape() {
        StringBuilder sb = new StringBuilder();
        sb.append(DIM).append("    ╚");
        for (int i = 0; i < TAMANHO; i++) sb.append("───");
        sb.append("╝").append(RESET).append("\n");
        return sb.toString();
    }

    private String legendaProprioTabuleiro() {
        return DIM + "  Legenda: " + RESET
             + GREEN + "■" + RESET + " navio  "
             + RED   + "✕" + RESET + " atingido  "
             + YELLOW + "●" + RESET + " água  "
             + BLUE   + "~" + RESET + " livre\n";
    }

    private String legendaTabuleiradversario() {
        return DIM + "  Legenda: " + RESET
             + RED    + "✕" + RESET + " acerto  "
             + YELLOW + "●" + RESET + " água  "
             + DIM + BLUE + "~" + RESET + " não atirado\n";
    }

    /**
     * Pad de uma string com sequências ANSI (ignora chars de escape no cálculo da largura).
     */
    private static String padAnsi(String s, int width) {
        int visible = s.replaceAll("\u001B\\[[;\\d]*m", "").length();
        int pad = Math.max(0, width - visible);
        return s + " ".repeat(pad);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public List<Navio>          getNavios()          { return Collections.unmodifiableList(navios); }
    public Map<String, Boolean> getTirosRecebidos()  { return Collections.unmodifiableMap(tirosRecebidos); }
    public Map<String, String>  getTirosEfetuados()  { return Collections.unmodifiableMap(tirosEfetuados); }

    // ── Serialização para protocolo (texto sem ANSI) ──────────────────────────

    public String toStringProprioTabuleiro() {
        return buildGridTexto(true);
    }
    public String toStringTabuleirAdversario() {
        return buildGridTexto(false);
    }

    private String buildGridTexto(boolean proprio) {
        StringBuilder sb = new StringBuilder();
        String titulo = proprio ? "=== O MEU TABULEIRO ===" : "=== ADVERSÁRIO ===";
        sb.append(titulo).append("\n");
        sb.append("    1  2  3  4  5  6  7  8  9  10\n");
        for (int i = 0; i < TAMANHO; i++) {
            sb.append(String.format("%c   ", LINHAS[i]));
            for (int j = 0; j < TAMANHO; j++) {
                String cel = "" + LINHAS[i] + (j + 1);
                if (proprio) sb.append(simboloTextoProprioTabueiro(cel)).append("  ");
                else         sb.append(simboloTextoAdversario(cel)).append("  ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private char simboloTextoProprioTabueiro(String cel) {
        for (Navio n : navios) {
            if (n.getCelulasAtingidas().contains(cel)) return 'X';
            if (n.ocupaCelula(cel)) return 'N';
        }
        if (tirosRecebidos.containsKey(cel) && !tirosRecebidos.get(cel)) return 'o';
        return '~';
    }

    private char simboloTextoAdversario(String cel) {
        String r = tirosEfetuados.get(cel);
        if (r == null) return '~';
        if (r.startsWith("AGUA")) return 'o';
        return 'X';
    }
}
