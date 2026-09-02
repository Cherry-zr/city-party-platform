import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

function loadHttpsOptions(env) {
  const certPath = env.DEV_HTTPS_CERT_PATH?.trim()
  const keyPath = env.DEV_HTTPS_KEY_PATH?.trim()
  if (!certPath && !keyPath) {
    return undefined
  }
  if (!certPath || !keyPath) {
    throw new Error('DEV_HTTPS_CERT_PATH and DEV_HTTPS_KEY_PATH must be configured together')
  }
  return {
    cert: readFileSync(resolve(certPath)),
    key: readFileSync(resolve(keyPath))
  }
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    server: {
      port: 5173,
      https: command === 'serve' ? loadHttpsOptions(env) : undefined,
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true
        },
        '/uploads': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true
        },
        '/ws': {
          target: 'ws://127.0.0.1:8080',
          ws: true,
          changeOrigin: true
        }
      }
    }
  }
})
