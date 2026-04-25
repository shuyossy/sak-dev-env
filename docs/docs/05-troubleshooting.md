---
sidebar_position: 6
---

# トラブルシューティング

## 起動時のよくある詰まりどころ

### `SPRING_AI_OPENAI_API_KEY` 未設定で AI 提案が 502 になる

**症状**：`POST /api/trips/{id}/ai/suggest-activities` が `502 Bad Gateway`、ログに `AI suggestion failed`。

**原因**：実 LLM 接続モードで起動しており、API キーが未設定（または不正）。

**対処**：

- まず動作確認だけしたいなら `mock` プロファイルで起動してください。
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
  ```
- 実 LLM に繋ぐ場合は環境変数を設定してから起動。
  ```bash
  export SPRING_AI_OPENAI_API_KEY=sk-xxxxxxxx
  export SPRING_AI_OPENAI_BASE_URL=https://api.openai.com
  export SPRING_AI_OPENAI_MODEL=gpt-4o-mini
  ./mvnw spring-boot:run
  ```

### ポート 8080 が他プロセスに使われている

**症状**：起動時に `Web server failed to start. Port 8080 was already in use.`

**対処**：

- 使用中のプロセスを停止する、または
- 一時的に別ポートで起動する。
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081 \
    -Dspring-boot.run.profiles=mock
  ```
  起動後の URL は `http://localhost:8081/sampleapp/trips`。

### 起動が遅い / リソースを食う

**症状**：起動 / リロードに時間がかかる、CPU 使用率が高い。

**原因と対処**：

- Spring Boot DevTools のクラスローダ再起動（`spring-boot-devtools` を有効化している場合）。開発体験向上のために短時間の再起動が走ります。不要なら一時的に dev ツールを無効化（`spring.devtools.restart.enabled=false`）。
- 初回ビルドは Maven が依存をダウンロードします。社内ネット制約環境では Nexus 等のミラーを `~/.m2/settings.xml` に設定してください。

### H2 コンソールに繋がらない

サンプルアプリは **H2 コンソール（`/h2-console`）を有効化していません**（`spring.h2.console.enabled` を未設定 = デフォルト無効）。
H2 インメモリの内容を直接覗きたい場合は、開発時に限り以下を `application.properties` に追加してください。

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

接続情報：

| 項目     | 値                      |
| -------- | ----------------------- |
| JDBC URL | `jdbc:h2:mem:sampleapp` |
| User     | `sa`                    |
| Password | （空）                  |

> ボイラープレートとしては「本番環境で誤って公開しない」ためにデフォルト無効としています。コミット前に元に戻すことを推奨します。

## 動作中の挙動について

### Activity を追加したのに、Trip 期間外と言われる

**症状**：`400 Bad Request` で `{ "message": "date は Trip の期間内である必要があります" }`。

**原因**：`Activity.date` が `Trip.startDate` 〜 `Trip.endDate` の範囲外。
Service 層（`TripService` / `ItinerarySuggestionService`）でドメインルールとして検証しています。

**対処**：先に Trip の期間を更新（`PUT /api/trips/{id}`）してから Activity を追加してください。

### AI 提案が同じ内容ばかり返る

`mock` プロファイルでは `AiConfig#mockChatModel` が **固定 JSON** を返す仕様です。これは「ネット接続なしで UI / API の振る舞いを確認するため」のスタブ。
バリエーションが必要なら実 LLM 接続モードで起動してください。

### 再起動するとデータが消える

H2 インメモリ + `spring.jpa.hibernate.ddl-auto=create-drop` のため、**起動ごとに DB が再作成**され、`data.sql` が再投入されます。サンプルアプリの仕様です。

## ログ・トレース

レスポンスに `traceId` が含まれる 502 / 500 エラーが返ってきたら、その `traceId` でアプリログを grep してください。

```bash
grep <traceId> ./logs/sak-sample.log
```

ログ行のヘッダにも `[アプリ名,traceId,spanId]` の MDC が出力されます（`logging.pattern.level`）。

## ビルド・テスト

| 症状                          | 確認ポイント                                                                                                        |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `./mvnw test` が落ちる        | JDK バージョンが 21 か（`./mvnw -v`）                                                                               |
| `mvn site` で警告が大量に出る | サンプルアプリ自体には警告は出ない設計です。社内追加コードに対して `rules/README.md` の判定フローで対応してください |
| JS テストが落ちる             | `node -v` / `npm -v` がそれぞれ 22.14.0+ / 10.9.0+ か                                                               |
