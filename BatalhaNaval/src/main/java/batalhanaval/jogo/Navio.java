package batalhanaval.jogo;
import java.io.Serializable;
import java.util.*;

/** Um navio no tabuleiro: tipo, células ocupadas e células atingidas */
public class Navio implements Serializable {
    private static final long serialVersionUID = 1L;
    private final TipoNavio tipo;
    private final Set<String> celulas;
    private final Set<String> atingidas = new HashSet<>();

    public Navio(TipoNavio tipo, Set<String> celulas) {
        this.tipo    = tipo;
        this.celulas = new HashSet<>(celulas);
    }

    public TipoNavio getTipo()             { return tipo; }
    public Set<String> getCelulas()        { return Collections.unmodifiableSet(celulas); }

    /** Regista tiro. @return true se acertou neste navio */
    public boolean receberTiro(String chave) {
        if (celulas.contains(chave)) { atingidas.add(chave); return true; }
        return false;
    }
    public boolean estaAfundado()          { return atingidas.containsAll(celulas); }
    public boolean celulaAtingida(String c){ return atingidas.contains(c); }
}