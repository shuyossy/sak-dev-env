雛形

```
# ID:
- PBI名:
- ステータス: [to do/in progress/done]
- ユーザストーリー/背景
- 受け入れ基準
- 注意事項
- 指摘事項（in progressの場合のみ）
```

# ID: 1

- PBI名: 開発環境整備
- ステータス: in progress
- 受け入れ基準
  - [x] ローカル開発環境が整備されていること
    - [x] .vscode の整備（settings.json / tasks.json / launch.json を共有）
    - [x] pre-commit / commit-msg / pre-push フックの整備（Husky + lint-staged + commitlint）
  - [x] CI/CD パイプラインが整備されていること
    - [x] .gitlab-ci.yml と .gitlab/ci/\*.yml で stage 分割済み
    - [x] MR パイプラインと main パイプラインの 2 系統
    - [x] Docker イメージのビルド + SBOM 生成 + 脆弱性スキャン
    - [x] docusaurus の GitLab Pages 自動デプロイ
  - [x] Javaの品質担保戦略の標準準拠化（指摘事項対応、2026-04-24）
  - [x] Javaの品質担保戦略の明文化（rules/README.md 作成、指摘事項対応、2026-04-24）
  - [x] Checkstyle 二層構成の非対称性解消（blocking ruleset を pre-push / CI でも再実行、advisory / blocking のファイル名ペア化、指摘事項対応、2026-04-24）
  - [x] 品質ツール（Checkstyle / PMD / SpotBugs / JUnit / JaCoCo）の `mvn site` レポート出力対応と GitLab Pages への `/reports/` 公開（指摘事項対応、2026-04-24）
- 注意事項
- 指摘事項

# ID: 2

- PBI名: サンプルアプリの作成
- ステータス: done
- 受け入れ基準
  - [x] サンプルアプリが作成されていること
    - [x] お題は旅程作成アプリ（Trip + Activity の CRUD）
    - [x] ドメインは`sak.sample`
    - [x] コンテキストパスは`sampleapp`
    - [x] H2DBのインメモリを利用、Spring Data JPA で操作
    - [x] Lombokを利用（@Getter / @Setter / @RequiredArgsConstructor / @Slf4j）
    - [x] トランザクションスクリプトパターンで実装（Service 層に集約）
    - [x] テスト整備（Java: 21 ケース、JavaScript: 3 ケース）
    - [x] フロントエンドは jQuery / Bootstrap を webjars 経由で取得
    - [x] ドキュメント整備（docusaurus 5 章 + intro、mermaid 対応）
    - [x] SpringAI 導入（OpenAI Compatible、mock プロファイルで実 LLM 不要）
      - [x] WeatherTool（@Tool）と SuggestedActivity（structured output）を組み合わせ、
            天気を取得して Activity 提案＋登録する機能を実装
  - [x] 静的解析ツールのチェック内容が最適化されている
        （ボイラープレート方針に沿って構造的衝突を rule property / exclude で TOBE 化、
        意思決定は rules/README.md §4.5 の決定ログに記録）
- 注意事項
  - できるだけシンプルに、コード量少なく作成すること
  - 静的解析を回避するようなアノテーションやコメントをコード内に記載することは禁止
  - 静的解析チェック内容の最適化については、本ボイラープレートを適用したチームが品質とスピードを両立しながら開発できるように実施すること
    - 実装中のサンプルアプリに警告がでないようにその場しのぎでチェック内容を変更するのではなく、理想のTOBEを考えた上でチェック内容を最適化すること
- 指摘事項（in progressの場合のみ）
