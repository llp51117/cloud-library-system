import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',  //前端 允许局域网其他设备访问
    port: 5173,
    open: true,
    proxy: {
      // 代理后端接口
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 代理本地图片（头像 / 封面）
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
