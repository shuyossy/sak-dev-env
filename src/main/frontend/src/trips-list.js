// 旅程一覧画面のスクリプト：新規作成モーダル送信のみ。
import { api } from './api-client.js';

$(function () {
  $('#tripCreateForm').on('submit', async function (e) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(this));
    try {
      await api.post('/api/trips', data);
      location.reload();
    } catch (err) {
      console.error(err);
      alert('作成に失敗しました');
    }
  });
});
