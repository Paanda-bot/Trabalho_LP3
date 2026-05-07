# ⚓ Batalha Naval — TP3 (Laboratórios de Programação)

## Como correr NO NETBEANS (modo mais simples)

1. **File → Open Project** → selecciona a pasta `BatalhaNaval/`
2. Clica **Clean and Build** (`Shift+F11`)
3. Clica **Run Project** (`F6`)
4. Aparece o **menu gráfico do LauncherIDE**:
   - Clica **🎮 Jogar Agora** → abre o servidor automaticamente + 2 janelas de cliente

## Como correr em terminais separados

```bash
# Terminal 1 — Servidor
mvn exec:java -Dexec.mainClass="batalhanaval.servidor.ServidorMain"

# Terminal 2 — Cliente 1
mvn exec:java -Dexec.mainClass="batalhanaval.cliente.ClienteMain"

# Terminal 3 — Cliente 2
mvn exec:java -Dexec.mainClass="batalhanaval.cliente.ClienteMain"
```

## Regras do jogo

| Navio         | Tamanho | Quantidade |
|---------------|---------|------------|
| Torpedeiro    | 1 casa  | 4          |
| Submarino     | 2 casas | 3          |
| Fragata       | 3 casas | 2          |
| Cruzador      | 4 casas | 1          |
| Porta-aviões  | 5 casas | 1          |

- 3 tiros por turno
- Resultados: Água / Acertou / Afundou
- Save automático em falha de ligação
- Comando `GUARDAR` durante o jogo

## Estrutura UML (simplificada)

```
┌─────────────────┐          ┌──────────────────┐
│  ServidorMain   │ cria     │   GestorJogo     │
│  (main/porta)   │─────────▶│  implements      │
└─────────────────┘          │  Runnable        │
                             │  ─────────────── │
                             │  EstadoJogo      │
                             │  2× LeitorCliente│
                             └──────────────────┘
                                      │ usa
                              ┌───────▼────────┐
                              │  EstadoJogo    │
                              │  ──────────── │
                              │  Tabuleiro ×2  │
                              │  Navio[]       │
                              └───────────────┘

┌──────────────────┐  callback  ┌─────────────────┐
│ LigacaoServidor  │──────────▶│  InterfaceCLI   │
│ LeitorServidor   │           │  impl Callback  │
│ (Thread daemon)  │           │  wait/notify    │
└──────────────────┘           └─────────────────┘
```

## Ficheiro de save

Os saves são gravados em `saves/jogo_XXXXXXXX.dat` (na pasta de trabalho).
Para carregar um jogo, escreve `CARREGAR` durante a partida.
