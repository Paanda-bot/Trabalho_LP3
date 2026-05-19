package batalhanaval.jogo;
import java.io.Serializable;
import java.util.*;

/** Tabuleiro 10x10. Chave de célula: "A1" a "J10" */
public class Tabuleiro implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int TAMANHO = 10;
    public static final char[] LINHAS = {'A','B','C','D','E','F','G','H','I','J'};

    private final List<Navio> navios = new ArrayList<>();
    private final Map<String,String> tirosRecebidos = new HashMap<>();

    /** Tenta colocar navio. false se inválido (fora do tabuleiro ou sobreposição) */
    public boolean posicionarNavio(Navio navio) {
        for (String c : navio.getCelulas()) {
            if (!celulaValida(c) || navioNaCelula(c) != null) return false;
        }
        navios.add(navio);
        return true;
    }

    public boolean celulaValida(String chave) {
        if (chave == null || chave.length() < 2) return false;
        char l = Character.toUpperCase(chave.charAt(0));
        try { int c = Integer.parseInt(chave.substring(1));
              return l>='A' && l<='J' && c>=1 && c<=TAMANHO; }
        catch (NumberFormatException e) { return false; }
    }

    public Navio navioNaCelula(String chave) {
        for (Navio n : navios) if (n.getCelulas().contains(chave)) return n;
        return null;
    }

    /** Processa tiro. @return "AGUA", "ACERTOU:Tipo" ou "AFUNDOU:Tipo". null se inválido. */
    public String receberTiro(String chave) {
        if (!celulaValida(chave) || tirosRecebidos.containsKey(chave)) return null;
        Navio n = navioNaCelula(chave);
        if (n == null) { tirosRecebidos.put(chave,"AGUA"); return "AGUA"; }
        n.receberTiro(chave);
        String res = (n.estaAfundado() ? "AFUNDOU:" : "ACERTOU:") + n.getTipo().getNome();
        tirosRecebidos.put(chave, res);
        return res;
    }

    public boolean jaDisparado(String c)       { return tirosRecebidos.containsKey(c); }
    public boolean todosNaviosAfundados()       { return navios.stream().allMatch(Navio::estaAfundado); }

    public boolean posicionamentoConcluido() {
        Map<TipoNavio,Integer> cnt = new HashMap<>();
        for (Navio n : navios) cnt.merge(n.getTipo(), 1, Integer::sum);
        for (TipoNavio t : TipoNavio.values())
            if (cnt.getOrDefault(t,0) < t.getQuantidade()) return false;
        return true;
    }

    public List<Navio> getNavios()                    { return Collections.unmodifiableList(navios); }
    public Map<String,String> getTirosRecebidos()     { return Collections.unmodifiableMap(tirosRecebidos); }
}