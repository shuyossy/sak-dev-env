// REST API への薄いラッパ。jQuery の $.ajax を JSON でやり取りするだけ。
// グローバルに jQuery が読み込まれている前提（Thymeleaf 側で webjar からロード）。

const CONTEXT = '/sampleapp';

function url(path) {
  return CONTEXT + path;
}

export const api = {
  async post(path, data) {
    return $.ajax({
      url: url(path),
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(data),
    });
  },
  async put(path, data) {
    return $.ajax({
      url: url(path),
      method: 'PUT',
      contentType: 'application/json',
      data: JSON.stringify(data),
    });
  },
  async del(path) {
    return $.ajax({ url: url(path), method: 'DELETE' });
  },
};

// テストや別モジュールから単独で URL を組み立てたい場合に使う
export function apiUrl(path) {
  return url(path);
}
