package batalhanaval.util;
import java.util.*;

/** Calcula células de um navio dado posição + orientação */
public class UtilNavio {
    private UtilNavio() {}

    /**
     * @param linha      ex: 'B'
     * @param coluna     ex: 4
     * @param tamanho    número de células
     * @param orientacao 'H' ou 'V'
     * @return células ex: ["B4","B5","B6"], vazio se sair fora dos limites
     */
    public static Set<String> calcularCelulas(char linha, int coluna, int tamanho, char orientacao) {
        Set<String> c = new LinkedHashSet<>();
        for (int i = 0; i < tamanho; i++) {
            char l = orientacao=='V' ? (char)(linha+i) : linha;
            int  col = orientacao=='H' ? coluna+i : coluna;
            if (l<'A'||l>'J'||col<1||col>10) return new LinkedHashSet<>();
            c.add(""+l+col);
        }
        return c;
    }

    /** @return [linha(char), coluna(int)] ou null */
    public static Object[] parseCelula(String chave) {
        if (chave==null||chave.length()<2) return null;
        char l = Character.toUpperCase(chave.charAt(0));
        try { return new Object[]{l, Integer.parseInt(chave.substring(1))}; }
        catch (NumberFormatException e) { return null; }
    }
}