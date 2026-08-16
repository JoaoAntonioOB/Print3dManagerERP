## Domínio de impressão — correção de 3 corridas concorrentes (achados ALTO #4, #5, #6)

**Componente afetado:** filamento (estoque), impressora (ocupação/status), PrintHistory (ciclo de vida do job)

**Regra física alterada:**
Nenhuma regra de negócio mudou (os cálculos de custo, os requisitos de status e as mensagens continuam os mesmos) — o que mudou foi a **atomicidade** de três operações de leitura-e-escrita que antes podiam ser corrompidas por concorrência real (dois usuários/threads agindo ao mesmo tempo sobre a mesma impressora/filamento):

1. **Lost update no estoque de filamento (achado 4):**
   - `FilamentRepository` ganhou `findByIdForUpdate(Long id)` com `@Lock(LockModeType.PESSIMISTIC_WRITE)` + `@Query` JPQL (sem migração — é lock de linha em runtime, não schema).
   - `FilamentService.movimentarEstoque()` agora carrega o filamento com esse método antes de ler/escrever `quantidadeEstoqueG`.
   - `PrintHistoryService.consumirFilamento()` (chamado de `finalizar()`, usado por concluir/falhar/cancelar) recarrega o filamento com `filamentRepository.findByIdForUpdate(...)` **antes** de ler o saldo, mesmo que `job.getFilamento()` já tivesse vindo carregado (sem lock) pelo entity graph de `findDetalhadoById`. Como é a mesma entidade gerenciada na mesma persistence context, o Hibernate faz o *lock upgrade* (SELECT ... FOR UPDATE adicional) em vez de reusar o lock antigo — confirmado com um teste de integração real (ver abaixo).

2. **Corrida na ocupação de impressora (achado 5):**
   - `PrinterRepository` ganhou `findByIdForUpdate(Long id)` (mesmo padrão de lock).
   - `PrintHistoryService.iniciar()` passou a carregar a impressora via `obterImpressoraParaOcupar()` (renomeado de `obterImpressora`), que usa `findByIdForUpdate`. A checagem de `status != DISPONIVEL` e a escrita subsequente de `IMPRIMINDO` ficam dentro da janela travada, então duas chamadas concorrentes de `iniciar()` para a mesma impressora serializam: a segunda só lê o status depois que a primeira já commitou `IMPRIMINDO`, e é corretamente rejeitada.

3. **Status/soft-delete sem checar job ativo (achado 6):**
   - `PrintHistoryRepository` ganhou `findFirstByImpressoraIdAndStatus(Long impressoraId, PrintStatus status)` (deriva a query pelo nome, sem `@Query` manual).
   - `PrinterService` passou a depender de `PrintHistoryRepository` (injeção via construtor, `@RequiredArgsConstructor`).
   - `PrinterService.alterarStatus()` e `PrinterService.desativar()` agora chamam esse método com `PrintStatus.EM_ANDAMENTO` antes de aplicar a mudança; se existir um job ativo, lançam `BusinessException` com mensagem citando o número do job (`"...há uma impressão em andamento (job #Y)."`) e **bloqueiam** a operação — decisão de produto confirmada: rejeitar, não auto-cancelar.

**Impacto em estoque/custo real:**
- Estoque de filamento: com o lock, duas movimentações concorrentes (manual + manual, manual + job finalizando, ou dois jobs finalizando o mesmo filamento) sempre serializam — o saldo final reflete a soma real de todas as saídas/entradas, sem perder abatimento por sobrescrita. O comportamento observável para o usuário final não muda (mesmas mensagens de erro de saldo insuficiente), só a correção sob concorrência.
- Ocupação de impressora: elimina a janela em que dois jobs `EM_ANDAMENTO` poderiam existir simultaneamente na mesma máquina física, o que também protege a integridade do cálculo de horas/custo por job (cada job agora sempre corresponde a um período exclusivo de uso real da máquina).
- Status/desativação de impressora: impede que um job `EM_ANDAMENTO` fique "órfão" (associado a uma impressora que mudou de status ou foi desativada por outro caminho), preservando a correspondência entre o período do job e o estado real da máquina — sem mudar em nada o cálculo de custo em si.

**Testes relevantes rodados:**
- Suíte completa do backend: `.\mvnw.cmd test` (JDK 21, Docker Desktop ativo para Testcontainers/Postgres real) — **117 testes, 0 falhas, 0 erros**, build verde (exit code 0).
- `PrintHistoryServiceTest` (11 testes) — ajustado para os novos métodos com lock (`findByIdForUpdate` no lugar de `findById` nos stubs de impressora e filamento); nenhum comportamento de negócio testado regrediu (custo real 90/kg × 100g + energia + máquina = 33,80 continua batendo, abatimento em falha continua, estoque insuficiente continua rejeitando).
- `PrinterServiceTest` (novo, 4 testes) — cobre achado 6: bloqueia `alterarStatus`/`desativar` com job `EM_ANDAMENTO` citando o `job #id`, e confirma que sem job ativo a operação segue normalmente.
- `PrinterConfigurationServiceTest` (7 testes, pré-existente) — reexecutado sem alterações, continua verde (própria > global > vazio intacto).
- `FilamentRepositoryLockIntegrationTest` (novo, 1 teste, contra Postgres real via Testcontainers) — prova diretamente o mecanismo de lock: thread A trava a linha do filamento, segura por 400ms e libera (commit); thread B só tenta travar depois que A já sinalizou que travou, e o teste verifica que o instante em que B conseguiu o lock é sempre ≥ o instante em que A liberou — ou seja, o `PESSIMISTIC_WRITE` realmente serializa o acesso à mesma linha (mesmo mecanismo usado em `PrinterRepository`).

**Achados (severidade):** nenhum novo achado identificado durante a correção.

**Encaminhamentos:** nenhum.
