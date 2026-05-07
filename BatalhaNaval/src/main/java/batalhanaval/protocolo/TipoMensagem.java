package batalhanaval.protocolo;

/**
 * Enumeração com todos os tipos de mensagens trocadas entre servidor e cliente.
 * O protocolo de comunicação é baseado em texto simples (linhas), onde cada linha
 * começa com o TipoMensagem seguido de ":" e os dados.
 *
 * Exemplos de mensagens:
 *   POSICIONAR_NAVIO:A1 H 5       -> posiciona navio na linha A, coluna 1, horizontal, tamanho 5
 *   TIRO:B3                       -> dispara tiro na célula B3
 *   RESULTADO_TIRO:AGUA:B3        -> servidor informa que o tiro foi água
 *   RESULTADO_TIRO:ACERTOU:FRAGATA:B3 -> acertou uma Fragata
 */
public enum TipoMensagem {

    // ── Mensagens do CLIENTE para o SERVIDOR ──────────────────────────────────

    /** Cliente envia o nome/ID do jogo a que se quer ligar ou criar */
    LIGAR,

    /** Cliente envia a posição de um navio: POSICIONAR_NAVIO:<linha><col> <H|V> <tamanho> */
    POSICIONAR_NAVIO,

    /** Cliente confirma que terminou de posicionar todos os navios */
    PRONTO,

    /** Cliente dispara um tiro: TIRO:<linha><col>  (ex: TIRO:A3) */
    TIRO,

    /** Cliente pede para guardar o jogo */
    GUARDAR,

    /** Cliente pede para carregar um jogo guardado */
    CARREGAR,

    /** Cliente informa que quer sair / desligar */
    SAIR,

    // ── Mensagens do SERVIDOR para o CLIENTE ─────────────────────────────────

    /** Servidor confirma ligação e informa o número do jogador (1 ou 2) */
    LIGADO,

    /** Servidor indica que está à espera do segundo jogador */
    AGUARDAR,

    /** Servidor pede ao cliente para posicionar os seus navios */
    INICIAR_POSICIONAMENTO,

    /** Servidor confirma que um navio foi posicionado com sucesso */
    NAVIO_POSICIONADO,

    /** Servidor informa que a posição do navio é inválida */
    NAVIO_INVALIDO,

    /** Servidor informa o resultado de um tiro: RESULTADO_TIRO:<AGUA|ACERTOU|AFUNDOU>:<tipo_navio>:<coordenada> */
    RESULTADO_TIRO,

    /** Servidor informa que é o turno deste jogador (pode atirar 3 vezes) */
    TEU_TURNO,

    /** Servidor informa que é o turno do adversário */
    TURNO_ADVERSARIO,

    /** Servidor informa que o jogo terminou: FIM_JOGO:<VITORIA|DERROTA|EMPATE> */
    FIM_JOGO,

    /** Servidor confirma que o jogo foi guardado com sucesso */
    JOGO_GUARDADO,

    /** Servidor confirma que o jogo foi carregado com sucesso */
    JOGO_CARREGADO,

    /** Servidor informa um erro genérico */
    ERRO,

    /** Servidor envia o estado completo dos tabuleiros ao cliente */
    ESTADO_TABULEIRO,

    /** Servidor informa que o adversário se desligou */
    ADVERSARIO_DESLIGADO,

    /** Servidor informa quantos tiros ainda restam no turno */
    TIROS_RESTANTES,

    /** Servidor informa o tiro do adversário (para actualizar o tabuleiro do cliente) */
    TIRO_ADVERSARIO
}
