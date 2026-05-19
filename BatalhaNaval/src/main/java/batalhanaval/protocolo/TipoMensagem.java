package batalhanaval.protocolo;

/** Todos os tipos de mensagem do protocolo cliente↔servidor */
public enum TipoMensagem {
    // Ligação
    LIGADO, JOGO_RECUPERADO,
    // Posicionamento
    INICIAR_POSICIONAMENTO, POSICIONAR_NAVIO,
    NAVIO_POSICIONADO, ERRO_POSICIONAMENTO, POSICIONAMENTO_COMPLETO,
    // Jogo
    TEU_TURNO, TURNO_ADVERSARIO, TIRO, RESULTADO_TIRO, TIRO_INVALIDO,
    // Fim
    FIM_JOGO,
    // Save
    GUARDAR, JOGO_GUARDADO, CARREGAR,
    // Geral
    INFO, DESLIGAR;

    public static TipoMensagem fromString(String s) {
        try { return TipoMensagem.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}