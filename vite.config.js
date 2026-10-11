// ═══════════════════════════════════════════════════════════════════════════════
// npm install -D vite@8.3.0 --save-exact  (o la versión que uses)
// ═══════════════════════════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════════════════════════
// vite.config.js
// ═══════════════════════════════════════════════════════════════════════════════

import { fileURLToPath, URL } from 'url'
import { defineConfig, loadEnv, createLogger } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'
import viteImagemin from 'vite-plugin-imagemin'
import { VitePWA } from 'vite-plugin-pwa'
import compression from 'vite-plugin-compression'
import Pages from 'vite-plugin-pages'
import { viteStaticCopy } from 'vite-plugin-static-copy'
import removeConsole from 'vite-plugin-remove-console'
import { visualizer } from 'rollup-plugin-visualizer'
import os from 'os'
import qrcode from 'qrcode'

const logger = createLogger()
const originalWarn = logger.warn
logger.warn = (msg, options) => {
  if (typeof msg === 'string' && (
    msg.includes('Some chunks are larger than') ||
    msg.includes('larger than 500 kB') ||
    msg.includes('code-split the application')
  )) return
  originalWarn(msg, options)
}

export default defineConfig(({ command, mode }) => {
  const isBuild = command === 'build'
  const isDev = !isBuild
  const env = loadEnv(mode, process.cwd(), '')

  const apiUrl = env.VITE_API_URL || '/contramano/api'
  const backendTarget = env.VITE_BACKEND_URL || 'http://localhost:8000'

  // ✅ Flag: cuando compilamos para APK, NO comprimir (Gradle ve duplicados)
  const isCapBuild = !!process.env.CAP_BUILD

  let localIp = 'localhost'
  if (isDev) {
    const nets = os.networkInterfaces()
    for (const name of Object.keys(nets)) {
      for (const net of nets[name]) {
        if (net.family === 'IPv4' && !net.internal && net.address.startsWith('192.168.')) {
          localIp = net.address
          break
        }
      }
      if (localIp !== 'localhost') break
    }
  }

  const hmrHost = env.VITE_HMR_HOST || localIp

  console.log(`\n🔧 API proxy : ${backendTarget}`)
  console.log(`🔧 API URL   : ${apiUrl}`)
  console.log(`🔧 IP local  : ${localIp}`)
  console.log(`🔧 HMR host  : ${hmrHost}`)
  console.log(`🔧 CAP_BUILD : ${isCapBuild ? 'SI (sin gzip/brotli)' : 'no'}\n`)

  return {
    base: isBuild ? './' : '/contramano/',
    customLogger: logger,

    optimizeDeps: {
      include: [
        'vue', 'vue-router', 'axios', 'bootstrap', 'jquery',
        'chart.js', 'chart.js/auto', 'three', 'crypto-js',
        'moment',
        'wavesurfer.js', 'html5-qrcode', 'xlsx', 'exceljs',
        'jspdf-autotable', 'html2pdf.js', 'file-saver', 'jszip'
      ],
      exclude: ['@iconify-json/*', '@capacitor/*'],
    },

    plugins: [
      vue(),
      Pages({ dirs: 'src/pages', extensions: ['vue'] }),
      Components({
        resolvers: [IconsResolver({ prefix: 'Icon' })],
        dts: false,
      }),
      Icons({ autoInstall: true, compiler: 'vue3' }),

      isBuild && VitePWA({
        registerType: 'autoUpdate',
        injectRegister: 'auto',
        manifest: false,
        workbox: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
          maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
          navigateFallback: '/contramano/index.html',
          navigateFallbackDenylist: [/^\/contramano\/api/],
        },
        devOptions: { enabled: false },
      }),

      // ✅ Solo comprimir si NO es build de APK
      isBuild && !isCapBuild && compression({
        algorithm: 'gzip',
        ext: '.gz',
        threshold: 10240,
        deleteOriginFile: false,
      }),
      isBuild && !isCapBuild && compression({
        algorithm: 'brotliCompress',
        ext: '.br',
        threshold: 10240,
        deleteOriginFile: false,
      }),

      viteStaticCopy({ targets: [{ src: 'public/*', dest: '.' }] }),

      isBuild && removeConsole({ includes: ['log', 'debug', 'info'] }),

      isBuild && viteImagemin({
        gifsicle: { optimizationLevel: 7 },
        optipng: { optimizationLevel: 7 },
        mozjpeg: { quality: 80 },
        pngquant: { quality: [0.8, 0.9], speed: 4 },
        svgo: {
          plugins: [
            { name: 'removeViewBox', active: false },
            { name: 'removeEmptyAttrs', active: false },
          ],
        },
      }),

      process.env.ANALYZE && visualizer({
        filename: 'dist/stats.html',
        gzipSize: true,
        brotliSize: true,
      }),

      isDev && {
        name: 'qr-code-generator',
        apply: 'serve',
        configureServer(server) {
          const generateQR = async () => {
            if (localIp === 'localhost') return
            const url = `http://${localIp}:5173/contramano/`
            try {
              const qr = await qrcode.toString(url, { type: 'terminal', small: true })
              console.log('\n====================================')
              console.log('📱 URL para tu celular (Red Local):')
              console.log(`🌐 ${url}`)
              console.log(qr)
              console.log('====================================\n')
            } catch {
              console.log(`🌐 URL para celular: ${url}`)
            }
          }
          server.httpServer?.once('listening', generateQR)
        },
      },
    ].filter(Boolean),

    resolve: {
      alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
    },

    server: {
      port: 5173,
      host: '0.0.0.0',
      strictPort: true,
      https: false,
      cors: true,
      allowedHosts: true,
      headers: { 'Cache-Control': 'no-store' },

      proxy: {
        '/contramano/api': {
          target: backendTarget,
          changeOrigin: true,
          secure: false,
        },
      },

      hmr: {
        protocol: 'ws',
        host: hmrHost,
        port: 5173,
        clientPort: 5173,
      },
    },

    build: {
      assetsDir: 'assets',
      target: 'esnext',
      minify: 'esbuild',
      sourcemap: false,
      chunkSizeWarningLimit: 99999,

      commonjsOptions: {
        include: [/lamejs/, /node_modules/],
        transformMixedEsModules: true,
      },

      rollupOptions: {
        output: {
          entryFileNames: 'js/[name]-[hash].js',
          chunkFileNames: 'js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            const ext = assetInfo.name.split('.').pop()?.toLowerCase()
            if (/png|jpe?g|gif|svg|webp/.test(ext)) return 'assets/img/[name]-[hash][extname]'
            if (/woff2?|eot|ttf|otf/.test(ext)) return 'assets/fonts/[name]-[hash][extname]'
            if (ext === 'css') return 'assets/css/[name]-[hash][extname]'
            return 'assets/[name]-[hash][extname]'
          },
          manualChunks(id) {
            if (id.includes('node_modules')) {
              if (id.includes('vue')) return 'vendor_vue'
              if (id.includes('bootstrap')) return 'vendor_bootstrap'
              if (id.includes('chart.js')) return 'vendor_chart'
              if (id.includes('three')) return 'vendor_three'
              if (id.includes('xlsx') || id.includes('exceljs')) return 'vendor_excel'
              if (id.includes('jspdf') || id.includes('html2pdf')) return 'vendor_pdf'
              return 'vendor_rest'
            }
          },
        },
      },
    },
  }
})