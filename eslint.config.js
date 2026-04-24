import js from '@eslint/js';
import security from 'eslint-plugin-security';
import prettier from 'eslint-config-prettier';
import globals from 'globals';

export default [
  {
    // docs/plans/ は設計書（lint 対象外）。
    // docs/node_modules, docs/build, docs/.docusaurus はビルド生成物・依存物。
    ignores: ['docs/plans/**', 'docs/node_modules/**', 'docs/build/**', 'docs/.docusaurus/**'],
  },
  js.configs.recommended,
  security.configs.recommended,
  {
    // docs/ 配下は独立した Docusaurus プロジェクト（Node スコープ + React/JSX）。
    // ルート Vite アプリとは別サブプロジェクトとして扱い、Node/Browser グローバルと
    // JSX パースを許可する（Docusaurus の推奨スキャフォールドに準拠）。
    files: ['docs/**/*.{js,mjs,cjs}'],
    languageOptions: {
      ecmaVersion: 2024,
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: { jsx: true },
      },
      globals: { ...globals.node, ...globals.browser },
    },
    rules: {
      // Docusaurus 雛形は React コンポーネント（JSX）を含む。
      // eslint-plugin-react を導入していないため、JSX 内で参照される識別子を
      // ESLint が検出できず no-unused-vars が誤検出する。雛形範囲では無効化する。
      'no-unused-vars': 'off',
    },
  },
  {
    files: ['src/main/frontend/**/*.js'],
    languageOptions: {
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.browser },
    },
    rules: {
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      eqeqeq: 'error',
      'no-var': 'error',
      'prefer-const': 'error',
    },
  },
  {
    // scripts/ 配下は lint-staged から起動される Node スクリプト群。
    // fileURLToPath 等の Node API と process/console グローバルを使う。
    files: ['scripts/**/*.{js,mjs,cjs}', '*.config.js', 'commitlint.config.js'],
    languageOptions: {
      ecmaVersion: 2024,
      sourceType: 'module',
      globals: { ...globals.node },
    },
  },
  {
    files: ['src/main/frontend/**/*.test.js'],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
  },
  prettier,
];
