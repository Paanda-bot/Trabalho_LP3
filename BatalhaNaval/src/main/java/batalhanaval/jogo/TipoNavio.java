package batalhanaval.jogo;

/**
 * Tipos de navios conforme o enunciado:
 *   4 × Torpedeiro  (1 casa)
 *   3 × Submarino   (2 casas)
 *   2 × Fragata     (3 casas)
 *   1 × Cruzador    (4 casas)
 *   1 × Porta-aviões(5 casas)
 */
public enum TipoNavio {
    TORPEDEIRO   (1, 4, "Torpedeiro",    "T"),
    SUBMARINO    (2, 3, "Submarino",     "S"),
    FRAGATA      (3, 2, "Fragata",       "F"),
    CRUZADOR     (4, 1, "Cruzador",      "C"),
    PORTA_AVIOES (5, 1, "Porta-aviões",  "P");

    private final int tamanho;
    private final int quantidade;
    private final String nome;
    private final String simbolo; // símbolo de 1 char para o tabuleiro

    TipoNavio(int tamanho, int quantidade, String nome, String simbolo) {
        this.tamanho   = tamanho;
        this.quantidade = quantidade;
        this.nome      = nome;
        this.simbolo   = simbolo;
    }

    public int    getTamanho()    { return tamanho; }
    public int    getQuantidade() { return quantidade; }
    public String getNome()       { return nome; }
    public String getSimbolo()    { return simbolo; }

    @Override public String toString() { return nome + "(" + tamanho + ")"; }
}
