package batalhanaval.protocolo;

/** Mensagem do protocolo. Formato: TIPO:dado1:dado2 */
public class Mensagem {
    private final TipoMensagem tipo;
    private final String[] dados;

    public Mensagem(TipoMensagem tipo, String... dados) {
        this.tipo = tipo; this.dados = dados;
    }
    public TipoMensagem getTipo()   { return tipo; }
    public String getDado(int i)    { return i < dados.length ? dados[i] : ""; }
    public int numDados()           { return dados.length; }

    @Override public String toString() {
        if (dados.length == 0) return tipo.name();
        return tipo.name() + ":" + String.join(":", dados);
    }

    public static Mensagem parse(String linha) {
        if (linha == null || linha.isBlank()) return null;
        String[] p = linha.trim().split(":", -1);
        TipoMensagem t = TipoMensagem.fromString(p[0]);
        if (t == null) return null;
        String[] d = new String[p.length-1];
        System.arraycopy(p, 1, d, 0, d.length);
        return new Mensagem(t, d);
    }
}