package batalhanaval.jogo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um navio colocado no tabuleiro.
 *
 * Um navio ocupa uma lista de células (coordenadas) no tabuleiro 10x10.
 * Cada célula pode ser:
 *  - intacta    → não foi atingida
 *  - atingida   → foi atingida (hit), mas o navio ainda não afundou
 *  - afundado   → todas as células atingidas, navio destruído
 *
 * A serialização (Serializable) é usada para guardar/carregar o estado do jogo em ficheiro.
 */
public class Navio implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Tipo e tamanho deste navio */
    private final TipoNavio tipo;

    /**
     * Lista de células ocupadas pelo navio.
     * Cada célula é uma String no formato "A1", "B3", etc.
     */
    private final List<String> celulas;

    /**
     * Lista de células já atingidas.
     * Quando celulasAtingidas.size() == celulas.size(), o navio afundou.
     */
    private final List<String> celulasAtingidas;

    /**
     * Cria um navio do tipo dado nas células especificadas.
     *
     * @param tipo   tipo do navio (define o tamanho esperado)
     * @param celulas lista de coordenadas que o navio ocupa
     */
    public Navio(TipoNavio tipo, List<String> celulas) {
        this.tipo = tipo;
        this.celulas = new ArrayList<>(celulas);
        this.celulasAtingidas = new ArrayList<>();
    }

    /**
     * Verifica se este navio ocupa a célula dada.
     *
     * @param celula coordenada no formato "A1"
     * @return true se o navio está nessa célula
     */
    public boolean ocupaCelula(String celula) {
        return celulas.contains(celula.toUpperCase());
    }

    /**
     * Regista um acerto neste navio.
     *
     * @param celula célula atingida
     * @return true se o acerto afundou o navio (última célula atingida)
     */
    public boolean receberTiro(String celula) {
        String c = celula.toUpperCase();
        if (celulas.contains(c) && !celulasAtingidas.contains(c)) {
            celulasAtingidas.add(c);
        }
        return estaAfundado();
    }

    /**
     * Verifica se o navio está completamente destruído.
     *
     * @return true se todas as células foram atingidas
     */
    public boolean estaAfundado() {
        return celulasAtingidas.size() >= celulas.size();
    }

    public TipoNavio getTipo() { return tipo; }
    public List<String> getCelulas() { return celulas; }
    public List<String> getCelulasAtingidas() { return celulasAtingidas; }

    @Override
    public String toString() {
        return tipo.getNome() + " em " + celulas + (estaAfundado() ? " [AFUNDADO]" : "");
    }
}
