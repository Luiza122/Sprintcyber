# FordRetain — Sprint 3 Cybersecurity

Entrega da **Sprint 3 de Cybersecurity** aplicada ao projeto FordRetain, com foco em DevSecOps, segurança de código e infraestrutura, observabilidade, resposta a incidentes, riscos e conformidade.

## Conteúdo do repositório

- `.github/workflows/security-pipeline.yml` — pipeline CI/CD de segurança com build/testes, SAST, SCA, secret scanning e container security.
- `src/main/java/.../security` — JWT, RBAC, rate limiting, auditoria e métricas de segurança.
- `src/main/java/.../controller/AuthController.java` — autenticação com validações e práticas de hardening.
- `monitoring/` — Prometheus, alertas e dashboard Grafana.
- `k8s/fordretain-api.yaml` — exemplo de IaC/Kubernetes com boas práticas de segurança.
- `docs/SPRINT3_CYBERSECURITY.md` — documentação técnica consolidada da Sprint 3.
- `APLICAR_PATCH.md` — instruções de aplicação das alterações no repositório principal do FordRetain.

## Controles contemplados

- SAST com CodeQL.
- SCA/Trivy e Dependabot.
- Secret scanning com Gitleaks.
- Container security com Trivy.
- JWT e controle de acesso por perfil (ADMIN, GERENTE e ANALISTA).
- Validação de entrada e rate limiting.
- Criptografia de dados sensíveis e separação de segredos.
- Logs de auditoria, métricas Prometheus e dashboards Grafana.
- Plano de resposta a incidentes.
- Revisão de riscos com STRIDE.
- Mapeamento com OWASP ASVS, OWASP API Security Top 10, OWASP Mobile Top 10 e LGPD.

## Observação

Este repositório reúne os artefatos e alterações da Sprint 3. A documentação em `docs/` descreve o que foi implementado, o que foi observado no pipeline e quais evidências reais devem ser capturadas após a execução do GitHub Actions e do ambiente de monitoramento.
