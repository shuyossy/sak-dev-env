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
  - [x] Javaの品質担保戦略の標準準拠化（指摘事項対応、2026-04-24）
- 注意事項
  - 設計書: docs/plans/2026-04-22-dev-env-setup-design.md
  - 実装計画書: docs/plans/2026-04-22-dev-env-setup.md
- 指摘事項（対応済）
  - Javaの品質担保戦略に関する改善 → 対応済（2026-04-24）
    - 当初インデントを"4"にする観点から Google 標準フォーマット採用を見送っていたが、実装途中で google-java-format に変更していた
      - Checkstyle フル ruleset を checkstyle 10.17.0 同梱の `google_checks.xml` **逐語コピー**に置換し、独自選択18モジュール構成を廃止
      - Eclipse formatter XML を Google 公式 `eclipse-java-google-style.xml` のフル版に置換し、VSCode 保存時整形も Spotless / Checkstyle と同じ Google Java Style に揃えた
      - line-length を Google 標準の 100 に厳格化（旧 120）
    - Checkstyle / PMD の独自定義を標準寄りに整理
      - PMD ruleset は 8 個の独自除外をすべて削除し、標準8カテゴリを逐語参照するだけに簡素化。カスタマイズ手順をファイル内コメントで案内
      - ソース個別の抑制は `@SuppressWarnings("PMD.<RuleName>")` + 理由コメントで可視化する方針
      - Checkstyle blocking ruleset（pre-commit 用）は "必ず止めたい" 用途で独立した責務として維持
    - 設計書 `docs/plans/2026-04-22-dev-env-setup-design.md` のインデント記述と rules/ ディレクトリ構造説明を最新状態に更新

# ID: 2

- PBI名: サンプルアプリの作成
- ステータス: to do
- 受け入れ基準
  - 簡単なサンプルアプリが作成されていること
- 注意事項
- 指摘事項（in progressの場合のみ）
