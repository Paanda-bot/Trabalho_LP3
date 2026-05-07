package batalhanaval.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║              BATALHA NAVAL — SERVIDOR                    ║
 * ║  Aceita pares de clientes e lança um GestorJogo por par  ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Utilização:
 *   java -cp ... batalhanaval.servidor.ServidorMain [porta]
 *
 * Dentro do NetBeans:
 *   Botão direito no projecto → Run Maven → "▶ Correr Servidor"
 */
public class ServidorMain {

    public static final int PORTA_PADRAO = 12345;

    // Cores ANSI para o log do servidor
    static final String RESET  = "\u001B[0m";
    static final String BOLD   = "\u001B[1m";
    static final String CYAN   = "\u001B[36m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String RED    = "\u001B[31m";
    static final String DIM    = "\u001B[2m";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Mapa de jogos activos: idJogo → GestorJogo */
    static final ConcurrentHashMap<String, GestorJogo> jogosActivos = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int porta = PORTA_PADRAO;
        if (args.length > 0) {
            try { porta = Integer.parseInt(args[0]); }
            catch (NumberFormatException ignored) {}
        }

        banner(porta);

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (true) {
                log(CYAN, "Aguardando Jogador 1...");
                Socket j1 = serverSocket.accept();
                log(GREEN, "Jogador 1 ligado: " + j1.getInetAddress().getHostAddress());

                log(CYAN, "Aguardando Jogador 2...");
                Socket j2 = serverSocket.accept();
                log(GREEN, "Jogador 2 ligado: " + j2.getInetAddress().getHostAddress());

                GestorJogo gestor = new GestorJogo(j1, j2);
                jogosActivos.put(gestor.getIdJogo(), gestor);

                Thread t = new Thread(gestor, "Jogo-" + gestor.getIdJogo());
                t.setDaemon(true);
                t.start();
                log(YELLOW, "Jogo " + BOLD + gestor.getIdJogo() + RESET + YELLOW + " iniciado!");
            }
        }
    }

    /** Formata e imprime uma linha de log do servidor */
    public static void log(String cor, String msg) {
        System.out.println(DIM + "[" + LocalTime.now().format(FMT) + "] " + RESET
                         + cor + "[SERVIDOR] " + msg + RESET);
    }

    private static void banner(int porta) {
        System.out.println(BOLD + CYAN);
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║     ⚓  BATALHA NAVAL — SERVIDOR  ⚓          ║");
        System.out.println("  ╠══════════════════════════════════════════════╣");
        System.out.printf( "  ║  Porta: %-37d║%n", porta);
        System.out.println("  ║  Aguardando ligações...                      ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println(RESET);
    }
}
