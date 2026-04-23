# 開発環境整備（PBI ID:1）実装計画

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** シフトレフトを実現する開発環境ボイラープレート（ローカル pre-commit/pre-push フック + GitLab CI/CD パイプライン + Docker 化 + docusaurus ドキュメント基盤）を整備する。

**Architecture:** Spring Boot 3.5（Maven）+ Vite + Thymeleaf 構成で、Husky+lint-staged をフック基盤に、Maven 公式プラグインと npm ツールを組み合わせて検査を段階的に配置する。CI は GitLab（セルフホスト）前提で stage 分割、Docker イメージはマルチステージで JRE-jammy ベース。

**Tech Stack:** Java 21 / Maven 3.9 / Spring Boot 3.5.14 / Node 22 / Vite / Vitest / ESLint v9 (flat config) / Prettier / Husky v9 / commitlint / Secretlint / Checkstyle / PMD / SpotBugs + find-sec-bugs / JaCoCo / OWASP Dependency-Check / Gitleaks / Trivy / hadolint / syft / docusaurus v3 / GitLab CI

**Design reference:** `docs/plans/2026-04-22-dev-env-setup-design.md`（以降「設計書」と略記）

---

## 前提

- 設計書の内容に基づいて実装する。設計書と矛盾するコードを書かない。
- 既に `git init` 済み、ブランチ `main`、初回 scaffolding コミットと設計書コミットが存在する。
- JDK 21 と Node 22 はホストにインストール済み。nvm 利用可能。
- 各タスクの末尾で Conventional Commits 規約に則ったコミットを作る。`Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>` を付与。
- TDD が自然に適用できるコード（Java test / Vitest smoke test）ではテスト先行。TDD が不自然な設定ファイルは「記述 → ツール実行で検証 → コミット」の順とする。

---

## Phase 1: ベースライン安定化

### Task 1: Spring Boot バージョンを SNAPSHOT から stable に固定

**Files:**
- Modify: `pom.xml`（9行目 version, 54-72行目 repositories/pluginRepositories）

**Step 1: pom.xml の version を書き換え**

```xml
<!-- 変更前 -->
<version>3.5.14-SNAPSHOT</version>
<!-- 変更後 -->
<version>3.5.14</version>
```

**Step 2: `<repositories>` と `<pluginRepositories>` の spring-snapshots ブロックを削除**

54〜72 行の `<repositories>...</repositories>` と `<pluginRepositories>...</pluginRepositories>` 全体を削除。

**Step 3: 依存が解決できることを確認**

Run: `./mvnw -N dependency:resolve -q`
Expected: エラーなし、exit 0

**Step 4: コミット**

```bash
git add pom.xml
git commit -m "$(cat <<'EOF'
chore: pin Spring Boot to 3.5.14 stable

SNAPSHOT 依存を排除し、snapshot リポジトリ参照も削除。
Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: ベースライン `./mvnw test` が通ることを確認

**Step 1: テスト実行**

Run: `./mvnw test`
Expected: `DemoApplicationTests` が 1 件通過、BUILD SUCCESS

通らない場合は Java 21 インストール確認と `JAVA_HOME` を見直す。

**Step 2: コミットなし**（検証のみ）

---

## Phase 2: ベース設定ファイル

### Task 3: `.editorconfig` 作成

**Files:**
- Create: `.editorconfig`

**Step 1: 書く**

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space

[*.java]
indent_size = 4

[*.{js,mjs,cjs,ts,json,yml,yaml,md}]
indent_size = 2

[*.xml]
indent_size = 2

[*.{bat,cmd}]
end_of_line = crlf

[Makefile]
indent_style = tab
```

**Step 2: コミット**

```bash
git add .editorconfig
git commit -m "chore: add .editorconfig for consistent formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: `.nvmrc` 作成

**Files:**
- Create: `.nvmrc`

**Step 1: 書く**

```
22
```

**Step 2: 検証**

Run: `nvm use`
Expected: `Now using node v22.x.x`

**Step 3: コミット**

```bash
git add .nvmrc
git commit -m "chore: pin Node.js to 22 via .nvmrc

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: `.gitignore` 更新（Vite 出力、node_modules、docusaurus 生成物）

**Files:**
- Modify: `.gitignore`

**Step 1: 末尾に追記**

```gitignore

### Node.js / frontend ###
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

### Vite build output (to src/main/resources/static) ###
src/main/resources/static/js/
src/main/resources/static/assets/

### docusaurus build artifacts ###
docs/.docusaurus/
docs/build/
public/

### Lint tool staging ###
target/lint-tools/

### OWASP Dependency-Check DB ###
.owasp-dc-data/

### Local env files ###
.env
.env.local
```

**Step 2: 既存の `.vscode/` 除外を書き換え（共有ファイルをコミット対象にする）**

```gitignore
### VS Code ###
# 変更前：
# .vscode/
# 変更後：
.vscode/*
!.vscode/extensions.json
!.vscode/settings.json
!.vscode/tasks.json
!.vscode/launch.json
```

**Step 3: 確認**

Run: `git status --ignored --short | head -20`
Expected: `target/`, 想定ディレクトリが ignore 対象に入っている。

**Step 4: コミット**

```bash
git add .gitignore
git commit -m "chore: extend .gitignore for Node/Vite/docusaurus and share .vscode configs

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 3: Java 品質ゲート基盤

### Task 6: `rules/` ディレクトリを作成してスケルトンを配置

**Files:**
- Create: `rules/checkstyle/checkstyle.xml`
- Create: `rules/checkstyle/checkstyle-blocking.xml`
- Create: `rules/checkstyle/suppressions.xml`
- Create: `rules/pmd/ruleset.xml`
- Create: `rules/pmd/ruleset-blocking.xml`
- Create: `rules/spotbugs/exclude.xml`
- Create: `rules/spotbugs/find-sec-bugs-include.xml`
- Create: `rules/jacoco/coverage-thresholds.xml`

**Step 1: `rules/checkstyle/checkstyle.xml`（フル ruleset、独自定義）**

外部 NW 依存をなくすため、`google_checks.xml` を curl で取得する代わりに**独自の自己完結 ruleset を直接書く**。`.editorconfig` の Java 4-space 規約と整合させ、設計書 § 3-2-1 に基づき severity を error/warning に振り分ける。

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
  "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
  <property name="charset" value="UTF-8"/>
  <property name="fileExtensions" value="java"/>

  <module name="FileTabCharacter">
    <property name="severity" value="error"/>
    <property name="eachLine" value="true"/>
  </module>
  <module name="RegexpSingleline">
    <property name="severity" value="error"/>
    <property name="format" value="\s+$"/>
    <property name="message" value="行末に空白があります"/>
  </module>
  <module name="LineLength">
    <property name="severity" value="warning"/>
    <property name="max" value="120"/>
  </module>

  <module name="SuppressionFilter">
    <property name="file" value="${config_loc}/suppressions.xml" default="suppressions.xml"/>
    <property name="optional" value="true"/>
  </module>

  <module name="TreeWalker">
    <!-- インポート -->
    <module name="UnusedImports"><property name="severity" value="error"/></module>
    <module name="RedundantImport"><property name="severity" value="error"/></module>
    <module name="AvoidStarImport"><property name="severity" value="error"/></module>
    <module name="CustomImportOrder">
      <property name="severity" value="warning"/>
      <property name="customImportOrderRules" value="STATIC###STANDARD_JAVA_PACKAGE###THIRD_PARTY_PACKAGE"/>
    </module>

    <!-- 構造的破綻 -->
    <module name="EmptyBlock">
      <property name="severity" value="error"/>
      <property name="option" value="text"/>
      <property name="tokens" value="LITERAL_CATCH"/>
    </module>
    <module name="MissingOverride"><property name="severity" value="error"/></module>
    <module name="EqualsHashCode"><property name="severity" value="error"/></module>
    <module name="SimplifyBooleanExpression"><property name="severity" value="error"/></module>
    <module name="SimplifyBooleanReturn"><property name="severity" value="warning"/></module>
    <module name="StringLiteralEquality"><property name="severity" value="error"/></module>
    <module name="ModifierOrder"><property name="severity" value="error"/></module>
    <module name="RedundantModifier"><property name="severity" value="warning"/></module>

    <!-- 命名規則 -->
    <module name="TypeName"><property name="severity" value="warning"/></module>
    <module name="MethodName"><property name="severity" value="warning"/></module>
    <module name="ParameterName"><property name="severity" value="warning"/></module>
    <module name="LocalVariableName"><property name="severity" value="warning"/></module>
    <module name="MemberName"><property name="severity" value="warning"/></module>
    <module name="ConstantName"><property name="severity" value="warning"/></module>
    <module name="PackageName">
      <property name="severity" value="warning"/>
      <property name="format" value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>
    </module>

    <!-- インデント（4-space、.editorconfig と整合） -->
    <module name="Indentation">
      <property name="severity" value="error"/>
      <property name="basicOffset" value="4"/>
      <property name="braceAdjustment" value="0"/>
      <property name="caseIndent" value="4"/>
      <property name="throwsIndent" value="8"/>
      <property name="lineWrappingIndentation" value="8"/>
      <property name="arrayInitIndent" value="4"/>
    </module>

    <!-- 複雑度 -->
    <module name="CyclomaticComplexity">
      <property name="severity" value="warning"/>
      <property name="max" value="15"/>
    </module>
    <module name="NestedIfDepth">
      <property name="severity" value="warning"/>
      <property name="max" value="4"/>
    </module>

    <!-- Javadoc -->
    <module name="MissingJavadocMethod">
      <property name="severity" value="warning"/>
      <property name="scope" value="public"/>
    </module>
    <module name="JavadocMethod">
      <property name="severity" value="warning"/>
      <property name="accessModifiers" value="public"/>
    </module>
    <module name="JavadocStyle"><property name="severity" value="warning"/></module>

    <!-- その他のベストプラクティス -->
    <module name="OneStatementPerLine"><property name="severity" value="warning"/></module>
    <module name="MultipleVariableDeclarations"><property name="severity" value="warning"/></module>
    <module name="UpperEll"><property name="severity" value="warning"/></module>
    <module name="ArrayTypeStyle"><property name="severity" value="warning"/></module>
    <module name="MissingSwitchDefault"><property name="severity" value="warning"/></module>
    <module name="FallThrough"><property name="severity" value="warning"/></module>
  </module>
</module>
```

**Step 2: `rules/checkstyle/checkstyle-blocking.xml`（pre-commit 用サブセット）**

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
  "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
  <property name="charset" value="UTF-8"/>
  <property name="severity" value="error"/>
  <property name="fileExtensions" value="java"/>

  <module name="RegexpSingleline">
    <property name="format" value="\s+$"/>
    <property name="message" value="行末に空白があります"/>
  </module>
  <module name="RegexpSingleline">
    <property name="format" value="\t"/>
    <property name="message" value="タブ文字を使用しています（スペース 4 つに変更してください）"/>
  </module>

  <module name="TreeWalker">
    <module name="UnusedImports"/>
    <module name="RedundantImport"/>
    <module name="AvoidStarImport"/>
    <module name="EmptyBlock">
      <property name="option" value="text"/>
      <property name="tokens" value="LITERAL_CATCH"/>
    </module>
    <module name="MissingOverride"/>
    <module name="EqualsHashCode"/>
    <module name="SimplifyBooleanExpression"/>
    <module name="StringLiteralEquality"/>
    <module name="ModifierOrder"/>
  </module>
</module>
```

**Step 3: `rules/checkstyle/suppressions.xml`**

```xml
<?xml version="1.0"?>
<!DOCTYPE suppressions PUBLIC
  "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
  "https://checkstyle.org/dtds/suppressions_1_2.dtd">
<suppressions>
  <!-- 自動生成コードや例外的な箇所をここに追加 -->
</suppressions>
```

**Step 4: `rules/checkstyle/eclipse-formatter.xml`**

Google Java Format 配布物に同梱の Eclipse profile XML を入手して配置。もしくは PBI ID:2 でコードが入ってから整備する旨のコメントだけ書いた placeholder でも可。初期は以下の最小版：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<profiles version="22">
  <profile kind="CodeFormatterProfile" name="google-java-format" version="22">
    <!-- Google Java Format 相当の設定。詳細は ID:2 以降で調整 -->
    <setting id="org.eclipse.jdt.core.formatter.tabulation.char" value="space"/>
    <setting id="org.eclipse.jdt.core.formatter.tabulation.size" value="4"/>
    <setting id="org.eclipse.jdt.core.formatter.indentation.size" value="2"/>
  </profile>
</profiles>
```

**Step 5: `rules/pmd/ruleset.xml`（フル ruleset）**

```xml
<?xml version="1.0"?>
<ruleset name="Project PMD Ruleset"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <description>Full PMD ruleset for CI use.</description>

  <rule ref="category/java/bestpractices.xml"/>
  <rule ref="category/java/errorprone.xml"/>
  <rule ref="category/java/multithreading.xml"/>
  <rule ref="category/java/performance.xml"/>
  <rule ref="category/java/security.xml"/>
  <rule ref="category/java/codestyle.xml">
    <exclude name="OnlyOneReturn"/>
    <exclude name="LocalVariableCouldBeFinal"/>
    <exclude name="MethodArgumentCouldBeFinal"/>
    <exclude name="CommentDefaultAccessModifier"/>
  </rule>
  <rule ref="category/java/design.xml">
    <exclude name="LawOfDemeter"/>
    <exclude name="LoosePackageCoupling"/>
  </rule>
  <rule ref="category/java/documentation.xml">
    <exclude name="CommentRequired"/>
  </rule>
</ruleset>
```

**Step 6: `rules/pmd/ruleset-blocking.xml`（pre-commit 用サブセット）**

```xml
<?xml version="1.0"?>
<ruleset name="Project PMD Blocking Ruleset"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <description>Blocking rules run during pre-commit. Only undisputed error-prone patterns.</description>

  <rule ref="category/java/errorprone.xml/AvoidCatchingThrowable"/>
  <rule ref="category/java/errorprone.xml/EmptyCatchBlock"/>
  <rule ref="category/java/errorprone.xml/EmptyFinallyBlock"/>
  <rule ref="category/java/errorprone.xml/AvoidBranchingStatementAsLastInLoop"/>
  <rule ref="category/java/errorprone.xml/CloseResource"/>
  <rule ref="category/java/errorprone.xml/DontImportSun"/>
  <rule ref="category/java/errorprone.xml/UseEqualsToCompareStrings"/>
</ruleset>
```

**Step 7: `rules/spotbugs/exclude.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
  <!-- 誤検知の除外をここに追加。ID:2 以降で具体化する想定 -->
</FindBugsFilter>
```

**Step 8: `rules/spotbugs/find-sec-bugs-include.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
  <Match>
    <Bug category="SECURITY"/>
  </Match>
  <Match>
    <Or>
      <Bug pattern="SQL_INJECTION_JDBC"/>
      <Bug pattern="SQL_INJECTION_JPA"/>
      <Bug pattern="SQL_INJECTION_SPRING_JDBC"/>
      <Bug pattern="XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER"/>
      <Bug pattern="XSS_REQUEST_WRAPPER"/>
      <Bug pattern="XSS_SERVLET"/>
      <Bug pattern="PATH_TRAVERSAL_IN"/>
      <Bug pattern="PATH_TRAVERSAL_OUT"/>
      <Bug pattern="COMMAND_INJECTION"/>
      <Bug pattern="LDAP_INJECTION"/>
      <Bug pattern="XXE_DOCUMENT"/>
      <Bug pattern="XXE_SAXPARSER"/>
      <Bug pattern="XXE_XMLREADER"/>
      <Bug pattern="XXE_XMLSTREAMREADER"/>
      <Bug pattern="WEAK_MESSAGE_DIGEST_MD5"/>
      <Bug pattern="WEAK_MESSAGE_DIGEST_SHA1"/>
      <Bug pattern="HARD_CODE_PASSWORD"/>
      <Bug pattern="HARD_CODE_KEY"/>
    </Or>
  </Match>
</FindBugsFilter>
```

**Step 9: `rules/jacoco/coverage-thresholds.xml`**

これは単独ファイルではなく、pom.xml の jacoco 設定にインライン書き込みする方が Maven プラグインとして扱いやすい。**このファイルは作らず**、pom.xml の Task 8 で直接しきい値を書く方針に変更する（ファイル分離の意義が薄く、Maven プラグインのネイティブ設定として pom.xml 内に持つ方がシンプル）。

**Step 10: コミット**

```bash
git add rules/
git commit -m "chore: add static analysis rulesets for Checkstyle, PMD, SpotBugs

- checkstyle: Google style + blocking subset for pre-commit
- pmd: full category ruleset + blocking subset
- spotbugs: find-sec-bugs security pattern includes

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: `pom.xml` にプロパティと依存を追加

**Files:**
- Modify: `pom.xml`

**Step 1: `<properties>` に lint tool バージョンを追加**

```xml
<properties>
  <java.version>21</java.version>
  <checkstyle.version>10.17.0</checkstyle.version>
  <pmd.version>7.0.0</pmd.version>
  <spotbugs.version>4.8.6</spotbugs.version>
  <find-sec-bugs.version>1.13.0</find-sec-bugs.version>
  <google-java-format.version>1.22.0</google-java-format.version>
  <jacoco.version>0.8.12</jacoco.version>
  <owasp-dc.version>10.0.4</owasp-dc.version>
  <spotless.version>2.43.0</spotless.version>
  <frontend-maven-plugin.version>1.15.1</frontend-maven-plugin.version>
  <node.version>v22.14.0</node.version>
  <npm.version>10.9.0</npm.version>
  <!-- Node/npm のダウンロード元。社内ミラー利用時は -Dnode.download.root=... 等で上書き -->
  <node.download.root>https://nodejs.org/dist/</node.download.root>
  <npm.download.root>https://registry.npmjs.org/npm/-/</npm.download.root>
</properties>
```

**Step 2: `<dependencies>` に Web/Thymeleaf/DevTools を追加**

既存の `spring-boot-starter` の直後に：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>runtime</scope>
  <optional>true</optional>
</dependency>
```

**Step 3: 依存解決確認**

Run: `./mvnw -N dependency:resolve -q`
Expected: exit 0

**Step 4: コミット**

```bash
git add pom.xml
git commit -m "chore(pom): declare lint tool versions and web dependencies

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 8: `pom.xml` に Maven プラグイン群を追加

**Files:**
- Modify: `pom.xml`（`<build><plugins>` 配下）

**Step 1: Spotless プラグイン**

```xml
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>${spotless.version}</version>
  <configuration>
    <java>
      <googleJavaFormat>
        <version>${google-java-format.version}</version>
      </googleJavaFormat>
      <removeUnusedImports/>
      <trimTrailingWhitespace/>
      <endWithNewline/>
    </java>
  </configuration>
  <executions>
    <execution>
      <phase>validate</phase>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

**Step 2: Checkstyle プラグイン**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-checkstyle-plugin</artifactId>
  <version>3.3.1</version>
  <dependencies>
    <dependency>
      <groupId>com.puppycrawl.tools</groupId>
      <artifactId>checkstyle</artifactId>
      <version>${checkstyle.version}</version>
    </dependency>
  </dependencies>
  <configuration>
    <configLocation>rules/checkstyle/checkstyle.xml</configLocation>
    <suppressionsLocation>rules/checkstyle/suppressions.xml</suppressionsLocation>
    <consoleOutput>true</consoleOutput>
    <failsOnError>true</failsOnError>
    <linkXRef>false</linkXRef>
  </configuration>
  <executions>
    <execution>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

**Step 3: PMD プラグイン**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-pmd-plugin</artifactId>
  <version>3.22.0</version>
  <configuration>
    <rulesets>
      <ruleset>rules/pmd/ruleset.xml</ruleset>
    </rulesets>
    <failOnViolation>true</failOnViolation>
    <printFailingErrors>true</printFailingErrors>
  </configuration>
  <executions>
    <execution>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

**Step 4: SpotBugs プラグイン（find-sec-bugs 同梱）**

```xml
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <version>4.8.6.2</version>
  <configuration>
    <effort>Max</effort>
    <threshold>Low</threshold>
    <xmlOutput>true</xmlOutput>
    <excludeFilterFile>rules/spotbugs/exclude.xml</excludeFilterFile>
    <plugins>
      <plugin>
        <groupId>com.h3xstream.findsecbugs</groupId>
        <artifactId>findsecbugs-plugin</artifactId>
        <version>${find-sec-bugs.version}</version>
      </plugin>
    </plugins>
  </configuration>
  <executions>
    <execution>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>
```

**Step 5: JaCoCo プラグイン**

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>${jacoco.version}</version>
  <executions>
    <execution>
      <id>prepare-agent</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

**Step 6: frontend-maven-plugin（後の Phase で Vite を動かすため）**

```xml
<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <version>${frontend-maven-plugin.version}</version>
  <configuration>
    <installDirectory>target</installDirectory>
    <workingDirectory>.</workingDirectory>
  </configuration>
  <executions>
    <execution>
      <id>install node and npm</id>
      <goals><goal>install-node-and-npm</goal></goals>
      <phase>generate-resources</phase>
      <configuration>
        <nodeVersion>${node.version}</nodeVersion>
        <npmVersion>${npm.version}</npmVersion>
        <!-- 社内ミラー利用時は -Dnode.download.root=... で上書き可能 -->
        <nodeDownloadRoot>${node.download.root}</nodeDownloadRoot>
        <npmDownloadRoot>${npm.download.root}</npmDownloadRoot>
      </configuration>
    </execution>
    <execution>
      <id>npm ci</id>
      <goals><goal>npm</goal></goals>
      <phase>generate-resources</phase>
      <configuration>
        <arguments>ci</arguments>
      </configuration>
    </execution>
    <execution>
      <id>npm run build</id>
      <goals><goal>npm</goal></goals>
      <phase>process-resources</phase>
      <configuration>
        <arguments>run build</arguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

**Step 7: プラグイン単体確認（Spotless のみ、他はまだ設定検証の段階）**

Run: `./mvnw spotless:check -q`
Expected: BUILD SUCCESS（Java ファイルが DemoApplication.java しかないので違反は出ないはず）。もし違反があれば `./mvnw spotless:apply` で整形してからコミット。

**Step 8: コミット**

```bash
git add pom.xml
git commit -m "build: add Spotless, Checkstyle, PMD, SpotBugs, JaCoCo, frontend plugins

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 9: `pom.xml` にプロファイルと `maven-dependency-plugin`（lint-setup）追加

**Files:**
- Modify: `pom.xml`

**Step 1: `<properties>` の後に `<profiles>` セクションを追加**

```xml
<profiles>
  <profile>
    <id>fast</id>
    <activation><activeByDefault>true</activeByDefault></activation>
    <!-- 既定動作。OWASP DC は動かさない -->
  </profile>

  <profile>
    <id>ci-mr</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.owasp</groupId>
          <artifactId>dependency-check-maven</artifactId>
          <version>${owasp-dc.version}</version>
          <configuration>
            <failBuildOnCVSS>7</failBuildOnCVSS>
            <dataDirectory>${project.basedir}/.owasp-dc-data</dataDirectory>
            <format>ALL</format>
            <!--
              社内プライベートネットワーク環境で利用する場合、NIST NVD への直接アクセスができないため
              以下のコメントアウトを参考に社内ミラー / プロキシを設定してください:
              <nvdApiServerUrl>https://nvd-mirror.internal.example.com/api/</nvdApiServerUrl>
              <nvdDatafeedUrl>https://nvd-mirror.internal.example.com/feed/json/cve/1.1/</nvdDatafeedUrl>
              <nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>
            -->
          </configuration>
          <executions>
            <execution>
              <phase>verify</phase>
              <goals><goal>check</goal></goals>
            </execution>
          </executions>
        </plugin>
        <plugin>
          <groupId>org.jacoco</groupId>
          <artifactId>jacoco-maven-plugin</artifactId>
          <executions>
            <execution>
              <id>check</id>
              <phase>verify</phase>
              <goals><goal>check</goal></goals>
              <configuration>
                <rules>
                  <rule>
                    <element>BUNDLE</element>
                    <limits>
                      <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.60</minimum>
                      </limit>
                      <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.50</minimum>
                      </limit>
                    </limits>
                  </rule>
                </rules>
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>

  <profile>
    <id>ci-main</id>
    <!-- ci-mr と同じ振る舞いを継承。将来のリリース固有ジョブ用に予約 -->
  </profile>

  <profile>
    <id>skip-frontend</id>
    <properties>
      <skip.npm>true</skip.npm>
    </properties>
    <!-- frontend-maven-plugin 側に skip フラグを配ってスキップ -->
    <build>
      <plugins>
        <plugin>
          <groupId>com.github.eirslett</groupId>
          <artifactId>frontend-maven-plugin</artifactId>
          <configuration>
            <skip>${skip.npm}</skip>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>

  <profile>
    <id>lint-setup</id>
    <dependencies>
      <!-- PMD 7.x は Maven Central に pmd-dist zip 配布なし / shaded jar もないため、
           pmd-cli と pmd-java を profile scope で宣言し、copy-dependencies で transitive 込みで配置する -->
      <dependency>
        <groupId>net.sourceforge.pmd</groupId>
        <artifactId>pmd-cli</artifactId>
        <version>${pmd.version}</version>
      </dependency>
      <dependency>
        <groupId>net.sourceforge.pmd</groupId>
        <artifactId>pmd-java</artifactId>
        <version>${pmd.version}</version>
      </dependency>
    </dependencies>
    <build>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-dependency-plugin</artifactId>
          <version>3.6.1</version>
          <executions>
            <!-- Checkstyle と google-java-format は単一 shaded jar なので copy goal -->
            <execution>
              <id>copy-single-jars</id>
              <phase>initialize</phase>
              <goals><goal>copy</goal></goals>
              <configuration>
                <artifactItems>
                  <artifactItem>
                    <!-- 10.x 以降 checkstyle main jar は shaded + Main-Class 付きで直接実行可能。
                         classifier=all は廃止されているので指定しない -->
                    <groupId>com.puppycrawl.tools</groupId>
                    <artifactId>checkstyle</artifactId>
                    <version>${checkstyle.version}</version>
                    <outputDirectory>${project.build.directory}/lint-tools</outputDirectory>
                    <destFileName>checkstyle-all.jar</destFileName>
                  </artifactItem>
                  <artifactItem>
                    <groupId>com.google.googlejavaformat</groupId>
                    <artifactId>google-java-format</artifactId>
                    <version>${google-java-format.version}</version>
                    <classifier>all-deps</classifier>
                    <outputDirectory>${project.build.directory}/lint-tools</outputDirectory>
                    <destFileName>google-java-format.jar</destFileName>
                  </artifactItem>
                </artifactItems>
              </configuration>
            </execution>
            <!-- PMD CLI は transitive deps を含むライブラリ群として配置 -->
            <execution>
              <id>copy-pmd-lib</id>
              <phase>initialize</phase>
              <goals><goal>copy-dependencies</goal></goals>
              <configuration>
                <outputDirectory>${project.build.directory}/lint-tools/pmd-lib</outputDirectory>
                <includeScope>runtime</includeScope>
                <!-- profile scope で宣言した pmd-cli / pmd-java + transitive のみ対象 -->
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

**Step 2: lint-setup 動作確認**

Run: `./mvnw -Plint-setup initialize -q && ls target/lint-tools/ target/lint-tools/pmd-lib/ | head -20`
Expected: 以下が存在すること：
- `target/lint-tools/checkstyle-all.jar`（single jar、shaded）
- `target/lint-tools/google-java-format.jar`（single jar、classifier=all-deps）
- `target/lint-tools/pmd-lib/pmd-cli-<version>.jar`、`pmd-java-<version>.jar`、`pmd-core-<version>.jar`、他 transitive deps 多数

PMD CLI は `java -cp "target/lint-tools/pmd-lib/*" net.sourceforge.pmd.cli.PmdCli check ...` 形式で起動する（Phase 5 Task 16 で `run-pmd.mjs` が実装）。

**Step 3: コミット**

```bash
git add pom.xml
git commit -m "build(pom): add Maven profiles (fast/ci-mr/ci-main/skip-frontend/lint-setup)

lint-setup プロファイルで maven-dependency-plugin が lint jar 群を
target/lint-tools/ に配置する仕組みを導入。

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 4: フロントエンド基盤

### Task 10: ルート `package.json` の骨格

**Files:**
- Create: `package.json`

**Step 1: 書く**

```json
{
  "name": "sak-dev-env-frontend",
  "version": "0.0.0",
  "private": true,
  "type": "module",
  "engines": {
    "node": ">=22.0.0 <23"
  },
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
  },
  "devDependencies": {
    "vite": "^5.4.0",
    "vitest": "^2.1.0",
    "@vitest/coverage-v8": "^2.1.0",
    "jsdom": "^25.0.0",
    "eslint": "^9.12.0",
    "@eslint/js": "^9.12.0",
    "globals": "^15.11.0",
    "eslint-plugin-security": "^3.0.1",
    "eslint-config-prettier": "^9.1.0",
    "prettier": "^3.3.3",
    "husky": "^9.1.6",
    "lint-staged": "^15.2.10",
    "@commitlint/cli": "^19.5.0",
    "@commitlint/config-conventional": "^19.5.0",
    "secretlint": "^9.0.0",
    "@secretlint/secretlint-rule-preset-recommend": "^9.0.0"
  }
}
```

**Step 2: 作成されていない scripts を npm が呼べないようダミーで用意**

この時点では `scripts/setup-lint-tools.mjs` がまだ無いので `npm install` が `postinstall` で失敗する。**この Task では `npm install` は実行しない**（Task 14 まで待つ）。

**Step 3: コミット**

```bash
git add package.json
git commit -m "build: add root package.json with frontend toolchain dependencies

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 11: `vite.config.js` 作成

**Files:**
- Create: `vite.config.js`

**Step 1: 書く**

```js
import { defineConfig } from 'vite';
import { resolve } from 'node:path';

export default defineConfig({
  root: resolve(__dirname, 'src/main/frontend'),
  build: {
    outDir: resolve(__dirname, 'src/main/resources/static'),
    emptyOutDir: false,
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'src/main/frontend/src/main.js'),
      },
      output: {
        entryFileNames: 'js/[name].js',
        chunkFileNames: 'js/[name].js',
        assetFileNames: 'assets/[name].[ext]',
      },
    },
  },
});
```

**Step 2: コミット（検証は Task 13 で）**

```bash
git add vite.config.js
git commit -m "build(frontend): add Vite config targeting static resources dir

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 12: `vitest.config.js` 作成

**Files:**
- Create: `vitest.config.js`

**Step 1: 書く**

```js
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['src/main/frontend/**/*.test.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: 'target/vitest-coverage',
      include: ['src/main/frontend/src/**/*.js'],
      thresholds: {
        lines: 60,
        branches: 50,
        functions: 60,
        statements: 60,
      },
    },
  },
});
```

**Step 2: コミット**

```bash
git add vitest.config.js
git commit -m "test(frontend): add Vitest config with coverage thresholds

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 13: フロントエンドスケルトン + smoke test（TDD）

**Files:**
- Create: `src/main/frontend/src/main.js`
- Test: `src/main/frontend/tests/smoke.test.js`

**Step 1: 失敗するテストを書く**

```js
// src/main/frontend/tests/smoke.test.js
import { describe, it, expect } from 'vitest';
import { greet } from '../src/main.js';

describe('greet', () => {
  it('returns a greeting for the given name', () => {
    expect(greet('world')).toBe('Hello, world!');
  });
});
```

**Step 2: テストが失敗することを確認**

Run: `npx vitest run --no-coverage`
Expected: "Cannot find module '../src/main.js'" で fail。

（注：`npm install` を先にしていない場合、`npx vitest` は動かない。この時点で依存をインストールしてよい。後続 Task 14 以降の scripts/ 作成が済むまで `npm install` の `postinstall` が失敗する可能性があるため、次の Step で先に scripts/ を作る）

実際の手順を現実的にするため、本 Task 13 の手順は Task 14-17 完了後に実施する。それまでスキップし、**Task 13 は Phase 5 完了後に再度戻ってくる**。

**Step 3: スキップ、Phase 5 後に戻る**

---

## Phase 5: Scripts 群 + setup-lint-tools

### Task 14: `scripts/setup-lint-tools.mjs`

**Files:**
- Create: `scripts/setup-lint-tools.mjs`

**Step 1: 書く**

```js
#!/usr/bin/env node
// target/lint-tools/ に Checkstyle / PMD / google-java-format の jar/配布物が揃っていることを保証する。
// 欠けていれば `./mvnw -Plint-setup initialize` を呼び出して Maven に解決させる。
// Maven ローカルリポジトリの位置（~/.m2 以外含む）に依存しないための間接層。

import { existsSync, readdirSync } from 'node:fs';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const PROJECT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const LINT_TOOLS_DIR = join(PROJECT_ROOT, 'target', 'lint-tools');

const PMD_LIB = join(LINT_TOOLS_DIR, 'pmd-lib');

function hasAll() {
  if (!existsSync(LINT_TOOLS_DIR)) return false;
  const entries = readdirSync(LINT_TOOLS_DIR);
  const checkstyle = entries.includes('checkstyle-all.jar');
  const gjf = entries.includes('google-java-format.jar');
  const pmdReady =
    existsSync(PMD_LIB) &&
    readdirSync(PMD_LIB).some((e) => e.startsWith('pmd-cli-') && e.endsWith('.jar'));
  return checkstyle && gjf && pmdReady;
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
  // 既に揃っている場合は何もしない
  process.exit(0);
}

runMavenLintSetup();

if (!hasAll()) {
  console.error('[setup-lint-tools] lint-tools are still incomplete after Maven setup.');
  process.exit(1);
}

console.log('[setup-lint-tools] OK');
```

**Step 2: 動作確認**

Run: `node scripts/setup-lint-tools.mjs && ls target/lint-tools/`
Expected: `checkstyle-all.jar`, `google-java-format.jar`, `pmd-bin-7.0.0/` が存在。

**Step 3: コミット**

```bash
git add scripts/setup-lint-tools.mjs
git commit -m "chore(scripts): add setup-lint-tools to populate target/lint-tools via Maven

Maven ローカルリポジトリの位置に依存せず lint jar を確保する仕組み。

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 15: `scripts/run-checkstyle.mjs`

**Files:**
- Create: `scripts/run-checkstyle.mjs`

**Step 1: 書く**

```js
#!/usr/bin/env node
// lint-staged から呼ばれ、変更 Java ファイルに対して Checkstyle (blocking ruleset) を実行。
// JVM を 1 回だけ立ち上げて複数ファイルを渡す。

import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';

const PROJECT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const JAR = join(PROJECT_ROOT, 'target', 'lint-tools', 'checkstyle-all.jar');
const CONFIG = join(PROJECT_ROOT, 'rules', 'checkstyle', 'checkstyle-blocking.xml');

const files = process.argv.slice(2);
if (files.length === 0) {
  // lint-staged が空で呼ぶケースあり
  process.exit(0);
}

if (!existsSync(JAR)) {
  console.error('[run-checkstyle] checkstyle jar not found. Run: node scripts/setup-lint-tools.mjs');
  process.exit(1);
}

const result = spawnSync('java', ['-jar', JAR, '-c', CONFIG, ...files], {
  stdio: 'inherit',
});
process.exit(result.status ?? 1);
```

**Step 2: 動作確認**

`src/main/java/com/example/demo/DemoApplication.java` を対象に直接実行：

Run: `node scripts/run-checkstyle.mjs src/main/java/com/example/demo/DemoApplication.java`
Expected: exit 0（違反なし）

**Step 3: コミット**

```bash
git add scripts/run-checkstyle.mjs
git commit -m "chore(scripts): add run-checkstyle wrapper for lint-staged

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 16: `scripts/run-pmd.mjs`

**Files:**
- Create: `scripts/run-pmd.mjs`

**Step 1: 書く**

```js
#!/usr/bin/env node
// lint-staged から呼ばれ、変更 Java ファイルに対して PMD (blocking ruleset) を実行。
// Maven Central には PMD 7.x の zip 配布がないため、pmd-cli + 依存 jar 群を
// `target/lint-tools/pmd-lib/` に配置して classpath で起動する（pom.xml lint-setup profile 参照）。

import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { readdirSync, existsSync } from 'node:fs';

const PROJECT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const PMD_LIB = join(PROJECT_ROOT, 'target', 'lint-tools', 'pmd-lib');
const RULESET = join(PROJECT_ROOT, 'rules', 'pmd', 'ruleset-blocking.xml');

const files = process.argv.slice(2);
if (files.length === 0) process.exit(0);

if (!existsSync(PMD_LIB) || readdirSync(PMD_LIB).length === 0) {
  console.error('[run-pmd] pmd-lib not populated. Run: node scripts/setup-lint-tools.mjs');
  process.exit(1);
}

// Windows と Unix で classpath 区切り文字が違う
const sep = process.platform === 'win32' ? ';' : ':';
const classpath = `${PMD_LIB}${sep === ':' ? '/*' : '\\*'}`;

// PMD 7 CLI: `net.sourceforge.pmd.cli.PmdCli check -R <ruleset> --no-progress -f text -d <files...>`
const args = [
  '-cp',
  classpath,
  'net.sourceforge.pmd.cli.PmdCli',
  'check',
  '-R',
  RULESET,
  '--no-progress',
  '-f',
  'text',
  '-d',
  ...files,
];
const result = spawnSync('java', args, { stdio: 'inherit' });
process.exit(result.status ?? 1);
```

**Step 2: 動作確認**

Run: `node scripts/run-pmd.mjs src/main/java/com/example/demo/DemoApplication.java`
Expected: exit 0（違反なし）

**Step 3: コミット**

```bash
git add scripts/run-pmd.mjs
git commit -m "chore(scripts): add run-pmd wrapper for lint-staged

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 17: `scripts/run-spotless-staged.mjs`

**Files:**
- Create: `scripts/run-spotless-staged.mjs`

**Step 1: 書く**

```js
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

// --replace で in-place 整形
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
```

**Step 2: 動作確認**

Run: `node scripts/run-spotless-staged.mjs src/main/java/com/example/demo/DemoApplication.java`
Expected: exit 0。ファイルが変更された場合は `git diff` で確認。

**Step 3: コミット**

```bash
git add scripts/run-spotless-staged.mjs
git commit -m "chore(scripts): add run-spotless-staged wrapper for incremental formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 18: `npm install` を走らせて依存導入 + Husky 初期化

**Step 1: インストール**

Run: `npm install`
Expected: 依存ツリー解決、`.husky/` ディレクトリが `prepare` スクリプトにより初期化、`postinstall` で `setup-lint-tools.mjs` が走って `target/lint-tools/` が populate される。

**Step 2: 成果物確認**

Run: `ls .husky/ target/lint-tools/`
Expected:
- `.husky/` に何らかのファイル（`_/` 等）
- `target/lint-tools/` に `checkstyle-all.jar`, `google-java-format.jar`, `pmd-bin-*`

**Step 3: `package-lock.json` をコミット**

```bash
git add package-lock.json
git commit -m "build: lock frontend dependencies

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 13（再開）: フロントエンドスケルトン + smoke test

**Files:**
- Create: `src/main/frontend/src/main.js`
- Test: `src/main/frontend/tests/smoke.test.js`

**Step 1: 失敗するテスト**

```js
// src/main/frontend/tests/smoke.test.js
import { describe, it, expect } from 'vitest';
import { greet } from '../src/main.js';

describe('greet', () => {
  it('returns a greeting for the given name', () => {
    expect(greet('world')).toBe('Hello, world!');
  });
});
```

**Step 2: 失敗を確認**

Run: `npx vitest run --no-coverage`
Expected: fail（"Cannot find module"）

**Step 3: 最小実装**

```js
// src/main/frontend/src/main.js
export function greet(name) {
  return `Hello, ${name}!`;
}
```

**Step 4: 通ることを確認**

Run: `npx vitest run --no-coverage`
Expected: 1 pass

**Step 5: Vite ビルド確認**

Run: `npm run build && ls src/main/resources/static/js/`
Expected: `main.js` が生成されている

**Step 6: コミット**

```bash
git add src/main/frontend
git commit -m "feat(frontend): add minimal entry point with smoke test

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 6: Lint / Format ツール設定

### Task 19: ESLint 設定

**Files:**
- Create: `eslint.config.js`

**Step 1: 書く**

```js
import js from '@eslint/js';
import security from 'eslint-plugin-security';
import prettier from 'eslint-config-prettier';
import globals from 'globals';

export default [
  js.configs.recommended,
  security.configs.recommended,
  {
    files: ['src/main/frontend/**/*.js'],
    languageOptions: {
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser },
    },
    rules: {
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'eqeqeq': 'error',
      'no-var': 'error',
      'prefer-const': 'error',
    },
  },
  {
    files: ['src/main/frontend/**/*.test.js'],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
  },
  prettier,
];
```

**Step 2: 動作確認**

Run: `npm run lint`
Expected: 違反なしで exit 0

**Step 3: コミット**

```bash
git add eslint.config.js
git commit -m "chore(lint): add ESLint flat config with security plugin

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 20: Prettier 設定

**Files:**
- Create: `.prettierrc.json`
- Create: `.prettierignore`

**Step 1: `.prettierrc.json`**

```json
{
  "printWidth": 100,
  "tabWidth": 2,
  "useTabs": false,
  "semi": true,
  "singleQuote": true,
  "trailingComma": "all",
  "endOfLine": "lf"
}
```

**Step 2: `.prettierignore`**

```
node_modules/
target/
src/main/resources/static/js/
src/main/resources/static/assets/
.husky/_/
package-lock.json
```

**Step 3: 動作確認**

Run: `npm run format:check`
Expected: 違反があれば `npm run format` で整形してから再確認。

**Step 4: コミット**

```bash
git add .prettierrc.json .prettierignore
git commit -m "chore(lint): add Prettier config

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 21: Secretlint 設定

**Files:**
- Create: `.secretlintrc.json`
- Create: `.secretlintignore`

**Step 1: `.secretlintrc.json`**

```json
{
  "rules": [
    { "id": "@secretlint/secretlint-rule-preset-recommend" }
  ]
}
```

**Step 2: `.secretlintignore`**

```
node_modules/
target/
src/main/resources/static/js/
src/main/resources/static/assets/
.husky/_/
package-lock.json
```

**Step 3: 動作確認**

Run: `npm run secretlint`
Expected: 違反なしで exit 0

**Step 4: コミット**

```bash
git add .secretlintrc.json .secretlintignore
git commit -m "chore(security): add Secretlint config with recommended preset

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 7: Git フック

### Task 22: lint-staged 設定（`package.json` に追記）

**Files:**
- Modify: `package.json`

**Step 1: トップレベルに `lint-staged` キー追加**

```json
{
  "lint-staged": {
    "*.{js,mjs,cjs}": [
      "prettier --write",
      "eslint --fix --max-warnings=0"
    ],
    "*.java": [
      "node scripts/run-spotless-staged.mjs",
      "node scripts/run-checkstyle.mjs",
      "node scripts/run-pmd.mjs"
    ],
    "*.{json,md,yml,yaml}": [
      "prettier --write"
    ],
    "*": [
      "secretlint"
    ]
  }
}
```

**Step 2: 動作確認**

`.java` を一時的に編集してみて `npx lint-staged` 単体で動くか確認：

Run: `touch test.tmp && git add test.tmp && npx lint-staged && git rm --cached test.tmp && rm test.tmp`
Expected: lint-staged が secretlint を走らせて exit 0

**Step 3: コミット**

```bash
git add package.json
git commit -m "chore(hooks): configure lint-staged rules per file type

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 23: `commitlint.config.js`

**Files:**
- Create: `commitlint.config.js`

**Step 1: 書く**

```js
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'subject-case': [0],
    'header-max-length': [2, 'always', 100],
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'chore', 'docs', 'style', 'refactor', 'perf', 'test', 'build', 'ci', 'revert'],
    ],
  },
};
```

**Step 2: 動作確認**

Run: `echo "bogus commit message" | npx commitlint`
Expected: fail（type が enum にないため）

Run: `echo "feat: add sample feature" | npx commitlint`
Expected: pass

**Step 3: コミット**

```bash
git add commitlint.config.js
git commit -m "chore(hooks): add commitlint config with Conventional Commits

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 24: `.husky/pre-commit`

**Files:**
- Create: `.husky/pre-commit`

**Step 1: 書く**

```sh
npx lint-staged
```

**Step 2: 実行ビット付与**

Run: `chmod +x .husky/pre-commit`

**Step 3: コミット**

```bash
git add .husky/pre-commit
git commit -m "chore(hooks): add pre-commit hook running lint-staged

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 25: `.husky/commit-msg`

**Files:**
- Create: `.husky/commit-msg`

**Step 1: 書く**

```sh
npx --no -- commitlint --edit "$1"
```

**Step 2: 実行ビット付与**

Run: `chmod +x .husky/commit-msg`

**Step 3: 検証**

規約違反メッセージでコミット試行：

Run: `echo "tmp" > tmp.txt && git add tmp.txt && git commit -m "bad message" || echo "(blocked as expected)" && git reset HEAD tmp.txt && rm tmp.txt`
Expected: commit-msg が fail してコミット拒否

**Step 4: コミット**

```bash
git add .husky/commit-msg
git commit -m "chore(hooks): add commit-msg hook enforcing commitlint

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 26: `.husky/pre-push`

**Files:**
- Create: `.husky/pre-push`

**Step 1: 書く**

```sh
set -e

echo "[pre-push] ESLint (all) ..."
npm run lint

echo "[pre-push] Prettier check ..."
npm run format:check

echo "[pre-push] Vitest ..."
npm test

echo "[pre-push] npm audit ..."
npm run audit

echo "[pre-push] Maven verify (full static analysis + tests) ..."
./mvnw -T 1C -Pfast verify
```

**Step 2: 実行ビット付与**

Run: `chmod +x .husky/pre-push`

**Step 3: コミット**

```bash
git add .husky/pre-push
git commit -m "chore(hooks): add pre-push hook running full-project verification

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 27: フック全体のスモークテスト

**Step 1: 正常コミットが通ることを確認**

Run: `touch tmp.txt && git add tmp.txt && git commit -m "test: verify hook pipeline" && git reset HEAD~1 && rm tmp.txt`
Expected: pre-commit（lint-staged が secretlint を走らせる）と commit-msg が通る

**Step 2: pre-push は実 push なしで検証（ジョブ内容を直接実行）**

Run: `bash .husky/pre-push`
Expected: 全検査 pass（BUILD SUCCESS）

所要時間が 3 分を大幅に超えないこと。

**Step 3: コミットなし**（検証のみ）

---

## Phase 8: VSCode

### Task 28: `.vscode/settings.json`

**Files:**
- Create: `.vscode/settings.json`

**Step 1: 書く**

```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  },
  "[java]": {
    "editor.defaultFormatter": "redhat.java"
  },
  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[json]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[markdown]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.format.settings.url": "rules/checkstyle/eclipse-formatter.xml",
  "files.eol": "\n",
  "files.insertFinalNewline": true,
  "files.trimTrailingWhitespace": true,
  "eslint.validate": ["javascript"],
  "eslint.workingDirectories": [{ "mode": "auto" }]
}
```

**Step 2: コミット**

```bash
git add .vscode/settings.json
git commit -m "chore(vscode): add shared workspace settings

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 29: `.vscode/tasks.json`

**Files:**
- Create: `.vscode/tasks.json`

**Step 1: 書く**

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Backend: Build",
      "type": "shell",
      "command": "./mvnw clean package",
      "group": { "kind": "build", "isDefault": true }
    },
    {
      "label": "Backend: Test",
      "type": "shell",
      "command": "./mvnw test",
      "group": { "kind": "test", "isDefault": true }
    },
    {
      "label": "Backend: Run (DevTools)",
      "type": "shell",
      "command": "./mvnw spring-boot:run"
    },
    {
      "label": "Frontend: Build",
      "type": "shell",
      "command": "npm run build"
    },
    {
      "label": "Frontend: Build (Watch)",
      "type": "shell",
      "command": "npm run build:watch",
      "isBackground": true
    },
    {
      "label": "Frontend: Test",
      "type": "shell",
      "command": "npm test"
    },
    {
      "label": "Frontend: Test (Watch)",
      "type": "shell",
      "command": "npm run test:watch",
      "isBackground": true
    },
    {
      "label": "Lint: All",
      "type": "shell",
      "command": "npm run lint && ./mvnw -DskipTests verify"
    }
  ]
}
```

**Step 2: コミット**

```bash
git add .vscode/tasks.json
git commit -m "chore(vscode): add shared build/test tasks

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 30: `.vscode/launch.json` を更新

**Files:**
- Modify: `.vscode/launch.json`

**Step 1: Vitest Debug 設定を追加**

```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot-DemoApplication<demo>",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "mainClass": "com.example.demo.DemoApplication",
      "projectName": "demo",
      "args": "",
      "envFile": "${workspaceFolder}/.env"
    },
    {
      "type": "node",
      "name": "Vitest: Current File",
      "request": "launch",
      "autoAttachChildProcesses": true,
      "skipFiles": ["<node_internals>/**", "**/node_modules/**"],
      "program": "${workspaceFolder}/node_modules/vitest/vitest.mjs",
      "args": ["run", "${relativeFile}"],
      "smartStep": true,
      "console": "integratedTerminal"
    }
  ]
}
```

**Step 2: コミット**

```bash
git add .vscode/launch.json
git commit -m "chore(vscode): add Vitest debug configuration

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 9: Docker

### Task 31: `.dockerignore`

**Files:**
- Create: `.dockerignore`

**Step 1: 書く**

```
# ビルド成果物
target/
node_modules/
src/main/resources/static/js/
src/main/resources/static/assets/

# VCS / IDE
.git/
.gitignore
.gitattributes
.vscode/
.idea/

# ドキュメント・テスト
docs/
README.md
HELP.md
PBI.md
AGENTS.md
CLAUDE.md

# CI 設定
.gitlab/
.gitlab-ci.yml
.husky/

# ローカル環境固有
.env
.env.*
.DS_Store

# Lint 設定（ビルド不要）
rules/
eslint.config.js
vitest.config.js
.prettierrc.json
.prettierignore
commitlint.config.js
.secretlintrc.json
.secretlintignore
.editorconfig
.nvmrc
```

**Step 2: コミット**

```bash
git add .dockerignore
git commit -m "build(docker): add .dockerignore to keep build context minimal

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 32: `Dockerfile`（マルチステージ）

**Files:**
- Create: `Dockerfile`

**Step 1: 書く**

```dockerfile
# syntax=docker/dockerfile:1.7

ARG JDK_IMAGE=eclipse-temurin:21-jdk-jammy
ARG JRE_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${JDK_IMAGE} AS builder

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY wrapper/ wrapper/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY src/ src/
COPY vite.config.js ./

RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    ./mvnw -B -DskipTests package

RUN cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---------------------------------------------------------

FROM ${JRE_IMAGE} AS runtime

RUN groupadd --system --gid 1001 app \
 && useradd --system --uid 1001 --gid app --home /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=builder --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/application/ ./

USER app:app
EXPOSE 8080

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE="prod" \
    TZ="Asia/Tokyo"

# Actuator 未導入なら以下の HEALTHCHECK ブロックを削除してください（ID:2 で Actuator を追加予定）
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher"]
```

**Step 2: ビルド検証**

Run: `DOCKER_BUILDKIT=1 docker build -t sak-dev-env:local .`
Expected: BUILD SUCCESS

**Step 3: hadolint 検証**

Run: `docker run --rm -i hadolint/hadolint < Dockerfile`
Expected: 指摘なし、または軽微な warning のみ

**Step 4: コミット**

```bash
git add Dockerfile
git commit -m "build(docker): add multi-stage Dockerfile with JRE-jammy base

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 10: GitLab CI/CD

### Task 33: `.gitlab/ci/_defaults.yml`

**Files:**
- Create: `.gitlab/ci/_defaults.yml`

**Step 1: 書く**

```yaml
variables:
  IMAGE_MAVEN: "maven:3.9-eclipse-temurin-21"
  IMAGE_NODE: "node:22"
  IMAGE_DOCKER: "docker:27"
  IMAGE_DIND: "docker:27-dind"
  IMAGE_TRIVY: "aquasec/trivy:latest"
  IMAGE_GITLEAKS: "zricethezav/gitleaks:latest"
  IMAGE_HADOLINT: "hadolint/hadolint:latest-alpine"
  IMAGE_SYFT: "anchore/syft:latest"

  MAVEN_CLI_OPTS: "-B -Dmaven.repo.local=.m2/repository --no-transfer-progress"

  # 社内プライベートネットワーク環境では Trivy DB を社内ミラーから取得するため、
  # 以下のコメントアウトを参考に環境固有値を設定してください:
  # TRIVY_DB_REPOSITORY: "registry.internal.example.com/aquasecurity/trivy-db"
  # TRIVY_JAVA_DB_REPOSITORY: "registry.internal.example.com/aquasecurity/trivy-java-db"
  # TRIVY_NO_PROGRESS: "true"

.cache_maven: &cache_maven
  key: "maven-$CI_COMMIT_REF_SLUG"
  paths:
    - .m2/repository/

.cache_node: &cache_node
  key: "node-$CI_COMMIT_REF_SLUG"
  paths:
    - node_modules/

.cache_lint_tools: &cache_lint_tools
  key: "lint-tools"
  paths:
    - target/lint-tools/

.cache_owasp: &cache_owasp
  key: "owasp-dc-data"
  paths:
    - .owasp-dc-data/
```

**Step 2: コミット**

```bash
git add .gitlab/ci/_defaults.yml
git commit -m "ci: add _defaults.yml centralizing image versions and caches

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 34: `.gitlab/ci/setup.yml`

**Files:**
- Create: `.gitlab/ci/setup.yml`

**Step 1: 書く**

```yaml
setup:maven:
  stage: setup
  image: $IMAGE_MAVEN
  cache:
    - <<: *cache_maven
    - <<: *cache_lint_tools
  script:
    - ./mvnw $MAVEN_CLI_OPTS -N dependency:go-offline
    - ./mvnw $MAVEN_CLI_OPTS -Plint-setup initialize
  artifacts:
    paths:
      - target/lint-tools/
    expire_in: 1 day

setup:node:
  stage: setup
  image: $IMAGE_NODE
  cache:
    - <<: *cache_node
  script:
    - npm ci --ignore-scripts
  artifacts:
    paths:
      - node_modules/
    expire_in: 1 day
```

`--ignore-scripts` は CI 上で postinstall の `setup-lint-tools.mjs`（Maven 呼び出し）をスキップするため。`setup:maven` ジョブ側で既に lint-tools を配置済み。

**Step 2: コミット**

```bash
git add .gitlab/ci/setup.yml
git commit -m "ci: add setup stage for maven and node dependencies

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 35: `.gitlab/ci/quality.yml`

**Files:**
- Create: `.gitlab/ci/quality.yml`

**Step 1: 書く**

```yaml
lint:format:
  stage: quality
  image: $IMAGE_NODE
  needs: ["setup:node"]
  script:
    - npm run format:check

lint:eslint:
  stage: quality
  image: $IMAGE_NODE
  needs: ["setup:node"]
  script:
    - npm run lint

lint:checkstyle:
  stage: quality
  image: $IMAGE_MAVEN
  needs: ["setup:maven"]
  cache:
    - <<: *cache_maven
  script:
    - ./mvnw $MAVEN_CLI_OPTS checkstyle:check

lint:pmd:
  stage: quality
  image: $IMAGE_MAVEN
  needs: ["setup:maven"]
  cache:
    - <<: *cache_maven
  script:
    - ./mvnw $MAVEN_CLI_OPTS pmd:check

lint:spotbugs:
  stage: quality
  image: $IMAGE_MAVEN
  needs: ["setup:maven"]
  cache:
    - <<: *cache_maven
  script:
    - ./mvnw $MAVEN_CLI_OPTS compile spotbugs:check

test:java:
  stage: quality
  image: $IMAGE_MAVEN
  needs: ["setup:maven"]
  cache:
    - <<: *cache_maven
  script:
    - ./mvnw $MAVEN_CLI_OPTS test
  artifacts:
    when: always
    reports:
      junit: target/surefire-reports/TEST-*.xml
    paths:
      - target/jacoco.exec
    expire_in: 1 week

test:js:
  stage: quality
  image: $IMAGE_NODE
  needs: ["setup:node"]
  script:
    - npm run test:coverage
  artifacts:
    when: always
    paths:
      - target/vitest-coverage/
    expire_in: 1 week

coverage:java:
  stage: quality
  image: $IMAGE_MAVEN
  needs: ["test:java"]
  cache:
    - <<: *cache_maven
  script:
    - ./mvnw $MAVEN_CLI_OPTS -Pci-mr jacoco:report jacoco:check

secret:gitleaks:
  stage: quality
  image:
    name: $IMAGE_GITLEAKS
    entrypoint: [""]
  script:
    - gitleaks detect --source=. --no-git=false --verbose
```

**Step 2: コミット**

```bash
git add .gitlab/ci/quality.yml
git commit -m "ci: add quality stage jobs (lint, test, coverage, secret scan)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 36: `.gitlab/ci/security.yml`

**Files:**
- Create: `.gitlab/ci/security.yml`

**Step 1: 書く**

```yaml
sca:owasp:
  stage: security
  image: $IMAGE_MAVEN
  needs: ["setup:maven"]
  cache:
    - <<: *cache_maven
    - <<: *cache_owasp
  script:
    - ./mvnw $MAVEN_CLI_OPTS -Pci-mr dependency-check:check
  artifacts:
    when: always
    paths:
      - target/dependency-check-report.html
      - target/dependency-check-report.json
    expire_in: 1 week

sca:trivy-fs:
  stage: security
  image:
    name: $IMAGE_TRIVY
    entrypoint: [""]
  script:
    - trivy fs --severity HIGH,CRITICAL --exit-code 1 --ignore-unfixed .
```

**Step 2: コミット**

```bash
git add .gitlab/ci/security.yml
git commit -m "ci: add security stage jobs (OWASP DC, Trivy fs)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 37: `.gitlab/ci/package.yml`

**Files:**
- Create: `.gitlab/ci/package.yml`

**Step 1: 書く**

```yaml
build:jar:
  stage: package
  image: $IMAGE_MAVEN
  needs: ["test:java", "test:js"]
  cache:
    - <<: *cache_maven
    - <<: *cache_node
  script:
    - ./mvnw $MAVEN_CLI_OPTS -DskipTests package
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 week

build:image:
  stage: package
  image: $IMAGE_DOCKER
  services:
    - name: $IMAGE_DIND
      alias: docker
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
    DOCKER_HOST: "tcp://docker:2376"
    DOCKER_CERT_PATH: "/certs/client"
    DOCKER_TLS_VERIFY: "1"
  needs: ["build:jar"]
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker save $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA -o image.tar
    - docker run --rm -v "$PWD:/work" -w /work $IMAGE_SYFT packages docker-archive:image.tar -o cyclonedx-json > sbom.cdx.json
  artifacts:
    paths:
      - image.tar
      - sbom.cdx.json
    expire_in: 1 week
```

**Step 2: コミット**

```bash
git add .gitlab/ci/package.yml
git commit -m "ci: add package stage (JAR build, Docker image build, SBOM)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 38: `.gitlab/ci/scan.yml`

**Files:**
- Create: `.gitlab/ci/scan.yml`

**Step 1: 書く**

```yaml
lint:hadolint:
  stage: scan
  image:
    name: $IMAGE_HADOLINT
    entrypoint: [""]
  script:
    - hadolint Dockerfile

scan:trivy-image:
  stage: scan
  image:
    name: $IMAGE_TRIVY
    entrypoint: [""]
  needs: ["build:image"]
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
  script:
    - trivy image --input image.tar --severity HIGH,CRITICAL --exit-code 1 --ignore-unfixed
```

**Step 2: コミット**

```bash
git add .gitlab/ci/scan.yml
git commit -m "ci: add scan stage (hadolint, Trivy image)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 39: `.gitlab/ci/pages.yml`

**Files:**
- Create: `.gitlab/ci/pages.yml`

**Step 1: 書く**

```yaml
pages:
  stage: pages
  image: $IMAGE_NODE
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
  cache:
    key: "docs-$CI_COMMIT_REF_SLUG"
    paths:
      - docs/node_modules/
  before_script:
    - cd docs
    - npm ci
  script:
    - npm run build
    - mv build ../public
  artifacts:
    paths:
      - public
    expire_in: 1 week
```

**Step 2: コミット**

```bash
git add .gitlab/ci/pages.yml
git commit -m "ci: add pages job for docusaurus deploy to GitLab Pages

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 40: `.gitlab/ci/release.yml`

**Files:**
- Create: `.gitlab/ci/release.yml`

**Step 1: 書く**

```yaml
release:image:
  stage: release
  image: $IMAGE_DOCKER
  services:
    - name: $IMAGE_DIND
      alias: docker
  variables:
    DOCKER_TLS_CERTDIR: "/certs"
    DOCKER_HOST: "tcp://docker:2376"
    DOCKER_CERT_PATH: "/certs/client"
    DOCKER_TLS_VERIFY: "1"
  needs: ["build:image", "scan:trivy-image"]
  rules:
    - if: '$CI_COMMIT_BRANCH == "main" && $CI_REGISTRY != null'
  before_script:
    - echo "$CI_REGISTRY_PASSWORD" | docker login -u "$CI_REGISTRY_USER" --password-stdin "$CI_REGISTRY"
  script:
    - docker load -i image.tar
    - docker tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
    - docker tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA $CI_REGISTRY_IMAGE:latest
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA
    - docker push $CI_REGISTRY_IMAGE:latest
```

**Step 2: コミット**

```bash
git add .gitlab/ci/release.yml
git commit -m "ci: add release job pushing Docker image to Container Registry

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 41: ルート `.gitlab-ci.yml`

**Files:**
- Create: `.gitlab-ci.yml`

**Step 1: 書く**

```yaml
include:
  - local: '.gitlab/ci/_defaults.yml'
  - local: '.gitlab/ci/setup.yml'
  - local: '.gitlab/ci/quality.yml'
  - local: '.gitlab/ci/security.yml'
  - local: '.gitlab/ci/package.yml'
  - local: '.gitlab/ci/scan.yml'
  - local: '.gitlab/ci/pages.yml'
  - local: '.gitlab/ci/release.yml'

workflow:
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == "main"'

stages:
  - setup
  - quality
  - security
  - package
  - scan
  - pages
  - release
```

**Step 2: Lint（GitLab CI Lint API が使えない場合はスキップ）**

GitLab セルフホストが手元で起動していない場合、YAML 構文チェックだけ行う：

Run: `python3 -c "import yaml, sys; [yaml.safe_load(open(f)) for f in sys.argv[1:]]" .gitlab-ci.yml .gitlab/ci/*.yml`
Expected: エラーなし

**Step 3: コミット**

```bash
git add .gitlab-ci.yml
git commit -m "ci: add root .gitlab-ci.yml composing stage definitions

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 11: docusaurus

### Task 42: `docs/` に docusaurus 雛形を配置

**Files:**
- Create: `docs/package.json`
- Create: `docs/docusaurus.config.js`
- Create: `docs/sidebars.js`
- Create: `docs/docs/intro.md`
- Create: `docs/src/pages/index.js`
- Create: `docs/src/css/custom.css`
- Create: `docs/static/img/.gitkeep`

**Step 1: `docs/package.json`**

```json
{
  "name": "sak-dev-env-docs",
  "version": "0.0.0",
  "private": true,
  "scripts": {
    "start": "docusaurus start",
    "build": "docusaurus build",
    "serve": "docusaurus serve",
    "clear": "docusaurus clear"
  },
  "dependencies": {
    "@docusaurus/core": "^3.5.2",
    "@docusaurus/preset-classic": "^3.5.2",
    "@mdx-js/react": "^3.0.0",
    "clsx": "^2.0.0",
    "prism-react-renderer": "^2.3.0",
    "react": "^18.3.0",
    "react-dom": "^18.3.0"
  },
  "devDependencies": {
    "@docusaurus/module-type-aliases": "^3.5.2",
    "@docusaurus/types": "^3.5.2"
  },
  "engines": {
    "node": ">=22.0.0 <23"
  }
}
```

**Step 2: `docs/docusaurus.config.js`**

```js
export default {
  title: 'sak-dev-env',
  tagline: 'モダン開発環境ボイラープレートのドキュメント',
  favicon: 'img/favicon.ico',
  url: process.env.CI_PAGES_URL ?? 'http://localhost:3000',
  baseUrl: process.env.CI_PAGES_URL
    ? new URL(process.env.CI_PAGES_URL).pathname
    : '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  i18n: {
    defaultLocale: 'ja',
    locales: ['ja'],
  },
  presets: [
    [
      'classic',
      {
        docs: { sidebarPath: './sidebars.js' },
        blog: false,
        theme: { customCss: './src/css/custom.css' },
      },
    ],
  ],
};
```

**Step 3: `docs/sidebars.js`**

```js
export default {
  defaultSidebar: [{ type: 'autogenerated', dirName: '.' }],
};
```

**Step 4: `docs/docs/intro.md`**

```md
---
sidebar_position: 1
---

# はじめに

sak-dev-env のボイラープレート ドキュメントサイトです。本文は PBI ID:2（サンプルアプリ作成）で追加されます。
```

**Step 5: `docs/src/pages/index.js`**

```js
import React from 'react';
import Layout from '@theme/Layout';

export default function Home() {
  return (
    <Layout title="Home" description="sak-dev-env docs">
      <main style={{ padding: '2rem' }}>
        <h1>sak-dev-env</h1>
        <p>モダン開発環境ボイラープレートのドキュメントです。</p>
        <p>
          <a href="./docs/intro">ドキュメントを読む</a>
        </p>
      </main>
    </Layout>
  );
}
```

**Step 6: `docs/src/css/custom.css`**

```css
:root {
  --ifm-color-primary: #2e6cb5;
}
```

**Step 7: `docs/static/img/.gitkeep`**

空ファイル。

**Step 8: ローカルビルド確認**

Run: `cd docs && npm install && npm run build && cd ..`
Expected: `docs/build/` が生成される

**Step 9: コミット**

```bash
git add docs/ 
git commit -m "docs: scaffold docusaurus site with Japanese default locale

PBI ID:1 の範囲は雛形のみ。本文は ID:2 で追加。

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 12: README 更新

### Task 43: README.md に前提環境・セットアップ・運用ガイドを追加

**Files:**
- Modify: `README.md`（既存が無ければ新規作成）

**Step 1: 書く**

```md
# sak-dev-env

モダンな開発環境のサンプルを提供する Spring Boot + Thymeleaf + Vite ボイラープレート。

## 前提環境

以下を開発者マシンにインストール済みであること：

| ツール | バージョン | 備考 |
|---|---|---|
| JDK | 21 | `JAVA_HOME` を通しておく |
| Node.js | 22 (LTS) | `nvm` 等で切替可能 |
| Docker | 27 以降 | イメージビルドと Trivy スキャン用 |

### VSCode 拡張機能（推奨）

以下がインストール済みであることを前提に `.vscode/settings.json` / `tasks.json` が設計されています：

- Extension Pack for Java (`vscjava.vscode-java-pack`)
- Spring Boot Extension Pack (`vmware.vscode-boot-dev-pack`)
- ESLint (`dbaeumer.vscode-eslint`)
- Prettier (`esbenp.prettier-vscode`)
- EditorConfig (`editorconfig.editorconfig`)
- Checkstyle (`shengchen.vscode-checkstyle`)
- GitLab Workflow (`gitlab.gitlab-workflow`)

## 初回セットアップ

```bash
nvm use                             # .nvmrc に従って Node 22 へ
npm install                         # 依存導入 + husky 有効化 + lint-tools 展開
./mvnw -N dependency:go-offline     # Maven 依存のウォームアップ
```

動作確認：

```bash
./mvnw test      # Java ユニットテスト
npm test         # Vitest
npm run build    # Vite ビルド（src/main/resources/static/ に出力）
```

## 開発サイクル

### コミットまで

1. コード変更
2. `git add <files>`
3. `git commit` → pre-commit フック（lint-staged で変更ファイルの Prettier/ESLint/Checkstyle/PMD/Secretlint）+ commit-msg フック（Conventional Commits 検証）が自動実行

### プッシュまで

`git push` 時に pre-push フックで以下が順次実行されます（所要 1〜3 分）：

- ESLint 全体 / Prettier format:check
- Vitest
- npm audit
- `./mvnw -Pfast verify`（Checkstyle/PMD フル、SpotBugs+find-sec-bugs、JUnit、JaCoCo）

### `--no-verify` バイパスは原則禁止

ローカルフックは技術的にバイパス可能ですが、運用上は禁止し、例外時はコミットメッセージに理由を残してください。GitLab のブランチ保護で CI パイプライン成功を merge 条件にすることで最終的な品質は担保されています。

## CI/CD

`.gitlab-ci.yml` が以下 2 系統のパイプラインを定義します：

- **MR パイプライン**（Merge Request 時）: setup → quality → security → package → scan
- **main パイプライン**（main push 時）: 上記 + pages（GitLab Pages デプロイ）+ release（Container Registry push）

詳細は `docs/plans/2026-04-22-dev-env-setup-design.md` を参照。

### 社内ミラーへの切り替え一覧

社内プライベートネットワーク環境（`AGENTS.md` の制約条件）でこのボイラープレートを利用する場合、
以下の 4 箇所が外部インターネット接続を要求します。それぞれ社内ミラー / プロキシを設定してください。

| # | 設定箇所 | 切替対象 | 既定値 |
|---|---|---|---|
| 1 | `.gitlab/ci/_defaults.yml` の `IMAGE_*` 変数 | Docker Hub の各種公式イメージ | `maven:3.9-eclipse-temurin-21` 等 |
| 2 | `.gitlab/ci/_defaults.yml` の `TRIVY_DB_REPOSITORY` / `TRIVY_JAVA_DB_REPOSITORY` | Trivy 脆弱性 DB（`ghcr.io/aquasecurity/trivy-db`） | コメントアウト済（既定は ghcr） |
| 3 | `pom.xml` の `ci-mr` profile 内 OWASP Dependency-Check 設定 (`<nvdApiServerUrl>` 等) | NIST NVD への直接アクセス | コメントアウト済（既定は NIST 直接） |
| 4 | `pom.xml` の `<node.download.root>` / `<npm.download.root>` プロパティ | `nodejs.org/dist/` からの Node バイナリ取得 | `https://nodejs.org/dist/` |

## Docker

```bash
docker build -t sak-dev-env:local .
docker run --rm -p 8080:8080 sak-dev-env:local
docker run --rm aquasec/trivy image sak-dev-env:local
```

## ドキュメント

docusaurus プロジェクトは `docs/` 配下にあります。

```bash
cd docs
npm install
npm start                # http://localhost:3000 で起動
```

本番公開先は GitLab Pages（main ブランチへの push 時に自動デプロイ）。URL は `$CI_PAGES_URL`。

## ディレクトリ構成

```
.
├── .gitlab/ci/              CI stage 定義（分割）
├── .husky/                  Git フック
├── .vscode/                 共有 VSCode 設定
├── docs/                    docusaurus（ドキュメント）
├── rules/                   Checkstyle / PMD / SpotBugs ruleset
├── scripts/                 lint-staged 用ラッパー
├── src/
│   ├── main/frontend/       フロントエンドソース（Vite）
│   ├── main/java/           Spring Boot ソース
│   └── main/resources/      Spring Boot リソース + Vite 出力先
├── Dockerfile               マルチステージビルド
├── pom.xml                  Maven（Spring Boot）
├── package.json             フロントエンド + lint ツール
└── vite.config.js / vitest.config.js / eslint.config.js / .prettierrc.json / commitlint.config.js
```

## ライセンス

未定。
```

**Step 2: コミット**

```bash
git add README.md
git commit -m "docs: add comprehensive README with setup, workflow, CI guidance

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Phase 13: 最終検証

### Task 44: ローカル受け入れ検証（L1〜L8）

設計書 第10章の検証シナリオを順次実行。**コミットは作らない、検証のみ**。

**Step 1: L1 - クリーン状態でセットアップ完走**

```bash
rm -rf node_modules target
npm install
./mvnw -N dependency:go-offline
ls target/lint-tools/
```
Expected: 全コマンド exit 0、`target/lint-tools/` に 3 種の成果物が展開。

**Step 2: L2 - `./mvnw verify`**

Run: `./mvnw -Pfast verify`
Expected: BUILD SUCCESS

**Step 3: L3 - Maven から Vite ビルド連動**

```bash
./mvnw -DskipTests package
ls src/main/resources/static/js/main.js
```
Expected: `main.js` が生成されている

**Step 4: L4 - Vitest**

Run: `npm test`
Expected: 1 test passed

**Step 5: L5 - pre-commit が違反を拾う**

```bash
echo "class Bad { public static void main(String[]a){int    x=1;} }" > src/main/java/com/example/demo/Bad.java
git add src/main/java/com/example/demo/Bad.java
git commit -m "test: trigger blocking"  || echo "BLOCKED_AS_EXPECTED"
git reset HEAD src/main/java/com/example/demo/Bad.java
rm src/main/java/com/example/demo/Bad.java
```
Expected: pre-commit フックが fail し、`BLOCKED_AS_EXPECTED` が出力される。

**Step 6: L6 - Secretlint が AWS ダミーキーを拾う**

```bash
echo "AWS_KEY=AKIAXXXXXXXXXXXXXXXX" > .env.sample
git add .env.sample
git commit -m "test: trigger secretlint" || echo "BLOCKED_AS_EXPECTED"
git reset HEAD .env.sample
rm .env.sample
```
Expected: `BLOCKED_AS_EXPECTED`

**Step 7: L7 - commit-msg が規約違反を弾く**

```bash
echo "tmp" > tmp.txt
git add tmp.txt
git commit -m "fix it" || echo "BLOCKED_AS_EXPECTED"
git reset HEAD tmp.txt
rm tmp.txt
```
Expected: `BLOCKED_AS_EXPECTED`

**Step 8: L8 - pre-push 相当を手動実行**

Run: `bash .husky/pre-push`
Expected: 全検査 pass

---

### Task 45: Docker ビルド + Trivy スキャン（C5, C6 相当）

**Step 1: イメージビルド**

Run: `DOCKER_BUILDKIT=1 docker build -t sak-dev-env:local .`
Expected: BUILD SUCCESS

**Step 2: Trivy スキャン**

Run: `docker run --rm aquasec/trivy:latest image --severity HIGH,CRITICAL --exit-code 0 sak-dev-env:local`
Expected: スキャン完了。HIGH/CRITICAL の脆弱性があれば内容を記録し、後続対応を判断。

**Step 3: コミットなし**（検証のみ）

---

### Task 46: 最終ディレクトリ構造チェック

設計書 第9章の一覧と実在ファイルを照合。

Run: `git ls-files | sort`
Expected: 新規作成ファイルがすべてコミット済み、抜け落ちなし。

もし抜け漏れがあれば該当 Task に戻って追加。

---

### Task 47: `PBI.md` の ID:1 ステータスを `done` に更新し、受け入れチェックリストを追記

**Files:**
- Modify: `PBI.md`

**Step 1: ID:1 のステータス更新と受け入れチェックリスト追記**

```md
# ID: 1
- PBI名: 開発環境整備
- ステータス: done
- 受け入れ基準
  - [x] ローカル開発環境が整備されていること
    - [x] .vscode の整備（settings.json / tasks.json / launch.json を共有）
    - [x] pre-commit / commit-msg / pre-push フックの整備（Husky + lint-staged + commitlint）
  - [x] CI/CDパイプラインが整備されていること
    - [x] .gitlab-ci.yml と .gitlab/ci/*.yml で stage 分割済み
    - [x] MR パイプラインと main パイプラインの 2 系統
    - [x] Docker イメージのビルド + SBOM 生成 + 脆弱性スキャン
    - [x] docusaurus の GitLab Pages 自動デプロイ
- 注意事項
  - 設計書: docs/plans/2026-04-22-dev-env-setup-design.md
  - 実装計画: docs/plans/2026-04-22-dev-env-setup.md
```

**Step 2: コミット**

```bash
git add PBI.md
git commit -m "docs(pbi): mark PBI ID:1 as done with acceptance checklist

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## 完了条件

- Task 1 から Task 47 までが全てコミット済みであること
- `git log --oneline` で Conventional Commits の履歴が連なること
- Task 44 の L1〜L8 全シナリオが期待通りに動作すること
- Task 45 の Docker ビルドが成功すること
- 設計書（`2026-04-22-dev-env-setup-design.md`）第9章のファイル一覧と `git ls-files` の出力が一致すること

## 既知の限界 / 後続 PBI 引き渡し

- 実際の Spring Boot コントローラ / 画面 / エンティティは ID:2 で追加
- カバレッジしきい値（60%/50%）は ID:2 の実装完了後に再評価
- Actuator 依存は ID:2 で追加予定。それまで Dockerfile の `HEALTHCHECK` は暫定。
- `rules/checkstyle/eclipse-formatter.xml` は最小版。詳細調整は ID:2 のコード投入後。
- GitLab 側のブランチ保護・MR 必須化設定は管理画面側のため README で明記のみ
