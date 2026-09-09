# SPRINT 3 — CYBERSECURITY — FordRetain

**Projeto:** FordRetain API — Ford FIAP Challenge 2026  
**Escopo deste repositório:** API Java, pipeline DevSecOps, Docker/Kubernetes e observabilidade  
**Base técnica:** Java 17, Spring Boot 3.5.16, Spring Security, JWT, Oracle, Docker, Kubernetes, Prometheus, Grafana e GitHub Actions  
**Documento único da entrega:** este arquivo reúne as quatro atividades exigidas na rubrica.

## Integrantes

- Fernanda Rocha Menon — RM 554673
- Luiza Macena Dantas — RM 556237
- Luan Ramos Garcia de Souza — RM 558537
- Matheus Ricciotti — RM 556930
- Matheus Bortolotto — RM 555189

---

# Visão geral da solução

A Sprint 3 evolui a segurança do FordRetain para um modelo **DevSecOps**, em que controles de segurança deixam de existir apenas como documentação e passam a fazer parte do ciclo de desenvolvimento, build, análise, empacotamento e aprovação para deploy.

Fluxo implantado:

```text
Commit / Pull Request
        |
        +--> Build + Testes + SBOM CycloneDX
        +--> SAST / CodeQL
        +--> SCA / Trivy FS + Dependabot
        +--> Secret Scanning / Gitleaks
        +--> IaC Security / Trivy Config
        +--> Docker Build + Trivy Image
                         |
                         v
                  Security Gate
                         |
                aprovado / bloqueado
                         |
                     Deploy
```

A execução real da **PR #18**, workflow **Security Pipeline**, run **20**, validou com sucesso todos os jobs obrigatórios: Build/Testes/SBOM, SAST/CodeQL, SCA/Trivy, Gitleaks, IaC/Trivy, Container/Trivy e Gate final para deploy.

---

# Atividade 1 — Pipeline DevSecOps Integrado (Peso 3,0)

## Objetivo

Demonstrar como a segurança é incorporada ao pipeline desde o commit/PR até o estágio de aprovação para deploy.

## Arquivo principal

`.github/workflows/security-pipeline.yml`

## Etapas implementadas

| Etapa | Ferramenta | Função | Risco reduzido |
|---|---|---|---|
| Build e testes | Maven | Compila e valida o projeto | regressão, erro de build e quebra de controles |
| SBOM | CycloneDX | Gera inventário de componentes | falta de rastreabilidade de dependências |
| SAST | CodeQL | Analisa padrões vulneráveis no código | falhas de implementação e fluxo de dados inseguro |
| SCA | Trivy FS | Analisa dependências e componentes | CVEs conhecidos em bibliotecas |
| Atualização contínua | Dependabot | Abre PRs de atualização | dependências desatualizadas |
| Secret scanning | Gitleaks | Procura credenciais no histórico | vazamento de senha, token ou chave |
| IaC Security | Trivy Config | Analisa Docker/IaC | configuração insegura de infraestrutura |
| Container Security | Trivy Image | Analisa SO/JRE/JAR da imagem | CVEs empacotados no artefato final |
| Security Gate | GitHub Actions | Bloqueia promoção em falha crítica | deploy de artefato inseguro |

## Security gates

O pipeline produz relatório para severidades **HIGH** e **CRITICAL**, mas o gate automático é configurado para falhar quando encontra vulnerabilidade **CRITICAL** corrigível. Isso mantém visibilidade dos HIGH e impede que o artefato avance quando existe risco crítico conhecido.

O pipeline também aplica timeouts por job e permissões mínimas do `GITHUB_TOKEN` (`contents: read` e `security-events: write`).

## Correção real realizada durante a Sprint

A execução anterior do pipeline em `main` identificou vulnerabilidades críticas dentro da imagem, especialmente no stack antigo baseado em Spring Boot 3.2.0/Tomcat. Em vez de desabilitar ou enfraquecer o Trivy, a Sprint corrigiu a causa:

1. Spring Boot atualizado de **3.2.0 para 3.5.16**.
2. Springdoc atualizado para **2.8.17**.
3. JJWT atualizado para **0.12.6**.
4. O Tomcat embutido foi removido do artefato e substituído por **Undertow**, mantendo Spring MVC/Servlet.
5. A imagem Alpine executa atualização dos pacotes de segurança antes de rodar a aplicação.
6. O gate de vulnerabilidade crítica foi mantido ativo.

Resultado: na execução real da PR #18, o job **Container Security — Trivy** e seu security gate concluíram com sucesso.

## SBOM

O plugin CycloneDX é executado na fase `verify` do Maven e gera:

`target/bom.json`

No GitHub Actions, o arquivo é publicado como artefato:

`fordretain-sbom-cyclonedx`

A SBOM melhora rastreabilidade, auditoria e resposta a vulnerabilidades, pois permite identificar quais componentes fazem parte do build entregue.

## Evidências desta atividade

- `.github/workflows/security-pipeline.yml`
- `.github/dependabot.yml`
- `pom.xml`
- `Dockerfile`
- PR #18 / Security Pipeline / run 20
- prints indicados em `evidencias/README.md`

---

# Atividade 2 — Segurança em Código e Infraestrutura (Peso 2,5)

## Objetivo

Evidenciar práticas de segurança aplicadas diretamente no código e nos artefatos de infraestrutura.

## 2.1 Criptografia local

O projeto utiliza criptografia **AES-GCM** para proteção de campo sensível. O serviço implementado usa:

- AES em modo GCM;
- IV aleatório por operação;
- autenticação de integridade fornecida pelo GCM;
- segredo e salt externos ao código;
- ausência de chave hardcoded no repositório.

Variáveis relacionadas:

```text
CRYPTO_SECRET
CRYPTO_SALT
```

Evidência: `src/main/java/com/ford/fordretain/security/CryptoUtils.java`.

## 2.2 JWT seguro

O serviço JWT aplica:

- assinatura HS512;
- chave mínima de 64 bytes;
- data de emissão;
- expiração configurável;
- claim de perfil/role;
- verificação de assinatura e expiração;
- segredo obtido por variável de ambiente.

Variável:

```text
JWT_SECRET
```

O valor real nunca deve ser versionado.

Evidência: `src/main/java/com/ford/fordretain/security/JwtService.java`.

## 2.3 Validação de entrada

O DTO de login utiliza Bean Validation:

- e-mail obrigatório;
- formato válido de e-mail;
- tamanho máximo do e-mail;
- senha obrigatória;
- limite mínimo e máximo de senha.

O limite máximo evita payload excessivo e reduz riscos de abuso no processamento de credenciais.

Evidência: `src/main/java/com/ford/fordretain/dto/LoginRequestDTO.java`.

## 2.4 Rate limiting

O `RateLimitFilter` usa Bucket4j e mantém dois limites configuráveis:

- limite geral por IP;
- limite mais restrito para `/api/v1/auth/login`.

Quando o limite é excedido:

- retorna HTTP `429`;
- envia `Retry-After: 60`;
- registra evento de segurança;
- incrementa métrica específica.

Configuração padrão:

```text
RATE_LIMIT_RPM=60
LOGIN_RATE_LIMIT_RPM=10
```

Evidência: `src/main/java/com/ford/fordretain/security/RateLimitFilter.java`.

## 2.5 Controle de acesso por perfil — RBAC

O domínio FordRetain usa três perfis:

| Perfil | Permissões principais |
|---|---|
| `ADMIN` | acesso administrativo, DELETE e Actuator protegido |
| `GERENTE` | predição, dashboard, leitura e alteração |
| `ANALISTA` | consultas e leads |

Exemplos de regra:

- DELETE de cliente: somente `ADMIN`;
- PUT de cliente: `ADMIN` ou `GERENTE`;
- GET de clientes/leads: `ADMIN`, `GERENTE` ou `ANALISTA`;
- predição/dashboard: `ADMIN` ou `GERENTE`.

Os nomes `Brigadista`, `Gestor` e `Administrador` mostrados no enunciado representam exemplos de perfis. No FordRetain, o equivalente de separação de privilégios é `ANALISTA`, `GERENTE` e `ADMIN`.

Evidência: `src/main/java/com/ford/fordretain/security/SecurityConfig.java`.

## 2.6 CORS e autenticação stateless

A configuração de segurança:

- usa sessão `STATELESS`;
- aceita CORS somente de origens definidas na allowlist;
- não utiliza wildcard de origem por padrão;
- protege endpoints do Actuator;
- mantém somente login, health e documentação explicitamente liberados.

Variável:

```text
CORS_ALLOWED_ORIGINS
```

## 2.7 Senhas

Senhas de usuários são tratadas com `BCryptPasswordEncoder`. Senhas não devem aparecer em logs, exceptions ou respostas da API.

## 2.8 Docker hardening

O `Dockerfile` aplica:

- multi-stage build;
- separação entre JDK/Maven e runtime JRE;
- atualização de pacotes do Alpine;
- usuário não-root UID/GID 1001;
- workdir dedicado;
- healthcheck;
- nenhuma credencial incorporada à imagem.

## 2.9 Kubernetes / IaC Security

O arquivo `k8s/fordretain-api.yaml` contém:

- `automountServiceAccountToken: false`;
- `seccompProfile: RuntimeDefault`;
- `runAsNonRoot: true`;
- UID/GID não-root;
- `allowPrivilegeEscalation: false`;
- `readOnlyRootFilesystem: true`;
- `capabilities.drop: ["ALL"]`;
- secrets via `SecretKeyRef`;
- volume temporário dedicado;
- readiness/liveness probes;
- requests e limits de CPU/memória;
- Service do tipo `ClusterIP`.

Além da revisão manual, o manifesto e o Dockerfile passam pelo job **IaC Security — Trivy Config** no CI.

## 2.10 MQTT/TLS para IoT

**Não aplicável ao código executável deste repositório.** O módulo versionado aqui é a API FordRetain e não possui broker MQTT, firmware ou cliente IoT.

Não foi criada uma falsa evidência de MQTT/TLS. Se um módulo IoT for integrado, o requisito deverá incluir transporte TLS, autenticação do dispositivo, gestão/rotação de certificados e autorização por tópico.

## Evidências desta atividade

- `src/main/java/com/ford/fordretain/security/`
- `src/main/java/com/ford/fordretain/dto/LoginRequestDTO.java`
- `Dockerfile`
- `k8s/fordretain-api.yaml`
- commits da PR #18
- roteiro de prints em `evidencias/README.md`

---

# Atividade 3 — Observabilidade, Monitoramento e Resposta (Peso 2,0)

## Objetivo

Mostrar como o sistema detecta, registra e responde a incidentes de segurança.

## 3.1 Logs estruturados

O projeto utiliza log estruturado JSON e um filtro de auditoria. Eventos observáveis incluem:

- autenticação;
- falhas de autenticação;
- respostas 401/403;
- rate limiting;
- método HTTP;
- endpoint;
- status;
- duração;
- request ID;
- principal mascarado quando aplicável.

Segredos, senhas e JWT completos não devem ser registrados.

Exemplo ilustrativo de formato esperado:

```json
{
  "service": "fordretain-api",
  "event": "access_denied",
  "request_id": "<uuid>",
  "http_method": "DELETE",
  "http_path": "/api/v1/clientes/42",
  "http_status": 403
}
```

O exemplo é apenas de formato. A evidência da entrega deve ser um log **real** gerado pela execução.

## 3.2 Métricas

Micrometer + Prometheus fornecem métricas padrão de HTTP/JVM e métricas de segurança próprias:

- `fordretain_security_login_total{result="success"}`;
- `fordretain_security_login_total{result="failure"}`;
- `fordretain_security_access_denied_total`;
- `fordretain_security_rate_limited_total`.

Evidência: `src/main/java/com/ford/fordretain/security/SecurityMetrics.java`.

## 3.3 Prometheus

A configuração em `monitoring/prometheus/` inclui scrape da API e regras de alertas.

Eventos relevantes para alerta:

- excesso de erros HTTP 5xx;
- aumento de falhas de login;
- volume de 401/403;
- aumento de bloqueios por rate limit.

## 3.4 Grafana

O dashboard provisionado fica em:

`monitoring/grafana/dashboards/fordretain-security.json`

O provisionamento automático fica em:

- `monitoring/grafana/provisioning/datasources/`
- `monitoring/grafana/provisioning/dashboards/`

O ambiente local é iniciado por:

```bash
cd monitoring
docker compose -f docker-compose.monitoring.yml up -d
```

Prometheus: porta 9090.  
Grafana: porta 3000.

## 3.5 Cobertura por componente

| Componente | Cobertura neste repositório |
|---|---|
| API | logs, métricas, alertas e dashboard implementados |
| Mobile | backend é observado; crash/performance local dependem do repositório mobile |
| ML | integração deve adicionar latência, erros e métricas de drift quando o módulo estiver conectado |
| IoT | não aplicável ao escopo deste repositório |

A documentação diferencia claramente controle existente de controle futuro, evitando declarar evidência que não existe no código atual.

## 3.6 Plano de resposta a incidentes

Fluxo adotado:

### 1. Detecção

Origem possível:
- alerta Prometheus/Grafana;
- evento de log;
- falha no security gate;
- alerta de dependência;
- comportamento anormal de autenticação.

### 2. Análise

- confirmar evento;
- identificar endpoint e período;
- correlacionar `request_id`;
- verificar usuário/perfil;
- avaliar impacto em confidencialidade, integridade e disponibilidade;
- classificar severidade.

### 3. Contenção

Exemplos:
- suspender deploy;
- restringir endpoint/perfil;
- revogar credencial ou token;
- bloquear origem no gateway/WAF quando aplicável;
- isolar workload afetado.

### 4. Erradicação

- corrigir código/configuração;
- atualizar dependência vulnerável;
- remover segredo exposto e rotacionar credenciais;
- revisar permissões;
- aplicar novo build pelo pipeline.

### 5. Recuperação

- restaurar serviço;
- validar integridade;
- restaurar backup quando necessário;
- verificar métricas e logs;
- confirmar ausência de recorrência.

### 6. Pós-incidente

- registrar causa raiz;
- documentar timeline;
- criar ação preventiva;
- atualizar ameaça/controle;
- revisar alertas e testes.

## Evidências desta atividade

- `src/main/resources/logback-spring.xml`
- `src/main/java/com/ford/fordretain/security/AuditLogFilter.java`
- `src/main/java/com/ford/fordretain/security/SecurityMetrics.java`
- `monitoring/prometheus/`
- `monitoring/grafana/`
- `monitoring/docker-compose.monitoring.yml`
- prints reais descritos em `evidencias/README.md`

---

# Atividade 4 — Compliance, Riscos e Segurança Contínua (Peso 2,5)

## Objetivo

Demonstrar aderência a boas práticas, normas e políticas de segurança, além de estabelecer uma rotina contínua.

## 4.1 Revisão final de riscos — STRIDE

Escala: probabilidade e impacto de 1 (baixo) a 3 (alto). O risco residual considera os controles implementados.

| STRIDE | Cenário | Prob. | Impacto | Controle principal | Residual |
|---|---|---:|---:|---|---|
| Spoofing | uso de identidade/token inválido | 2 | 3 | JWT HS512, expiração, BCrypt, rate limit | médio |
| Tampering | alteração indevida de dados/payload | 2 | 3 | validação, RBAC, auditoria | médio |
| Repudiation | negação de ação crítica | 2 | 2 | logs JSON + request ID | baixo/médio |
| Information Disclosure | exposição de dados/segredos | 2 | 3 | AES-GCM, secrets externos, mascaramento | médio |
| Denial of Service | abuso/flood da API | 2 | 3 | Bucket4j, resources limits, alertas | médio |
| Elevation of Privilege | perfil inferior executar função elevada | 2 | 3 | RBAC por endpoint/método | médio |

### Riscos DevSecOps adicionais

| Risco | Controle |
|---|---|
| biblioteca com CVE | Trivy SCA + Dependabot |
| segredo commitado | Gitleaks |
| configuração IaC insegura | Trivy Config |
| imagem com CVE crítica | Trivy Image + gate |
| componente desconhecido no build | SBOM CycloneDX |
| alteração insegura chegar ao deploy | security gate obrigatório |

## 4.2 OWASP ASVS

Controles do projeto se relacionam com áreas do ASVS:

- autenticação;
- gerenciamento de token/sessão;
- controle de acesso;
- validação de entrada;
- criptografia;
- logging e monitoramento;
- configuração segura;
- proteção de dados.

O projeto não afirma certificação formal ASVS; o documento registra **mapeamento de práticas** aplicadas ao escopo da API.

## 4.3 OWASP API Security Top 10

Principais riscos tratados no FordRetain:

- autorização por objeto/função: RBAC e regras por endpoint;
- autenticação quebrada: JWT, expiração, validação e rate limit de login;
- consumo irrestrito de recursos: rate limiting e limites no Kubernetes;
- configuração insegura: CORS restrito, secrets externos, container non-root, IaC scan;
- inventário e exposição de APIs: documentação OpenAPI e rotas controladas;
- dependências vulneráveis: SCA, Dependabot e SBOM.

## 4.4 OWASP Mobile Top 10

A API fornece controles que apoiam autenticação, autorização, validação e proteção do backend. Entretanto, itens específicos do cliente mobile — armazenamento local, hardening do app, proteção do binário e telemetria do dispositivo — devem ser verificados no repositório mobile e não são apresentados aqui como implementação existente.

## 4.5 LGPD

Dados pessoais tratados pelo domínio podem incluir nome, e-mail e telefone.

Práticas aplicadas/propostas:

- minimização de dados;
- controle de acesso por necessidade;
- criptografia de campo sensível;
- segredo fora do código;
- logs sem senha/token;
- mascaramento quando aplicável;
- rastreabilidade de ações;
- retenção definida pelo processo de negócio;
- backup protegido;
- resposta a incidente;
- revisão periódica de permissões.

A adequação jurídica completa depende também de processos organizacionais, base legal, políticas de privacidade, contratos, retenção e atendimento aos direitos dos titulares — itens externos a este repositório de código.

## 4.6 Plano de segurança contínua

| Rotina | Frequência | Evidência esperada |
|---|---|---|
| Dependabot Maven/GitHub Actions/Docker | semanal | PRs automáticas |
| Build + SAST + SCA + Gitleaks + IaC + Container | todo push/PR | GitHub Actions |
| Revisão de HIGH | semanal | backlog/issue e atualização planejada |
| Revisão de RBAC/permissões | mensal | checklist de perfis |
| Testes de segurança | a cada release | workflow + evidências |
| Revisão STRIDE | release relevante | documento atualizado |
| Rotação de segredos | trimestral ou após incidente | registro operacional |
| Backup | conforme política do ambiente | logs do job de backup |
| Teste de restauração | mensal/trimestral | evidência de restore |
| Revisão de dashboard/alertas | mensal | checklist de observabilidade |

## 4.7 Backup e recuperação

O repositório não contém a infraestrutura real do banco Oracle de produção, portanto não simula um backup inexistente. O controle operacional recomendado exige:

1. backup automatizado e criptografado;
2. retenção definida;
3. acesso mínimo ao repositório de backup;
4. cópia separada do ambiente principal;
5. teste periódico de restauração;
6. registro de RPO/RTO conforme criticidade do negócio.

A evidência final deve vir do ambiente que hospeda o banco, caso esteja disponível para o projeto.

---

# Checklist final de conformidade

## Pipeline DevSecOps

- [x] Build/compilação no CI
- [x] SAST com CodeQL
- [x] SCA com Trivy
- [x] Dependabot semanal
- [x] Secret scanning com Gitleaks
- [x] Container Security com Trivy
- [x] IaC Security com Trivy Config
- [x] SBOM CycloneDX
- [x] security gate para vulnerabilidade CRITICAL
- [x] gate final para estágio de deploy
- [x] execução real do pipeline com todos os jobs aprovados na PR #18 / run 20

## Código e infraestrutura

- [x] JWT HS512
- [x] chave JWT mínima validada
- [x] expiração de token
- [x] RBAC por perfil
- [x] validação de entrada
- [x] rate limiting geral e de login
- [x] HTTP 429 + Retry-After
- [x] CORS com allowlist
- [x] BCrypt
- [x] criptografia AES-GCM
- [x] segredos externos ao código
- [x] Docker multi-stage/non-root
- [x] Kubernetes hardened
- [x] secrets Kubernetes por referência
- [x] análise IaC automatizada
- [x] MQTT/TLS classificado corretamente como não aplicável ao módulo atual

## Observabilidade e resposta

- [x] logs estruturados
- [x] request ID/auditoria
- [x] métricas Micrometer
- [x] endpoint Prometheus
- [x] métricas de segurança
- [x] regras de alertas
- [x] dashboard Grafana provisionado
- [x] fluxo detecção → análise → contenção → erradicação → recuperação
- [ ] prints reais de Grafana/Prometheus/logs devem ser adicionados após execução local do ambiente de monitoramento

## Compliance e segurança contínua

- [x] STRIDE revisado
- [x] riscos DevSecOps mapeados
- [x] OWASP ASVS mapeado
- [x] OWASP API Security Top 10 mapeado
- [x] OWASP Mobile Top 10 delimitado ao escopo disponível
- [x] LGPD mapeada
- [x] rotina de dependências
- [x] rotina de testes de segurança
- [x] rotina de auditoria de permissões
- [x] plano operacional de backup/recuperação

---

# Evidências finais a anexar

A entrega técnica está implementada no repositório. Para completar a parte visual pedida pela rubrica, os únicos itens que não podem ser fabricados são os prints de execução.

O roteiro de captura está em:

`evidencias/README.md`

Prioridade de prints:

1. Pipeline geral da PR #18 / run 20.
2. Build/Testes/SBOM.
3. CodeQL.
4. Trivy SCA.
5. Gitleaks.
6. Trivy IaC.
7. Trivy Container.
8. Gate final para deploy.
9. Grafana.
10. Prometheus.
11. Log JSON real.
12. Teste RBAC com 403.
13. Teste de rate limit com 429.

Nenhuma evidência deve exibir credenciais, JWT completo, secrets ou dados pessoais reais.

---

# Conclusão

A Sprint 3 transforma os controles de segurança do FordRetain em um processo contínuo e verificável. O código aplica autenticação, autorização, validação, rate limiting, criptografia e tratamento seguro de segredos; a infraestrutura aplica hardening de container e Kubernetes; o pipeline realiza SAST, SCA, secret scanning, IaC e container security; a observabilidade fornece logs, métricas, alertas e dashboard; e a camada de governança consolida STRIDE, OWASP, LGPD e rotina de segurança contínua.

O principal resultado técnico é que o security gate permaneceu rigoroso e, após a correção das dependências e do servidor embutido, a execução real da PR #18 passou integralmente, inclusive no scan da imagem e no gate final para deploy.
