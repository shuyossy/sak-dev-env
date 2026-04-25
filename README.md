# sak-dev-env

モダンな開発環境のサンプルを提供する Spring Boot + Thymeleaf + Vite ボイラープレート。シフトレフト（セキュリティも含めたソフトウェア品質の担保）を大前提として、ローカルでの pre-commit/pre-push フックと GitLab CI/CD パイプラインで、バグや脆弱性をリモートに持ち込まない・デプロイさせない仕組みを提供する。

## 前提環境

以下を開発者マシンにインストール済みであること：

| ツール  | バージョン | 備考                                                                            |
| ------- | ---------- | ------------------------------------------------------------------------------- |
| JDK     | 21 (LTS)   | `java -version` で 21 が出ること。macOS なら `/usr/libexec/java_home -V` で確認 |
| Node.js | 22 (LTS)   | `.nvmrc` で固定。`nvm use` で切替                                               |
| Docker  | 27 以降    | Docker Desktop または Docker Engine。BuildKit 対応が必要                        |

### VSCode 拡張機能（推奨、動作前提）

以下がインストール済みであることを前提に `.vscode/settings.json` / `.vscode/tasks.json` が設計されています：

- Extension Pack for Java (`vscjava.vscode-java-pack`)
- Spring Boot Extension Pack (`vmware.vscode-boot-dev-pack`)
- ESLint (`dbaeumer.vscode-eslint`)
- Prettier (`esbenp.prettier-vscode`)
- EditorConfig (`editorconfig.editorconfig`)
- Checkstyle for Java (`shengchen.vscode-checkstyle`)
- GitLab Workflow (`gitlab.gitlab-workflow`)

## 初回セットアップ

```bash
nvm use                             # .nvmrc に従って Node 22 へ切替
npm install                         # 依存導入 + husky 有効化 + lint-tools 自動展開
./mvnw -N dependency:go-offline     # Maven 依存のウォームアップ
```

`npm install` の `postinstall` で `scripts/setup-lint-tools.mjs` が走り、Checkstyle/PMD/google-java-format の jar が `target/lint-tools/` に自動配置されます。これにより開発者は追加のバイナリインストール不要。

### 動作確認

```bash
./mvnw test      # Java ユニットテスト + Vite ビルド連携
npm test         # Vitest (フロントエンド)
npm run build    # Vite 本番ビルド（src/main/resources/static/ に出力）
```

すべて BUILD SUCCESS / pass であれば環境整備完了です。

## 開発サイクル

### コミットまで（pre-commit / commit-msg）

`git commit` 時に以下が自動実行されます：

- **pre-commit**（所要 〜15 秒）：
  - lint-staged が変更ファイル毎に：
    - `*.java` → Spotless (google-java-format) → Checkstyle blocking ruleset → PMD blocking ruleset
    - `*.{js,mjs,cjs}` → Prettier → ESLint (`--max-warnings=0`)
    - `*.{json,md,yml,yaml}` → Prettier
    - 全ファイル → Secretlint でシークレット検知
- **commit-msg**：commitlint で Conventional Commits 規約を検証
  - 許可 type: `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `revert`

### プッシュまで（pre-push）

`git push` 時に以下が自動実行されます（所要 1〜3 分）：

- ESLint 全体
- Prettier `format:check`
- Vitest 全テスト
- `npm audit` (`--audit-level=high`)
- `./mvnw -Pfast verify`：Checkstyle/PMD フル ruleset + SpotBugs+find-sec-bugs + JUnit + JaCoCo（OWASP DC は CI 側）

### `--no-verify` の扱い

ローカルフックは `git commit --no-verify` / `git push --no-verify` でバイパス可能ですが、**原則禁止**。例外的にバイパスした場合は、コミットメッセージ本文に理由を記載してください。CI 側で同等以上のチェックが必ず走るため、最終的な品質ゲートは GitLab のブランチ保護（main への直接 push 禁止 + MR パイプライン成功を merge 条件化）で担保します。

## CI/CD パイプライン

`.gitlab-ci.yml` + `.gitlab/ci/*.yml` が以下 2 系統のパイプラインを定義します：

| トリガ                | Stage 構成                                  | 概要                                                                                           |
| --------------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| **MR パイプライン**   | setup → quality → security → package → scan | Merge Request 作成・更新時。main へのマージ可否判定。Docker image は build まで（push しない） |
| **main パイプライン** | MR と同じ + pages + release                 | main ブランチ push 時。Container Registry に image push、GitLab Pages に docusaurus デプロイ   |

主要ジョブ：

- **quality stage**：Prettier / ESLint / Checkstyle / PMD / SpotBugs+find-sec-bugs / JUnit / Vitest + coverage / Gitleaks
- **security stage**：OWASP Dependency-Check (Java SCA) / Trivy fs (JS SCA)
- **package stage**：Spring Boot JAR / Docker image (BuildKit, multi-stage) + SBOM (syft → CycloneDX)
- **scan stage**：hadolint / Trivy image（CVE HIGH 以上で fail）
- **pages stage**：docusaurus → GitLab Pages デプロイ（main 限定）
- **release stage**：Container Registry に image push（main 限定、`CI_REGISTRY` 設定時のみ）

詳細は [`docs/plans/2026-04-22-dev-env-setup-design.md`](docs/plans/2026-04-22-dev-env-setup-design.md)（設計書）および [`docs/plans/2026-04-22-dev-env-setup.md`](docs/plans/2026-04-22-dev-env-setup.md)（実装計画書）を参照してください。

## 社内プライベートネットワーク環境への適用

本ボイラープレートは社内プライベートネットワークから Maven Central および npm registry にアクセス可能な環境を前提としていますが、それ以外の外部インターネット接続が必要な箇所は以下 4 点です。社内ミラー / プロキシを設定してください：

| #   | 設定箇所                                                                             | 切替対象                                           | 既定値                               |
| --- | ------------------------------------------------------------------------------------ | -------------------------------------------------- | ------------------------------------ |
| 1   | `.gitlab/ci/_defaults.yml` の `IMAGE_*` 変数                                         | Docker Hub の各種公式イメージ                      | `maven:3.9-eclipse-temurin-21` 等    |
| 2   | `.gitlab/ci/_defaults.yml` の `TRIVY_DB_REPOSITORY` / `TRIVY_JAVA_DB_REPOSITORY`     | Trivy 脆弱性 DB（`ghcr.io/aquasecurity/trivy-db`） | コメントアウト済（既定は ghcr）      |
| 3   | `pom.xml` の `ci-mr` profile 内 OWASP Dependency-Check 設定 (`<nvdApiServerUrl>` 等) | NIST NVD への直接アクセス                          | コメントアウト済（既定は NIST 直接） |
| 4   | `pom.xml` の `<node.download.root>` / `<npm.download.root>` プロパティ               | `nodejs.org/dist/` からの Node バイナリ取得        | `https://nodejs.org/dist/`           |

## Docker

```bash
# ローカルビルド
DOCKER_BUILDKIT=1 docker build -t sak-dev-env:local .

# 起動
docker run --rm -p 8080:8080 sak-dev-env:local

# Trivy スキャン（ローカル検証用）
docker run --rm aquasec/trivy:latest image sak-dev-env:local
```

Dockerfile はマルチステージ（builder: `eclipse-temurin:21-jdk-jammy`、runtime: `eclipse-temurin:21-jre-jammy`）。Spring Boot Layered JAR と BuildKit cache mount で初回以降のビルドを高速化。社内ミラー利用時は `--build-arg JDK_IMAGE=... --build-arg JRE_IMAGE=...` で切替可能。

## ドキュメント

`docs/` 配下は docusaurus プロジェクトです（ルートの `package.json` とは独立）。

```bash
cd docs
npm install
npm start                # http://localhost:3000 で起動
npm run build            # 本番ビルド → docs/build/
```

本番公開は GitLab Pages（main ブランチへの push 時に自動デプロイ）。URL は GitLab CI の `$CI_PAGES_URL` に従います。

## ディレクトリ構造

```
.
├── .gitlab/ci/              CI stage 定義（ジョブ分割）
├── .husky/                  Git フック（pre-commit / commit-msg / pre-push）
├── .vscode/                 共有 VSCode 設定（settings / tasks / launch）
├── docs/                    docusaurus（ドキュメント）
│   └── plans/               設計書・実装計画書（本リポジトリの設計記録）
├── rules/                   Checkstyle / PMD / SpotBugs ruleset（full + blocking）
├── scripts/                 lint-staged 用ラッパー（Java tool 起動を Node から隠蔽）
├── src/
│   ├── main/frontend/       フロントエンドソース（Vite）
│   ├── main/java/           Spring Boot ソース
│   └── main/resources/      Spring Boot リソース + Vite 出力先（gitignore）
├── Dockerfile               マルチステージビルド
├── pom.xml                  Maven（Spring Boot + 各種 lint / SCA プラグイン）
├── package.json             フロントエンド + ルート開発ツール（husky / lint-staged / commitlint 等）
└── <設定ファイル群>         vite.config.js / vitest.config.js / eslint.config.js / .prettierrc.json / commitlint.config.js / .secretlintrc.json / .editorconfig / .nvmrc / .gitignore / .gitattributes
```

## ライセンス

未定。
