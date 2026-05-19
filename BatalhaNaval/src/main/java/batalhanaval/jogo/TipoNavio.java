package batalhanaval.jogo;
import java.io.Serializable;

/** Tipos de navios e as suas regras (quantidade e tamanho) */
public enum TipoNavio implements Serializable {
    TORPEDEIRO  ("Torpedeiro",   1, 4),
    SUBMARINO   ("Submarino",    2, 3),
    FRAGATA     ("Fragata",      3, 2),
    CRUZADOR    ("Cruzador",     4, 1),
    PORTA_AVIOES("Porta-avioes", 5, 1);

    private final String nome;
    private final int tamanho;
    private final int quantidade;

    TipoNavio(String nome, int tamanho, int quantidade) {
        this.nome = nome; this.tamanho = tamanho; this.quantidade = quantidade;
    }
    public String getNome()       { return nome; }
    public int    getTamanho()    { return tamanho; }
    public int    getQuantidade() { return quantidade; }
    @Override public String toString() { return nome; }
}