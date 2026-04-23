#!/usr/bin/env node
// target/lint-tools/ に Checkstyle / PMD / google-java-format の jar 群が揃っていることを保証する。
// 欠けていれば `./mvnw -Plint-setup initialize` を呼び出して Maven に解決させる。
// Maven ローカルリポジトリの位置（~/.m2 以外含む）に依存しないための間接層。

import { existsSync, readdirSync } from 'node:fs';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const PROJECT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const LINT_TOOLS_DIR = join(PROJECT_ROOT, 'target', 'lint-tools');
const LIB = join(LINT_TOOLS_DIR, 'lib');

function hasAll() {
  if (!existsSync(LINT_TOOLS_DIR)) return false;
  const entries = readdirSync(LINT_TOOLS_DIR);
  const gjf = entries.includes('google-java-format.jar');
  if (!existsSync(LIB)) return false;
  const libEntries = readdirSync(LIB);
  const checkstyle = libEntries.some((e) => e.startsWith('checkstyle-') && e.endsWith('.jar'));
  const pmd = libEntries.some((e) => e.startsWith('pmd-cli-') && e.endsWith('.jar'));
  return gjf && checkstyle && pmd;
}

function resolveMaven() {
  const wrapper = process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw';
  return join(PROJECT_ROOT, wrapper);
}

function runMavenLintSetup() {
  const mvn = resolveMaven();
  console.log('[setup-lint-tools] Resolving lint tool jars via Maven...');
  const result = spawnSync(mvn, ['-Plint-setup', 'initialize', '-q'], {
    cwd: PROJECT_ROOT,
    stdio: 'inherit',
  });
  if (result.status !== 0) {
    console.error('[setup-lint-tools] ./mvnw -Plint-setup initialize failed.');
    process.exit(result.status ?? 1);
  }
}

if (hasAll()) {
  process.exit(0);
}

runMavenLintSetup();

if (!hasAll()) {
  console.error('[setup-lint-tools] lint-tools are still incomplete after Maven setup.');
  process.exit(1);
}

console.log('[setup-lint-tools] OK');
