import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const treeFile = path.resolve(root, process.argv[2] ?? 'output/backend-dependencies.json');
const tree = JSON.parse(fs.readFileSync(treeFile, 'utf8'));
const dependencies = new Map();
function walk(node) {
  if (node.scope !== 'test' && node.scope !== 'provided' && node.groupId !== 'org.akira') {
    const name = `${node.groupId}:${node.artifactId}`;
    dependencies.set(`${name}@${node.version}`, { name, version: node.version });
  }
  for (const child of node.children ?? []) walk(child);
}
walk(tree);
const packages = [...dependencies.values()];
try {
  const response = await fetch('https://api.osv.dev/v1/querybatch', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, signal: AbortSignal.timeout(60000),
    body: JSON.stringify({ queries: packages.map(item => ({ package: { ecosystem: 'Maven', name: item.name }, version: item.version })) }),
  });
  if (!response.ok) throw new Error(`OSV HTTP ${response.status}`);
  const result = await response.json();
  if (!Array.isArray(result.results) || result.results.length !== packages.length) throw new Error('Incomplete OSV response');
  const findings = result.results.flatMap((item, index) => item.vulns?.length
    ? [{ ...packages[index], vulnerabilities: item.vulns.map(value => value.id) }] : []);
  fs.mkdirSync(path.join(root, 'output'), { recursive: true });
  fs.writeFileSync(path.join(root, 'output/backend-osv-audit.json'), JSON.stringify({ checkedAt: new Date().toISOString(), packageCount: packages.length, findings }, null, 2));
  console.log(`OSV: checked ${packages.length} runtime Maven dependencies; ${findings.length} packages with advisories.`);
  for (const finding of findings) console.log(`${finding.name}@${finding.version}: ${finding.vulnerabilities.join(', ')}`);
  process.exitCode = findings.length ? 1 : 0;
} catch (error) {
  console.error(`Backend dependency audit could not complete: ${error.message}`);
  process.exitCode = 2;
}
