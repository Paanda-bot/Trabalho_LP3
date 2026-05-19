package batalhanaval.servidor;
import java.io.IOException;
import java.net.*;

/** Ponto de entrada do servidor. Corre em terminal separado. */
public class ServidorMain {
    public static final int PORTA_PADRAO = 12345;

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;
        System.out.println("=== BATALHA NAVAL - SERVIDOR ===");
        System.out.println("Porta: " + porta + " | Aguardando ligacoes...");

        try (ServerSocket ss = new ServerSocket(porta)) {
            while (true) {
                System.out.println("\n[S] Aguardando jogador 1...");
                Socket c1 = ss.accept();
                System.out.println("[S] Jogador 1 ligado: " + c1.getInetAddress());
                System.out.println("[S] Aguardando jogador 2...");
                Socket c2 = ss.accept();
                System.out.println("[S] Jogador 2 ligado: " + c2.getInetAddress());

                String id = "jogo_" + System.currentTimeMillis();
                Thread t = new Thread(new GestorJogo(c1, c2, id), "Jogo-"+id);
                t.setDaemon(true);
                t.start();
                System.out.println("[S] Jogo " + id + " iniciado.");
            }
        }
    }
}