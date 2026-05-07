package batalhanaval.cliente;

import java.io.IOException;

/**
 * Ponto de entrada do Cliente de Batalha Naval.
 *
 * Fluxo de arranque:
 *  1. Lê host e porta dos argumentos (ou usa padrão)
 *  2. Cria a InterfaceCLI
 *  3. Cria a LigacaoServidor passando a CLI como callback
 *  4. Lança a thread de leitura do servidor (LeitorServidor)
 *  5. Inicia o loop principal da CLI na thread principal
 *
 * Utilização:
 *   java -jar BatalhaNaval-cliente.jar [host] [porta]
 *
 * Exemplos:
 *   java -jar BatalhaNaval-cliente.jar
 *   java -jar BatalhaNaval-cliente.jar 192.168.1.5
 *   java -jar BatalhaNaval-cliente.jar 192.168.1.5 8080
 */
public class ClienteMain {

    public static void main(String[] args) {
        String host = "localhost";
        int porta = 12345;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try {
                porta = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("[CLIENTE] Porta inválida, a usar 12345.");
            }
        }

        System.out.println("[CLIENTE] A ligar a " + host + ":" + porta + "...");

        try {
            // Usamos um array de um elemento para passar a referência ao lambda/inner class
            // (Java exige que variáveis usadas em inner classes sejam effectively final)
            final InterfaceCLI[] cliHolder = new InterfaceCLI[1];

            // Cria a ligação ao servidor; o callback é a CLI (criada logo a seguir)
            LigacaoServidor ligacao = new LigacaoServidor(host, porta,
                    new LigacaoServidor.MensagemCallback() {
                        @Override
                        public void onMensagemRecebida(batalhanaval.protocolo.Mensagem mensagem) {
                            // Delega para a CLI assim que estiver disponível
                            if (cliHolder[0] != null) cliHolder[0].onMensagemRecebida(mensagem);
                        }
                        @Override
                        public void onDesconexao() {
                            if (cliHolder[0] != null) cliHolder[0].onDesconexao();
                        }
                    });

            // Cria a CLI com a ligação
            cliHolder[0] = new InterfaceCLI(ligacao);

            // Lança a thread de leitura do servidor (daemon → termina com o programa)
            Thread leitor = new Thread(ligacao, "LeitorServidor");
            leitor.setDaemon(true);
            leitor.start();

            // Inicia o loop principal da interface (thread principal)
            cliHolder[0].iniciar();

        } catch (IOException e) {
            System.err.println("[CLIENTE] Não foi possível ligar: " + e.getMessage());
            System.err.println("Verifique que o servidor está em " + host + ":" + porta);
        }
    }
}
