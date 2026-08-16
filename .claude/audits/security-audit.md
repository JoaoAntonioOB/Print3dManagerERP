## Auditoria de segurança — `security/**`, autorização (`@PreAuthorize`), upload STL/3MF, CORS/JWT/rate limit, armazenamento de token no frontend, dependências

### Achados

1. [ALTO] Segredo JWT com fallback silencioso ativo em **todos** os perfis, inclusive `prod` — sem fail-fast como as credenciais de banco
   - Onde: `backend/src/main/resources/application.yml:63` (`secret: ${JWT_SECRET:cHJpbnQzZC1tYW5hZ2VyLWVycC1kZXYtc2VjcmV0LWtleS0yNTYtYml0cy1taW5pbXVtLXNpemU=}`) e `docker-compose.yml:36` (`JWT_SECRET: ${JWT_SECRET:-cHJpbnQzZC1tYW5hZ2VyLWVycC1kZXYtc2VjcmV0LWtleS0yNTYtYml0cy1taW5pbXVtLXNpemU=}`) — o mesmo valor Base64 commitado no repositório em texto claro.
   - Cenário de exploração: um operador roda `docker compose up -d` num servidor real sem criar o `.env` (ou esquece a linha `JWT_SECRET`). O `SPRING_PROFILES_ACTIVE` já cai em `prod` por default no próprio compose, o boot **não falha** (diferente de `DATABASE_URL/USERNAME/PASSWORD`, que em `application-prod.yml` não têm default e derrubam o boot se ausentes), e a API sobe assinando tokens com a chave pública conhecida do repositório GitHub. Qualquer pessoa com acesso ao histórico do repo pode forjar um JWT válido com `role: ADMINISTRADOR` e `uid` de qualquer usuário, sem precisar de senha — bypass total de autenticação.
   - Recomendação: replicar em `application-prod.yml` o mesmo padrão já usado para o banco — `application.security.jwt.secret: ${JWT_SECRET}` **sem** valor default (fail-fast se ausente) — e remover o fallback do `JWT_SECRET` em `docker-compose.yml` (deixar sem `:-default`, forçando erro claro se `.env` não existir). É uma mudança de poucas linhas com alto retorno, dado que a Seção 3.6 do `PROJECT_CONTEXT.md` já promete "prod: tudo via env vars, sem defaults de segredos" — o código diverge dessa própria decisão para este segredo específico.
   - Agente sugerido para aplicar a correção: backend (yml) / devops (docker-compose.yml)

2. [ALTO] Postgres publicado diretamente no host com senha que também cai para um valor fraco conhecido se `.env` faltar
   - Onde: `docker-compose.yml:9-13` (`POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-print3d}`, `ports: ["5432:5432"]`).
   - Cenário de exploração: mesmo cenário de deploy sem `.env` do achado 1 — o Postgres fica acessível a partir de qualquer host que alcance a porta 5432 da máquina (não só da rede interna do Docker), autenticando com usuário/senha `print3d`/`print3d`, expondo clientes, financeiro e dados operacionais completos sem precisar passar pela API. Isso é mais grave que o achado da JWT porque nem exige forjar token — é acesso direto ao dado.
   - Recomendação: não publicar 5432 para o host em produção (o backend já alcança o Postgres pela rede interna `print3d-network`; bastaria remover o bloco `ports:` do serviço `postgres` ou restringi-lo a `127.0.0.1:5432:5432` para debug local). Reforçar no `.env.example`/README que a senha do banco deve ser trocada antes de qualquer deploy exposto à internet — já há um comentário nesse sentido, mas nada impede o boot com o default.
   - Agente sugerido para aplicar a correção: devops (docker-compose.yml) — encaminhar também para `database` por tratar de credencial/exposição do Postgres.

3. [MÉDIO] Reuso de refresh token detectado, mas não derruba as demais sessões da "família" do token
   - Onde: `backend/src/main/java/com/print3dmanager/erp/security/auth/AuthService.java:48-64` (método `refresh`).
   - Cenário de exploração: um refresh token é roubado (ex.: XSS, malware, log vazado — ver achado 5) e usado pelo atacante antes do usuário legítimo. O atacante recebe um novo par de tokens válido; quando o usuário legítimo tentar usar o token antigo (já revogado pela rotação do atacante), recebe 401 — o que o alerta de que algo está errado, mas **não existe nenhuma ação automática que revogue a sessão do atacante** nesse momento. O código já tem o método pronto para isso: `RefreshTokenRepository.revogarTodosDoUsuario(id)` (usado em desativação de usuário e troca de senha, `UserService.java:80` e `:107`).
   - Recomendação: em `AuthService.refresh`, quando `!atual.isValido()` for por token **já revogado** (reuso, não simples expiração), chamar `refreshTokenRepository.revogarTodosDoUsuario(usuario.getId())` antes de lançar o 401 — isso implementa a detecção de reuso completa (padrão OWASP de rotação de refresh token: reuso de token revogado é sinal de comprometimento e deve encerrar toda a família de sessões, não só rejeitar a chamada atual). Mudança pequena e localizada.
   - Agente sugerido para aplicar a correção: backend

4. [MÉDIO] `react-router-dom` na versão instalada tem advisory de severidade alta (`npm audit`)
   - Onde: `frontend/package.json` (`"react-router-dom": "^7.18.1"`, resolvendo dentro do intervalo vulnerável 7.12.0–7.18.1); confirmado via `npm audit` → GHSA-qwww-vcr4-c8h2 ("RSC Mode CSRF Bypass Allows Action Execution Before 400 Response").
   - Cenário de exploração: a vulnerabilidade é específica do **modo RSC** (React Server Components) do React Router, que este projeto não usa (é uma SPA Vite servida estaticamente pelo NGINX, sem framework de RSC) — por isso a exploração prática aqui é baixa/nula. Ainda assim, é uma versão com CVE aberto rodando em produção, e a correção é trivial.
   - Recomendação: `npm audit fix` (ou bump manual para a versão corrigida) no `frontend/`; revalidar `npm run build`/`lint` depois.
   - Agente sugerido para aplicar a correção: frontend

5. [BAIXO] Ausência de `Content-Security-Policy` no NGINX, agravando o impacto de um eventual XSS dado que os tokens ficam em `localStorage`
   - Onde: `frontend/nginx.conf:16-19` (tem `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, mas nenhum `Content-Security-Policy`) e `frontend/src/auth/auth-storage.ts:8-18` (access token **e** refresh token gravados em `localStorage`, alcançáveis por qualquer JavaScript executado no domínio).
   - Cenário de exploração: como não há sessão baseada em cookie `httpOnly` (decisão consciente e correta para uma API stateless com Bearer token — não é o achado em si), qualquer XSS bem-sucedido (ex.: dependência de terceiro comprometida, campo de texto livre renderizado sem sanitização em alguma tela futura) consegue ler `print3d.accessToken` **e** `print3d.refreshToken` diretamente do `localStorage` e exfiltrá-los, resultando em sequestro de sessão persistente (refresh token válido por 7 dias, com rotação — mas a rotação não impede o roubo inicial). Uma CSP restritiva (`script-src 'self'`, sem `unsafe-inline`) reduziria drasticamente a superfície de XSS que chega a esse ponto.
   - Recomendação: adicionar `Content-Security-Policy` no `nginx.conf` (mínimo: `default-src 'self'; connect-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'` — MUI costuma exigir `unsafe-inline` em estilos, mas não em scripts). Migrar a guarda de tokens para cookie `httpOnly`/`SameSite=Strict` é uma mudança maior de arquitetura (contrato de API, CORS, refresh flow) — fica registrada como opção futura, não como correção obrigatória desta rodada, já que o escopo pede para apontar o risco e não redesenhar o frontend.
   - Agente sugerido para aplicar a correção: devops (CSP no nginx.conf) — mudança de storage do token seria backend+frontend, se decidido futuramente.

6. [BAIXO] Backend publicado diretamente no host (`8080:8080`), fora do NGINX
   - Onde: `docker-compose.yml:41-42` (serviço `backend`, além do serviço `frontend` na porta 80).
   - Cenário de exploração: qualquer cliente que alcance a porta 8080 da máquina acessa a API sem passar pelos cabeçalhos de segurança do NGINX (`X-Frame-Options` etc. não fazem diferença para chamadas de API puras, mas ainda é uma segunda porta de entrada para inventariar/monitorar). **Não** encontrei um vetor de bypass do rate limiting por spoofing de `X-Forwarded-For` nessa porta: o `RemoteIpValve` só confia no cabeçalho quando o peer TCP direto está nas faixas privadas configuradas (`server.tomcat.remoteip`, `application.yml:38-41`), e tráfego genuinamente externo direto a 8080 chega com o IP real do atacante como peer, então o valve não substitui `getRemoteAddr()` — o rate limit por IP continua correto. Ainda assim, expor a porta é desnecessário: só o `frontend`/NGINX precisa alcançar o backend, e isso já acontece pela rede interna `print3d-network`.
   - Recomendação: remover o mapeamento `ports: ["8080:8080"]` do serviço `backend` em produção (manter acessível só via NGINX `/api`), ou restringir a `127.0.0.1:8080:8080` quando o acesso direto for necessário para debug local.
   - Agente sugerido para aplicar a correção: devops

7. [BAIXO] Janela fixa do rate limiter permite rajada de até ~2× o limite na fronteira da janela
   - Onde: `backend/src/main/java/com/print3dmanager/erp/security/ratelimit/RateLimitService.java:35-45` (`permitir` — janela fixa por `Clock`, não *sliding window*/*token bucket*).
   - Cenário de exploração: um atacante de força bruta em `/auth/login` pode disparar 10 tentativas no último segundo da janela corrente e mais 10 no primeiro segundo da janela seguinte — 20 tentativas em ~1-2s, o dobro do limite nominal (`RATE_LIMIT_AUTH=10`/60s). Isso não anula a proteção (ainda limita ordens de grandeza de força bruta), mas é uma fraqueza conhecida de contadores de janela fixa frente a *sliding window log* ou *token bucket*.
   - Recomendação: opcional para o escopo atual (TCC/instância única, login já protegido por BCrypt + refresh rotation); se quiser endurecer, trocar para janela deslizante (guardar timestamps recentes por chave, ou usar dois contadores de janela fixa consecutivos com peso proporcional ao tempo decorrido). Não bloqueante.
   - Agente sugerido para aplicar a correção: backend

8. [BAIXO] Sem verificação automatizada de CVEs de dependências no pipeline (Maven e npm)
   - Onde: `backend/pom.xml` (nenhum plugin `dependency-check`/similar configurado) e ausência de `.github/` (sem Dependabot/CI configurado no repo).
   - Cenário de exploração: não é uma vulnerabilidade em si — é a lacuna de processo que deixou o achado 4 (`react-router-dom`) só visível porque rodei `npm audit` manualmente nesta auditoria; o lado Maven não tem ferramenta equivalente configurada para eu nem rodar (`org.owasp:dependency-check-maven` não está no `pom.xml`, e uma execução ad-hoc exigiria acesso à NVD e tempo fora do escopo desta análise pontual).
   - Recomendação: habilitar Dependabot (ou `npm audit`/`mvn org.owasp:dependency-check-maven:check` em CI) para pegar CVEs novos automaticamente em vez de depender de auditorias manuais esporádicas.
   - Agente sugerido para aplicar a correção: devops

9. [BAIXO] Política de senha com mínimo de 6 caracteres
   - Onde: `backend/src/main/java/com/print3dmanager/erp/user/dto/UserCreateRequest.java:23` e `ChangePasswordRequest.java:14` (`@Size(min = 6, max = 100)`).
   - Cenário de exploração: senhas de 6 caracteres (ex.: `123456`) passam na validação; combinado com BCrypt (que já mitiga ataque offline por força bruta pelo custo computacional), o risco real é senhas triviais serem adivinhadas online — mas o rate limit de `/auth/login` (10/min por IP) já limita bastante esse vetor. Para um ERP com dados financeiros/de clientes e "ambição comercial", vale um mínimo maior.
   - Recomendação: subir o mínimo para 8-10 caracteres; opcionalmente medidor de força no frontend (cosmético, não bloqueante).
   - Agente sugerido para aplicar a correção: backend

10. [BAIXO] Veredito formal sobre upload STL/3MF sem inspeção de magic bytes (encaminhado por `printing3d`) — **aceitável como está**, com recomendação de hardening não bloqueante
    - Onde: `backend/src/main/java/com/print3dmanager/erp/order/service/OrderItemFileService.java:134-149` (`validarESanitizar` — só checa extensão contra `{stl, 3mf}`).
    - Análise: os fatores que reduzem o risco a um nível aceitável são todos confirmados no código, não presumidos — (a) upload exige `ADMINISTRADOR`/`OPERADOR` autenticados (`OrderItemFileController.java:38-46`, `PODE_GERENCIAR`), não é endpoint público; (b) download também exige autenticação interna (`PODE_CONSULTAR`, nenhum papel `CLIENTE`/anônimo alcança); (c) o `Content-Type` da resposta de download **nunca** reflete dado do upload — vem de um mapa fixo por extensão (`CONTENT_TYPES`, linha 40-42), então não há confusão de tipo MIME nem chance de o navegador renderizar o arquivo como HTML/script; (d) `Content-Disposition: attachment` é sempre forçado (`OrderItemFileController.java:71-73`), então o navegador nunca tenta exibir o conteúdo inline; (e) path traversal é bloqueado por `resolverSeguro` (linhas 178-184); (f) o `Content-Type` declarado pelo cliente no multipart não seria mais confiável que a extensão mesmo se checado (é 100% controlado pelo atacante) — só magic bytes reais agregariam garantia adicional.
    - Veredito: **aceitável como está** para o vetor de navegador (não há caminho de XSS/sniffing de conteúdo explorável a partir desse endpoint). Severidade **BAIXO**, não MÉDIO — rebaixo a classificação inicial de `printing3d` porque, ao inspecionar os dois lados do fluxo (upload E download) juntos, as mitigações já neutralizam o vetor mais realista (execução no navegador). O risco residual é apenas "a ERP vira um lugar onde um usuário interno já autorizado pode gravar bytes arbitrários com nome `.stl`/`.3mf`" — que exige que esse usuário já tenha `OPERADOR`/`ADMINISTRADOR`, ou seja, já é confiável pelo modelo de ameaça do sistema.
    - Recomendação (hardening opcional, não bloqueante): checagem leve de estrutura antes de gravar — STL binário começa com um header de 80 bytes seguido de `uint32` de triângulos coerente com o tamanho do arquivo (`84 + 50×N` bytes), STL ASCII começa com `solid`, e 3MF é um ZIP válido contendo `[Content_Types].xml`. Rejeitar o que não bater reduz o uso do endpoint como "drop" de arquivo genérico e pega uploads corrompidos cedo (ganho de UX também, não só segurança).
    - Agente sugerido para aplicar a correção (se decidido implementar o hardening opcional): backend

### Veredito sobre rate limiting em memória (encaminhado por `backend`)

Confirmado: a decisão documentada em `PROJECT_CONTEXT.md` ("`RateLimitService`... em memória, adequado à instância única; cluster exigiria Redis") **continua adequada** para o cenário atual do projeto — Docker Compose de instância única, sem orquestração horizontal. Não é reclassificado como achado de severidade alta; mantido como **INFORMATIVO** (decisão consciente, correta para o contexto). O único ponto técnico adicional que localizei foi o comportamento de janela fixa (achado 7 acima, BAIXO) — distinto da limitação "não escala para cluster" já documentada, então reportado separadamente em vez de reafirmar o mesmo achado do `backend-audit.md`.

### Sem achados nesta área

- `SecurityConfig` (`backend/.../security/SecurityConfig.java`): chain stateless, CSRF desabilitado corretamente (não há cookie de sessão para forjar), rotas públicas restritas a `/auth/**`, `/public/**`, Swagger e `/actuator/health|info` — nenhuma rota de negócio ficou public por engano.
- `JwtAuthenticationFilter`: token inválido/expirado apenas segue sem autenticar (não lança exceção capturada incorretamente); usuário **recarregado do banco a cada requisição** — desativação tem efeito imediato mesmo com access token ainda válido (confirmado por leitura direta do filtro).
- Mensagens de erro de autenticação: `GlobalExceptionHandler.handleCredenciaisInvalidas` usa mensagem genérica ("E-mail ou senha inválidos") especificamente em `/auth/login`, evitando enumeração de e-mails cadastrados; `DatabaseUserDetailsService` lança `UsernameNotFoundException` com mensagem detalhada, mas ela nunca vaza ao cliente porque o Spring Security a converte em `BadCredentialsException` antes do handler.
- Revogação de sessão: desativação de usuário (`UserService.desativar`) e troca de senha (`UserService.trocarSenha`) chamam `refreshTokenRepository.revogarTodosDoUsuario`, cortando acesso imediatamente — confirmado por leitura direta, consistente com a Etapa 7 documentada.
- Path traversal em upload/download (`OrderItemFileService.resolverSeguro`): `raiz.resolve(relativo).normalize()` + `startsWith(raiz)` barra corretamente `../`; nome gravado em disco é sempre o sanitizado (sem `/`, `\`, fora de `[a-zA-Z0-9._-]`) — não depende de `resolverSeguro` para ser seguro na escrita, só na leitura/remoção, e ambos os casos foram conferidos.
- IDOR em item de pedido: `OrderItemFileService.obterItemDoPedido` valida que o item pertence ao `pedidoId` da URL antes de qualquer operação — item de outro pedido dá 404, não vaza dado nem permite acesso cruzado.
- `@PreAuthorize` amostrado em `OrderController`, `PrintHistoryController`, `OrderItemFileController`, `ClientController`: todos os endpoints têm anotação explícita método a método (sem herança de classe), coerente com as constantes `PODE_GERENCIAR`/`PODE_CONSULTAR` documentadas — cruzado com a conclusão de 100% de cobertura do `backend-audit.md`, sem divergência encontrada nos controllers reamostrados por este agente.
- CORS (`SecurityConfig.corsConfigurationSource`): origens vêm de lista configurável (`CorsProperties`, sem wildcard), `allowCredentials(true)` combinado com lista explícita de origens (não wildcard) — configuração correta, sem a armadilha comum de `*` + credentials.
- BCrypt (`PasswordEncoder` bean) para hash de senha; nenhum DTO de resposta (`UserResponse` etc.) expõe o campo `senha`; nenhuma entidade `User` usa `@ToString`/`@Data` que pudesse vazar o hash em logs.
- Refresh token: rotação confirmada (`AuthService.refresh` revoga o token usado e emite novo par), token opaco (UUID, não JWT) armazenado no banco com `expiraEm`/`revogado`, `isValido()` checa os dois. Login/refresh não vazam se o e-mail existe.
- Segredos do banco em `prod`: `application-prod.yml` exige `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` sem nenhum default — falha rápido se ausentes, ao contrário do JWT (achado 1). Esse é o padrão correto que deveria ser replicado para o JWT.
- Link público de orçamento (`shareToken`): UUID de 128 bits, não enumerável por força bruta em prazo viável mesmo sem rate limit; rate limit de `/public/**` (30/min/IP) já documentado e adequado como camada extra.
- `DefaultAdminPasswordWarner`: decisão de logar (não bloquear) já documentada e justificada para ambiente de avaliação do TCC; comportamento conferido e consistente com a documentação — nenhuma surpresa.

---

**Arquivos/caminhos relevantes citados nesta auditoria** (todos absolutos):
- `C:\repository\Print3d Manager ERP\backend\src\main\resources\application.yml`
- `C:\repository\Print3d Manager ERP\backend\src\main\resources\application-prod.yml`
- `C:\repository\Print3d Manager ERP\docker-compose.yml`
- `C:\repository\Print3d Manager ERP\.env.example`
- `C:\repository\Print3d Manager ERP\backend\src\main\java\com\print3dmanager\erp\security\auth\AuthService.java`
- `C:\repository\Print3d Manager ERP\backend\src\main\java\com\print3dmanager\erp\security\ratelimit\RateLimitService.java`
- `C:\repository\Print3d Manager ERP\backend\src\main\java\com\print3dmanager\erp\order\service\OrderItemFileService.java`
- `C:\repository\Print3d Manager ERP\backend\src\main\java\com\print3dmanager\erp\order\controller\OrderItemFileController.java`
- `C:\repository\Print3d Manager ERP\frontend\src\auth\auth-storage.ts`
- `C:\repository\Print3d Manager ERP\frontend\nginx.conf`
- `C:\repository\Print3d Manager ERP\frontend\package.json`
- `C:\repository\Print3d Manager ERP\backend\src\main\java\com\print3dmanager\erp\user\dto\UserCreateRequest.java`
