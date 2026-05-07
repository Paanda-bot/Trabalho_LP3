package batalhanaval.protocolo;

/**
 * Representa uma mensagem trocada entre cliente e servidor.
 *
 * Formato de texto: TIPO:dado1:dado2:...
 *
 * Exemplo:  "RESULTADO_TIRO:ACERTOU:FRAGATA:B3"
 *            tipo=RESULTADO_TIRO, dados=["ACERTOU","FRAGATA","B3"]
 *
 * A comunicação é feita via Sockets com PrintWriter / BufferedReader,
 * cada mensagem numa linha de texto terminada por '\n'.
 */
public class Mensagem {

    /** Tipo da mensagem (enum TipoMensagem) */
    private final TipoMensagem tipo;

    /** Dados adicionais da mensagem (podem ser nulos ou vazios) */
    private final String dados;

    /**
     * Constrói uma mensagem com tipo e dados.
     *
     * @param tipo  tipo da mensagem
     * @param dados dados em texto livre (ex: "A3" ou "ACERTOU:FRAGATA:B3")
     */
    public Mensagem(TipoMensagem tipo, String dados) {
        this.tipo = tipo;
        this.dados = dados;
    }

    /**
     * Constrói uma mensagem só com tipo (sem dados).
     *
     * @param tipo tipo da mensagem
     */
    public Mensagem(TipoMensagem tipo) {
        this(tipo, "");
    }

    /** @return o tipo desta mensagem */
    public TipoMensagem getTipo() {
        return tipo;
    }

    /** @return os dados em texto da mensagem (pode ser "") */
    public String getDados() {
        return dados;
    }

    /**
     * Converte a mensagem para a linha de texto enviada pelo socket.
     * Formato: "TIPO:dados\n"
     *
     * @return string pronta para ser enviada via PrintWriter.println()
     */
    public String serializar() {
        if (dados == null || dados.isEmpty()) {
            return tipo.name();
        }
        return tipo.name() + ":" + dados;
    }

    /**
     * Lê uma linha de texto recebida pelo socket e constrói o objecto Mensagem.
     *
     * @param linha linha recebida pelo BufferedReader
     * @return objecto Mensagem correspondente, ou null se a linha for inválida
     */
    public static Mensagem deserializar(String linha) {
        if (linha == null || linha.isBlank()) {
            return null;
        }
        // Divide na primeira vírgula ":"
        int idx = linha.indexOf(':');
        try {
            if (idx == -1) {
                // Mensagem sem dados
                TipoMensagem tipo = TipoMensagem.valueOf(linha.trim().toUpperCase());
                return new Mensagem(tipo);
            } else {
                String tipoStr = linha.substring(0, idx).trim().toUpperCase();
                String dados = linha.substring(idx + 1);
                TipoMensagem tipo = TipoMensagem.valueOf(tipoStr);
                return new Mensagem(tipo, dados);
            }
        } catch (IllegalArgumentException e) {
            // Tipo desconhecido - ignora
            System.err.println("[PROTOCOLO] Mensagem desconhecida: " + linha);
            return null;
        }
    }

    @Override
    public String toString() {
        return "Mensagem{" + tipo + ", dados='" + dados + "'}";
    }
}
