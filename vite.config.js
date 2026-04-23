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
        main: resolve(__dirname, 'src/main/frontend/src/main.js'),
      },
      output: {
        entryFileNames: 'js/[name].js',
        chunkFileNames: 'js/[name].js',
        assetFileNames: 'assets/[name].[ext]',
      },
    },
  },
});
