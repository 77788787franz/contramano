<template>
  <div class="app-bg-cover" :style="{ backgroundImage: `url(${backgroundImage})` }"></div>
  <div class="app-bg-overlay"></div>

  <div class="navbar-bottom">
    <nav class="bottom-nav">
      <router-link :to="linkEncriptado('home')" class="nav-btn" :class="{ active: currentRouteName === 'home' }">
        <span class="active-dot"></span>
        <div class="menu-icon"><Icon icon="solar:home-smile-bold-duotone" class="fs-22px"></Icon></div>
      </router-link>
      <button class="nav-btn" :class="{ active: sidebarOpen }" @click="toggleSidebarMobile">
        <span class="active-dot"></span>
        <svg viewBox="0 0 24 24" class="nav-icon">
          <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/>
        </svg>
      </button>
    </nav>
    <div class="home-indicator"></div>
  </div>

  <Sidebar/>
  <Header/>
  <div class="app-content">
    <router-view/>
    <div class="app-sidebar-mobile-backdrop" @click="closeSidebarMobile"></div>
  </div>
</template>

<script>
import Header from "@/components/Header.vue"
import Sidebar from "@/components/Sidebar.vue"
import CryptoJS from 'crypto-js'

import { App as CapacitorApp } from '@capacitor/app'
import { Capacitor } from '@capacitor/core'

const CLAVE = import.meta.env.VITE_ENCRYPTION_KEY || 'tu-clave-secreta-aqui'

const bgModules = import.meta.glob('@/assets/img/login-bg/*.jpg', {
  query: '?url', import: 'default', eager: true,
})
const bgList = Object.values(bgModules)

export default {
  name: "App",
  components: { Header, Sidebar },

  provide() {
    return {
      setAppBackground: (url) => {
        this.backgroundImage = url
        localStorage.setItem('selectedBackground', url)
      },
      setRandomAppBackground: () => this.setRandomBackground(),
      getAppBgList: () => this.bgList,
    }
  },

  data() {
    return {
      bgList,
      backgroundImage: '',
      currentTheme: 'theme-orange',
      isSidebarOpen: false,
      sidebarOpen: false,
      _appStateHandle: null,
    }
  },

  computed: {
    currentRouteName() {
      if (this.$route.name === 'enc' && this.$route.params.t) {
        try {
          const normalizado = String(this.$route.params.t)
            .replace(/-/g, '+').replace(/_/g, '/').replace(/\./g, '=')
          return CryptoJS.AES.decrypt(normalizado, CLAVE).toString(CryptoJS.enc.Utf8)
        } catch (e) { return '' }
      }
      return this.$route.name || this.$route.path.replace('/', '')
    },
  },

  watch: {
    '$route'() {
      if (this.sidebarOpen) this.closeSidebarMobile()
    },
  },

  async mounted() {
    this.bgList.forEach(src => { const img = new Image(); img.src = src })
    const savedBackground = localStorage.getItem('selectedBackground')
    if (savedBackground && this.bgList.includes(savedBackground)) {
      this.backgroundImage = savedBackground
    } else {
      this.setRandomBackground()
    }
    this.loadSavedTheme()
    this.removeAIWidget()
    this.fixStatusBar()

    try {
      this._appStateHandle = await CapacitorApp.addListener('appStateChange', ({ isActive }) => {
        if (isActive) this.fixStatusBar()
      })
    } catch (e) { /* en web no existe */ }
  },

  beforeUnmount() {
    if (this._appStateHandle && this._appStateHandle.remove) {
      this._appStateHandle.remove()
    }
  },

  methods: {
    async fixStatusBar() {
      if (!Capacitor.isNativePlatform()) return
      try {
        const { StatusBar } = await import('@capacitor/status-bar')
        await StatusBar.setOverlaysWebView({ overlay: false })
        await StatusBar.setBackgroundColor({ color: '#000000' })
      } catch (e) {
        console.warn('[StatusBar] ignorado:', e)
      }
    },

    setRandomBackground() {
      if (!this.bgList || this.bgList.length === 0) return
      const randomIndex = Math.floor(Math.random() * this.bgList.length)
      this.backgroundImage = this.bgList[randomIndex]
      localStorage.setItem('selectedBackground', this.backgroundImage)
    },

    encriptar(texto) {
      const enc = CryptoJS.AES.encrypt(String(texto), CLAVE).toString()
      return enc.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '.')
    },
    linkEncriptado(nombreRuta) {
      return { name: 'enc', params: { t: this.encriptar(nombreRuta) } }
    },

    toggleSidebarMobile() {
      this.sidebarOpen = !this.sidebarOpen
      const appElement = document.querySelector('.app')
      if (appElement) appElement.classList.toggle('app-sidebar-mobile-toggled', this.sidebarOpen)
    },
    closeSidebarMobile() {
      this.sidebarOpen = false
      const appElement = document.querySelector('.app')
      if (appElement) appElement.classList.remove('app-sidebar-mobile-toggled')
    },
    toggleSidebar() { this.isSidebarOpen = !this.isSidebarOpen },

    changeTheme(themeClass) {
      const body = document.body
      const themeClasses = [
        'theme-red','theme-pink','theme-orange','theme-yellow',
        'theme-lime','theme-green','theme-teal','theme-cyan',
        'theme-purple','theme-indigo','theme-gray-600',
      ]
      body.classList.remove(...themeClasses)
      if (themeClass) body.classList.add(themeClass)
      this.currentTheme = themeClass
      localStorage.setItem('selectedTheme', themeClass)
    },
    loadSavedTheme() {
      const savedTheme = localStorage.getItem('selectedTheme')
      if (savedTheme) {
        this.currentTheme = savedTheme
        this.changeTheme(savedTheme)
      } else {
        this.changeTheme('theme-orange')
      }
    },

    removeAIWidget() {
      const selector = [
        '[class*="gemini"]','[class*="sparkle"]','[class*="ai-"]',
        '[class*="ai_"]','[class*="chatbot"]','[class*="assistant"]',
        '[data-ai]','[id*="gemini"]','[id*="sparkle"]','[id*="ai-"]',
      ].join(',')
      const clean = () => {
        document.querySelectorAll(selector).forEach(el => {
          if (el.closest('.navbar-bottom')) return
          el.style.display = 'none'
          el.remove()
        })
      }
      const observer = new MutationObserver(clean)
      observer.observe(document.body, { childList: true, subtree: true })
      setTimeout(clean, 1000)
    },
  },
}
</script>

<style scoped>
.app-bg-cover { position: fixed; inset: 0; background-size: cover; background-position: center; z-index: -2; transition: background-image 0.6s ease; }
.app-bg-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.55); z-index: -1; }

:deep([class*="gemini"]),
:deep([class*="sparkle"]),
:deep([class*="ai-btn"]),
:deep([class*="ai-widget"]),
:deep([class*="chatbot"]),
:deep([class*="assistant"]),
:deep([data-ai]),
:deep([id*="gemini"]),
:deep([id*="sparkle"]),
:deep([id*="ai-"]) {
  display: none !important; visibility: hidden !important; opacity: 0 !important;
  pointer-events: none !important; width: 0 !important; height: 0 !important;
  overflow: hidden !important;
}

.navbar-bottom {
  position: fixed; bottom: 0; left: 0; right: 0; width: 100%;
  background: rgba(0, 0, 0, 0.45) !important;
  border: none !important;
  border-top: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 -8px 30px rgba(0, 0, 0, 0.35) !important;
  z-index: 50; display: flex; flex-direction: column; align-items: center;
  padding-bottom: 8px;
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
}
.bottom-nav { display: flex; align-items: center; justify-content: space-around; width: 100%; max-width: 420px; padding: 10px 8px 6px; }
.nav-btn { position: relative; display: flex; align-items: center; justify-content: center; width: 46px; height: 46px; background: transparent !important; border: none !important; border-radius: 14px; cursor: pointer; padding: 0; outline: none; text-decoration: none; color: inherit; -webkit-tap-highlight-color: transparent; transition: all 0.25s ease; }
.nav-btn:hover  { background: rgba(255,255,255,0.06) !important; }
.nav-btn:active { transform: scale(0.92); }
.nav-icon { width: 24px; height: 24px; fill: rgba(255,255,255,0.45); transition: fill 0.25s ease, transform 0.25s ease; }
.nav-btn.active .nav-icon { fill: #ffffff; transform: translateY(-2px); }
.active-dot { position: absolute; top: 6px; right: 6px; width: 8px; height: 8px; background: #FFD700; border-radius: 50%; box-shadow: 0 0 8px rgba(255,215,0,0.9); opacity: 0; transform: translateY(8px); transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); z-index: 2; }
.nav-btn.active .active-dot { opacity: 1; transform: translateY(0); }
.home-indicator { width: 134px; height: 3px; background: #ffffff; border-radius: 3px; margin-top: 2px; }

@media (max-width: 768px) {
  .bottom-nav { max-width: 380px; }
  .nav-btn { width: 44px; height: 44px; }
  .nav-icon { width: 22px; height: 22px; }
  .active-dot { width: 7px; height: 7px; top: 5px; right: 5px; }
  .home-indicator { width: 120px; }
}
@media (max-width: 480px) {
  .bottom-nav { max-width: 360px; padding: 8px 6px 4px; }
  .nav-btn { width: 42px; height: 42px; }
  .nav-icon { width: 20px; height: 20px; }
  .active-dot { width: 6px; height: 6px; top: 4px; right: 4px; }
  .home-indicator { width: 110px; }
  .navbar-bottom { backdrop-filter: blur(22px) saturate(180%); -webkit-backdrop-filter: blur(22px) saturate(180%); background: rgba(0, 0, 0, 0.5) !important; }
}
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .navbar-bottom { padding-bottom: calc(8px + env(safe-area-inset-bottom)); }
}

.app-sidebar-mobile-backdrop { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.29); z-index: 999; }
.app.app-sidebar-mobile-toggled .app-sidebar-mobile-backdrop { display: block; }
.app .app-sidebar { z-index: 9999 !important; position: fixed; left: 0; top: 0; bottom: 0; width: 260px; transition: transform 0.3s ease; }
@media (min-width: 992px) { .app .app-sidebar { transform: translateX(0) !important; } }
@media (max-width: 991.98px) {
  .app .app-sidebar { transform: translateX(-100%) !important; }
  .app.app-sidebar-mobile-toggled .app-sidebar { transform: translateX(0) !important; }
}
.app-content { margin-left: 260px; transition: margin-left 0.3s ease; padding-bottom: 0px; position: relative; z-index: 1; }
@media (max-width: 991.98px) { .app-content { margin-left: 0; } }

html, body { overflow-x: hidden; scrollbar-width: none; -ms-overflow-style: none; }
html::-webkit-scrollbar, body::-webkit-scrollbar { display: none; width: 0; height: 0; }
* { scrollbar-width: none; -ms-overflow-style: none; }
*::-webkit-scrollbar { display: none; width: 0; height: 0; }
.app-content, .app-sidebar { scrollbar-width: none; -ms-overflow-style: none; }
.app-content::-webkit-scrollbar, .app-sidebar::-webkit-scrollbar { display: none; width: 0; height: 0; }
</style>