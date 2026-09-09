# SPRINT 3 - CYBERSECURITY - FordRetain

**Projeto:** FordRetain API - Ford FIAP Challenge 2026  
**Repositório analisado:** `Matheus-Bortolotto/fordretain-api-Java`  
**Base técnica:** Java 17, Spring Boot, Oracle, JWT, Spring Security, Docker e GitHub Actions.

## Integrantes
- Fernanda Rocha Menon - RM 554673
- Luiza Macena Dantas - RM 556237
- Luan Ramos Garcia de Souza - RM 558537
- Matheus Ricciotti - RM 556930
- Matheus Bortolotto - RM 555189

## Diagnóstico inicial do repositório

O repositório já possuía uma base de segurança relevante: autenticação JWT, RBAC com `ADMIN`, `GERENTE` e `ANALISTA`, validação Jakarta Bean Validation, rate limiting com Bucket4j, logs de auditoria, criptografia AES-GCM do telefone do cliente, Docker multi-stage/non-root, Dependabot e pipeline com CodeQL, Gitleaks e Trivy.

Na execução de GitHub Actions observada em 08/09/2026, o SAST/CodeQL e o Secret Scanning/Gitleaks concluíram com sucesso. O build falhou em uma PR do Dependabot porque secrets de repositório não são disponibilizados nesse contexto e `JWT_SECRET` chegou vazio aos testes. O SCA por `dependency-review-action` também falhou porque o Dependency Graph do repositório não estava habilitado. Como o job de container dependia do build, ele foi ignorado. Esta Sprint corrige esses dois pontos de integração.

---

# Atividade 1 - Pipeline DevSecOps Integrado (Peso 3,0)

## Pipeline proposto

`Commit/PR -> Build + Testes -> SAST CodeQL -> SCA Trivy FS + Dependabot -> Gitleaks -> Docker Build -> Trivy Container -> Security Gate -> Deploy`

### Etapas e redução de risco

| Etapa | Ferramenta | Risco reduzido |
|---|---|---|
| Build/Test | Maven + JUnit/MockMvc | Regressões funcionais e de autorização |
| SAST | CodeQL | Padrões vulneráveis no código antes do merge |
| SCA | Trivy FS + Dependabot | Dependências vulneráveis e componentes desatualizados |
| Secret Scanning | Gitleaks | Senhas/tokens/chaves versionadas por acidente |
| Container Security | Trivy Image | CVEs do SO/JRE/JAR e dependências empacotadas |
| Security Gate | GitHub Actions | Impede promoção quando existe falha crítica |

### Correções reais aplicadas
1. Testes recebem segredos exclusivamente de teste em `src/test/resources/application.properties`, portanto PRs do Dependabot deixam de depender de secrets de produção.
2. O SCA do pipeline deixa de depender do Dependency Graph e usa Trivy filesystem; o Dependabot continua fazendo atualização automática semanal.
3. Trivy passa a bloquear vulnerabilidades **CRITICAL**; HIGH permanece visível para correção com SLA definido.
4. Actions de checkout/setup-java foram atualizadas para versões atuais, eliminando o warning de runtime Node antigo observado na execução.

---

# Atividade 2 - Segurança em Código e Infraestrutura (Peso 2,5)

## Criptografia local
O código existente usa AES/GCM/NoPadding com chave de 256 bits derivada por PBKDF2 e IV aleatório por mensagem para proteger telefone do cliente no banco. `CRYPTO_SECRET` e `CRYPTO_SALT` continuam externos ao repositório.

## API hardening
- JWT assinado explicitamente com HS512 e chave mínima de 64 bytes.
- Expiração padrão reduzida para 1 hora e parametrizável por `JWT_EXPIRATION_MS`.
- Rate limit geral de 60 req/min/IP e limite mais restrito no login (10/min/IP), ambos configuráveis.
- Resposta 429 contém `Retry-After`.
- CORS deixou de usar wildcard e passa a ser definido por `CORS_ALLOWED_ORIGINS`.
- DTOs do projeto já aplicam `@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@Min` e `@Max`.
- Mensagens de autenticação não revelam se um e-mail existe.
- Logs de login usam e-mail mascarado.

## RBAC
A separação de perfis é aderente ao domínio FordRetain:
- ADMIN: acesso completo, inclusive DELETE e Actuator.
- GERENTE: predição, dashboard, leitura e atualização.
- ANALISTA: consultas e leads, sem operações administrativas.

Os perfis `Brigadista/Gestor/Administrador` do enunciado são exemplos; no FordRetain os equivalentes de negócio são `ANALISTA/GERENTE/ADMIN`.

## IaC Security
O Dockerfile do projeto já executa a aplicação com usuário não-root. A Sprint adiciona `k8s/fordretain-api.yaml` com `runAsNonRoot`, `allowPrivilegeEscalation: false`, filesystem somente leitura, drop de capabilities, seccomp, probes, limites de CPU/memória e secrets por referência.

## MQTT/TLS
**Não aplicável ao repositório FordRetain API.** Não existe módulo MQTT ou dispositivo IoT neste projeto. Não foi criada uma evidência fictícia. Caso um módulo IoT seja incluído futuramente, o requisito deve usar MQTT sobre TLS com autenticação mútua/certificados e rotação de credenciais.

---

# Atividade 3 - Observabilidade, Monitoramento e Resposta (Peso 2,0)

## Logs estruturados
Foi adicionado `logback-spring.xml` com saída JSON e MDC. Cada requisição auditada inclui `request_id`, método, path, status HTTP, duração, IP e principal mascarado. Eventos 401/403 geram evento `access_denied`.

Exemplo de log (formato ilustrativo):
```json
{"service":"fordretain-api","event":"access_denied","request_id":"...","http_method":"DELETE","http_path":"/api/v1/clientes/42","http_status":"403","principal":"a***@ford.com"}
```

## Métricas
Micrometer + Prometheus expõem, além das métricas padrão HTTP/JVM:
- `fordretain_security_login_total{result="success|failure"}`
- `fordretain_security_access_denied_total`
- `fordretain_security_rate_limited_total`

## Dashboard e alertas
A pasta `monitoring/` contém Prometheus, regras de alertas e dashboard provisionado do Grafana. Painéis: falhas de login, 401/403, rate limit, HTTP 5xx, requisições por status e latência p95.

Alertas propostos:
- excesso de HTTP 5xx;
- volume anormal de 401/403;
- pico de rate limiting;
- muitas falhas de login.

### Mobile, ML e IoT
- Mobile: monitoramento do backend cobre erros/latência das chamadas; crash/performance do app deve ser instrumentado no repositório mobile.
- ML: o README define o microserviço de ML como futuro. Quando integrado, incluir latência, erro, drift e distribuição dos perfis preditos.
- IoT: não aplicável ao FordRetain.

## Resposta a incidentes
1. **Detecção:** alerta, log estruturado ou falha de segurança no pipeline.
2. **Análise:** correlacionar request_id, usuário mascarado, endpoint, IP e timeline.
3. **Contenção:** revogar token/sessão, restringir rota/perfil, bloquear origem no WAF/gateway, pausar deploy.
4. **Erradicação:** corrigir vulnerabilidade, atualizar dependência, rotacionar segredo e revisar permissões.
5. **Recuperação:** restaurar serviço/backups, validar integridade, acompanhar métricas e registrar lições aprendidas.

---

# Atividade 4 - Compliance, Riscos e Segurança Contínua (Peso 2,5)

## STRIDE final
| Categoria | Cenário FordRetain | Controle |
|---|---|---|
| Spoofing | Roubo/forja de identidade | BCrypt, JWT HS512, expiração, rate limit |
| Tampering | Alteração de payload ou registro | Bean Validation, RBAC, prepared statements, auditoria |
| Repudiation | Usuário negar ação crítica | logs JSON, request_id, status, principal mascarado |
| Information Disclosure | Exposição de telefone/e-mail | AES-GCM, minimização de logs, acesso por perfil |
| Denial of Service | Flood de API/login | Bucket4j, limite específico de login, alertas |
| Elevation of Privilege | Analista executar ação de gerente/admin | regras RBAC por método/endpoint |

## Mapeamento OWASP
- **OWASP ASVS:** autenticação, gerenciamento de sessão/token, controle de acesso, validação, criptografia, logging e configuração segura.
- **OWASP API Top 10:** foco em Broken Object/Function Level Authorization, Broken Authentication, Unrestricted Resource Consumption, Security Misconfiguration e inventário/documentação de APIs.
- **OWASP Mobile Top 10:** controles de API apoiam autenticação, autorização e transporte seguro, porém armazenamento local, pinning, obfuscação e hardening do aplicativo precisam ser verificados no repositório mobile.

## LGPD
Dados pessoais tratados no FordRetain incluem, pelo menos, nome, e-mail e telefone. Controles aplicados/propostos: finalidade definida, minimização, criptografia de campo sensível, logs sem senha/token, mascaramento do e-mail nos logs, RBAC, retenção definida, backup seguro e resposta a incidentes.

## Plano de segurança contínua
| Rotina | Frequência | Evidência |
|---|---|---|
| Dependabot | semanal | PRs de dependência |
| SAST/SCA/Gitleaks/Trivy | todo push/PR | GitHub Actions |
| Revisão de permissões | mensal | checklist RBAC |
| Revisão de vulnerabilidades HIGH | semanal | issue/backlog + SLA |
| Teste de restauração de backup | mensal | ata/log de restore |
| Rotação de segredos | trimestral ou após incidente | registro operacional |
| Threat modeling STRIDE | a cada release relevante | documento atualizado |

## Checklist de conformidade
- [x] SAST no CI/CD
- [x] SCA no CI/CD + Dependabot
- [x] Secret scanning
- [x] Container scanning
- [x] Testes automatizados sem secrets de produção
- [x] Criptografia local
- [x] JWT e RBAC
- [x] Validação de entrada
- [x] Rate limiting
- [x] CORS restrito por ambiente
- [x] Logs estruturados
- [x] Métricas Prometheus
- [x] Dashboard Grafana provisionado
- [x] Alertas de segurança
- [x] Plano de resposta a incidentes
- [x] STRIDE revisado
- [x] OWASP ASVS / API / Mobile mapeados
- [x] LGPD mapeada
- [x] IaC com boas práticas
- [ ] Evidência de execução pós-merge: deve ser capturada após a branch desta Sprint rodar no GitHub Actions.
- [ ] MQTT/TLS: não aplicável ao escopo atual.

## Como executar a observabilidade localmente
1. Suba a API na porta 8080 com as variáveis de ambiente obrigatórias.
2. Na pasta `monitoring`, copie `.env.example` para `.env` e troque a senha do Grafana.
3. Execute `docker compose -f docker-compose.monitoring.yml up -d`.
4. Prometheus: porta 9090. Grafana: porta 3000.
5. O dashboard `FordRetain - Segurança e Observabilidade` é carregado automaticamente.

## Critério para deploy
O deploy só deve ocorrer após build/test, CodeQL, SCA, Gitleaks e Trivy concluírem os gates definidos. O repositório ainda não possui um ambiente de cloud/Kubernetes configurado no GitHub; por isso a automação desta Sprint termina no **gate de aprovação para deploy** e inclui o manifesto Kubernetes seguro para o próximo estágio.
