import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('api-client', () => {
  beforeEach(() => {
    // global.$ をモックして jQuery $.ajax 呼び出しを観測する
    global.$ = { ajax: vi.fn().mockResolvedValue({ id: 1 }) };
    // ESM の動的 import で毎回新しいモジュール状態を得る
    vi.resetModules();
  });

  it('post は context-path 付きで $.ajax を JSON で呼ぶ', async () => {
    const { api } = await import('../src/api-client.js');
    await api.post('/api/trips', { title: 'x' });
    expect(global.$.ajax).toHaveBeenCalledWith({
      url: '/sampleapp/api/trips',
      method: 'POST',
      contentType: 'application/json',
      data: '{"title":"x"}',
    });
  });

  it('put は context-path 付きで $.ajax を JSON で呼ぶ', async () => {
    const { api } = await import('../src/api-client.js');
    await api.put('/api/trips/1', { title: 'y' });
    expect(global.$.ajax).toHaveBeenCalledWith({
      url: '/sampleapp/api/trips/1',
      method: 'PUT',
      contentType: 'application/json',
      data: '{"title":"y"}',
    });
  });

  it('del は context-path 付きで DELETE 呼び出しになる', async () => {
    const { api } = await import('../src/api-client.js');
    await api.del('/api/trips/1');
    expect(global.$.ajax).toHaveBeenCalledWith({
      url: '/sampleapp/api/trips/1',
      method: 'DELETE',
    });
  });
});
