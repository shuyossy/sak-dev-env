#!/usr/bin/env node
// lint-staged から呼ばれ、変更 Java ファイルに google-java-format を直接適用。
// Spotless の Maven プラグインは全体走査になるため、pre-commit では使わない。
// 整形後のファイルは lint-staged の機能により自動で re-stage される。

import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';

const PROJECT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const JAR = join(PROJECT_ROOT, 'target', 'lint-tools', 'google-java-format.jar');

const files = process.argv.slice(2);
if (files.length === 0) process.exit(0);

if (!existsSync(JAR)) {
  console.error('[run-spotless-staged] google-java-format jar not found. Run: node scripts/setup-lint-tools.mjs');
  process.exit(1);
}

// --replace で in-place 整形。JDK 16+ の module access flag が必要。
const result = spawnSync(
  'java',
  [
    '--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED',
    '--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED',
    '--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED',
    '--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED',
    '--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED',
    '--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED',
    '-jar',
    JAR,
    '--replace',
    ...files,
  ],
  { stdio: 'inherit' },
);
process.exit(result.status ?? 1);
