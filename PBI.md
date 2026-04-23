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
- ステータス: done
- 受け入れ基準
  - [x] ローカル開発環境が整備されていること
    - [x] .vscode の整備（settings.json / tasks.json / launch.json を共有）
    - [x] pre-commit / commit-msg / pre-push フックの整備（Husky + lint-staged + commitlint）
  - [x] CI/CD パイプラインが整備されていること
    - [x] .gitlab-ci.yml と .gitlab/ci/\*.yml で stage 分割済み
    - [x] MR パイプラインと main パイプラインの 2 系統
    - [x] Docker イメージのビルド + SBOM 生成 + 脆弱性スキャン
    - [x] docusaurus の GitLab Pages 自動デプロイ
- 注意事項
  - 設計書: docs/plans/2026-04-22-dev-env-setup-design.md
  - 実装計画書: docs/plans/2026-04-22-dev-env-setup.md

# ID: 2

- PBI名: サンプルアプリの作成
- ステータス: to do
- 受け入れ基準
  - 簡単なサンプルアプリが作成されていること
- 注意事項
- 指摘事項（in progressの場合のみ）
