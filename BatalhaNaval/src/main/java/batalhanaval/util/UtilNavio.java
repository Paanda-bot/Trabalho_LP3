package batalhanaval.util;

import batalhanaval.jogo.Navio;
import batalhanaval.jogo.Tabuleiro;
import batalhanaval.jogo.TipoNavio;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitários para calcular as células que um navio ocupa,
 * a partir de uma posição inicial, orientação e tamanho.
 *
 * Usado tanto pelo servidor (para validar) como pelo cliente (para pré-visualizar).
 */
public class UtilNavio {

    /**
     * Calcula as células ocupadas por um navio dado o ponto inicial, orientação e tamanho.
     *
     * @param inicio      célula inicial (ex: "A1")
     * @param orientacao  'H' para horizontal, 'V' para vertical
     * @param tamanho     número de casas do navio
     * @return lista de células, ou null se fora dos limites
     */
    public static List<String> calcularCelulas(String inicio, char orientacao, int tamanho) {
        if (!Tabuleiro.celulaValida(inicio)) return null;

        char linha = Character.toUpperCase(inicio.charAt(0));
        int coluna = Integer.parseInt(inicio.substring(1));

        List<String> celulas = new ArrayList<>();

        for (int i = 0; i < tamanho; i++) {
            String celula;
            if (Character.toUpperCase(orientacao) == 'H') {
                // Expande para a direita
                int novaCol = coluna + i;
                if (novaCol > 10) return null; // Fora do tabuleiro
                celula = "" + linha + novaCol;
            } else {
                // Expande para baixo
                char novaLinha = (char) (linha + i);
                if (novaLinha > 'J') return null; // Fora do tabuleiro
                celula = "" + novaLinha + coluna;
            }
            if (!Tabuleiro.celulaValida(celula)) return null;
            celulas.add(celula);
        }
        return celulas;
    }

    /**
     * Cria um objecto Navio a partir dos parâmetros de posicionamento.
     *
     * @param tipo        tipo do navio
     * @param inicio      célula inicial
     * @param orientacao  'H' ou 'V'
     * @return Navio criado, ou null se os parâmetros forem inválidos
     */
    public static Navio criarNavio(TipoNavio tipo, String inicio, char orientacao) {
        List<String> celulas = calcularCelulas(inicio, orientacao, tipo.getTamanho());
        if (celulas == null) return null;
        return new Navio(tipo, celulas);
    }

    /**
     * Serializa os dados de posicionamento numa string para o protocolo.
     * Formato: "TIPO_NAVIO:INICIO:ORIENTACAO"
     *
     * Exemplo: "FRAGATA:C3:H"
     *
     * @param tipo       tipo do navio
     * @param inicio     célula inicial
     * @param orientacao 'H' ou 'V'
     * @return string do protocolo
     */
    public static String serializarPosicionamento(TipoNavio tipo, String inicio, char orientacao) {
        return tipo.name() + ":" + inicio.toUpperCase() + ":" + Character.toUpperCase(orientacao);
    }

    /**
     * Desserializa a string do protocolo para criar um Navio.
     * Formato esperado: "TIPO_NAVIO:INICIO:ORIENTACAO"
     *
     * @param dados string recebida no protocolo
     * @return Navio criado, ou null se inválido
     */
    public static Navio deserializarPosicionamento(String dados) {
        if (dados == null) return null;
        String[] partes = dados.split(":");
        if (partes.length < 3) return null;
        try {
            TipoNavio tipo = TipoNavio.valueOf(partes[0].toUpperCase());
            String inicio = partes[1].toUpperCase();
            char orientacao = partes[2].toUpperCase().charAt(0);
            return criarNavio(tipo, inicio, orientacao);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
