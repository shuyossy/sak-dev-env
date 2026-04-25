-- 起動時の初期データ（H2 インメモリ向け）
-- defer-datasource-initialization=true により JPA がテーブル作成した後に実行される。

INSERT INTO trip (title, destination, start_date, end_date, created_at, updated_at)
VALUES ('京都2泊3日', '京都', DATE '2026-05-01', DATE '2026-05-03', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO activity (trip_id, date, time, title, location, created_at, updated_at)
VALUES (1, DATE '2026-05-01', TIME '09:00', '清水寺', '京都市東山区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (1, DATE '2026-05-01', TIME '12:00', '湯豆腐ランチ', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
