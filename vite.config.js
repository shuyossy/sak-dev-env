import { defineConfig } from 'vite';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  root: resolve(__dirname, 'src/main/frontend'),
  build: {
    outDir: resolve(__dirname, 'src/main/resources/static'),
    emptyOutDir: false,
    rollupOptions: {
      input: {
        'trips-list': resolve(__dirname, 'src/main/frontend/src/trips-list.js'),
        'trips-detail': resolve(__dirname, 'src/main/frontend/src/trips-detail.js'),
      },
      output: {
        entryFileNames: 'js/[name].js',
        chunkFileNames: 'js/[name].js',
        assetFileNames: 'assets/[name].[ext]',
        // 複数 input をビルドするため format は ES modules（Vite 既定）。
        // 共通コード（api-client）は chunkFileNames で別ファイル化されるため、
        // テンプレート側の <script> は type="module" で読み込む。
      },
    },
  },
});
