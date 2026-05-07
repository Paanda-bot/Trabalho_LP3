package batalhanaval.protocolo;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  PROTOCOLO DE COMUNICAÇÃO — Batalha Naval                ║
 * ║  Todas as mensagens trocadas via Socket TCP              ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Formato de cada mensagem (linha de texto terminada em \n):
 *   TIPO:dado1:dado2:...
 *
 * Exemplos:
 *   POSICIONAR_NAVIO:FRAGATA:C3:H
 *   RESULTADO_TIRO:AFUNDOU:Fragata:B3
 *   FIM_JOGO:VITORIA
 */
public enum TipoMensagem {
    // ── CLIENTE → SERVIDOR ──────────────────────────────────
    LIGAR,            // Pedido de ligação inicial
    POSICIONAR_NAVIO, // "TIPO:CELULA:ORIENTACAO"  ex: FRAGATA:C3:H
    PRONTO,           // Cliente terminou posicionamento
    TIRO,             // "CELULA"  ex: B4
    GUARDAR,          // Pedido de guardar jogo
    CARREGAR,         // "caminho_ficheiro"
    SAIR,             // Cliente quer sair

    // ── SERVIDOR → CLIENTE ──────────────────────────────────
    LIGADO,               // "numJogador:idJogo"
    AGUARDAR,             // Aguardar adversário
    INICIAR_POSICIONAMENTO,
    NAVIO_POSICIONADO,    // "descricao"
    NAVIO_INVALIDO,       // "motivo"
    RESULTADO_TIRO,       // "AGUA|ACERTOU|AFUNDOU:NomeNavio:coordenada"
    TIRO_ADVERSARIO,      // Mesmo formato, para actualizar o adversário
    TEU_TURNO,            // "mensagem"
    TURNO_ADVERSARIO,     // "mensagem"
    TIROS_RESTANTES,      // "n"
    FIM_JOGO,             // "VITORIA|DERROTA|EMPATE"
    JOGO_GUARDADO,        // "caminho"
    JOGO_CARREGADO,       // "mensagem"
    ESTADO_TABULEIRO,     // "tabProprio||tabAdversario"  (newlines escapados)
    ADVERSARIO_DESLIGADO, // "mensagem"
    ERRO                  // "descricao"
}
