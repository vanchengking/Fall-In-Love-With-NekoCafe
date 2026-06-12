import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/healthz': 'http://localhost:8080',
    },
    // 不要在这里全局覆盖 Content-Type：会把 JS 模块也标成 text/html，
    // 浏览器按 MIME 严格校验直接拒绝执行，页面空白。HTML 编码由 index.html 的 <meta charset> 保证。
  },
})
