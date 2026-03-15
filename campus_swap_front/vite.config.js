import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    // 自动导入 Vue / Vue Router / Pinia API
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: false,
    }),
    // 自动注册 Element Plus 组件
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false,
    }),
  ],

  resolve: {
    alias: {
      // @ 指向 src 目录
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    port: 5173,
    proxy: {
      // 开发时将 /api 请求代理到后端，避免跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 代理上传文件的静态资源访问
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 代理动态相关的API请求
      '/post': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 代理物品相关的API请求（如果使用/item/my-for-post）
      '/item': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 代理换物记录相关的API请求
      '/swap': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
