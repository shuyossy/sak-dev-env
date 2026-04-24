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
- ステータス: to do
- 受け入れ基準
  - 簡単なサンプルアプリが作成されていること
- 注意事項
- 指摘事項（in progressの場合のみ）
