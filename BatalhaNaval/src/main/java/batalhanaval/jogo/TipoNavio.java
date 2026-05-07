package batalhanaval.jogo;

/**
 * Enumeração com os tipos de navios do jogo Batalha Naval.
 *
 * Regras do enunciado:
 *   4 navios de 1 casa  (Torpedeiros)
 *   3 navios de 2 casas (Submarinos)
 *   2 navios de 3 casas (Fragatas)
 *   1 navio  de 4 casas (Cruzador)
 *   1 navio  de 5 casas (Porta-aviões)
 *
 * Total de navios: 11
 */
public enum TipoNavio {

    TORPEDEIRO(1, 4, "Torpedeiro"),
    SUBMARINO(2, 3, "Submarino"),
    FRAGATA(3, 2, "Fragata"),
    CRUZADOR(4, 1, "Cruzador"),
    PORTA_AVIOES(5, 1, "Porta-aviões");

    /** Tamanho em casas do tabuleiro */
    private final int tamanho;

    /** Quantidade deste tipo de navio no jogo */
    private final int quantidade;

    /** Nome legível para o utilizador */
    private final String nome;

    TipoNavio(int tamanho, int quantidade, String nome) {
        this.tamanho = tamanho;
        this.quantidade = quantidade;
        this.nome = nome;
    }

    public int getTamanho() { return tamanho; }
    public int getQuantidade() { return quantidade; }
    public String getNome() { return nome; }

    @Override
    public String toString() { return nome + "(" + tamanho + ")"; }
}
