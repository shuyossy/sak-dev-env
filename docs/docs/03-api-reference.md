---
sidebar_position: 4
---

# REST API リファレンス

すべてのエンドポイントはコンテキストパス `/sampleapp` 配下です。レスポンスは JSON。
日付は ISO 8601（`yyyy-MM-dd`）、時刻は `HH:mm` 形式で扱います。

## エンドポイント一覧

| メソッド | パス                                        | 用途                | 成功ステータス |
| -------- | ------------------------------------------- | ------------------- | -------------- |
| GET      | `/api/trips`                                | 旅程一覧取得        | 200            |
| GET      | `/api/trips/{id}`                           | 旅程詳細取得        | 200            |
| POST     | `/api/trips`                                | 旅程作成            | 201            |
| PUT      | `/api/trips/{id}`                           | 旅程更新            | 200            |
| DELETE   | `/api/trips/{id}`                           | 旅程削除（cascade） | 204            |
| POST     | `/api/trips/{tripId}/activities`            | Activity 追加       | 201            |
| PUT      | `/api/trips/{tripId}/activities/{id}`       | Activity 更新       | 200            |
| DELETE   | `/api/trips/{tripId}/activities/{id}`       | Activity 削除       | 204            |
| POST     | `/api/trips/{tripId}/ai/suggest-activities` | AI 提案 → 自動登録  | 201            |

## Trip リソース

### `POST /api/trips`

リクエスト:

```json
{
  "title": "京都2泊3日",
  "destination": "京都",
  "startDate": "2026-05-01",
  "endDate": "2026-05-03"
}
```

レスポンス（201）:

```json
{
  "id": 2,
  "title": "京都2泊3日",
  "destination": "京都",
  "startDate": "2026-05-01",
  "endDate": "2026-05-03",
  "activities": []
}
```

### `GET /api/trips/{id}`

レスポンス（200）:

```json
{
  "id": 1,
  "title": "京都2泊3日",
  "destination": "京都",
  "startDate": "2026-05-01",
  "endDate": "2026-05-03",
  "activities": [
    {
      "id": 1,
      "date": "2026-05-01",
      "time": "09:00",
      "title": "清水寺",
      "location": "京都市東山区",
      "note": null
    }
  ]
}
```

### `PUT /api/trips/{id}`

リクエスト形式は `POST` と同一。レスポンスは更新後の `TripResponse`（200）。

### `DELETE /api/trips/{id}`

レスポンスボディなし（204）。`cascade=ALL` + `orphanRemoval=true` のため Activity も自動削除されます。

## Activity リソース

### `POST /api/trips/{tripId}/activities`

リクエスト:

```json
{
  "date": "2026-05-02",
  "time": "10:00",
  "title": "金閣寺見学",
  "location": "京都市",
  "note": "雨天時は屋内施設へ振替"
}
```

レスポンス（201）:

```json
{
  "id": 5,
  "date": "2026-05-02",
  "time": "10:00",
  "title": "金閣寺見学",
  "location": "京都市",
  "note": "雨天時は屋内施設へ振替"
}
```

### `PUT /api/trips/{tripId}/activities/{id}`

リクエスト形式は同上。レスポンスは更新後の `ActivityResponse`（200）。

### `DELETE /api/trips/{tripId}/activities/{id}`

レスポンスボディなし（204）。

## AI 提案

### `POST /api/trips/{tripId}/ai/suggest-activities`

リクエスト:

```json
{
  "date": "2026-05-02"
}
```

処理の流れ:

1. Service が Trip を取得し、`date` が Trip 期間内であることを検証
2. `ChatClient` に `WeatherTool`（`@Tool`）と `SuggestedActivity` の structured output 指定を渡してプロンプト送信
3. LLM が必要に応じて `getWeather(date, location)` を呼び出し、結果を踏まえた `SuggestedActivity` を JSON で返却
4. Service が `SuggestedActivity` を `Activity` に変換して保存

レスポンス（201、`ActivityResponse`）:

```json
{
  "id": 6,
  "date": "2026-05-02",
  "time": "10:00",
  "title": "金閣寺見学",
  "location": "京都市",
  "note": "曇りでも屋外散策可"
}
```

## エラー応答パターン

`GlobalExceptionHandler` で集約しています。

### 400 Bad Request — バリデーションエラー（`@Valid`）

```json
{
  "errors": [{ "field": "title", "message": "must not be blank" }]
}
```

### 400 Bad Request — ドメインルール違反

`startDate > endDate`、Activity の `date` が Trip 期間外、など。

```json
{ "message": "endDate は startDate 以降である必要があります" }
```

### 404 Not Found

`TripNotFoundException` / `ActivityNotFoundException`。

```json
{ "message": "Trip not found: id=999" }
```

### 502 Bad Gateway — AI 提案失敗

LLM 呼び出しが例外、または structured output が空。`traceId` が同梱されるので問い合わせ時に提示できます。

```json
{
  "message": "AI 提案の取得に失敗しました",
  "traceId": "65f3a1c2e4b0a8f9..."
}
```

### 500 Internal Server Error — 予期しない例外

```json
{
  "message": "internal error",
  "traceId": "65f3a1c2e4b0a8f9..."
}
```

## 認証

サンプルアプリは Spring Security を導入しておらず、認証はかかっていません。ボイラープレート適用先で必要に応じて追加してください。
