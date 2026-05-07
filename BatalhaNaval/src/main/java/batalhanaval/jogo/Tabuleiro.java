package batalhanaval.jogo;

import java.io.Serializable;
import java.util.*;

/**
 * Representa o tabuleiro 10x10 de um jogador.
 *
 * Linhas:  A–J  (índice 0–9)
 * Colunas: 1–10 (índice 0–9)
 *
 * O tabuleiro guarda:
 *  - os navios posicionados
 *  - as células já atingidas por tiros do adversário
 *  - as células em que o próprio jogador atirou (para o tabuleiro do adversário)
 *
 * A serialização é necessária para guardar/carregar o estado em ficheiro.
 */
public class Tabuleiro implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Letras das linhas (A a J) */
    public static final char[] LINHAS = {'A','B','C','D','E','F','G','H','I','J'};

    /** Número de colunas e linhas */
    public static final int TAMANHO = 10;

    /** Lista de navios posicionados neste tabuleiro */
    private final List<Navio> navios = new ArrayList<>();

    /**
     * Células do tabuleiro atingidas por tiros do adversário.
     * Chave: coordenada (ex: "A3")
     * Valor: true = acertou em navio, false = água
     */
    private final Map<String, Boolean> tirosRecebidos = new HashMap<>();

    /**
     * Células em que este jogador atirou (no tabuleiro do adversário).
     * Chave: coordenada (ex: "B7")
     * Valor: resultado: "AGUA", "ACERTOU", "AFUNDOU"
     */
    private final Map<String, String> tirosEfetuados = new HashMap<>();

    // ─── Posicionamento de navios ───────────────────────────────────────────

    /**
     * Tenta colocar um navio no tabuleiro.
     *
     * Valida:
     *  1. As células estão dentro dos limites do tabuleiro
     *  2. Não sobrepõem navios já existentes
     *  3. O número de navios deste tipo ainda não atingiu o limite
     *
     * @param navio navio a colocar
     * @return true se o navio foi colocado com sucesso, false se posição inválida
     */
    public boolean colocarNavio(Navio navio) {
        // Verifica limite de navios deste tipo
        long countTipo = navios.stream()
                .filter(n -> n.getTipo() == navio.getTipo())
                .count();
        if (countTipo >= navio.getTipo().getQuantidade()) {
            return false; // Já tem o máximo deste tipo
        }

        // Verifica cada célula
        for (String celula : navio.getCelulas()) {
            if (!celulaValida(celula)) return false;
            if (celulasOcupadas().contains(celula)) return false;
        }

        navios.add(navio);
        return true;
    }

    /**
     * Verifica se uma coordenada é válida (dentro do tabuleiro 10x10).
     *
     * @param celula coordenada ex: "A1", "J10"
     * @return true se válida
     */
    public static boolean celulaValida(String celula) {
        if (celula == null || celula.length() < 2 || celula.length() > 3) return false;
        char linha = Character.toUpperCase(celula.charAt(0));
        if (linha < 'A' || linha > 'J') return false;
        try {
            int col = Integer.parseInt(celula.substring(1));
            return col >= 1 && col <= 10;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Devolve todas as células actualmente ocupadas por navios.
     */
    private Set<String> celulasOcupadas() {
        Set<String> ocupadas = new HashSet<>();
        for (Navio n : navios) {
            ocupadas.addAll(n.getCelulas());
        }
        return ocupadas;
    }

    /**
     * Verifica se todos os navios obrigatórios foram colocados.
     * (conforme as quantidades em TipoNavio)
     *
     * @return true se o tabuleiro está completo
     */
    public boolean estaCompleto() {
        for (TipoNavio tipo : TipoNavio.values()) {
            long count = navios.stream()
                    .filter(n -> n.getTipo() == tipo)
                    .count();
            if (count < tipo.getQuantidade()) return false;
        }
        return true;
    }

    // ─── Gestão de tiros recebidos ──────────────────────────────────────────

    /**
     * Processa um tiro recebido neste tabuleiro (atirado pelo adversário).
     *
     * @param celula coordenada do tiro
     * @return resultado: "AGUA", "ACERTOU:<NomeNavio>", "AFUNDOU:<NomeNavio>", ou "JA_ATIRADO"
     */
    public String receberTiro(String celula) {
        String c = celula.toUpperCase();

        // Verifica se já foi atirado aqui
        if (tirosRecebidos.containsKey(c)) {
            return "JA_ATIRADO";
        }

        // Verifica se acertou algum navio
        for (Navio navio : navios) {
            if (navio.ocupaCelula(c)) {
                boolean afundou = navio.receberTiro(c);
                tirosRecebidos.put(c, true); // acertou
                if (afundou) {
                    return "AFUNDOU:" + navio.getTipo().getNome();
                } else {
                    return "ACERTOU:" + navio.getTipo().getNome();
                }
            }
        }

        // Água
        tirosRecebidos.put(c, false);
        return "AGUA";
    }

    /**
     * Regista o resultado de um tiro que este jogador efetuou (no adversário).
     *
     * @param celula coordenada do tiro
     * @param resultado resultado devolvido pelo servidor
     */
    public void registarTiroEfetuado(String celula, String resultado) {
        tirosEfetuados.put(celula.toUpperCase(), resultado);
    }

    /**
     * Verifica se todos os navios do tabuleiro foram afundados.
     *
     * @return true se o jogador perdeu (todos os navios afundados)
     */
    public boolean todosNaviosAfundados() {
        if (navios.isEmpty()) return false;
        return navios.stream().allMatch(Navio::estaAfundado);
    }

    // ─── Getters ────────────────────────────────────────────────────────────

    public List<Navio> getNavios() { return Collections.unmodifiableList(navios); }
    public Map<String, Boolean> getTirosRecebidos() { return Collections.unmodifiableMap(tirosRecebidos); }
    public Map<String, String> getTirosEfetuados() { return Collections.unmodifiableMap(tirosEfetuados); }

    // ─── Representação em texto (para CLI) ──────────────────────────────────

    /**
     * Gera a string do tabuleiro próprio (mostra navios, acertos, água).
     * Símbolos:
     *   '~' = água não atingida
     *   'N' = navio intacto
     *   'X' = navio atingido
     *   'o' = água atingida (miss)
     *
     * @return string multi-linha do tabuleiro
     */
    public String toStringProprioTabuleiro() {
        char[][] grid = new char[TAMANHO][TAMANHO];

        // Preenche tudo com água
        for (char[] row : grid) Arrays.fill(row, '~');

        // Marca navios intactos
        for (Navio navio : navios) {
            for (String celula : navio.getCelulas()) {
                int[] idx = celulaParaIndices(celula);
                if (idx != null) grid[idx[0]][idx[1]] = 'N';
            }
            // Marca células atingidas
            for (String celula : navio.getCelulasAtingidas()) {
                int[] idx = celulaParaIndices(celula);
                if (idx != null) grid[idx[0]][idx[1]] = 'X';
            }
        }

        // Marca águas atingidas
        for (Map.Entry<String, Boolean> entry : tirosRecebidos.entrySet()) {
            if (!entry.getValue()) { // false = água
                int[] idx = celulaParaIndices(entry.getKey());
                if (idx != null) grid[idx[0]][idx[1]] = 'o';
            }
        }

        return gridParaString(grid, "O MEU TABULEIRO");
    }

    /**
     * Gera a string do tabuleiro do adversário (mostra apenas tiros efetuados).
     * Símbolos:
     *   '~' = não atirado ainda
     *   'X' = acertou ou afundou
     *   'o' = água
     */
    public String toStringTabuleirAdversario() {
        char[][] grid = new char[TAMANHO][TAMANHO];
        for (char[] row : grid) Arrays.fill(row, '~');

        for (Map.Entry<String, String> entry : tirosEfetuados.entrySet()) {
            int[] idx = celulaParaIndices(entry.getKey());
            if (idx != null) {
                String res = entry.getValue();
                if (res.startsWith("AGUA")) {
                    grid[idx[0]][idx[1]] = 'o';
                } else {
                    grid[idx[0]][idx[1]] = 'X';
                }
            }
        }

        return gridParaString(grid, "TABULEIRO DO ADVERSARIO");
    }

    /**
     * Converte uma célula ("A1") para índices da matriz [linha][coluna].
     */
    private static int[] celulaParaIndices(String celula) {
        if (celula == null || celula.length() < 2) return null;
        int linha = Character.toUpperCase(celula.charAt(0)) - 'A';
        try {
            int col = Integer.parseInt(celula.substring(1)) - 1;
            if (linha < 0 || linha >= TAMANHO || col < 0 || col >= TAMANHO) return null;
            return new int[]{linha, col};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Formata o grid 10x10 numa string com cabeçalho de colunas e rótulos de linhas */
    private static String gridParaString(char[][] grid, String titulo) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ").append(titulo).append(" ===\n");
        sb.append("   1  2  3  4  5  6  7  8  9  10\n");
        for (int i = 0; i < TAMANHO; i++) {
            sb.append(LINHAS[i]).append("  ");
            for (int j = 0; j < TAMANHO; j++) {
                sb.append(grid[i][j]).append("  ");
            }
            sb.append("\n");
        }
        sb.append("\nLegenda: ~ agua | N navio | X acertado | o falhou\n");
        return sb.toString();
    }
}
