package batalhanaval.cliente;

import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;

import java.io.*;
import java.net.Socket;

/**
 * Gere a ligação ao servidor via Socket.
 *
 * Esta classe corre numa Thread dedicada para ler mensagens do servidor
 * sem bloquear a interface do cliente.
 *
 * Modelo de concorrência:
 *  - A thread principal do cliente: trata do input do utilizador e envia mensagens
 *  - Esta thread (LeitorServidor): lê mensagens recebidas do servidor e chama callbacks
 *
 * Usando uma interface (Callback) para notificar o InterfaceCLI quando chegam mensagens,
 * tornando o código desacoplado.
 */
public class LigacaoServidor implements Runnable {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final MensagemCallback callback;
    private volatile boolean activo = true;

    /**
     * Interface de callback: chamada quando chega uma mensagem do servidor.
     * O InterfaceCLI implementa esta interface para reagir às mensagens.
     */
    public interface MensagemCallback {
        void onMensagemRecebida(Mensagem mensagem);
        void onDesconexao();
    }

    /**
     * Constrói a ligação ao servidor.
     *
     * @param host     endereço IP ou hostname do servidor
     * @param porta    porta do servidor
     * @param callback callback para processar mensagens recebidas
     * @throws IOException se não conseguir ligar
     */
    public LigacaoServidor(String host, int porta, MensagemCallback callback) throws IOException {
        this.callback = callback;
        this.socket = new Socket(host, porta);
        this.out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        System.out.println("[CLIENTE] Ligado a " + host + ":" + porta);
    }

    /**
     * Loop de leitura: corre numa Thread dedicada e lê mensagens continuamente.
     * Cada mensagem recebida é passada ao callback para ser processada pelo cliente.
     */
    @Override
    public void run() {
        try {
            String linha;
            while (activo && (linha = in.readLine()) != null) {
                Mensagem msg = Mensagem.deserializar(linha);
                if (msg != null && callback != null) {
                    callback.onMensagemRecebida(msg);
                }
            }
        } catch (IOException e) {
            if (activo) {
                System.err.println("[CLIENTE] Ligação perdida: " + e.getMessage());
            }
        } finally {
            if (callback != null) {
                callback.onDesconexao();
            }
        }
    }

    /**
     * Envia uma mensagem ao servidor.
     *
     * @param tipo  tipo da mensagem
     * @param dados dados da mensagem
     */
    public void enviarMensagem(TipoMensagem tipo, String dados) {
        if (out != null) {
            Mensagem msg = new Mensagem(tipo, dados);
            out.println(msg.serializar());
        }
    }

    /**
     * Envia uma mensagem sem dados ao servidor.
     */
    public void enviarMensagem(TipoMensagem tipo) {
        enviarMensagem(tipo, "");
    }

    /** Termina a ligação ao servidor */
    public void desligar() {
        activo = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isActivo() { return activo && socket != null && socket.isConnected(); }
}
