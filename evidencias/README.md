# Evidências — Sprint 3 Cybersecurity

Esta pasta é reservada para **evidências reais de execução**. Não inserir prints simulados ou fabricados.

## Padrão de nomes

Use os seguintes nomes para facilitar a correção:

```text
01-pipeline-geral.png
02-build-testes-sbom.png
03-codeql-sast.png
04-trivy-sca.png
05-gitleaks-secret-scanning.png
06-trivy-iac.png
07-trivy-container.png
08-security-gate.png
09-grafana-dashboard.png
10-prometheus-target.png
11-prometheus-alertas.png
12-log-json-auditoria.png
13-rbac-403.png
14-rate-limit-429.png
```

## Checklist de captura

### 1. Pipeline DevSecOps

- [ ] **01-pipeline-geral.png** — tela do GitHub Actions mostrando todos os jobs da execução.
- [ ] **02-build-testes-sbom.png** — job `Build, Testes e SBOM` concluído e artefato `fordretain-sbom-cyclonedx` disponível.
- [ ] **03-codeql-sast.png** — job `SAST - CodeQL` concluído.
- [ ] **04-trivy-sca.png** — relatório/gate SCA do Trivy.
- [ ] **05-gitleaks-secret-scanning.png** — Gitleaks concluído sem segredo versionado.
- [ ] **06-trivy-iac.png** — análise de configuração/IaC.
- [ ] **07-trivy-container.png** — relatório de vulnerabilidades da imagem.
- [ ] **08-security-gate.png** — `Gate final para deploy` aprovado.

O print deve mostrar o nome do repositório, o nome do job e o resultado. Evite cortar a parte que identifica a execução.

### 2. Código e infraestrutura

Além dos commits e arquivos do repositório, capture:

- [ ] **13-rbac-403.png** — requisição autenticada com perfil sem permissão recebendo HTTP 403.
- [ ] **14-rate-limit-429.png** — evidência controlada de HTTP 429 e header `Retry-After`.

Arquivos que podem ser apresentados diretamente na correção:

- `src/main/java/com/ford/fordretain/security/JwtService.java`
- `src/main/java/com/ford/fordretain/security/SecurityConfig.java`
- `src/main/java/com/ford/fordretain/security/RateLimitFilter.java`
- `src/main/java/com/ford/fordretain/security/CryptoUtils.java`
- `src/main/java/com/ford/fordretain/dto/LoginRequestDTO.java`
- `Dockerfile`
- `k8s/fordretain-api.yaml`

### 3. Observabilidade

- [ ] **09-grafana-dashboard.png** — dashboard `FordRetain - Segurança e Observabilidade` com painéis carregados.
- [ ] **10-prometheus-target.png** — target da API em estado `UP` no Prometheus.
- [ ] **11-prometheus-alertas.png** — regras de alerta carregadas.
- [ ] **12-log-json-auditoria.png** — linha de log JSON contendo evento, request ID, método/path/status e sem senha/JWT.

### 4. Compliance e segurança contínua

A evidência principal é o documento `docs/SPRINT3_CYBERSECURITY.md`, especialmente:

- revisão STRIDE;
- mapeamento OWASP ASVS;
- OWASP API Security Top 10;
- OWASP Mobile Top 10;
- LGPD;
- plano de segurança contínua;
- checklist de conformidade.

## Como subir as evidências

Depois de capturar os arquivos, coloque-os nesta pasta e faça um commit com uma mensagem semelhante a:

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
