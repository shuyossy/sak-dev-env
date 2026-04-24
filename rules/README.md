# Java 品質担保戦略（rules/ ガイド）

このドキュメントは、本ボイラープレートにおける **Java の品質担保戦略** を初心者にも分かる形で説明するためのものです。どの開発サイクルのフェーズで、どのツールが、何をチェックして、何をチェックしないのか、を網羅的にまとめます。

`rules/` 配下の設定ファイル（Checkstyle / PMD / SpotBugs）と、それを呼び出す `pom.xml` / `.husky/` / `.gitlab/ci/` / `scripts/` / `.vscode/` が本戦略の実装です。

---

## 1. 大局観：シフトレフトの 4 階層

バグ・脆弱性・スタイル逸脱を **リモートに持ち出さず、デプロイもさせない** ことが目的です。同じ観点を複数のフェーズで段階的に強化します。

```
[ Editor ]  保存するたびに自動整形（ヒューマンフィードバックを最速にする）
     ↓
[ pre-commit ]  変更ファイルだけを軽量チェック（コミットを壊さない、ただし致命は止める）
     ↓
[ pre-push ]  リポジトリ全体を完全静的解析 + ユニットテスト（リモート push 前の最終検問）
     ↓
[ CI (MR / main) ]  pre-push と同等 + カバレッジ閾値 + SCA + 秘密情報 + イメージスキャン
```

- **後ろのフェーズは前のフェーズを内包する**（例: pre-push に通ったコードは、CI でも pre-push と同じ項目を再検証される）。
- **前のフェーズは後ろのフェーズの 部分集合 ＋ 速度重視**（例: pre-commit は変更ファイルだけを対象に、致命的パターンだけ走らせる）。

---

## 2. フェーズ × ツール 責務マトリクス（早見表）

以下の記号で集約します：

- ✅ 実行し **違反で build/commit/push/CI を失敗させる**（hard gate）
- 📝 実行するが **情報表示のみ、違反でも build は落ちない**（soft gate / 助言）
- ➖ 実行しない

hard gate と soft gate を分けるのは、シフトレフトの効果を最大化しつつ開発者のフリクションを最小化するため。重大な欠陥（フォーマット崩れ / 致命的バグ / セキュリティ / 依存 CVE / カバレッジ未達）は ✅ で確実に止め、スタイル微調整の気付きは 📝 で流す、という役割分担を明示的に設計している。

| 観点                                   | ツール                                                 | Editor      | pre-commit              | pre-push              | CI MR                      | CI main |
| -------------------------------------- | ------------------------------------------------------ | ----------- | ----------------------- | --------------------- | -------------------------- | ------- |
| フォーマット適用                       | google-java-format (jar)                               | ➖          | ✅ (差分 in-place)      | ➖                    | ➖                         | ➖      |
| フォーマット適用                       | VSCode redhat.java + eclipse-formatter.xml             | ✅ (保存時) | ➖                      | ➖                    | ➖                         | ➖      |
| フォーマット差分検知                   | Spotless Maven Plugin                                  | ➖          | ➖                      | ✅ (`spotless:check`) | ✅                         | ✅      |
| 致命的スタイル/コード（hard gate）     | Checkstyle (`checkstyle-blocking.xml`)                 | ➖          | ✅ (差分 / pre-commit)  | ✅ (全体 / `verify`)  | ✅ (`checkstyle:check`)    | ✅      |
| Google Java Style 全規則（soft gate）  | Checkstyle (`checkstyle-advisory.xml` = google_checks) | ➖          | ➖                      | 📝 (`verify`)         | 📝 (`checkstyle:check`)    | 📝      |
| 致命的バグ・誤用                       | PMD (`ruleset-blocking.xml`)                           | ➖          | ✅ (差分)               | ➖                    | ➖                         | ➖      |
| PMD 7.x 標準全カテゴリ                 | PMD (`ruleset.xml`)                                    | ➖          | ➖                      | ✅ (`verify`)         | ✅ (`pmd:check`)           | ✅      |
| バイトコード解析（バグ＋セキュリティ） | SpotBugs + FindSecBugs                                 | ➖          | ➖                      | ✅ (`verify`)         | ✅ (`spotbugs:check`)      | ✅      |
| ユニットテスト                         | JUnit (surefire)                                       | ➖          | ➖                      | ✅ (`test`)           | ✅                         | ✅      |
| カバレッジ計測（レポート生成のみ）     | JaCoCo report                                          | ➖          | ➖                      | ✅                    | ✅                         | ✅      |
| カバレッジ閾値チェック                 | JaCoCo check (ci-mr profile)                           | ➖          | ➖                      | ➖                    | ✅ (LINE≥60% / BRANCH≥50%) | ✅      |
| 依存脆弱性 (Java)                      | OWASP Dependency-Check (ci-mr profile)                 | ➖          | ➖                      | ➖                    | ✅ (CVSS≥7 で fail)        | ✅      |
| 依存脆弱性 (FS / JS)                   | Trivy fs                                               | ➖          | ➖                      | ➖                    | ✅ (HIGH/CRITICAL)         | ✅      |
| 秘密情報（ファイル内容）               | Secretlint                                             | ➖          | ✅ (差分含む全ファイル) | ➖                    | ➖                         | ➖      |
| 秘密情報（Git 履歴）                   | Gitleaks                                               | ➖          | ➖                      | ➖                    | ✅                         | ✅      |
| コミットメッセージ                     | Commitlint                                             | ➖          | ✅ (commit-msg hook)    | ➖                    | ➖                         | ➖      |
| Dockerfile lint                        | Hadolint                                               | ➖          | ➖                      | ➖                    | ✅                         | ✅      |
| Container Image 脆弱性                 | Trivy image                                            | ➖          | ➖                      | ➖                    | ✅                         | ✅      |
| SBOM 生成                              | Syft (CycloneDX)                                       | ➖          | ➖                      | ➖                    | ✅                         | ✅      |

> 参考値：pre-commit ≈ 15 秒、pre-push ≈ 1〜3 分、CI パイプライン全体 ≈ 数分〜十数分（キャッシュ有無による）。

---

## 3. フェーズ別の詳細

### 3.1 Editor（VSCode）

実装：`.vscode/settings.json`

| 設定                                                               | 効果                                                        |
| ------------------------------------------------------------------ | ----------------------------------------------------------- |
| `editor.formatOnSave: true`                                        | 保存時に自動整形                                            |
| `"[java]": { "editor.defaultFormatter": "redhat.java" }`           | Java は Red Hat 拡張で整形                                  |
| `java.format.settings.url: rules/checkstyle/eclipse-formatter.xml` | Google Java Style 準拠の Eclipse JDT フォーマッタ設定を強制 |

- **見る**：ファイル保存時のフォーマット適用。
- **見ない**：ロジックのバグ、スタイル以外のコード品質、セキュリティ。拡張を入れていない開発者にはそもそも効かない（強制力なし）。
- **狙い**：ヒューマンフィードバックを最速で返し、pre-commit 以降で「ほぼ問題にならない」状態を目指す。

> 前提拡張機能は [ルート README の "VSCode 拡張機能" 節](../README.md#vscode-拡張機能推奨動作前提) を参照。

### 3.2 pre-commit（`.husky/pre-commit` → `npx lint-staged`）

`package.json` の `lint-staged` 定義が本体です。**変更ファイルだけ**を対象にします。

```jsonc
"lint-staged": {
  "*.java": [
    "node scripts/run-spotless-staged.mjs", // 1) google-java-format で in-place 整形
    "node scripts/run-checkstyle.mjs",      // 2) Checkstyle blocking ruleset
    "node scripts/run-pmd.mjs"              // 3) PMD blocking ruleset
  ],
  "*.{js,mjs,cjs}": ["prettier --write", "eslint --fix --max-warnings=0"],
  "*.{json,md,yml,yaml}": ["prettier --write"],
  "*": ["secretlint"]
}
```

**Java に関して**：

- **1) Spotless (google-java-format 1.22.0)**：
  - 見る：変更ファイルのフォーマット。
  - 動作：違反があれば **その場で in-place 修正**し、lint-staged が自動で再 stage する（コミットを止めない）。
  - 見ない：スタイル以外のコード品質。
  - pre-commit で差分走査にするため、Maven プラグインではなく jar を直接呼ぶ（`scripts/run-spotless-staged.mjs`）。

- **2) Checkstyle blocking（`rules/checkstyle/checkstyle-blocking.xml`）**：
  - 見る：以下の「議論の余地がない致命パターン」のみ（severity=error）。
    - 行末空白（`RegexpSingleline`）
    - Tab 文字（`FileTabCharacter`, `eachLine=true`）
    - EOF 改行抜け（`NewlineAtEndOfFile`）
    - 未使用 import / 重複 import / `*` import
    - 空 catch ブロック（`EmptyCatchBlock`, 変数名 `expected` は意図的 no-op として許容）
    - `@Override` 抜け
    - `equals()` と `hashCode()` 不整合
    - 冗長な boolean 式 / 文字列リテラルの `==` 比較
    - `public static final int` のような修飾子順序違反
    - 行末空白 / Tab / EOF 改行の 3 点は Spotless (google-java-format) が常に修正するため平時は冗長だが、`git commit --no-verify` で Spotless をバイパスされた際の最終保険として pre-push / CI で意味を持つので重複して残している。
  - 見ない：Google Java Style 全般（それは同じファイルの advisory 版 = `checkstyle-advisory.xml` が pre-push / CI で助言レベルで見る）。
  - スコープを絞る理由：pre-commit は高速であるべきで、かつ「まだ書きかけ」の微調整的違反で止めたくないため。
  - **同じ blocking ruleset は pre-push / CI でも再実行される**（`pom.xml` の `maven-checkstyle-plugin` / `checkstyle-blocking` execution）。pre-commit を `--no-verify` でバイパスした変更や、差分以外に混入した違反はここで必ず捕捉する。

- **3) PMD blocking（`rules/pmd/ruleset-blocking.xml`）**：
  - 見る：errorprone カテゴリから厳選した 6 種 + `CloseResource` / `EmptyControlStatement`。具体的には `AvoidCatchingThrowable` / `EmptyCatchBlock` / `EmptyControlStatement` / `AvoidBranchingStatementAsLastInLoop` / `CloseResource` / `DontImportSun` / `UseEqualsToCompareStrings`。
  - 見ない：PMD の残り全カテゴリ（bestpractices/codestyle/design/documentation/multithreading/performance/security）は pre-push 以降でまとめて走る。

**Java 以外（参考）**：

- **secretlint**（全ファイル対象）：API キー / トークン / 秘密鍵などのシークレットを正規表現で検出。履歴は見ない（それは CI の gitleaks の仕事）。
- **prettier / eslint / commitlint** の詳細はこのドキュメントのスコープ外（ルート README を参照）。

### 3.3 pre-push（`.husky/pre-push`）

```sh
npm run lint               # ESLint 全体
npm run format:check       # Prettier 差分検知
npm test                   # Vitest
npm run audit              # npm audit --audit-level=high
./mvnw -T 1C -Pfast verify # Java 静的解析 + テスト
```

`./mvnw -Pfast verify` は `fast` プロファイル（既定で active、`ci-mr` の OWASP DC / JaCoCo check を含まない）で以下が動作します：

| Maven フェーズ | 実行される内容                                                                                                                |
| -------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `validate`     | **Spotless check**（違反があれば fail）                                                                                       |
| `test`         | JUnit (surefire) + **JaCoCo report**（閾値チェックはなし）                                                                    |
| `verify`       | **Checkstyle check**（advisory + blocking の 2 execution） / **PMD check** / **SpotBugs check**（FindSecBugs プラグイン込み） |

- **Checkstyle は 2 つの ruleset で構成される**（`pom.xml` の maven-checkstyle-plugin に 2 本の execution が登録されている）：
  - **`rules/checkstyle/checkstyle-advisory.xml`（google_checks 逐語コピー / soft gate / 📝 情報表示）**
    - 見る：Google Java Style Guide の全規則（命名規約 / インデント / JavaDoc / import 整列 / 括弧の位置 等）。
    - 見ない：実行時バグ、スレッドセーフティ、セキュリティ。
    - **ビルドを落とさない**。Checker ルート直下 `severity="warning"` + plugin 既定 `violationSeverity="error"` により違反は Maven 出力に `[WARN]` として表示されるだけ。
    - これは Google 本家が google_checks を助言レベルと位置付けている設計。スタイル微調整で build が落ちるフリクションを避け、Google Java Style への「気付き」を提供することに徹する。
    - 独自ルールは増やさない方針（逐語コピー維持）。

  - **`rules/checkstyle/checkstyle-blocking.xml`（プロジェクト独自 / hard gate / ✅ ブロック）**
    - 見る：議論の余地がない致命パターンのみ（行末空白・Tab・EOF 改行・未使用 import・空 catch（`expected` 変数名は許容）・`@Override` 抜け・equals & hashCode 不整合・冗長 boolean・`String` の `==` 比較・modifier 順）。
    - 見ない：スタイル微調整（それは advisory 側）。インデント・import 並び順・WhitespaceAround などフォーマット領域は Spotless (google-java-format) が整形で担保するため blocking に重複させない（Spotless 通過後のコードに対して blocking が違反を上げるとデッドロックを生むため）。
    - **ビルド / コミットを必ず落とす**。Checker ルート直下 `severity="error"` により違反は errorCount にカウントされ、Maven でも Checkstyle CLI でも exit コードが非ゼロになる。
    - 実行経路は 2 系統：
      1. pre-commit（`scripts/run-checkstyle.mjs` 経由）→ 変更ファイル（差分）を対象に高速に走る。
      2. pre-push / CI（`maven-checkstyle-plugin` の `checkstyle-blocking` execution 経由）→ リポジトリ全体を対象に走る。
    - 同じ blocking ruleset を pre-push/CI でも再実行することで、`git commit --no-verify` でバイパスされた変更や、差分以外に混入した違反も確実に止める。

- **PMD（`rules/pmd/ruleset.xml`）**：
  - 見る：PMD 7.x の標準 8 カテゴリ（`bestpractices`, `codestyle`, `design`, `documentation`, `errorprone`, `multithreading`, `performance`, `security`）を **そのまま** 参照。
  - 見ない：カスタムルール（定義していない）。
  - **`failOnViolation=true`** なので違反は build を失敗させる。
  - 除外方針：`<exclude>` を直書きせず、個別抑制は `@SuppressWarnings("PMD.XXX")` または行末 `// NOPMD - 理由` を推奨。ルールセット全体で除外が必要になった時のみ `ruleset.xml` を編集する。

- **SpotBugs + FindSecBugs**：
  - 見る：
    - SpotBugs 本体：バイトコードを解析し、null 逆参照 / リソースリーク / Equality & HashCode の不整合 / 並行性バグ 等 400+ パターン。
    - FindSecBugs プラグイン：SQLi / XSS / XXE / Path Traversal / コマンドインジェクション / 弱い暗号 / ハードコードされたパスワード・鍵 等のセキュリティ脆弱性。`pom.xml` で spotbugs-maven-plugin の `<plugins>` に `com.h3xstream.findsecbugs:findsecbugs-plugin` を宣言しているだけで、FindSecBugs の全 detector が自動的に有効になる（include リストは不要）。
  - 見ない：ソースコードの文字列パターン（行長・空白など）。スタイルは見ない。
  - 設定：`effort=Max`（最も厳密な解析）、`threshold=Low`（Low 優先度以上を全て報告）、`check` ゴール（報告があれば build fail）。
  - 誤検知の除外は `rules/spotbugs/exclude.xml` に追記する（`<excludeFilterFile>` として参照されている）。

- **JaCoCo**：
  - 見る：`report` ゴールによりカバレッジレポート（`target/site/jacoco/`）を生成。
  - 見ない：pre-push では閾値チェックをしない（`fast` profile）。遅延を避けるため、カバレッジ閾値は CI 側に寄せている。

### 3.4 CI（GitLab MR / main パイプライン）

`.gitlab-ci.yml` が `.gitlab/ci/*.yml` を include。2 系統の pipeline が走ります：

- **MR pipeline**：`setup → quality → security → package → scan`
- **main pipeline**：MR と同じ ＋ `pages`（docusaurus 公開） ＋ `release`（image push）

Java 品質担保に関連する主な job（`ci-mr` profile が有効）：

| Stage    | Job                | 実体                                                                               | 見る                                                                                                                                                                    | 見ない                                                    |
| -------- | ------------------ | ---------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------- |
| quality  | `lint:format`      | `npm run format:check`                                                             | Prettier 差分                                                                                                                                                           | Java（Spotless は `lint:checkstyle` 経由で Maven が見る） |
| quality  | `lint:eslint`      | `npm run lint`                                                                     | JS のコード品質                                                                                                                                                         | Java                                                      |
| quality  | `lint:checkstyle`  | `./mvnw checkstyle:check@checkstyle-advisory checkstyle:check@checkstyle-blocking` | **advisory**（`checkstyle-advisory.xml` = google_checks、情報表示のみ）＋ **blocking**（`checkstyle-blocking.xml`、致命パターン、違反で fail）の 2 execution を明示起動 | ロジック / セキュリティ                                   |
| quality  | `lint:pmd`         | `./mvnw pmd:check`                                                                 | PMD 全カテゴリ（`ruleset.xml`）                                                                                                                                         | バイトコード解析                                          |
| quality  | `lint:spotbugs`    | `./mvnw compile spotbugs:check`                                                    | バグ + セキュリティ（FindSecBugs 込み）                                                                                                                                 | ソース文字列パターン                                      |
| quality  | `test:java`        | `./mvnw test`                                                                      | JUnit 実行 + JaCoCo report 生成                                                                                                                                         | カバレッジ閾値（`coverage:java` が担当）                  |
| quality  | `test:js`          | `npm run test:coverage`                                                            | Vitest + coverage                                                                                                                                                       | Java                                                      |
| quality  | `coverage:java`    | `./mvnw -Pci-mr jacoco:report jacoco:check`                                        | **LINE ≥ 60% / BRANCH ≥ 50%**（未達で fail）                                                                                                                            | ―                                                         |
| quality  | `secret:gitleaks`  | `gitleaks detect --source=. --no-git=false`                                        | Git 履歴含む秘密情報                                                                                                                                                    | ファイル単体のパターン（secretlint が補完）               |
| security | `sca:owasp`        | `./mvnw -Pci-mr dependency-check:check`                                            | Java 依存の既知 CVE（CVSS ≥ 7 で fail）                                                                                                                                 | JS / OS / Container                                       |
| security | `sca:trivy-fs`     | `trivy fs --severity HIGH,CRITICAL --ignore-unfixed .`                             | ファイルシステム / JS 依存                                                                                                                                              | ソースコードのバグ                                        |
| package  | `build:image`      | `docker build` + `syft ... -o cyclonedx-json`                                      | ―                                                                                                                                                                       | ―                                                         |
| scan     | `lint:hadolint`    | `hadolint Dockerfile`                                                              | Dockerfile のベストプラクティス違反                                                                                                                                     | コンテナ内容                                              |
| scan     | `scan:trivy-image` | `trivy image --severity HIGH,CRITICAL --ignore-unfixed`                            | OS パッケージ / 最終 image 内の依存 CVE                                                                                                                                 | Dockerfile の書き方（Hadolint が担当）                    |

> 社内プライベートネットワーク下で **NVD 直接アクセスができない場合**、OWASP DC は `pom.xml` の `ci-mr` profile にコメントで示した `<nvdApiServerUrl>` / `<nvdDatafeedUrl>` / `<nvdApiKey>` で社内ミラーに切り替えてください（詳細は [ルート README § 社内プライベートネットワーク環境への適用](../README.md#社内プライベートネットワーク環境への適用)）。

---

## 4. ツール別「見る／見ない」まとめ（俯瞰用）

| ツール                                                             | 見る（Do）                                                                                                                                                                       | 見ない（Don't）                                                                                                            | カバーフェーズ                             |
| ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| google-java-format / Spotless / eclipse-formatter.xml              | フォーマット（括弧位置 / インデント / 空白 / import 並び / 末尾改行）                                                                                                            | 命名 / セマンティクス / バグ                                                                                               | Editor, pre-commit, pre-push, CI           |
| Checkstyle (`checkstyle-blocking.xml` / hard gate)                 | 行末空白 / Tab / EOF 改行 / 未使用 import / 空 catch（`expected` 変数名は許容） / `@Override` / equals&hashCode / 文字列 `==` / 修飾子順。**違反で必ず build / commit を落とす** | Google Java Style 全般（インデント・import 並び順・WhitespaceAround 等は Spotless と衝突するため blocking に入れない）     | pre-commit（差分）＋ pre-push / CI（全体） |
| Checkstyle (`checkstyle-advisory.xml` = google_checks / soft gate) | Google Java Style の全規則（命名 / JavaDoc / ブロック構造 など）を **助言レベル（📝 情報表示のみ、build を落とさない）** で指摘                                                  | バイトコード / 依存 CVE / ランタイムバグ。hard gate はフォーマット=Spotless、致命パターン=`checkstyle-blocking.xml` に分担 | pre-push, CI                               |
| PMD (`ruleset-blocking.xml`)                                       | 致命的な error-prone パターン（7 種）                                                                                                                                            | その他 PMD ルール                                                                                                          | pre-commit                                 |
| PMD (`ruleset.xml`)                                                | PMD 7.x 標準 8 カテゴリ（bestpractices / codestyle / design / documentation / errorprone / multithreading / performance / security）                                             | フォーマット微調整 / バイトコード解析 / 依存 CVE                                                                           | pre-push, CI                               |
| SpotBugs + FindSecBugs                                             | バイトコード解析によるバグパターン + セキュリティ脆弱性（SQLi/XSS/XXE/Path Traversal/Crypto/Hard-coded secrets など）                                                            | ソースコードのスタイル / 書式                                                                                              | pre-push, CI                               |
| JUnit (surefire)                                                   | 開発者が書いた単体テスト                                                                                                                                                         | テストが未記述のコード                                                                                                     | pre-push, CI                               |
| JaCoCo                                                             | 行・分岐カバレッジ。CI では **LINE≥60% / BRANCH≥50%** を閾値に                                                                                                                   | テストが書かれているかの「質」（= 何をアサートしているか）                                                                 | pre-push (report), CI (check)              |
| OWASP Dependency-Check                                             | `pom.xml` が引き込む **Java 依存** の既知 CVE（CVSS ≥ 7 で fail）                                                                                                                | JS / OS 依存 / コード自体                                                                                                  | CI                                         |
| Trivy fs                                                           | ファイルシステム / **JS 依存 (package-lock)** の HIGH/CRITICAL CVE                                                                                                               | Java 依存（OWASP DC が担当）                                                                                               | CI                                         |
| Trivy image                                                        | 最終 Container Image の OS + 依存 CVE                                                                                                                                            | Dockerfile 記述スタイル                                                                                                    | CI                                         |
| Hadolint                                                           | Dockerfile の記述 anti-pattern                                                                                                                                                   | image の中身                                                                                                               | CI                                         |
| Syft                                                               | CycloneDX 形式の SBOM 生成                                                                                                                                                       | 脆弱性検知そのもの（Trivy が担当）                                                                                         | CI                                         |
| Secretlint                                                         | 変更を含む全ファイルのシークレット文字列                                                                                                                                         | Git 履歴                                                                                                                   | pre-commit                                 |
| Gitleaks                                                           | Git 履歴全体のシークレット                                                                                                                                                       | 現在未 stage のファイル（secretlint が補完）                                                                               | CI                                         |

---

## 4.1. なぜ Checkstyle は `checkstyle-advisory.xml` と `checkstyle-blocking.xml` の 2 ファイルに分かれているのか

`rules/checkstyle/` 配下に **役割の異なる 2 つの Checkstyle 設定** が共存しています。名前は似ていますが、**振る舞い・用途・severity のすべてが意図的に異なります**。

| 比較軸                    | `checkstyle-advisory.xml`                                                    | `checkstyle-blocking.xml`                                                                                                                             |
| ------------------------- | ---------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| 中身                      | Google 公式の google_checks.xml 逐語コピー（350 以上のルール）               | プロジェクト独自の致命パターン集合（10 前後のルール）                                                                                                 |
| Checker ルートの severity | `warning`                                                                    | `error`                                                                                                                                               |
| 違反時の挙動              | Maven / CLI とも **exit 0**（`[WARN]` として情報表示のみ）                   | Maven / CLI とも **exit 非ゼロ**（build / commit FAILURE）                                                                                            |
| 役割                      | **soft gate**（気付き・助言）                                                | **hard gate**（絶対に通さない）                                                                                                                       |
| 実行場所                  | pre-push / CI（`maven-checkstyle-plugin` / `checkstyle-advisory` execution） | **pre-commit**（`scripts/run-checkstyle.mjs`、差分対象）＋ **pre-push / CI**（`maven-checkstyle-plugin` / `checkstyle-blocking` execution、全体対象） |
| 更新方針                  | 独自改変しない（逐語コピーを維持、checkstyle の version up 時に上書き）      | プロジェクト判断で随時追加。ただし誤検知が実質ゼロで修正方法が自明なものだけ                                                                          |

### この設計の狙い

1. **"hard gate は一貫してブロックする"** を保証する：blocking ruleset は pre-commit と pre-push/CI の両方で error として検査されるため、`git commit --no-verify` でバイパスされた変更や、差分以外に混入した違反も最終的に pre-push / CI で必ず捕捉される。pre-commit 限定の hard gate では shift-left の実効性が損なわれるため、二重に走らせている。
2. **advisory は手を入れず Google 本家の設計（severity=warning）をそのまま使う**：google_checks は数百規模のルールを含むため、全部を hard gate 化するとスタイル微調整のために build が落ちる事象が多発して生産性を損なう。Google 本家が "google_checks は助言レベル" と位置付けているのと同じ運用にする。
3. **役割分担を明確にする**：「絶対に通さない / 通してもよいが気付きたい」という 2 段階のセマンティクスをファイル名レベルで分離することで、初見の開発者でも各ルールの取り扱いを一目で判断できるようにする。

### 運用上の帰結

- 新しいルールを **blocking に** 足したい場合：議論の余地がない致命パターンであることを必ず確認する（誤検知が実質ゼロで、修正方法が自明で、他のツール — Spotless / PMD / SpotBugs — でカバーされていないもの）。
- 新しいルールを **advisory に** 足したい場合：原則として足さない。google_checks.xml は逐語コピーを維持する。どうしても必要なら別途独自 ruleset ファイルを設ける設計変更を検討。
- pre-commit が重すぎると感じたら blocking ruleset の対象を絞る。pre-push / CI はどうせ全体を検査するため、pre-commit は "最速で止めたい致命" だけに集中させてよい。

---

## 5. Suppression（例外抑制）の方法

各ツールとも、**まずは個別のソース注釈で抑制**することを推奨します。ルールセット全体から外すのは最後の手段です。

| ツール     | 個別抑制                                                                                                                | 範囲抑制                                                                         |
| ---------- | ----------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Checkstyle | `@SuppressWarnings("checkstyle:<CheckName>")` または `// CHECKSTYLE.OFF: <CheckName>` … `// CHECKSTYLE.ON: <CheckName>` | `rules/checkstyle/suppressions.xml` に `<suppress files="..." checks="..."/>`    |
| PMD        | `@SuppressWarnings("PMD.<RuleName>")` または行末 `// NOPMD - 理由`                                                      | `rules/pmd/ruleset.xml` 内の `<rule ref=".../>` を展開し `<exclude name="..."/>` |
| SpotBugs   | `@SuppressFBWarnings(value = "...", justification = "...")`（com.github.spotbugs:spotbugs-annotations）                 | `rules/spotbugs/exclude.xml` に `<Match>...</Match>`                             |

抑制時は **理由をコメントまたは justification に必ず残す** こと。レビュアが後で追跡できるようにするためです。

---

## 6. ルール / バージョン更新手順

### 6.1 Checkstyle（advisory = google_checks / blocking）の更新

1. `pom.xml` の `<checkstyle.version>` を上げる。
2. advisory（google_checks）を更新する場合：同じタグの `google_checks.xml` を checkstyle 配布物（`checkstyle-all.jar` もしくは GitHub の同タグ）から取得し、`rules/checkstyle/checkstyle-advisory.xml` の **本家ヘッダ以降** を逐語で上書きする（独自プロパティ `org.checkstyle.google.suppressionfilter.config` の参照は維持される）。
3. blocking を更新する場合：`rules/checkstyle/checkstyle-blocking.xml` にルールを追加／削除する。ファイル冒頭の「収録方針」基準（誤検知が実質ゼロ・生産性を落とさない粒度）を満たすことを必ず確認。
4. `./mvnw checkstyle:check` を実行し、advisory・blocking の双方の挙動を棚卸し（advisory は `[WARN]` 出力の増減、blocking は exit コードで検査）。

### 6.2 PMD の更新

1. `pom.xml` の `<pmd.version>` を上げる。
2. `rules/pmd/ruleset.xml` はカテゴリ参照のみなので通常そのまま。ただし PMD のメジャー更新時はルール名の統合・改名に注意（例：PMD 7 で `EmptyFinallyBlock` は `EmptyControlStatement` に統合）。
3. `./mvnw pmd:check` を実行し、差分を棚卸し。

### 6.3 SpotBugs / FindSecBugs の更新

1. `pom.xml` の `<spotbugs.version>` / `<find-sec-bugs.version>` を上げる。
2. `./mvnw compile spotbugs:check` を実行し、差分を棚卸し。
3. 誤検知は `rules/spotbugs/exclude.xml` に追記（必ず `<!-- 理由 -->` を書く）。
4. FindSecBugs の検出パターンリスト（<https://find-sec-bugs.github.io/bugs.htm>）で新規 detector・改名を確認。

### 6.4 google-java-format（および eclipse-formatter.xml）の更新

1. `pom.xml` の `<google-java-format.version>` を上げる。
2. `google/styleguide` の同タグ相当から `eclipse-java-google-style.xml` を取得し、`rules/checkstyle/eclipse-formatter.xml` の XML 宣言以下を差し替える（VSCode の redhat.java と整合を取るため）。
3. `./mvnw spotless:apply` でリポジトリ全体を再整形し、差分をコミット。

---

## 7. 運用上の補足

- **`--no-verify` は原則禁止**。やむを得ずバイパスした場合はコミットメッセージに理由を記載し、CI で等価以上のチェックが走ることを前提とする（詳細はルート README § `--no-verify` の扱い）。
- **pre-commit で止まったとき**：まず出力に従い `./mvnw spotless:apply` や `@SuppressWarnings` 追加で対処する。`run-*.mjs` が jar を見つけられない旨のエラーが出たら `node scripts/setup-lint-tools.mjs` を再実行。
- **pre-push が遅いと感じたとき**：`./mvnw -T 1C -Pfast verify` をローカルで先行実行しておくと push 時には Maven のインクリメンタル差分のみで済む。
- **`target/lint-tools/` の中身**：`npm install` の postinstall で自動配置される Checkstyle / PMD / google-java-format の jar 群。手動で消した場合は `node scripts/setup-lint-tools.mjs`。

---

## 関連ドキュメント

- ルート全体像・開発サイクル・社内ネットワーク対応：[`../README.md`](../README.md)
- プロジェクト制約・技術構成：[`../AGENTS.md`](../AGENTS.md)
- PBI（プロジェクトバックログ）：[`../PBI.md`](../PBI.md)
