// 旅程詳細画面のスクリプト：Trip 編集 / 削除、Activity 追加 / 削除、AI 提案。
import { api } from './api-client.js';

$(function () {
  const tripId = $('body').data('tripId');

  // 旅程の更新
  $('#tripEditForm').on('submit', async function (e) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(this));
    try {
      await api.put(`/api/trips/${tripId}`, data);
      location.reload();
    } catch (err) {
      console.error(err);
      alert('更新に失敗しました');
    }
  });

  // 旅程の削除
  $('#tripDeleteBtn').on('click', async function () {
    if (!confirm('この旅程を削除しますか？')) return;
    try {
      await api.del(`/api/trips/${tripId}`);
      location.href = '/sampleapp/trips';
    } catch (err) {
      console.error(err);
      alert('削除に失敗しました');
    }
  });

  // アクティビティ追加
  $('#activityCreateForm').on('submit', async function (e) {
    e.preventDefault();
    // 空文字は null に正規化（time / location / note は任意）。
    // bracket access を避け security/detect-object-injection 警告を回避。
    const entries = Array.from(new FormData(this).entries()).map(([k, v]) => [
      k,
      v === '' ? null : v,
    ]);
    const data = Object.fromEntries(entries);
    try {
      await api.post(`/api/trips/${tripId}/activities`, data);
      location.reload();
    } catch (err) {
      console.error(err);
      alert('追加に失敗しました');
    }
  });

  // アクティビティ削除（行のボタン）
  $(document).on('click', '.js-activity-delete', async function () {
    const activityId = $(this).data('activityId');
    if (!confirm('このアクティビティを削除しますか？')) return;
    try {
      await api.del(`/api/trips/${tripId}/activities/${activityId}`);
      location.reload();
    } catch (err) {
      console.error(err);
      alert('削除に失敗しました');
    }
  });

  // AI 提案
  $('#aiSuggestForm').on('submit', async function (e) {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(this));
    try {
      await api.post(`/api/trips/${tripId}/ai/suggest-activities`, data);
      location.reload();
    } catch (err) {
      console.error(err);
      alert('AI 提案に失敗しました');
    }
  });
});
