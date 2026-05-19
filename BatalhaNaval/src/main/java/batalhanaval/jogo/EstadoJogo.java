package batalhanaval.jogo;
import java.io.*;

/** Estado completo serializável — é este objecto que vai para o .dat */
public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String FICHEIRO_PADRAO = "batalha_naval_save.dat";
    public static final int TIROS_POR_TURNO = 3;

    public enum Fase { POSICIONAMENTO, EM_JOGO, TERMINADO }

    private final Tabuleiro tabuleiro1 = new Tabuleiro();
    private final Tabuleiro tabuleiro2 = new Tabuleiro();
    private Fase fase = Fase.POSICIONAMENTO;
    private int jogadorActual = 0;
    private int tirosRestantes = 0;
    private final String idJogo;

    public EstadoJogo(String id) { this.idJogo = id; }

    public Tabuleiro getTabuleiro(int j)       { return j==1 ? tabuleiro1 : tabuleiro2; }
    public Fase  getFase()                     { return fase; }
    public void  setFase(Fase f)               { this.fase = f; }
    public int   getJogadorActual()            { return jogadorActual; }
    public void  setJogadorActual(int j)       { this.jogadorActual = j; }
    public int   getTirosRestantes()           { return tirosRestantes; }
    public void  setTirosRestantes(int t)      { this.tirosRestantes = t; }
    public void  decrementarTiros()            { tirosRestantes--; }
    public String getIdJogo()                  { return idJogo; }
    public int   adversario(int j)             { return j==1 ? 2 : 1; }

    public void guardar(String caminho) throws IOException {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(caminho)))
        { o.writeObject(this); }
    }
    public static EstadoJogo carregar(String caminho) throws IOException, ClassNotFoundException {
        try (ObjectInputStream i = new ObjectInputStream(new FileInputStream(caminho)))
        { return (EstadoJogo) i.readObject(); }
    }
    public static boolean existeSave(String c) { return new File(c).exists(); }
}