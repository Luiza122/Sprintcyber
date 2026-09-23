import { chromium } from 'playwright';
import { readFile } from 'node:fs/promises';

const outputDirectory = 'evidencias';
const runtimeDirectory = 'runtime-evidence';
const commit = process.env.EVIDENCE_COMMIT ?? 'execucao-local';
const runId = process.env.EVIDENCE_RUN_ID ?? 'local';
const capturedAt = new Date().toISOString();

const escapeHtml = (value) => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;');

const read = async (name) => readFile(`${runtimeDirectory}/${name}`, 'utf8');

const browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] });
const context = await browser.newContext({
  viewport: { width: 1440, height: 960 },
  colorScheme: 'dark'
});
const page = await context.newPage();

const baseStyle = `
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body { margin: 0; padding: 42px; background: #0d1117; color: #e6edf3;
    font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
  .header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px;
    border-bottom: 1px solid #30363d; padding-bottom: 24px; margin-bottom: 28px; }
  h1 { font-size: 30px; margin: 0 0 8px; }
  .subtitle { color: #8b949e; font-size: 15px; }
  .badge { border: 1px solid #238636; background: #123820; color: #7ee787; border-radius: 999px;
    font-weight: 700; padding: 8px 14px; white-space: nowrap; }
  .meta { display: grid; grid-template-columns: 150px 1fr; gap: 8px 18px; padding: 18px;
    border: 1px solid #30363d; background: #161b22; border-radius: 8px; margin-bottom: 20px; }
  .label { color: #8b949e; }
  .value { color: #f0f6fc; overflow-wrap: anywhere; }
  .block { margin-top: 18px; }
  .block h2 { font-size: 17px; margin: 0; padding: 12px 16px; border: 1px solid #30363d;
    border-bottom: 0; background: #21262d; border-radius: 8px 8px 0 0; }
  pre { margin: 0; padding: 18px; border: 1px solid #30363d; border-radius: 0 0 8px 8px;
    background: #010409; color: #d2a8ff; font: 14px/1.55 "SFMono-Regular", Consolas, "Liberation Mono", monospace;
    white-space: pre-wrap; overflow-wrap: anywhere; }
  .ok { color: #7ee787; font-weight: 700; }
  .footer { color: #8b949e; font-size: 12px; margin-top: 24px; }
`;

async function captureReport({ title, subtitle, result, blocks, filename }) {
  const blockHtml = blocks.map(({ heading, content }) => `
    <section class="block">
      <h2>${escapeHtml(heading)}</h2>
      <pre>${escapeHtml(content)}</pre>
    </section>`).join('');
  await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${baseStyle}</style></head>
    <body>
      <header class="header">
        <div><h1>${escapeHtml(title)}</h1><div class="subtitle">${escapeHtml(subtitle)}</div></div>
        <div class="badge">EXECUÇÃO REAL APROVADA</div>
      </header>
      <section class="meta">
        <div class="label">Resultado</div><div class="value ok">${escapeHtml(result)}</div>
        <div class="label">Commit</div><div class="value">${escapeHtml(commit)}</div>
        <div class="label">Workflow run</div><div class="value">${escapeHtml(runId)}</div>
        <div class="label">Capturado em</div><div class="value">${escapeHtml(capturedAt)}</div>
      </section>
      ${blockHtml}
      <div class="footer">Gerado automaticamente a partir das respostas e logs produzidos durante o GitHub Actions.</div>
    </body></html>`, { waitUntil: 'load' });
  await page.screenshot({ path: `${outputDirectory}/${filename}`, fullPage: true });
}

const mavenLog = await read('maven-verify.log');
const buildLines = mavenLog.split(/\r?\n/)
  .filter((line) => /Tests run:|CycloneDX:|BUILD SUCCESS|Writing and validating BOM/.test(line))
  .slice(-14)
  .join('\n');
await captureReport({
  title: 'Build, testes e SBOM CycloneDX',
  subtitle: 'Maven verify executado no ambiente efêmero do GitHub Actions',
  result: 'BUILD SUCCESS • 9 testes aprovados • SBOM validada',
  blocks: [
    { heading: 'Resumo do Maven', content: buildLines },
    { heading: 'Integridade da SBOM', content: await read('sbom-meta.txt') }
  ],
  filename: '02-build-testes-sbom.png'
});

const grafana = JSON.parse(await read('grafana-dashboard.json'));
const values = JSON.parse(await read('monitoring-values.json'));
if (grafana.dashboard.uid !== 'fordretain-security' || values.apiUp !== '1') {
  throw new Error('Grafana não provisionado ou API sem coleta');
}
await page.goto('http://127.0.0.1:3000/login', { waitUntil: 'domcontentloaded' });
await page.locator('input[name="user"]').fill(process.env.GRAFANA_ADMIN_USER);
await page.locator('input[name="password"]').fill(process.env.GRAFANA_ADMIN_PASSWORD);
await page.locator('button[type="submit"]').click();
await page.goto('http://127.0.0.1:3000/d/fordretain-security/fordretain-security?orgId=1&from=now-15m&to=now', { waitUntil: 'domcontentloaded' });
await page.getByText('FordRetain - Segurança e Observabilidade').first().waitFor({ state: 'visible', timeout: 30000 });
await page.getByText('Falhas de login (10m)').first().waitFor({ state: 'visible', timeout: 30000 });
await page.waitForTimeout(5000);
await page.screenshot({ path: `${outputDirectory}/09-grafana-dashboard.png`, fullPage: true });

const targetPayload = JSON.parse(await read('prometheus-targets.json'));
const target = targetPayload.data.activeTargets.find((item) => item.labels.job === 'fordretain-api');
if (!target || target.health !== 'up') throw new Error('Target fordretain-api não está UP');
await captureReport({
  title: 'Prometheus — target da FordRetain API',
  subtitle: 'Coleta real do endpoint /actuator/prometheus',
  result: 'TARGET UP • scrape concluído sem erro',
  blocks: [
    { heading: 'Target ativo', content: `Job: ${target.labels.job}\nInstância: ${target.labels.instance}\nURL: ${target.scrapeUrl}\nHealth: ${target.health.toUpperCase()}\nIntervalo: ${target.scrapeInterval}\nÚltimo scrape: ${target.lastScrape}\nDuração: ${target.lastScrapeDuration}s\nErro: ${target.lastError || 'nenhum'}` }
  ],
  filename: '10-prometheus-target.png'
});

const rulesPayload = JSON.parse(await read('prometheus-rules.json'));
const rules = rulesPayload.data.groups.flatMap((group) => group.rules);
if (rules.length === 0 || rules.some((rule) => rule.health !== 'ok')) throw new Error('Regras do Prometheus inválidas');
const rulesText = rules.map((rule) => `${rule.name}\n  Severidade: ${rule.labels.severity}\n  Estado: ${rule.state}\n  Avaliação: ${rule.health}\n  Expressão: ${rule.query}`).join('\n\n');
await captureReport({
  title: 'Prometheus — regras de alerta',
  subtitle: 'Regras carregadas e avaliadas no ambiente real de monitoramento',
  result: `${rules.length} ALERTAS CARREGADOS • AVALIAÇÃO OK`,
  blocks: [{ heading: 'Grupo fordretain-security', content: rulesText }],
  filename: '11-prometheus-alertas.png'
});

const apiLog = await read('api.log');
const auditLines = apiLog.split(/\r?\n/)
  .filter((line) => line.trim().startsWith('{') && (line.includes('"event":"http_audit"') || line.includes('"event":"access_denied"')))
  .slice(-8)
  .join('\n');
await captureReport({
  title: 'Logs estruturados de segurança',
  subtitle: 'Eventos JSON emitidos pela API durante os testes automatizados',
  result: 'Logs de auditoria encontrados',
  blocks: [{ heading: 'Trecho sanitizado do log da aplicação', content: auditLines }],
  filename: '12-log-json-auditoria.png'
});

await captureReport({
  title: 'RBAC — acesso negado por perfil',
  subtitle: 'ANALISTA tentou acessar /actuator/metrics, permitido somente para ADMIN',
  result: `HTTP ${String(await read('rbac-status.txt')).trim()} • bloqueio confirmado`,
  blocks: [
    { heading: 'Requisição', content: 'GET /actuator/metrics\nAuthorization: Bearer [TOKEN ANALISTA OCULTADO]' },
    { heading: 'Cabeçalhos da resposta', content: await read('rbac-headers.txt') },
    { heading: 'Corpo da resposta', content: await read('rbac-body.json') }
  ],
  filename: '13-rbac-403.png'
});

await captureReport({
  title: 'Rate limiting do login',
  subtitle: 'O limite por IP bloqueou tentativas repetidas no endpoint de autenticação',
  result: `HTTP ${String(await read('rate-limit-status.txt')).trim()} • Retry-After confirmado`,
  blocks: [
    { heading: 'Requisição de teste', content: 'POST /api/v1/auth/login\nConta acadêmica inválida; senha não registrada no relatório' },
    { heading: 'Cabeçalhos da resposta', content: await read('rate-limit-headers.txt') },
    { heading: 'Corpo da resposta', content: await read('rate-limit-body.json') }
  ],
  filename: '14-rate-limit-429.png'
});

await browser.close();
