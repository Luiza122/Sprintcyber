# Evidências — Sprint 3 Cybersecurity

Esta pasta é reservada para **evidências reais de execução**. Não inserir prints simulados ou fabricados.

## Evidências já adicionadas

| Arquivo | O que comprova | Status |
|---|---|---|
| `00-estrutura-projeto.png` | organização das pastas e artefatos | disponível |
| `01-pipeline-geral.jpg` | execução completa do Security Pipeline aprovada | disponível |
| `02-build-testes-sbom.png` | build, 10 testes e SBOM CycloneDX gerada | disponível |
| `03-codeql-sast.jpg` | análise SAST com CodeQL aprovada | disponível |
| `04-trivy-sca.jpg` | análise SCA com Trivy aprovada | disponível |
| `05-gitleaks-secret-scanning.jpg` | secret scanning com Gitleaks aprovado | disponível |
| `06-trivy-iac.jpg` | análise IaC com Trivy Config aprovada | disponível |
| `07-trivy-container.jpg` | análise da imagem com Trivy aprovada | disponível |
| `08-security-gate.jpg` | gate final para deploy aprovado | disponível |
| `09-grafana-dashboard.png` | screenshot direto da interface Grafana provisionada | disponível |
| `10-prometheus-target.png` | target da API em estado `UP` | disponível |
| `11-prometheus-alertas.png` | quatro regras de alerta carregadas | disponível |
| `12-log-json-auditoria.png` | logs JSON sanitizados com request ID | disponível |
| `13-rbac-403.png` | perfil sem permissão recebendo HTTP 403 | disponível |
| `14-rate-limit-429.png` | limite de login retornando HTTP 429 e `Retry-After` | disponível |
| `15-swagger-login-admin-200.png` | login ADMIN executado com HTTP 200 | disponível |
| `16-swagger-login-admin-request.png` | payload de autenticação de demonstração | disponível |
| `17-swagger-login-admin-response.png` | retorno de perfil e JWT | disponível |
| `18-swagger-login-gerente-200.png` | login GERENTE executado com HTTP 200 | disponível |
| `19-swagger-validacao-400.png` | validação de entrada retornando HTTP 400 | disponível |
| `20-maven-test-build-success.png` | teste Maven e build concluídos com sucesso | disponível |

Os arquivos `01` a `14` foram gerados ou capturados a partir de execuções reais. O arquivo `09` é uma captura direta do navegador no Grafana; `02` e `10` a `14` são relatórios visuais gerados automaticamente a partir de logs e respostas HTTP da execução, com os dados brutos no artefato do Actions. O workflow
`.github/workflows/runtime-evidence.yml` reproduz automaticamente as evidências `02` e `09` a `14`,
mantendo os resultados vinculados ao commit e ao número da execução do GitHub Actions.

- Pipeline e scanners: [PR #22 — Security Pipeline, run #42](https://github.com/Luiza122/Sprintcyber/actions/runs/34908099517).
- API, monitoramento e testes HTTP: [Runtime Evidence da integração, run 35922198862](https://github.com/Luiza122/Sprintcyber/actions/runs/35922198862), com dashboard final e dados brutos anexados.
- Validação do código e security gate: [Security Pipeline da PR #25, run 35922204237](https://github.com/Luiza122/Sprintcyber/actions/runs/35922204237).

Os JWTs visíveis nos registros do Swagger pertencem à demonstração local e possuem expiração curta. Em novas capturas, oculte o valor completo do token e mantenha apenas o início necessário para comprovar que ele foi emitido.

## Padrão de nomes

Use os seguintes nomes para facilitar a correção:

```text
01-pipeline-geral.jpg
02-build-testes-sbom.png
03-codeql-sast.jpg
04-trivy-sca.jpg
05-gitleaks-secret-scanning.jpg
06-trivy-iac.jpg
07-trivy-container.jpg
08-security-gate.jpg
09-grafana-dashboard.png
10-prometheus-target.png
11-prometheus-alertas.png
12-log-json-auditoria.png
13-rbac-403.png
14-rate-limit-429.png
```

## Checklist de captura

### 1. Pipeline DevSecOps

- [x] **01-pipeline-geral.jpg** — tela do GitHub Actions mostrando todos os jobs da execução.
- [x] **02-build-testes-sbom.png** — build, 10 testes e artefato CycloneDX validados.
- [x] **03-codeql-sast.jpg** — job `SAST - CodeQL` concluído.
- [x] **04-trivy-sca.jpg** — relatório/gate SCA do Trivy.
- [x] **05-gitleaks-secret-scanning.jpg** — Gitleaks concluído sem segredo versionado.
- [x] **06-trivy-iac.jpg** — análise de configuração/IaC.
- [x] **07-trivy-container.jpg** — relatório de vulnerabilidades da imagem.
- [x] **08-security-gate.jpg** — `Gate final para deploy` aprovado.

O print deve mostrar o nome do repositório, o nome do job e o resultado. Evite cortar a parte que identifica a execução.

### 2. Código e infraestrutura

Além dos commits e arquivos do repositório, capture:

- [x] **13-rbac-403.png** — requisição autenticada com perfil sem permissão recebendo HTTP 403.
- [x] **14-rate-limit-429.png** — evidência controlada de HTTP 429 e header `Retry-After`.

Evidências complementares já disponíveis:

- [x] login ADMIN com HTTP 200 e JWT;
- [x] login GERENTE com HTTP 200 e JWT;
- [x] validação de payload com HTTP 400;
- [x] execução de testes com `BUILD SUCCESS`.

Arquivos que podem ser apresentados diretamente na correção:

- `src/main/java/com/ford/fordretain/security/JwtService.java`
- `src/main/java/com/ford/fordretain/security/SecurityConfig.java`
- `src/main/java/com/ford/fordretain/security/RateLimitFilter.java`
- `src/main/java/com/ford/fordretain/security/CryptoUtils.java`
- `src/main/java/com/ford/fordretain/dto/LoginRequestDTO.java`
- `Dockerfile`
- `k8s/fordretain-api.yaml`

### 3. Observabilidade

- [x] **09-grafana-dashboard.png** — dashboard `FordRetain - Segurança e Observabilidade` provisionado, com valores coletados pelo Prometheus.
- [x] **10-prometheus-target.png** — target da API em estado `UP` no Prometheus.
- [x] **11-prometheus-alertas.png** — regras de alerta carregadas.
- [x] **12-log-json-auditoria.png** — logs JSON contendo evento, request ID, método/path/status e sem senha/JWT.

### 4. Compliance e segurança contínua

A evidência principal é o documento `docs/SPRINT3_CYBERSECURITY.md`, especialmente:

- revisão STRIDE;
- mapeamento OWASP ASVS;
- OWASP API Security Top 10;
- OWASP Mobile Top 10;
- LGPD;
- plano de segurança contínua;
- checklist de conformidade.

A imagem original da rubrica está em `docs/assets/rubrica-sprint3-cybersecurity.jpg` e foi vinculada ao relatório final.

## Sequência sugerida para a apresentação

1. Rubrica e arquitetura da solução.
2. Pipeline e security gate.
3. Build e testes automatizados.
4. Login, JWT e validação de entrada.
5. RBAC e rate limiting.
6. Prometheus, Grafana e logs.
7. STRIDE, OWASP, LGPD e plano contínuo.

## Como reproduzir as evidências

No GitHub, execute manualmente o workflow `Runtime Evidence`. Ele recompila o projeto, executa os testes, sobe a API e o ambiente de monitoramento, refaz os cenários controlados e publica os arquivos como artefato. As telas `01` e `03` a `08` podem ser atualizadas a partir da execução mais recente do `Security Pipeline`.

Ao atualizar os registros, use uma mensagem semelhante a:

```text
docs: adicionar evidências reais da Sprint 3
```

## O que não deve ser mostrado nos prints

Antes de salvar uma evidência, confira se ela não exibe:

- senha do Oracle;
- JWT completo;
- `JWT_SECRET`;
- chaves de criptografia;
- token do GitHub;
- dados pessoais reais de clientes.

Se algum valor sensível aparecer, gere uma nova evidência com dado de teste ou com o valor ocultado.
