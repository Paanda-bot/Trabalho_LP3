package batalhanaval.protocolo;

/**
 * Representa uma mensagem do protocolo de comunicação.
 *
 * Serialização: "TIPO:dados\n"
 * Desserialização: lê uma linha e separa no primeiro ':'
 */
public class Mensagem {
    private final TipoMensagem tipo;
    private final String dados;

    public Mensagem(TipoMensagem tipo, String dados) {
        this.tipo = tipo;
        this.dados = dados == null ? "" : dados;
    }
    public Mensagem(TipoMensagem tipo) { this(tipo, ""); }

    public TipoMensagem getTipo() { return tipo; }
    public String getDados()      { return dados; }

    /** Converte para linha de texto para enviar pelo socket */
    public String serializar() {
        return dados.isEmpty() ? tipo.name() : tipo.name() + ":" + dados;
    }

    /** Lê uma linha recebida e cria o objecto Mensagem correspondente */
    public static Mensagem deserializar(String linha) {
        if (linha == null || linha.isBlank()) return null;
        int idx = linha.indexOf(':');
        try {
            if (idx == -1) {
                return new Mensagem(TipoMensagem.valueOf(linha.trim().toUpperCase()));
            } else {
                String tipoStr = linha.substring(0, idx).trim().toUpperCase();
                String dados   = linha.substring(idx + 1);
                return new Mensagem(TipoMensagem.valueOf(tipoStr), dados);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[PROTOCOLO] Mensagem desconhecida: " + linha);
            return null;
        }
    }

    @Override public String toString() { return serializar(); }
}
