#!/usr/bin/env node
// lint-staged から呼ばれ、変更 Java ファイルに対して Checkstyle (blocking ruleset) を実行。
// Maven Central に shaded jar 配布がないため、pom.xml の lint-setup profile で
// `target/lint-tools/lib/` 配下に checkstyle + transitive を配置させた上で classpath 起動する。

import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';

const PROJECT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const LIB = join(PROJECT_ROOT, 'target', 'lint-tools', 'lib');
const CONFIG = join(PROJECT_ROOT, 'rules', 'checkstyle', 'checkstyle-blocking.xml');

const files = process.argv.slice(2);
if (files.length === 0) {
  process.exit(0);
}

if (
  !existsSync(LIB) ||
  !readdirSync(LIB).some((e) => e.startsWith('checkstyle-') && e.endsWith('.jar'))
) {
  console.error(
    '[run-checkstyle] checkstyle lib not populated. Run: node scripts/setup-lint-tools.mjs',
  );
  process.exit(1);
}

const classpath = process.platform === 'win32' ? `${LIB}\\*` : `${LIB}/*`;

const result = spawnSync(
  'java',
  ['-cp', classpath, 'com.puppycrawl.tools.checkstyle.Main', '-c', CONFIG, ...files],
  { stdio: 'inherit' },
);
process.exit(result.status ?? 1);
