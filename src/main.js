import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { Icon } from '@iconify/vue'

// ✅ PRIMERA LÍNEA


import '@/assets/css/vendor.min.css'
import '@/assets/css/transparent/app.min.css'
import '@/assets/toasty/toasty.min.css'
import '@/assets/popup/popupwindow.css'

import '@/assets/toasty/toasty.min.js'
import '@/assets/toasty/main.js'
import '@/assets/toasty/prism.js'
import '@/assets/popup/popupwindow.js'

import('@/assets/js/vendor.min.js').then(() => {}).catch(() => {})

const app = createApp(App)
app.use(router)
app.component('Icon', Icon)
app.mount('#app')
initAutoTranslate({ idioma: 'en' })