# Como aplicar este pacote

Este diretório contém **somente arquivos novos ou substituições** para a Sprint 3. Ele foi preparado com base no `main` do repositório `Matheus-Bortolotto/fordretain-api-Java` analisado em 08/09/2026.

Arquivos substituídos:
- `.github/workflows/security-pipeline.yml`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/java/com/ford/fordretain/security/JwtService.java`
- `src/main/java/com/ford/fordretain/security/RateLimitFilter.java`
- `src/main/java/com/ford/fordretain/security/AuditLogFilter.java`
- `src/main/java/com/ford/fordretain/security/SecurityConfig.java`
- `src/main/java/com/ford/fordretain/controller/AuthController.java`

Arquivos novos:
- `src/test/resources/application.properties`
- `src/main/resources/logback-spring.xml`
- `src/main/java/com/ford/fordretain/security/SecurityMetrics.java`
- `monitoring/**`
- `k8s/fordretain-api.yaml`
- `docs/SPRINT3_CYBERSECURITY.md`

Depois de aplicar, rode `mvn clean verify` e deixe o GitHub Actions executar todos os gates. Se um gate CRITICAL falhar, a correção da vulnerabilidade deve vir antes do merge.
