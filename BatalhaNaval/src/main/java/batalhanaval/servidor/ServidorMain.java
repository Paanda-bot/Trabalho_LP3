package batalhanaval.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Ponto de entrada do Servidor de Batalha Naval.
 *
 * O servidor:
 *  1. Abre um ServerSocket na porta indicada (padrão: 12345)
 *  2. Aceita ligações de clientes num loop infinito
 *  3. Cada par de clientes é gerido por um GestorJogo dedicado (nova Thread)
 *
 * Utilização:
 *   java -jar BatalhaNaval-servidor.jar [porta]
 *
 * Exemplo:
 *   java -jar BatalhaNaval-servidor.jar 12345
 */
public class ServidorMain {

    /** Porta padrão do servidor */
    public static final int PORTA_PADRAO = 12345;

    /**
     * Mapa de jogos activos: idJogo → GestorJogo
     * Permite que um cliente se reconecte a um jogo em curso.
     */
    private static final Map<String, GestorJogo> jogosActivos = new HashMap<>();

    public static void main(String[] args) {
        // Lê a porta dos argumentos ou usa a porta padrão
        int porta = PORTA_PADRAO;
        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[SERVIDOR] Porta inválida, a usar " + PORTA_PADRAO);
            }
        }

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      BATALHA NAVAL - SERVIDOR        ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("[SERVIDOR] A iniciar na porta " + porta + "...");

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[SERVIDOR] À espera de ligações...");

            // Loop principal: aceita dois clientes de cada vez para um jogo
            while (true) {
                System.out.println("[SERVIDOR] À espera do Jogador 1...");
                Socket jogador1 = serverSocket.accept();
                System.out.println("[SERVIDOR] Jogador 1 ligado: " + jogador1.getInetAddress());

                System.out.println("[SERVIDOR] À espera do Jogador 2...");
                Socket jogador2 = serverSocket.accept();
                System.out.println("[SERVIDOR] Jogador 2 ligado: " + jogador2.getInetAddress());

                // Cria um gestor para este par de jogadores e lança numa thread
                GestorJogo gestor = new GestorJogo(jogador1, jogador2, jogosActivos);
                Thread t = new Thread(gestor);
                t.setName("Jogo-" + gestor.getIdJogo());
                t.setDaemon(true); // Thread daemon: termina com o servidor
                t.start();

                System.out.println("[SERVIDOR] Jogo " + gestor.getIdJogo() + " iniciado.");
            }
        } catch (IOException e) {
            System.err.println("[SERVIDOR] Erro ao iniciar: " + e.getMessage());
        }
    }

    /** Regista um jogo activo no mapa global */
    public static synchronized void registarJogo(String idJogo, GestorJogo gestor) {
        jogosActivos.put(idJogo, gestor);
    }

    /** Remove um jogo do mapa quando termina */
    public static synchronized void removerJogo(String idJogo) {
        jogosActivos.remove(idJogo);
    }
}
