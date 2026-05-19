package batalhanaval.servidor;

import batalhanaval.jogo.*;
import batalhanaval.protocolo.*;
import batalhanaval.util.UtilNavio;

import java.io.*;
import java.net.Socket;
import java.util.*;

/** Árbitro de um jogo: valida tiros, gere turnos, guarda estado */
public class GestorJogo implements Runnable {
    private final String idJogo;
    private final Socket[] sockets        = new Socket[3];
    private final PrintWriter[] saida     = new PrintWriter[3];
    private final BufferedReader[] entrada = new BufferedReader[3];
    private EstadoJogo estado;
    private int jogadoresProntos = 0;

    public GestorJogo(Socket s1, Socket s2, String id) throws IOException {
        this.idJogo = id;
        sockets[1] = s1; sockets[2] = s2;
        for (int j=1;j<=2;j++) {
            saida[j]   = new PrintWriter(new OutputStreamWriter(sockets[j].getOutputStream(),"UTF-8"),true);
            entrada[j] = new BufferedReader(new InputStreamReader(sockets[j].getInputStream(),"UTF-8"));
        }
    }

    @Override public void run() {
        try {
            // Carrega save se existir
            if (EstadoJogo.existeSave(EstadoJogo.FICHEIRO_PADRAO)) {
                try { estado = EstadoJogo.carregar(EstadoJogo.FICHEIRO_PADRAO);
                      enviar(1,TipoMensagem.JOGO_RECUPERADO,"Jogo recuperado!");
                      enviar(2,TipoMensagem.JOGO_RECUPERADO,"Jogo recuperado!"); }
                catch (Exception e) { estado = new EstadoJogo(idJogo); }
            } else { estado = new EstadoJogo(idJogo); }

            enviar(1,TipoMensagem.LIGADO,"1",idJogo);
            enviar(2,TipoMensagem.LIGADO,"2",idJogo);

            if (estado.getFase() == EstadoJogo.Fase.POSICIONAMENTO) {
                enviar(1,TipoMensagem.INICIAR_POSICIONAMENTO,"Posicione os navios");
                enviar(2,TipoMensagem.INICIAR_POSICIONAMENTO,"Posicione os navios");
                Thread t1 = new Thread(()->tratarPosicionamento(1),"Pos-J1");
                Thread t2 = new Thread(()->tratarPosicionamento(2),"Pos-J2");
                t1.start(); t2.start(); t1.join(); t2.join();
            }

            if (estado.getFase() != EstadoJogo.Fase.TERMINADO) {
                estado.setFase(EstadoJogo.Fase.EM_JOGO);
                if (estado.getJogadorActual()==0) {
                    int ini = new Random().nextInt(2)+1;
                    estado.setJogadorActual(ini);
                    enviar(1,TipoMensagem.INFO,"Jogador "+ini+" comeca!");
                    enviar(2,TipoMensagem.INFO,"Jogador "+ini+" comeca!");
                }
                cicloJogo();
            }
        } catch (Exception e) {
            System.err.println("["+idJogo+"] Erro: "+e.getMessage());
            guardarJogo();
        } finally { fechar(); }
    }

    private void tratarPosicionamento(int jogador) {
        while (!estado.getTabuleiro(jogador).posicionamentoConcluido()) {
            try {
                String linha = entrada[jogador].readLine();
                if (linha==null) { guardarJogo(); return; }
                Mensagem m = Mensagem.parse(linha);
                if (m==null) continue;
                if (m.getTipo()==TipoMensagem.POSICIONAR_NAVIO) posicionar(jogador,m);
                else if (m.getTipo()==TipoMensagem.GUARDAR) {
                    guardarJogo(); enviar(jogador,TipoMensagem.JOGO_GUARDADO,"Guardado!");
                }
            } catch (IOException e) { guardarJogo(); return; }
        }
        enviar(jogador,TipoMensagem.INFO,"Posicionamento concluido! A aguardar adversario...");
        synchronized(this) {
            jogadoresProntos++;
            notifyAll();
            while (jogadoresProntos<2) {
                try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void posicionar(int jogador, Mensagem m) {
        try {
            TipoNavio tipo = TipoNavio.valueOf(m.getDado(0).toUpperCase());
            char l = m.getDado(1).toUpperCase().charAt(0);
            int  c = Integer.parseInt(m.getDado(2));
            char o = m.getDado(3).toUpperCase().charAt(0);
            Set<String> cells = UtilNavio.calcularCelulas(l,c,tipo.getTamanho(),o);
            if (cells.isEmpty()) { enviar(jogador,TipoMensagem.ERRO_POSICIONAMENTO,"Fora dos limites"); return; }
            boolean ok = estado.getTabuleiro(jogador).posicionarNavio(new Navio(tipo,cells));
            if (ok) enviar(jogador,TipoMensagem.NAVIO_POSICIONADO,tipo.getNome()+" colocado em "+cells);
            else    enviar(jogador,TipoMensagem.ERRO_POSICIONAMENTO,"Sobreposicao ou limite invalido");
        } catch (Exception e) { enviar(jogador,TipoMensagem.ERRO_POSICIONAMENTO,"Formato invalido"); }
    }

    private void cicloJogo() throws IOException {
        while (estado.getFase()==EstadoJogo.Fase.EM_JOGO) {
            int atual = estado.getJogadorActual();
            int adv   = estado.adversario(atual);
            estado.setTirosRestantes(EstadoJogo.TIROS_POR_TURNO);
            enviar(atual, TipoMensagem.TEU_TURNO, String.valueOf(EstadoJogo.TIROS_POR_TURNO));
            enviar(adv,   TipoMensagem.TURNO_ADVERSARIO, "Turno do Jogador "+atual);

            while (estado.getTirosRestantes()>0 && estado.getFase()==EstadoJogo.Fase.EM_JOGO) {
                String linha = entrada[atual].readLine();
                if (linha==null) { guardarJogo(); return; }
                Mensagem m = Mensagem.parse(linha);
                if (m==null) continue;
                if (m.getTipo()==TipoMensagem.TIRO)    processarTiro(atual,adv,m);
                else if (m.getTipo()==TipoMensagem.GUARDAR) {
                    guardarJogo(); enviar(atual,TipoMensagem.JOGO_GUARDADO,"Guardado!");
                }
            }
            if (estado.getFase()==EstadoJogo.Fase.EM_JOGO) estado.setJogadorActual(adv);
        }
    }

    private void processarTiro(int atual, int adv, Mensagem m) {
        // Mensagem: TIRO:Linha:Coluna  ex: TIRO:B:4  → chave "B4"
        String chave = m.getDado(0).toUpperCase() + m.getDado(1);
        if (estado.getTabuleiro(adv).jaDisparado(chave)) {
            enviar(atual,TipoMensagem.TIRO_INVALIDO,"Celula "+chave+" ja disparada"); return;
        }
        String res = estado.getTabuleiro(adv).receberTiro(chave);
        if (res==null) { enviar(atual,TipoMensagem.TIRO_INVALIDO,"Coordenada invalida"); return; }
        estado.decrementarTiros();
        enviar(atual, TipoMensagem.RESULTADO_TIRO, res, chave);
        enviar(adv,   TipoMensagem.RESULTADO_TIRO, res, chave);
        System.out.println("["+idJogo+"] J"+atual+" disparou "+chave+" -> "+res);
        if (estado.getTabuleiro(adv).todosNaviosAfundados()) {
            estado.setFase(EstadoJogo.Fase.TERMINADO);
            enviar(atual, TipoMensagem.FIM_JOGO,"VITORIA");
            enviar(adv,   TipoMensagem.FIM_JOGO,"DERROTA");
            new File(EstadoJogo.FICHEIRO_PADRAO).delete();
        }
    }

    private void guardarJogo() {
        try { if (estado!=null) estado.guardar(EstadoJogo.FICHEIRO_PADRAO); }
        catch (IOException e) { System.err.println("Erro ao guardar: "+e.getMessage()); }
    }
    private void enviar(int j, TipoMensagem t, String... d) { saida[j].println(new Mensagem(t,d)); }
    private void fechar() {
        for (int j=1;j<=2;j++) try { if(sockets[j]!=null) sockets[j].close(); } catch(IOException ignored){}
    }
}