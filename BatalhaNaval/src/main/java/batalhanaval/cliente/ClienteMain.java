package batalhanaval.cliente;

/**
 * Ponto de entrada do cliente em modo standalone (fora do LauncherIDE).
 *
 * Utilização:
 *   java -cp ... batalhanaval.cliente.ClienteMain [host] [porta]
 *
 * Dentro do NetBeans:
 *   botão direito → Run Maven → "▶ Correr Cliente"
 */
public class ClienteMain {
    public static void main(String[] args) throws Exception {
        String host = (args.length > 0) ? args[0] : "localhost";
        int porta   = (args.length > 1) ? Integer.parseInt(args[1])
                                        : batalhanaval.servidor.ServidorMain.PORTA_PADRAO;
        new InterfaceCLI(host, porta, 0).iniciar();
    }
}
