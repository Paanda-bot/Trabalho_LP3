package batalhanaval.cliente;

import batalhanaval.protocolo.Mensagem;
import batalhanaval.protocolo.TipoMensagem;

import java.io.*;
import java.net.Socket;

/**
 * ══════════════════════════════════════════════════════════
 *  LigacaoServidor — gere a ligação TCP ao servidor
 * ══════════════════════════════════════════════════════════
 *
 * UML:
 *   LigacaoServidor
 *   ─────────────────────────────────────────────────────
 *   - socket: Socket
 *   - out: PrintWriter
 *   - callback: MensagemCallback
 *   ─────────────────────────────────────────────────────
 *   + enviar(Mensagem): void
 *   + fechar(): void
 *   + isLigado(): boolean
 *
 *   [interface] MensagemCallback
 *   + onMensagemRecebida(Mensagem): void
 *
 *   [inner class] LeitorServidor implements Runnable
 *   Lê linhas do socket em loop e chama o callback.
 *   Corre numa Thread separada para não bloquear a UI.
 *
 * Por que precisamos de uma Thread aqui?
 *   → BufferedReader.readLine() bloqueia até chegar uma linha.
 *     Se a UI (InterfaceCLI) ficasse à espera do socket, não conseguia
 *     ler input do utilizador ao mesmo tempo. Com uma Thread dedicada
 *     apenas à leitura, a UI mantém-se sempre responsiva.
 */
public class LigacaoServidor {

    /** Interface de callback — a CLI implementa esta interface */
    public interface MensagemCallback {
        void onMensagemRecebida(Mensagem msg);
    }

    private final Socket           socket;
    private final PrintWriter      out;
    private final MensagemCallback callback;
    private volatile boolean       ligado = true;

    public LigacaoServidor(String host, int porta, MensagemCallback callback)
            throws IOException {
        this.socket   = new Socket(host, porta);
        this.out      = new PrintWriter(new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream())), true);
        this.callback = callback;

        // Lança a thread de leitura como daemon (termina quando a JVM terminar)
        Thread leitor = new Thread(new LeitorServidor(), "LeitorServidor");
        leitor.setDaemon(true);
        leitor.start();
    }

    /** Envia uma mensagem ao servidor */
    public void enviar(Mensagem msg) {
        if (ligado && out != null) out.println(msg.serializar());
    }

    /** Envia mensagem simples sem dados */
    public void enviar(TipoMensagem tipo) { enviar(new Mensagem(tipo)); }

    /** Fecha a ligação */
    public void fechar() {
        ligado = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isLigado() { return ligado && !socket.isClosed(); }

    // ── Thread de leitura ────────────────────────────────────────────────────

    private class LeitorServidor implements Runnable {
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
                String linha;
                while (ligado && (linha = in.readLine()) != null) {
                    Mensagem msg = Mensagem.deserializar(linha);
                    if (msg != null) callback.onMensagemRecebida(msg);
                }
            } catch (IOException e) {
                if (ligado) {
                    // Ligação perdida inesperadamente
                    callback.onMensagemRecebida(
                        new Mensagem(TipoMensagem.ADVERSARIO_DESLIGADO,
                                     "Ligação perdida: " + e.getMessage()));
                }
            } finally {
                ligado = false;
            }
        }
    }
}
