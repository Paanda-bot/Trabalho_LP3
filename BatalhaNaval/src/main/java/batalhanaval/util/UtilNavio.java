package batalhanaval.util;

import batalhanaval.jogo.Navio;
import batalhanaval.jogo.Tabuleiro;
import batalhanaval.jogo.TipoNavio;
import java.util.*;

/** Utilitários para calcular células e serializar/deserializar posicionamentos */
public class UtilNavio {

    public static List<String> calcularCelulas(String inicio, char orientacao, int tamanho) {
        if (!Tabuleiro.celulaValida(inicio)) return null;
        char linha = Character.toUpperCase(inicio.charAt(0));
        int  coluna = Integer.parseInt(inicio.substring(1));
        List<String> celulas = new ArrayList<>();
        for (int i = 0; i < tamanho; i++) {
            String cel;
            if (Character.toUpperCase(orientacao) == 'H') {
                int nc = coluna + i;
                if (nc > 10) return null;
                cel = "" + linha + nc;
            } else {
                char nl = (char)(linha + i);
                if (nl > 'J') return null;
                cel = "" + nl + coluna;
            }
            if (!Tabuleiro.celulaValida(cel)) return null;
            celulas.add(cel);
        }
        return celulas;
    }

    public static Navio criarNavio(TipoNavio tipo, String inicio, char orientacao) {
        List<String> celulas = calcularCelulas(inicio, orientacao, tipo.getTamanho());
        return celulas == null ? null : new Navio(tipo, celulas);
    }

    public static String serializarPosicionamento(TipoNavio tipo, String inicio, char orientacao) {
        return tipo.name() + ":" + inicio.toUpperCase() + ":" + Character.toUpperCase(orientacao);
    }

    public static Navio deserializarPosicionamento(String dados) {
        if (dados == null) return null;
        String[] p = dados.split(":");
        if (p.length < 3) return null;
        try {
            TipoNavio tipo = TipoNavio.valueOf(p[0].toUpperCase());
            return criarNavio(tipo, p[1].toUpperCase(), p[2].toUpperCase().charAt(0));
        } catch (IllegalArgumentException e) { return null; }
    }
}
