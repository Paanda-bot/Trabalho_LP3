package batalhanaval.jogo;

import java.io.Serializable;
import java.util.*;

/**
 * Representa um navio posicionado no tabuleiro.
 *
 * UML (simplificado):
 *   Navio
 *   ─────────────────
 *   - tipo: TipoNavio
 *   - celulas: List<String>
 *   - celulasAtingidas: List<String>
 *   ─────────────────
 *   + ocupaCelula(String): boolean
 *   + receberTiro(String): boolean   // true = afundou
 *   + estaAfundado(): boolean
 */
public class Navio implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TipoNavio tipo;
    private final List<String> celulas;
    private final List<String> celulasAtingidas = new ArrayList<>();

    public Navio(TipoNavio tipo, List<String> celulas) {
        this.tipo    = tipo;
        this.celulas = new ArrayList<>(celulas);
    }

    public boolean ocupaCelula(String celula) {
        return celulas.contains(celula.toUpperCase());
    }

    /**
     * Regista um acerto.
     * @return true se o navio ficou afundado com este tiro
     */
    public boolean receberTiro(String celula) {
        String c = celula.toUpperCase();
        if (celulas.contains(c) && !celulasAtingidas.contains(c)) {
            celulasAtingidas.add(c);
        }
        return estaAfundado();
    }

    public boolean estaAfundado() { return celulasAtingidas.size() >= celulas.size(); }

    public TipoNavio       getTipo()            { return tipo; }
    public List<String>    getCelulas()          { return Collections.unmodifiableList(celulas); }
    public List<String>    getCelulasAtingidas() { return Collections.unmodifiableList(celulasAtingidas); }
}
