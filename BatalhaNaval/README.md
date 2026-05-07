# Batalha Naval — TP3 — Laboratórios de Programação

## Como abrir no Apache NetBeans

1. Abrir o NetBeans
2. **File → Open Project**
3. Selecionar a pasta `BatalhaNaval/` (onde está o `pom.xml`)
4. O NetBeans reconhece automaticamente como projeto Maven
5. Clicar com botão direito no projeto → **Build** (ou `Shift+F11`)

---

## Como correr (dois terminais)

### Terminal 1 — Servidor
```
mvn compile exec:java -Dexec.mainClass="batalhanaval.servidor.ServidorMain"
```

### Terminal 2 — Cliente 1
```
mvn compile exec:java -Dexec.mainClass="batalhanaval.cliente.ClienteMain"
```

### Terminal 3 — Cliente 2
```
mvn compile exec:java -Dexec.mainClass="batalhanaval.cliente.ClienteMain"
```

---

## Comandos durante o jogo

- **Posicionamento:** siga as instruções no ecrã (ex: célula `A1`, orientação `H` ou `V`)
- **Atirar:** escreva a coordenada (ex: `B4`) quando for o seu turno
- **Guardar:** escreva `GUARDAR` durante o seu turno
- **Sair:** escreva `SAIR` durante o seu turno

---

## Estrutura do Projeto

```
src/main/java/batalhanaval/
├── protocolo/      → TipoMensagem, Mensagem  (protocolo de comunicação)
├── jogo/           → TipoNavio, Navio, Tabuleiro, EstadoJogo  (lógica do jogo)
├── util/           → UtilNavio  (cálculo de células)
├── servidor/       → ServidorMain, GestorJogo  (servidor + árbitro)
└── cliente/        → ClienteMain, LigacaoServidor, InterfaceCLI  (cliente CLI)
```
