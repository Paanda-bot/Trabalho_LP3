package batalhanaval.cliente;
import batalhanaval.protocolo.*;
import java.io.*;
import java.net.Socket;

/** Gere a ligação TCP. Thread de leitura em background. */
public class LigacaoServidor {
    public interface RecebidaCallback {
        void onMensagemRecebida(Mensagem msg);
    }

    private Socket socket;
    private PrintWriter saida;
    private BufferedReader entrada;
    private RecebidaCallback callback;
    private volatile boolean activo = false;

    public LigacaoServidor(RecebidaCallback cb) { this.callback = cb; }

    public void setCallback(RecebidaCallback cb) { this.callback = cb; }

    public void ligar(String host, int porta) throws IOException {
        socket  = new Socket(host, porta);
        saida   = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
        activo  = true;
        Thread t = new Thread(this::cicloLeitura,"LeitorServidor");
        t.setDaemon(true); t.start();
    }

    public void enviar(TipoMensagem tipo, String... dados) {
        if (activo && saida!=null) saida.println(new Mensagem(tipo,dados));
    }

    private void cicloLeitura() {
        try {
            String linha;
            while (activo && (linha=entrada.readLine())!=null) {
                Mensagem m = Mensagem.parse(linha);
                if (m!=null && callback!=null) callback.onMensagemRecebida(m);
            }
        } catch (IOException e) {
            if (activo && callback!=null)
                callback.onMensagemRecebida(new Mensagem(TipoMensagem.DESLIGAR,"Ligacao perdida"));
        } finally { activo = false; }
    }

    public void desligar() { activo=false; try{if(socket!=null)socket.close();}catch(IOException ignored){} }
    public boolean isActivo() { return activo; }
}