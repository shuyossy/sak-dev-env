# PBI ID:1 開発環境整備 設計書

- 作成日: 2026-04-22
- 対象 PBI: `PBI.md` ID:1「開発環境整備」
- ステータス: 設計確定、実装計画（writing-plans）へ引き渡し前

## 目的

シフトレフト（セキュリティを含めたソフトウェア品質担保）を大前提として、
開発環境からリモートリポジトリにバグを持ち出さず、CI/CD パイプライン上で
バグを含んだ資源をデプロイしないためのボイラープレート開発環境を整備する。

## 対応する受け入れ基準

| PBI の受け入れ基準 | 対応章 |
|---|---|
| ローカル開発環境が整備されている（`.vscode` / pre-commit, pre-push フック） | 第2章・第5章 |
| CI/CD パイプラインが整備されている | 第6章・第8章 |
| シフトレフト（全般） | 全章を貫通（検査の段階的配置） |

---

## 第1章：全体構成とツールマップ

### 1-1. 検査カテゴリ × 採用ツール × 実行ステージ

| # | カテゴリ | 対象 | ツール | 実行ステージ | 備考 |
|---|---|---|---|---|---|
| 1 | フォーマット | Java | Spotless（google-java-format） | pre-commit / CI | 変更ファイルのみ |
| 2 | フォーマット | JS / JSON / MD | Prettier | pre-commit / CI | 変更ファイルのみ |
| 3 | 静的解析 Lint | Java | Checkstyle（CLI, 変更ファイルのみ, blocking ruleset） | pre-commit / CI（フル ruleset） | 二段構え |
| 4 | 静的解析 Lint | Java | PMD（CLI, 変更ファイルのみ, blocking ruleset） | pre-commit / CI（フル ruleset） | 二段構え |
| 5 | バグ検出 / SAST | Java | SpotBugs + find-sec-bugs | pre-push / CI | コンパイル必要のため |
| 6 | 静的解析 Lint | JS | ESLint（`eslint-plugin-security` 同梱） | pre-commit / pre-push（全体）/ CI | |
| 7 | ユニットテスト | Java | JUnit 5 | pre-push / CI | |
| 8 | ユニットテスト | JS | Vitest | pre-push / CI | CI でカバレッジ |
| 9 | カバレッジ | Java | JaCoCo | CI | しきい値は pom.xml に宣言 |
| 10 | カバレッジ | JS | Vitest + v8 coverage | CI | しきい値は vitest.config.js に宣言 |
| 11 | シークレット検知 | 変更ファイル | Secretlint | pre-commit | npm 完結 |
| 12 | シークレット検知 | 全履歴 | Gitleaks | CI | Docker image 経由 |
| 13 | SCA | Java | OWASP Dependency-Check | CI | NVD キャッシュを CI cache で共有 |
| 14 | SCA | JS | npm audit + Trivy fs | npm audit = pre-push / Trivy fs = CI | |
| 15 | コンテナスキャン | Docker image | Trivy image | CI | HIGH 以上で fail |
| 16 | Dockerfile Lint | Dockerfile | hadolint | CI | |
| 17 | コミットメッセージ | commit-msg | commitlint（Conventional Commits） | commit-msg | `subject-case` は無効化（日本語件名許容） |

### 1-2. ステージごとのレイテンシ目標

| ステージ | 目標時間 | 検査内容 |
|---|---|---|
| `pre-commit` | 〜15 秒 | Spotless / Prettier / Checkstyle (blocking) / PMD (blocking) / ESLint / Secretlint（全部 lint-staged で変更ファイルのみ） |
| `commit-msg` | < 1 秒 | commitlint |
| `pre-push` | 1〜3 分 | Checkstyle/PMD フル / SpotBugs+find-sec-bugs / JUnit / Vitest / npm audit |
| `CI (MR)` | 5〜10 分 | 上記全部 + JaCoCo + Vitest coverage + OWASP DC + Gitleaks + Trivy fs + ESLint 全体 |
| `CI (main)` | 10〜15 分 | 上記 + Docker build + hadolint + Trivy image + docusaurus build & deploy |

### 1-3. 付随する決めごと

- Spring Boot バージョン: `3.5.14-SNAPSHOT` → **`3.5.14` stable 固定**、`<repositories>` の snapshot 参照削除。
- JDK: 21（開発者マシンにインストール済み前提、SDKMAN! は社内利用不可のため前提化しない）。
- Node.js: LTS 22.x を `.nvmrc` と `package.json#engines` で固定。
- ネットワーク制約への対応: Trivy / Gitleaks / hadolint は CI 側で公式 Docker イメージの `image:` 参照。ローカルでは Secretlint と npm audit で代替。

---

## 第2章：ローカル開発環境

### 2-1. ランタイム固定

| 対象 | 固定方法 | バージョン |
|---|---|---|
| JDK | README の前提環境欄に明記のみ | 21（インストール済み前提） |
| Maven | 既存の `mvnw` / Maven Wrapper | `3.9.14` |
| Node.js | `.nvmrc` + `package.json` の `engines` | `22.x` LTS |
| 文字コード / 改行 | `.editorconfig` + `.gitattributes` | UTF-8 / LF（`*.cmd` のみ CRLF 維持） |

### 2-2. `.vscode/` 構成

- 作成: `settings.json` / `tasks.json`、既存の `launch.json` に Vitest Debug 設定を追加
- 作成しない: `extensions.json`（拡張機能はインストール済み前提）
- `settings.json` のポイント:
  - `editor.formatOnSave: true`
  - `editor.codeActionsOnSave: { "source.fixAll.eslint": "explicit" }`
  - `java.configuration.updateBuildConfiguration: "automatic"`
  - `java.format.settings.url: rules/checkstyle/eclipse-formatter.xml`
  - `files.eol: "\n"` / `files.insertFinalNewline: true` / `files.trimTrailingWhitespace: true`
  - `eslint.validate: ["javascript"]`

#### `tasks.json` のタスク一覧

| タスク名 | コマンド |
|---|---|
| Backend: Build | `./mvnw clean package` |
| Backend: Test | `./mvnw test` |
| Backend: Run (DevTools) | `./mvnw spring-boot:run` |
| Frontend: Build | `npm run build` |
| Frontend: Build (Watch) | `npm run build:watch` |
| Frontend: Test | `npm test` |
| Frontend: Test (Watch) | `npm run test:watch` |
| Lint: All | `npm run lint && ./mvnw -DskipTests verify` |

#### `.gitignore` の書き換え

`.vscode/` 全体を無視している現状から、共有すべきファイルだけコミット対象にする：

```gitignore
.vscode/*
!.vscode/extensions.json
!.vscode/settings.json
!.vscode/tasks.json
!.vscode/launch.json
```

### 2-3. Husky 初期化フロー

```bash
nvm use
npm install                             # postinstall で husky と lint-tools の setup
./mvnw -N dependency:go-offline         # 初回のウォームアップ
```

`package.json#scripts.prepare: "husky"` と `postinstall: "node scripts/setup-lint-tools.mjs"` により、
クローン後 `npm install` 一発でフックが有効化されかつ lint 用 jar も `target/lint-tools/` に展開される。

---

## 第3章：Maven ビルド構成と Java 品質ゲート

### 3-1. `pom.xml` への変更サマリ

#### Spring Boot 安定化

- `3.5.14-SNAPSHOT` → `3.5.14`
- `<repositories>` / `<pluginRepositories>` の `spring-snapshots` ブロックを削除

#### 追加する依存

| 依存 | scope |
|---|---|
| `spring-boot-devtools` | `runtime`, `optional` |
| `spring-boot-starter-web` | compile |
| `spring-boot-starter-thymeleaf` | compile |

#### 追加する Maven プラグイン

| プラグイン | バインド phase |
|---|---|
| `spotless-maven-plugin` | `validate`（check） |
| `maven-checkstyle-plugin` | `verify`（フル ruleset） |
| `maven-pmd-plugin` | `verify`（フル ruleset） |
| `spotbugs-maven-plugin`（find-sec-bugs 同梱） | `verify` |
| `jacoco-maven-plugin` | `test`（prepare-agent）/ `verify`（report + check） |
| `dependency-check-maven` | default では未バインド、CI で明示実行 |
| `frontend-maven-plugin` | `generate-resources` / `process-resources` |
| `maven-dependency-plugin` | `lint-setup` プロファイルでのみ動作 |

#### Maven プロファイル

| profile | 用途 |
|---|---|
| `fast`（default active） | Lint・テストは走るが OWASP DC は skip |
| `ci-mr` | OWASP DC 追加 + JaCoCo しきい値検証有効化 |
| `ci-main` | `ci-mr` と同等 |
| `skip-frontend` | frontend-maven-plugin を skip |
| `lint-setup` | `maven-dependency-plugin` で lint 用 jar を `target/lint-tools/` に展開 |

### 3-2. `rules/` ディレクトリ構造

```
rules/
├── checkstyle/
│   ├── checkstyle.xml              # フル ruleset（google_checks.xml 逐語コピー、CI 用）
│   ├── checkstyle-blocking.xml     # pre-commit 用サブセット
│   ├── suppressions.xml
│   └── eclipse-formatter.xml       # Google公式 eclipse-java-google-style.xml フル版
├── pmd/
│   ├── ruleset.xml                 # フル ruleset（PMD 標準8カテゴリ逐語参照、除外なし）
│   └── ruleset-blocking.xml        # pre-commit 用
├── spotbugs/
│   ├── exclude.xml
│   └── find-sec-bugs-include.xml
└── jacoco/
    └── coverage-thresholds.xml
```

**標準準拠方針（PBI ID:1 指摘事項対応 2026-04-24 追記）**:

- `checkstyle.xml` は checkstyle 同梱の `google_checks.xml` を**逐語コピー**で保持する。独自定義は持たず、Spotless の google-java-format と同じスタイルガイドに整合させる。プロジェクト固有の「必ず止めたい」ルールは `checkstyle-blocking.xml`（pre-commit 用）で別管理する
- `eclipse-formatter.xml` は google/styleguide リポジトリ公開の `eclipse-java-google-style.xml` の**フル版**を保持する。VSCode の redhat.java 拡張が読む Eclipse JDT 形式の完全定義
- `pmd/ruleset.xml` は PMD 7.x の標準8カテゴリを逐語参照するだけに留める。ファイル横断の除外は原則置かず、個別抑制が必要になった場合は発生源のソースに `@SuppressWarnings("PMD.<RuleName>")` を付与する方針

#### Checkstyle blocking ruleset の設計方針

「議論の余地がない壊れ方」だけを止める。含めるルール（すべて severity=error）：

- RegexpSinglelineJava（行末スペース、タブ、`// TODO` 残留）
- UnusedImports / RedundantImport / AvoidStarImport
- EmptyBlock（空 catch 等）
- MissingOverride
- EqualsHashCode
- SimplifyBooleanExpression
- StringLiteralEquality
- ModifierOrder

含めない（CI の warning に留める）：JavadocMethod、LineLength、命名規則系、複雑度メトリクス。

#### PMD blocking ruleset

- AvoidCatchingThrowable
- EmptyCatchBlock / EmptyFinallyBlock
- AvoidBranchingStatementAsLastInLoop
- CloseResource
- DontImportSun
- UseEqualsToCompareStrings

#### SpotBugs + find-sec-bugs

`find-sec-bugs-include.xml` で以下のカテゴリを明示的に有効化：
SQL_INJECTION_*、XSS_*、PATH_TRAVERSAL_*、COMMAND_INJECTION、LDAP_INJECTION、XXE_*、WEAK_*、HARD_CODE_*

#### JaCoCo 初期しきい値

| スコープ | メトリック | 値 |
|---|---|---|
| BUNDLE | LINE COVERED_RATIO | 0.60 |
| BUNDLE | BRANCH COVERED_RATIO | 0.50 |

ID:2 のコード投入後に再評価する前提の暫定値。

### 3-3. pre-commit での Checkstyle / PMD 高速実行

ボイラープレート受領先で Maven ローカルキャッシュの位置（`~/.m2/repository` 以外の場合含む）が変わっても動くよう、
**Maven 自身に jar を解決させ、プロジェクト配下の `target/lint-tools/` に配置してから直接 Java CLI で起動**する方式を取る。

- `pom.xml` に `<checkstyle.version>` / `<pmd.version>` を一元宣言
- `lint-setup` プロファイルで `maven-dependency-plugin` の `copy` goal を用いて以下を配置:
  - Checkstyle: `target/lint-tools/checkstyle-all.jar`（`classifier=all`）
  - PMD: `target/lint-tools/pmd-bin-<version>/` （`type=zip` を展開）
  - google-java-format: `target/lint-tools/google-java-format.jar`（Spotless staged 用）
- `npm install` の postinstall が `scripts/setup-lint-tools.mjs` を実行し、欠損時は `./mvnw -Plint-setup initialize` を呼ぶ
- `scripts/run-checkstyle.mjs` / `run-pmd.mjs` / `run-spotless-staged.mjs` は `target/lint-tools/` を直接参照

結果として pre-commit スクリプトは Maven 設定（ローカルリポジトリ位置）を一切知らずに済む。

### 3-4. `./mvnw verify` 実行時の挙動

```
validate  → spotless:check
compile   → Java コンパイル + frontend-maven-plugin (Vite ビルド)
test      → JUnit + JaCoCo agent
verify    → checkstyle:check / pmd:check / spotbugs:check
          → jacoco:check（`ci-*` profile のみ）
          → dependency-check:check（`ci-*` profile のみ）
package   → Spring Boot JAR 生成
```

---

## 第4章：JavaScript ビルド構成と品質ゲート

### 4-1. ディレクトリ配置

```
src/main/frontend/
├── src/
│   ├── main.js
│   ├── components/
│   └── utils/
└── tests/
    └── *.test.js

src/main/resources/static/       # Vite 出力先（gitignore）
├── js/
└── assets/

# プロジェクトルート
vite.config.js
vitest.config.js
eslint.config.js
.prettierrc.json
.prettierignore
package.json
.nvmrc
```

`.gitignore` に追記：

```
src/main/resources/static/js/
src/main/resources/static/assets/
node_modules/
```

### 4-2. `package.json` の主要項目

```json
{
  "name": "sak-dev-env-frontend",
  "private": true,
  "engines": { "node": ">=22.0.0 <23" },
  "type": "module",
  "scripts": {
    "build": "vite build",
    "build:watch": "vite build --watch",
    "test": "vitest run",
    "test:watch": "vitest",
    "test:coverage": "vitest run --coverage",
    "lint": "eslint src/main/frontend",
    "lint:fix": "eslint src/main/frontend --fix",
    "format": "prettier --write .",
    "format:check": "prettier --check .",
    "audit": "npm audit --audit-level=high",
    "lint:java:checkstyle": "node scripts/run-checkstyle.mjs",
    "lint:java:pmd": "node scripts/run-pmd.mjs",
    "secretlint": "secretlint --secretlintignore .gitignore '**/*'",
    "prepare": "husky",
    "postinstall": "node scripts/setup-lint-tools.mjs"
  }
}
```

主要 devDependencies：vite, vitest, @vitest/coverage-v8, jsdom, eslint, @eslint/js, globals, eslint-plugin-security, eslint-config-prettier, prettier, husky, lint-staged, @commitlint/cli, @commitlint/config-conventional, secretlint, @secretlint/secretlint-rule-preset-recommend。

ID:1 時点の `dependencies` は空。

### 4-3. Vite 設定方針

- 出力先: `src/main/resources/static/` 配下の `js/` と `assets/`
- **ハッシュなし、マニフェストなし**（ボイラープレート初期値。cache-busting は ID:2 以降で必要なら導入）
- `emptyOutDir: false`（他の静的リソースと共存）
- 入口は `src/main/frontend/src/main.js`

### 4-4. Vitest 設定

- `environment: 'jsdom'`
- `include: ['src/main/frontend/**/*.test.js']`
- coverage: `provider: 'v8'`, `reportsDirectory: 'target/vitest-coverage'`
- しきい値: lines 60 / branches 50 / functions 60 / statements 60

`vite.config.js` と `vitest.config.js` は分離する（可読性と責務分離）。

### 4-5. ESLint（flat config v9）

- `@eslint/js` の recommended
- `eslint-plugin-security` の recommended
- 対象は `src/main/frontend/**/*.js`
- 追加ルール: `no-console` (warn, allow warn/error)、`no-unused-vars`、`eqeqeq`、`no-var`、`prefer-const`
- `eslint-config-prettier` を最後に適用しフォーマット系ルール無効化

### 4-6. Prettier

- `printWidth: 100`、`tabWidth: 2`、`semi: true`、`singleQuote: true`、`trailingComma: 'all'`、`endOfLine: 'lf'`
- `.prettierignore` で `node_modules/`、`target/`、Vite 出力ディレクトリを除外

### 4-7. JS 品質ゲートのステージ配分

| ステージ | 実行 | 所要 |
|---|---|---|
| `pre-commit`（lint-staged） | 変更 JS に prettier + eslint --fix、変更全ファイルに secretlint | 数秒 |
| `pre-push` | `npm run lint` + `npm test` + `npm run audit` | 30秒〜1分 |
| `CI (MR)` | 上記 + `npm run test:coverage` + `npm run format:check` + Trivy fs | 2〜3分 |
| `CI (main)` | 上記 + `npm run build`（Docker 同梱） | MR と同じ |

### 4-8. Java / JS 間の一致方針

| 項目 | 一致 / 不一致 |
|---|---|
| 行末 | LF で統一 |
| インデント | 2スペースで統一（Java は google-java-format、JS は Prettier） |
| カバレッジ | Line 60% / Branch 50% で一致 |
| セキュリティ静的解析 | 言語ごとに役割完結（Java=SpotBugs+find-sec-bugs、JS=eslint-plugin-security+npm audit） |

---

## 第5章：Git フック構成

### 5-1. `.husky/` ディレクトリ

Husky v9 流儀（shebang なし POSIX sh）：

```
.husky/
├── pre-commit
├── commit-msg
└── pre-push
```

### 5-2. pre-commit

```sh
npx lint-staged
```

### 5-3. commit-msg

```sh
npx --no -- commitlint --edit "$1"
```

`--no` で自動インストール抑止（社内ネットワークでの意図しないレジストリアクセス防止も兼ねる）。

### 5-4. pre-push

```sh
set -e
npm run lint
npm run format:check
npm test
npm run audit
./mvnw -T 1C -Pfast verify
```

`-T 1C` で並列ビルド、`-Pfast` で OWASP DC skip。SpotBugs / Checkstyle フル / PMD フル / JUnit / JaCoCo がここで走る。

### 5-5. `lint-staged` 設定

```json
{
  "lint-staged": {
    "*.{js,mjs,cjs}": ["prettier --write", "eslint --fix --max-warnings=0"],
    "*.java": [
      "node scripts/run-spotless-staged.mjs",
      "node scripts/run-checkstyle.mjs",
      "node scripts/run-pmd.mjs"
    ],
    "*.{json,md,yml,yaml}": ["prettier --write"],
    "*": ["secretlint"]
  }
}
```

### 5-6. commitlint 設定

- extends: `@commitlint/config-conventional`
- `subject-case: [0]`（日本語件名許容）
- `header-max-length: [2, 'always', 100]`
- `type-enum` で `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `revert` を許容

### 5-7. Secretlint 設定

- rules: `@secretlint/secretlint-rule-preset-recommend`
- `.secretlintignore` に `node_modules/`、`target/`、Vite 出力、`.husky/_/` を除外

### 5-8. `--no-verify` ポリシー

技術的には可能。原則禁止とし、例外時はコミットメッセージに理由を残す運用。実効性はサーバ側（GitLab ブランチ保護 + CI パイプライン必須）で担保する。

### 5-9. `scripts/` ラッパー一覧

```
scripts/
├── setup-lint-tools.mjs         # ./mvnw -Plint-setup initialize を呼ぶ
├── run-checkstyle.mjs           # target/lint-tools/checkstyle-all.jar
├── run-pmd.mjs                  # target/lint-tools/pmd-bin-*/bin/pmd
└── run-spotless-staged.mjs      # google-java-format.jar を直接起動
```

いずれも `target/lint-tools/` 欠損時に `setup-lint-tools.mjs` で自己回復。

---

## 第6章：CI/CD パイプライン

### 6-1. 前提インフラ

- GitLab セルフホスト版
- Runner: `docker` executor
- Container Registry: GitLab 同梱のものを標準、不在の場合は条件付き skip
- Docker-in-Docker 利用: `docker:27-dind`, `DOCKER_TLS_CERTDIR: "/certs"`

### 6-2. ステージ構成

```yaml
stages:
  - setup
  - quality
  - security
  - package
  - scan
  - pages
  - release
```

### 6-3. パイプラインのバリエーション

| トリガ | 動く stage |
|---|---|
| MR パイプライン | setup → quality → security → package (Docker build まで) → scan |
| main パイプライン | setup → quality → security → package → scan → pages → release |

feature ブランチ単独 push ではパイプライン非起動（`workflow:rules` で MR / main に限定）。

### 6-4. ジョブ一覧

| ジョブ | stage | イメージ | 主要コマンド |
|---|---|---|---|
| setup:maven | setup | maven:3.9-eclipse-temurin-21 | `./mvnw -N dependency:go-offline -Plint-setup initialize` |
| setup:node | setup | node:22 | `npm ci` |
| lint:format | quality | node:22 | `npm run format:check` |
| lint:eslint | quality | node:22 | `npm run lint` |
| lint:checkstyle | quality | maven:3.9-eclipse-temurin-21 | `./mvnw checkstyle:check` |
| lint:pmd | quality | 同上 | `./mvnw pmd:check` |
| lint:spotbugs | quality | 同上 | `./mvnw compile spotbugs:check` |
| test:java | quality | 同上 | `./mvnw test` |
| test:js | quality | node:22 | `npm run test:coverage` |
| secret:gitleaks | quality | zricethezav/gitleaks | `gitleaks detect --source=. --no-git=false` |
| coverage:java | quality | maven:3.9-eclipse-temurin-21 | `./mvnw -Pci-mr jacoco:report jacoco:check` |
| sca:owasp | security | maven:3.9-eclipse-temurin-21 | `./mvnw -Pci-mr dependency-check:check` |
| sca:trivy-fs | security | aquasec/trivy | `trivy fs --severity HIGH,CRITICAL --exit-code 1 .` |
| build:jar | package | maven:3.9-eclipse-temurin-21 | `./mvnw -DskipTests package` |
| build:image | package | docker:27 + docker:27-dind | `docker build` + `syft` で SBOM 生成 |
| lint:hadolint | scan | hadolint/hadolint | `hadolint Dockerfile` |
| scan:trivy-image | scan | aquasec/trivy | `trivy image --severity HIGH,CRITICAL --exit-code 1 <image>` |
| pages | pages | node:22 | docusaurus build（main のみ） |
| release:image | release | docker:27 + dind | Container Registry に push（main かつ `CI_REGISTRY` 設定時のみ） |

### 6-5. キャッシュ戦略

| キー | パス | 共有範囲 |
|---|---|---|
| `maven-$CI_COMMIT_REF_SLUG` | `.m2/repository/` | ブランチ単位 |
| `node-$CI_COMMIT_REF_SLUG` | `node_modules/` | ブランチ単位 |
| `lint-tools` | `target/lint-tools/` | プロジェクト横断 |
| `owasp-dc-data` | `.owasp-dc-data/` | プロジェクト横断 |

CI ジョブ内でのみ `-Dmaven.repo.local=.m2/repository` を付与（GitLab Runner のキャッシュ機構はジョブ実行ディレクトリ配下しか拾えないため）。開発者のローカルには影響しない。

### 6-6. アーティファクト

| 出力元 | 内容 | 保持 |
|---|---|---|
| test:java | JaCoCo exec | 1週間 |
| test:js | Vitest lcov | 1週間 |
| build:jar | `target/*.jar` | 1週間 |
| build:image | image ID + SBOM (cyclonedx-json) | 1週間 |
| pages | `public/` | 1週間 |

### 6-7. `.gitlab-ci.yml` 構造

ルート `.gitlab-ci.yml` は `include` のみ。ジョブ実体は `.gitlab/ci/` 配下に分割：

```
.gitlab/ci/
├── _defaults.yml
├── setup.yml
├── quality.yml
├── security.yml
├── package.yml
├── scan.yml
├── pages.yml
└── release.yml
```

### 6-8. 社内ミラー差し替え点

`.gitlab/ci/_defaults.yml` にすべての公式イメージ名を集約：

```yaml
variables:
  IMAGE_MAVEN: "maven:3.9-eclipse-temurin-21"
  IMAGE_NODE: "node:22"
  IMAGE_DOCKER: "docker:27"
  IMAGE_DIND: "docker:27-dind"
  IMAGE_TRIVY: "aquasec/trivy:latest"
  IMAGE_GITLEAKS: "zricethezav/gitleaks:latest"
  IMAGE_HADOLINT: "hadolint/hadolint:latest-alpine"
```

### 6-9. ブランチ保護運用（README に明記）

- main 直接 push 禁止
- MR 必須、パイプライン成功を merge 条件化
- `--no-verify` バイパスはこのサーバ側ゲートで実効性担保

---

## 第7章：Docker イメージ構成

### 7-1. 設計目標

- ランタイム極小化（JRE ベース）
- hadolint 既定ルール準拠
- Trivy の CVE ノイズ最小化
- 非 root 実行
- ヘルスチェック内蔵（Actuator 依存、ID:2 側前提）
- SBOM 生成
- 案件側カスタム容易（ARG で切替点）

### 7-2. ベースイメージ

**採用: `eclipse-temurin:21-jre-jammy`**（glibc 安定、Temurin 公式保守、Ubuntu 22.04 slim、~170MB）

Alpine は musl libc 由来の不具合リスク、OpenJDK 公式は最近の保守不安定を理由に不採用。

### 7-3. Dockerfile（マルチステージ）

設計ポイント：

1. 依存解決と Vite ビルドを先行レイヤに（ソース変更時にキャッシュが効く）
2. BuildKit の `--mount=type=cache` で `.m2` / `.npm` を永続化
3. Spring Boot Layered JAR（`java -Djarmode=tools -jar app.jar extract --layers`）で依存・ローダ・アプリを分離
4. `ARG JDK_IMAGE` / `ARG JRE_IMAGE` でベースイメージ切替可能
5. `HEALTHCHECK` は Actuator 前提でコメント付き（ID:2 で Actuator 依存追加時に有効化）
6. `--chown=app:app` で非 root ユーザー向けに権限明示
7. `ENTRYPOINT ["sh", "-c", ...]` で `JAVA_OPTS` 展開、`exec` で PID 1 を Java プロセスに

詳細は Dockerfile 本体（第9章のファイル構成に記載）を参照。

### 7-4. `.dockerignore`

- ビルド成果物（`target/`、`node_modules/`、Vite 出力）
- VCS / IDE / ドキュメント
- CI 設定（`.gitlab/`、`.gitlab-ci.yml`、`.husky/`）
- Lint 設定（`rules/`、`eslint.config.js` 等）

### 7-5. SBOM 生成

`build:image` ジョブ末尾で `syft` を使い CycloneDX フォーマットで `sbom.cdx.json` を出力、artifact として 90 日保管。`docker sbom` を避ける理由は Docker CE のバージョン差異による挙動不安定性。

### 7-6. レジストリプッシュ戦略

| タグ | タイミング |
|---|---|
| `:$CI_COMMIT_SHA` | main の全 push |
| `:$CI_COMMIT_SHORT_SHA` | main の全 push |
| `:latest` | main の全 push |
| `:vX.Y.Z` | Git タグ付与時 |

`CI_REGISTRY` 環境変数が未設定なら `release:image` ジョブは skip（レジストリ不在案件への配慮）。

### 7-7. ローカル Docker 運用手順（README 記載）

```bash
docker build -t sak-dev-env:local .
docker run --rm -p 8080:8080 sak-dev-env:local
docker run --rm aquasec/trivy image sak-dev-env:local
```

---

## 第8章：docusaurus と GitLab Pages

### 8-1. スコープ

- ID:1: docusaurus プロジェクト雛形、npm scripts、Pages デプロイジョブ、README ガイド
- ID:2: サンプルアプリのドキュメント本文

### 8-2. ディレクトリ構造

```
docs/
├── docusaurus.config.js
├── sidebars.js
├── package.json                # docs 専用（ルートと分離）
├── static/img/
├── docs/
│   └── intro.md                # ID:1 で雛形のみ
└── src/
    ├── pages/index.js
    └── css/custom.css
```

**docs 専用 `package.json` を置く理由**：docusaurus の重量依存を Vite / ESLint / Vitest 系と混ぜない、CI キャッシュを分離、案件側が docs を切り離しやすくする。

### 8-3. `docs/package.json`

- `@docusaurus/core`, `@docusaurus/preset-classic` (^3.x)
- `react`, `react-dom` (^18.x)
- scripts: start / build / serve / clear
- engines: node >=22.0.0 <23

### 8-4. `docusaurus.config.js` の要点

- `url` / `baseUrl` を `CI_PAGES_URL` 環境変数で動的決定（ボイラープレート受領先の GitLab 設定に自動追従）
- `onBrokenLinks: 'throw'` で CI で壊れたリンク検知
- `i18n.defaultLocale: 'ja'`
- `blog: false`（サンプル用途で不要）

### 8-5. ルート README 動線

- 本番（Pages）: `$CI_PAGES_URL` プレースホルダ記載
- ローカル確認: `cd docs && npm install && npm start`
- ドキュメント追加: `docs/docs/` 配下に md/mdx、`sidebars.js` で構造化

### 8-6. `.gitlab/ci/pages.yml`

```yaml
pages:
  stage: pages
  image: $IMAGE_NODE
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
  cache:
    key: "docs-$CI_COMMIT_REF_SLUG"
    paths: [docs/node_modules/]
  before_script: [cd docs, npm ci]
  script: [npm run build, mv build ../public]
  artifacts:
    paths: [public]
    expire_in: 1 week
```

GitLab Pages の規約（ジョブ名 `pages` 固定、公開パス `public/` 固定）に準拠。

### 8-7. Lint / Secret スキャン対象

- lint-staged の `*.{md,json}` エントリで docs/ 配下も Prettier 整形
- Secretlint は全ファイル対象のため docs/ もカバー
- ESLint は ID:1 時点で docs/ を対象外（`src/main/frontend/**/*.js` に限定）

### 8-8. docusaurus 初期化やり直し手順（README）

```bash
cd docs
npx create-docusaurus@latest . classic --javascript
# 生成後、docusaurus.config.js の url/baseUrl を 8-4 の形に書き換える
```

---

## 第9章：最終ディレクトリ構造

★=新規、△=修正、無印=既存維持。

```
sak-dev-env/
├── .gitlab/                          ★
│   └── ci/
│       ├── _defaults.yml             ★
│       ├── setup.yml                 ★
│       ├── quality.yml               ★
│       ├── security.yml              ★
│       ├── package.yml               ★
│       ├── scan.yml                  ★
│       ├── pages.yml                 ★
│       └── release.yml               ★
├── .gitlab-ci.yml                    ★
├── .husky/
│   ├── pre-commit                    ★
│   ├── commit-msg                    ★
│   └── pre-push                      ★
├── .mvn/                             既存
├── .vscode/
│   ├── launch.json                   △ Vitest Debug 追加
│   ├── settings.json                 ★
│   └── tasks.json                    ★
├── .editorconfig                     ★
├── .gitattributes                    既存
├── .gitignore                        △
├── .nvmrc                            ★
├── .prettierignore                   ★
├── .prettierrc.json                  ★
├── .secretlintignore                 ★
├── .secretlintrc.json                ★
├── AGENTS.md                         既存
├── CLAUDE.md                         既存
├── Dockerfile                        ★
├── .dockerignore                     ★
├── HELP.md                           既存
├── PBI.md                            既存
├── README.md                         △
├── commitlint.config.js              ★
├── docs/                             ★
│   ├── docusaurus.config.js          ★
│   ├── package.json                  ★
│   ├── sidebars.js                   ★
│   ├── docs/intro.md                 ★
│   ├── src/pages/index.js            ★
│   ├── src/css/custom.css            ★
│   └── static/img/.gitkeep           ★
├── eslint.config.js                  ★
├── mvnw / mvnw.cmd                   既存
├── package.json                      ★
├── package-lock.json                 ★（生成）
├── pom.xml                           △
├── rules/                            ★
│   ├── checkstyle/
│   │   ├── checkstyle.xml            ★
│   │   ├── checkstyle-blocking.xml   ★
│   │   ├── suppressions.xml          ★
│   │   └── eclipse-formatter.xml     ★
│   ├── pmd/
│   │   ├── ruleset.xml               ★
│   │   └── ruleset-blocking.xml      ★
│   ├── spotbugs/
│   │   ├── exclude.xml               ★
│   │   └── find-sec-bugs-include.xml ★
│   └── jacoco/
│       └── coverage-thresholds.xml   ★
├── scripts/                          ★
│   ├── setup-lint-tools.mjs          ★
│   ├── run-checkstyle.mjs            ★
│   ├── run-pmd.mjs                   ★
│   └── run-spotless-staged.mjs       ★
├── src/
│   ├── main/
│   │   ├── frontend/                 ★
│   │   │   ├── src/main.js           ★
│   │   │   └── tests/smoke.test.js   ★
│   │   ├── java/                     既存（空）
│   │   └── resources/                既存（空）
│   └── test/java/                    既存（空）
├── vite.config.js                    ★
├── vitest.config.js                  ★
└── wrapper/                          既存
```

成果物サマリ：新規 40+ ファイル、既存修正 4 ファイル（`.gitignore` / `.vscode/launch.json` / `pom.xml` / `README.md`）。既存削除なし。

---

## 第10章：検証戦略

### 10-1. ローカル環境検証

| # | シナリオ | コマンド | 期待 |
|---|---|---|---|
| L1 | クリーン clone → セットアップ | `nvm use && npm install && ./mvnw -N dependency:go-offline` | 全コマンド exit 0、`target/lint-tools/` に jar 展開 |
| L2 | 空コード状態で `./mvnw verify` | `./mvnw verify` | 全検査通過 |
| L3 | Vite ビルド連動 | `./mvnw -DskipTests package` | `src/main/resources/static/js/main.js` 生成 |
| L4 | Vitest | `npm test` | smoke test pass |
| L5 | pre-commit が違反を拾う | 空白混入 `.java` を stage → commit | pre-commit fail |
| L6 | pre-commit が secret を拾う | ダミー AWS key → commit | pre-commit fail |
| L7 | commit-msg が規約違反を弾く | `git commit -m "直した"` | commit-msg fail |
| L8 | pre-push が通る | 違反なしで push | 全検査 pass |
| L9 | VSCode Task からビルド | Task: Backend: Build | 成功 |

### 10-2. CI 検証

| # | シナリオ | 期待 |
|---|---|---|
| C1 | 空 feature で MR パイプライン | 全 stage green |
| C2 | フォーマット違反コミット | `lint:format` / `lint:checkstyle` fail |
| C3 | secret コミット | `secret:gitleaks` fail |
| C4 | 既知脆弱性依存追加 | `sca:owasp` fail |
| C5 | main マージで image build | `build:image` green、SBOM artifact |
| C6 | 古い JRE タグで build | `scan:trivy-image` fail |
| C7 | main マージで Pages 公開 | `$CI_PAGES_URL` で docusaurus 表示 |
| C8 | 2 回目以降のキャッシュ | setup ジョブが短縮 |

### 10-3. 受け入れチェックリスト（PBI に追記）

- [ ] セットアップ 3 コマンドが exit 0（L1）
- [ ] `.vscode/` 共有設定がリポジトリにコミット（L9）
- [ ] pre-commit / commit-msg / pre-push が動作（L5〜L8）
- [ ] MR パイプラインが green（C1）
- [ ] violation で MR が赤くなる（C2〜C4、C6）
- [ ] main で Docker image + SBOM 生成（C5）
- [ ] docusaurus が Pages 閲覧可能（C7）

### 10-4. 非目標

- 実アプリ実装（ID:2）
- カバレッジしきい値の妥当な調整（ID:2 実装後）
- 本番 JVM チューニング値（案件依存）
- シークレット管理（Vault 連携等）は案件要件次第
- デプロイ（Docker image 作成まで、以降は案件依存）

---

## 次のステップ

本設計を `superpowers:writing-plans` スキルに引き渡し、実装計画（どの順でファイルを作成・修正するか、依存順序、検証ポイント）を策定する。
