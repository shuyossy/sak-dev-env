---
sidebar_position: 1
---

# はじめに

`sak-dev-env` ボイラープレートに同梱されている **旅程作成サンプルアプリ**（`sampleapp`）のドキュメントです。
ボイラープレート適用先のチームが、Spring Boot ベースのモダンな開発環境（シフトレフト：静的解析・テスト・トレーシング）の動作を実コードで確認できるよう、最小限の機能で構成されています。

## 何ができるアプリか

- 旅程（Trip）と旅程内アクティビティ（Activity）の **CRUD**
  - Thymeleaf による 2 画面（一覧 / 詳細）+ AJAX + Bootstrap モーダル
- **AI 提案機能**：日付を指定すると、Spring AI 経由で LLM が天気を tool で取得し、その結果を踏まえた Activity を構造化レスポンス（structured output）で返して自動登録

> 「Spring AI の tool 呼び出し + structured output」を最小サンプルとして読めるようにしたコードを意図しています。

## 技術スタック（要点）

| 区分         | 採用                                                                      |
| ------------ | ------------------------------------------------------------------------- |
| バックエンド | Spring Boot 3.5 / Spring Data JPA / Bean Validation                       |
| 永続化       | H2 インメモリ（`spring.jpa.hibernate.ddl-auto=create-drop` + `data.sql`） |
| AI           | Spring AI（OpenAI Compatible）+ `mock` プロファイル時はスタブ ChatModel   |
| フロント     | Thymeleaf + jQuery + Bootstrap（webjar 経由）+ Vite                       |
| 観測性       | Micrometer Tracing (Brave) で `traceId` / `spanId` を MDC へ注入          |

## ドメイン / コンテキストパス

- パッケージ: `sak.sample`
- コンテキストパス: `/sampleapp`
- 起動後 URL: `http://localhost:8080/sampleapp/trips`

## 目次

1. [セットアップ・起動方法](./01-setup.md) — `mock` 起動 / 実 LLM 接続
2. [画面機能](./02-features.md) — 一覧 / 詳細 / Activity / AI 提案
3. [REST API リファレンス](./03-api-reference.md) — エンドポイント一覧と JSON 例
4. [アーキテクチャ](./04-architecture.md) — パッケージ構成 / ER 図 / シーケンス図
5. [トラブルシューティング](./05-troubleshooting.md) — よくある詰まりどころ

## 想定読者

- ボイラープレート適用先プロジェクトの開発者
- 「シフトレフト構成のサンプルが、実コード上でどう機能しているか」を確認したい方
