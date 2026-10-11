<template>
  <div class="cm-page">

    <!-- ───────── Título + estado ───────── -->
    <section class="cm-card cm-head">
      <div class="cm-logo"></div>
      <div class="cm-title">
        <h1>Contra<span>mano</span></h1>
        <p>Abre y prueba tus apps en automático</p>
      </div>
      <span class="cm-pill" :class="statusClass"><i></i>{{ statusText }}</span>
    </section>

    <!-- Aviso: versión web -->
    <section v-if="!isNative" class="cm-card cm-notice info">
      <Ico n="info" />
      <div>
        <b>Vista previa en el navegador.</b>
        Las apps de abajo son de ejemplo. En el teléfono (APK instalado) aparecen
        las apps reales que tienes instaladas.
      </div>
    </section>

    <!-- Aviso: falta activar accesibilidad -->
    <section v-if="isNative && !serviceEnabled" class="cm-card cm-notice warn">
      <Ico n="shield" />
      <div>
        <b>Falta activar el servicio de accesibilidad.</b>
        Ajustes → Accesibilidad → Apps instaladas → <b>Contramano</b> → Activar.
        <button class="cm-link" @click="openSettings">Abrir ajustes ahora</button>
      </div>
    </section>

    <!-- ───────── Resumen de la cola ───────── -->
    <section class="cm-card cm-summary">
      <div class="cm-tile-ic purple"><Ico n="apps" /></div>
      <div class="cm-summary-txt">
        <b>Cola de pruebas</b>
        <small>{{ selected.length }} apps · {{ minLabel }} c/u · {{ fmt(totalSec) }} total</small>
      </div>
      <div class="cm-steps">
        <button class="cm-sq" :disabled="running || minutes <= 0.5" @click="stepMinutes(-0.5)"><Ico n="minus" /></button>
        <button class="cm-sq" :disabled="running || minutes >= 30" @click="stepMinutes(0.5)"><Ico n="plus" /></button>
      </div>
    </section>

    <!-- ───────── Línea de tiempo (onda) ───────── -->
    <section class="cm-card cm-wave-card">
      <div class="cm-ruler">
        <span v-for="(t, i) in ruler" :key="i">{{ t }}</span>
      </div>
      <div class="cm-wave">
        <i v-for="(h, i) in bars" :key="i"
           :class="{ done: running && i / bars.length < progress }"
           :style="{ height: h + '%' }"></i>
      </div>
    </section>

    <!-- ───────── Transporte ───────── -->
    <section class="cm-card cm-transport">
      <button v-if="!running" class="cm-play" :disabled="!selected.length" @click="start"><Ico n="play" /></button>
      <button v-else class="cm-play stop" @click="stop"><Ico n="stop" /></button>

      <div class="cm-now">
        <b class="cm-time">{{ fmt(elapsedTotal) }} <small>/ {{ fmt(running ? status.total * runDuration : totalSec) }}</small></b>
        <small class="cm-now-app">
          <template v-if="running">App {{ status.index + 1 }}/{{ status.total }} · {{ currentName }}</template>
          <template v-else>Listo para iniciar</template>
        </small>
      </div>

      <button class="cm-sq" :class="{ on: loop }" :disabled="running" @click="loop = !loop" title="Repetir la lista">
        <Ico n="repeat" />
      </button>
    </section>

    <!-- ───────── Opciones (chips) ───────── -->
    <section class="cm-card cm-opts">
      <div class="cm-row">
        <label>MODO</label>
        <div class="cm-chips">
          <button class="cm-chip" :class="{ on: mode === 'random' }" :disabled="running" @click="mode = 'random'">Aleatorio</button>
          <button class="cm-chip" :class="{ on: mode === 'sequential' }" :disabled="running" @click="mode = 'sequential'">Secuencial</button>
          <button class="cm-chip" :class="{ on: mode === 'texts' }" :disabled="running" @click="mode = 'texts'">Por textos</button>
        </div>
      </div>

      <div v-if="mode === 'texts'" class="cm-row">
        <label>BOTONES</label>
        <input class="cm-input" type="text" v-model="textsInput" :disabled="running"
               placeholder="Siguiente, Continuar, Entrar…" />
      </div>

      <div class="cm-row">
        <label>TIEMPO</label>
        <div class="cm-chips">
          <button v-for="m in [1, 2, 3, 5]" :key="m" class="cm-chip" :class="{ on: minutes === m }"
                  :disabled="running" @click="minutes = m">{{ m }} min</button>
        </div>
      </div>

      <div class="cm-row">
        <label>CLICS</label>
        <div class="cm-chips">
          <button class="cm-chip" :class="{ on: intervalMs === 600 }" :disabled="running" @click="intervalMs = 600">Rápido</button>
          <button class="cm-chip" :class="{ on: intervalMs === 1200 }" :disabled="running" @click="intervalMs = 1200">Normal</button>
          <button class="cm-chip" :class="{ on: intervalMs === 2000 }" :disabled="running" @click="intervalMs = 2000">Lento</button>
        </div>
      </div>

      <div class="cm-row">
        <label>AL TERMINAR</label>
        <div class="cm-chips">
          <button class="cm-chip" :class="{ on: forceStop }" :disabled="running" @click="forceStop = !forceStop">Cerrar la app</button>
          <button class="cm-chip" :class="{ on: loop }" :disabled="running" @click="loop = !loop">Repetir lista</button>
        </div>
      </div>

      <div class="cm-cta-row">
        <button v-if="!running" class="cm-cta" :disabled="!selected.length" @click="start">
          <Ico n="play" /> Iniciar prueba ({{ selected.length }})
        </button>
        <button v-else class="cm-cta stop" @click="stop">
          <Ico n="stop" /> Detener
        </button>
        <button class="cm-sq danger" :disabled="!running" @click="stop" title="Detener"><Ico n="stop" /></button>
      </div>
    </section>

    <!-- ───────── Atajos (tiles) ───────── -->
    <section class="cm-tiles">
      <button class="cm-tile" :disabled="running" @click="selectAllFiltered">
        <span class="cm-tile-ic purple"><Ico n="selectall" /></span>
        <b>Marcar todas</b>
      </button>
      <button class="cm-tile" :disabled="running" @click="selected = []">
        <span class="cm-tile-ic red"><Ico n="trash" /></span>
        <b>Quitar todas</b>
      </button>
      <button class="cm-tile" :disabled="running" @click="loadApps">
        <span class="cm-tile-ic teal"><Ico n="refresh" /></span>
        <b>Recargar</b>
      </button>
    </section>

    <!-- ───────── Orden de la cola ───────── -->
    <section v-if="selectedApps.length > 1" class="cm-card">
      <h2>Orden de apertura</h2>
      <div v-for="(a, i) in selectedApps" :key="a.packageName" class="cm-q-item"
           :class="{ now: running && status.index === i }">
        <span class="cm-num">{{ i + 1 }}</span>
        <span class="cm-q-ic">
          <img v-if="a.icon" :src="a.icon" alt="" />
          <b v-else :style="{ background: colorFor(a) }">{{ a.name.charAt(0) }}</b>
        </span>
        <span class="cm-q-name">{{ a.name }}</span>
        <button class="cm-mini" :disabled="running || i === 0" @click="move(i, -1)"><Ico n="up" /></button>
        <button class="cm-mini" :disabled="running || i === selectedApps.length - 1" @click="move(i, 1)"><Ico n="down" /></button>
        <button class="cm-mini" :disabled="running" @click="toggle(a.packageName)"><Ico n="close" /></button>
      </div>
    </section>

    <!-- ───────── Apps del teléfono (selección) ───────── -->
    <section class="cm-card">
      <div class="cm-apps-head">
        <h2>Elige las apps a probar</h2>
        <span class="cm-count">{{ selected.length }}/{{ apps.length }}</span>
      </div>
      <div class="cm-search">
        <Ico n="search" />
        <input type="search" v-model="search" placeholder="Buscar app…" />
      </div>

      <p v-if="loading" class="cm-hint">Cargando apps…</p>
      <p v-else-if="!filteredApps.length" class="cm-hint">No hay apps para mostrar.</p>

      <div class="cm-grid">
        <button v-for="a in filteredApps" :key="a.packageName" class="cm-app"
                :class="{ on: isSelected(a.packageName) }" :disabled="running"
                @click="toggle(a.packageName)">
          <span class="cm-app-ic">
            <img v-if="a.icon" :src="a.icon" alt="" />
            <b v-else :style="{ background: colorFor(a) }">{{ a.name.charAt(0) }}</b>
            <em v-if="isSelected(a.packageName)">{{ orderOf(a.packageName) }}</em>
          </span>
          <span class="cm-app-name">{{ a.name }}</span>
        </button>
      </div>
    </section>

    <!-- ───────── Registro ───────── -->
    <section v-if="logs.length" class="cm-card">
      <h2>Registro</h2>
      <div class="cm-log">
        <div v-for="(l, i) in logs" :key="i">{{ l }}</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, h } from 'vue'
import { Capacitor } from '@capacitor/core'
import Contramano from '../services/contramano'

/* ── Iconos SVG propios (funcionan sin internet) ── */
const ICONS = {
  play: 'M8 5v14l11-7z',
  stop: 'M6 6h12v12H6z',
  search: 'M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
  close: 'M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
  up: 'M7.41 15.41 12 10.83l4.59 4.58L18 14l-6-6-6 6z',
  down: 'M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z',
  refresh: 'M17.65 6.35A7.958 7.958 0 0 0 12 4a8 8 0 1 0 7.73 10h-2.08A5.99 5.99 0 0 1 12 18a6 6 0 1 1 0-12c1.66 0 3.14.69 4.22 1.78L13 11h7V4z',
  repeat: 'M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z',
  minus: 'M19 13H5v-2h14v2z',
  plus: 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z',
  apps: 'M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z',
  selectall: 'M3 5h2V3c-1.1 0-2 .9-2 2zm0 8h2v-2H3v2zm4 8h2v-2H7v2zM3 9h2V7H3v2zm10-6h-2v2h2V3zm6 0v2h2c0-1.1-.9-2-2-2zM5 21v-2H3c0 1.1.9 2 2 2zm-2-4h2v-2H3v2zM9 3H7v2h2V3zm2 18h2v-2h-2v2zm8-8h2v-2h-2v2zm0 8c1.1 0 2-.9 2-2h-2v2zm0-12h2V7h-2v2zm0 8h2v-2h-2v2zm-4 4h2v-2h-2v2zm0-16h2V3h-2v2zM7 17h10V7H7v10zm2-8h6v6H9V9z',
  trash: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
  shield: 'M12 2 4 5v6c0 5.55 3.84 10.74 8 12 4.16-1.26 8-6.45 8-12V5l-8-3zm-2 15-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z',
  info: 'M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z',
}
const Ico = (props) =>
  h('svg', { viewBox: '0 0 24 24', class: 'cm-ico', fill: 'currentColor' }, [h('path', { d: ICONS[props.n] || '' })])
Ico.props = ['n']

/* ── Estado ── */
const isNative = Capacitor.isNativePlatform()

function load(key, def) {
  try {
    const v = localStorage.getItem(key)
    return v === null ? def : JSON.parse(v)
  } catch (e) {
    return def
  }
}

const DEMO_APPS = ['WhatsApp', 'YouTube', 'Chrome', 'Facebook', 'Instagram', 'TikTok', 'Maps',
  'Gmail', 'Telegram', 'Spotify', 'Netflix', 'Mercado Libre', 'Uber', 'Drive', 'Fotos', 'Banco']
  .map(n => ({ name: n, packageName: 'demo.' + n.toLowerCase().replace(/\s/g, ''), icon: '' }))

const apps = ref([])
const loading = ref(false)
const search = ref('')
const selected = ref(load('contramano.selected', []))
const minutes = ref(load('contramano.minutes', 2))
const intervalMs = ref(load('contramano.intervalMs', 1200))
const mode = ref(load('contramano.mode', 'random'))
const textsInput = ref(load('contramano.texts', ''))
const loop = ref(load('contramano.loop', false))
const forceStop = ref(load('contramano.forceStop', true))

const serviceEnabled = ref(false)
const running = ref(false)
const status = ref({ index: 0, total: 0, cycle: 1, packageName: '' })
const logs = ref([])
const startedAt = ref(0)
const runDuration = ref(120)
const now = ref(Date.now())
let listenerHandle = null
let ticker = null

watch([selected, minutes, intervalMs, mode, textsInput, loop, forceStop], () => {
  try {
    localStorage.setItem('contramano.selected', JSON.stringify(selected.value))
    localStorage.setItem('contramano.minutes', JSON.stringify(minutes.value))
    localStorage.setItem('contramano.intervalMs', JSON.stringify(intervalMs.value))
    localStorage.setItem('contramano.mode', JSON.stringify(mode.value))
    localStorage.setItem('contramano.texts', JSON.stringify(textsInput.value))
    localStorage.setItem('contramano.loop', JSON.stringify(loop.value))
    localStorage.setItem('contramano.forceStop', JSON.stringify(forceStop.value))
  } catch (e) { /* ignore */ }
}, { deep: true })

/* ── Cálculos ── */
const filteredApps = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return apps.value
  return apps.value.filter(a =>
    a.name.toLowerCase().includes(q) || a.packageName.toLowerCase().includes(q))
})

const selectedApps = computed(() =>
  selected.value.map(pkg => apps.value.find(a => a.packageName === pkg)).filter(Boolean))

const durationSec = computed(() => Math.round(Number(minutes.value || 2) * 60))
const totalSec = computed(() => selected.value.length * durationSec.value)
const minLabel = computed(() => {
  const m = Number(minutes.value || 0)
  return (Number.isInteger(m) ? m : m.toFixed(1)) + ' min'
})

const currentElapsed = computed(() =>
  startedAt.value ? Math.min(runDuration.value, Math.max(0, (now.value - startedAt.value) / 1000)) : 0)
const elapsedTotal = computed(() =>
  running.value ? status.value.index * runDuration.value + currentElapsed.value : 0)
const progress = computed(() => {
  const total = running.value ? status.value.total * runDuration.value : totalSec.value
  return total ? Math.min(1, elapsedTotal.value / total) : 0
})

const ruler = computed(() => {
  const total = running.value ? status.value.total * runDuration.value : totalSec.value
  return [0, 0.25, 0.5, 0.75, 1].map(f => fmt(Math.round(total * f)))
})

// Onda decorativa (siempre igual)
const bars = Array.from({ length: 56 }, (_, i) =>
  Math.round(22 + 68 * Math.abs(Math.sin(i * 0.63) * Math.cos(i * 0.19 + 1)) + 8 * Math.abs(Math.sin(i * 2.3))))

const currentName = computed(() => {
  const a = apps.value.find(x => x.packageName === status.value.packageName)
  return a ? a.name : status.value.packageName
})

const statusText = computed(() => {
  if (running.value) return 'Probando'
  if (!isNative) return 'Vista web'
  return serviceEnabled.value ? 'Servicio activo' : 'Servicio apagado'
})
const statusClass = computed(() => {
  if (running.value) return 'run'
  if (!isNative) return 'web'
  return serviceEnabled.value ? 'ok' : 'off'
})

/* ── Utilidades ── */
function fmt(sec) {
  const s = Math.max(0, Math.round(sec))
  return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0')
}

function colorFor(a) {
  let hsh = 0
  for (const ch of a.packageName) hsh = (hsh * 31 + ch.charCodeAt(0)) % 360
  return `hsl(${hsh}, 62%, 46%)`
}

function isSelected(pkg) { return selected.value.includes(pkg) }
function orderOf(pkg) { return selected.value.indexOf(pkg) + 1 }

function toggle(pkg) {
  const i = selected.value.indexOf(pkg)
  if (i >= 0) selected.value.splice(i, 1)
  else selected.value.push(pkg)
}

function move(i, dir) {
  const j = i + dir
  if (j < 0 || j >= selected.value.length) return
  const arr = selected.value
  ;[arr[i], arr[j]] = [arr[j], arr[i]]
}

function selectAllFiltered() {
  for (const a of filteredApps.value) {
    if (!selected.value.includes(a.packageName)) selected.value.push(a.packageName)
  }
}

function stepMinutes(d) {
  const v = Math.round((Number(minutes.value || 2) + d) * 10) / 10
  minutes.value = Math.min(30, Math.max(0.5, v))
}

function addLog(msg) {
  const t = new Date().toLocaleTimeString()
  logs.value.unshift(`[${t}] ${msg}`)
  if (logs.value.length > 40) logs.value.length = 40
}

/* ── Acciones con el plugin nativo ── */
async function loadApps() {
  loading.value = true
  try {
    if (!isNative) {
      apps.value = DEMO_APPS
    } else {
      const res = await Contramano.getInstalledApps()
      apps.value = res.apps || []
    }
    selected.value = selected.value.filter(p => apps.value.some(a => a.packageName === p))
  } catch (e) {
    addLog('No se pudo cargar la lista de apps: ' + (e && e.message ? e.message : e))
  } finally {
    loading.value = false
  }
}

async function refreshStatus() {
  if (!isNative) return
  try {
    const s = await Contramano.getStatus()
    serviceEnabled.value = !!s.serviceEnabled
    running.value = !!s.running
    if (s.running) {
      status.value = {
        index: s.index || 0, total: s.total || 0, cycle: s.cycle || 1, packageName: s.packageName || ''
      }
    }
  } catch (e) { /* sin plugin */ }
}

async function openSettings() {
  try { await Contramano.openAccessibilitySettings() } catch (e) { /* ignore */ }
}

async function start() {
  if (!isNative) {
    alert('Esto solo funciona en el teléfono.\nInstala el APK y ábrelo allí para probar las apps reales.')
    return
  }
  if (!serviceEnabled.value) {
    alert('Primero activa el servicio de accesibilidad de Contramano.')
    return openSettings()
  }
  const texts = textsInput.value.split(',').map(t => t.trim()).filter(Boolean)
  try {
    runDuration.value = durationSec.value
    await Contramano.startSession({
      packages: [...selected.value],
      durationSec: durationSec.value,
      intervalMs: Number(intervalMs.value),
      mode: mode.value,
      texts,
      loop: loop.value,
      forceStop: forceStop.value
    })
    running.value = true
    logs.value = []
  } catch (e) {
    const msg = e && e.message ? e.message : String(e)
    if (msg.includes('SERVICE_DISABLED')) {
      serviceEnabled.value = false
      alert('El servicio de accesibilidad no está activo.')
    } else {
      alert('No se pudo iniciar: ' + msg)
    }
  }
}

async function stop() {
  try { await Contramano.stopSession() } catch (e) { /* ignore */ }
  running.value = false
  startedAt.value = 0
}

function onEvent(e) {
  switch (e.type) {
    case 'serviceConnected':
      serviceEnabled.value = true
      break
    case 'started':
      running.value = true
      addLog('Sesión iniciada')
      break
    case 'appStarted':
      status.value = { index: e.index, total: e.total, cycle: e.cycle, packageName: e.packageName }
      startedAt.value = Date.now()
      addLog(`Abriendo ${e.packageName}`)
      break
    case 'appFinished':
      addLog(`Cerrando ${e.packageName}`)
      break
    case 'log':
      addLog(e.message)
      break
    case 'finished':
      running.value = false
      startedAt.value = 0
      addLog('Cola terminada ✔')
      break
    case 'stopped':
      running.value = false
      startedAt.value = 0
      addLog('Detenido')
      break
  }
}

function onVisible() {
  if (document.visibilityState === 'visible') refreshStatus()
}

onMounted(async () => {
  ticker = setInterval(() => { now.value = Date.now() }, 500)
  if (isNative) {
    try { listenerHandle = await Contramano.addListener('sessionEvent', onEvent) } catch (e) { /* ignore */ }
  }
  document.addEventListener('visibilitychange', onVisible)
  await refreshStatus()
  await loadApps()
})

onBeforeUnmount(() => {
  if (ticker) clearInterval(ticker)
  document.removeEventListener('visibilitychange', onVisible)
  if (listenerHandle && listenerHandle.remove) listenerHandle.remove()
})
</script>

<style scoped>
.cm-page {
  --purple: #8b5cf6;
  --purple-d: #6d28d9;
  --cyan: #22d3ee;
  --red: #ef4444;
  --teal: #14b8a6;
  --line: rgba(255, 255, 255, 0.09);
  max-width: 520px; margin: 0 auto;
  padding: 10px 12px 130px;
  color: #eef0f7; font-family: system-ui, -apple-system, 'Segoe UI', sans-serif;
}
.cm-ico { width: 20px; height: 20px; flex: none; }

/* tarjeta de cristal */
.cm-card {
  background: rgba(18, 20, 32, 0.62);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 12px;
  margin-bottom: 12px;
  backdrop-filter: blur(14px) saturate(150%);
  -webkit-backdrop-filter: blur(14px) saturate(150%);
}
.cm-card h2 { font-size: 13px; letter-spacing: .04em; margin: 0 0 10px; color: #b9bfd6; font-weight: 600; }
.cm-hint { font-size: 13px; color: #8b93ab; margin: 6px 0; }

/* cabecera */
.cm-head { display: flex; align-items: center; gap: 12px; }
.cm-logo { width: 42px; height: 42px; border-radius: 50%; flex: none; position: relative;
  border: 4px solid #f97316; box-shadow: 0 0 14px rgba(249, 115, 22, .45); }
.cm-logo::after { content: ''; position: absolute; inset: 7px; border-radius: 50%; background: #f97316; }
.cm-title { flex: 1; min-width: 0; }
.cm-title h1 { margin: 0; font-size: 22px; font-weight: 800; color: #fff; line-height: 1.1; }
.cm-title h1 span { font-weight: 300; }
.cm-title p { margin: 2px 0 0; font-size: 12px; color: #8b93ab; }
.cm-pill { display: inline-flex; align-items: center; gap: 6px; font-size: 11.5px; padding: 5px 9px;
  border-radius: 999px; background: rgba(255,255,255,.07); border: 1px solid var(--line); white-space: nowrap; }
.cm-pill i { width: 8px; height: 8px; border-radius: 50%; background: #6b7280; }
.cm-pill.ok i { background: #22c55e; box-shadow: 0 0 8px #22c55e; }
.cm-pill.run i { background: var(--purple); box-shadow: 0 0 8px var(--purple); animation: cm-pulse 1s infinite; }
.cm-pill.off i { background: var(--red); }
.cm-pill.web i { background: var(--cyan); }
@keyframes cm-pulse { 50% { opacity: .35; } }

/* avisos */
.cm-notice { display: flex; gap: 10px; font-size: 13px; line-height: 1.4; color: #d7dbea; }
.cm-notice .cm-ico { margin-top: 1px; }
.cm-notice.info { border-color: rgba(34, 211, 238, .35); }
.cm-notice.info .cm-ico { color: var(--cyan); }
.cm-notice.warn { border-color: rgba(249, 115, 22, .5); }
.cm-notice.warn .cm-ico { color: #f97316; }
.cm-link { display: block; margin-top: 8px; background: rgba(249,115,22,.18); color: #fdba74;
  border: 1px solid rgba(249,115,22,.5); border-radius: 10px; padding: 8px 12px; font-weight: 600; }

/* iconos con fondo (tiles) */
.cm-tile-ic { width: 44px; height: 44px; border-radius: 13px; display: inline-flex; align-items: center;
  justify-content: center; flex: none; color: #fff; }
.cm-tile-ic .cm-ico { width: 24px; height: 24px; }
.cm-tile-ic.purple { background: linear-gradient(135deg, #6d28d9, #8b5cf6); }
.cm-tile-ic.red { background: linear-gradient(135deg, #b91c1c, #ef4444); }
.cm-tile-ic.teal { background: linear-gradient(135deg, #0f766e, #14b8a6); }

/* resumen */
.cm-summary { display: flex; align-items: center; gap: 12px; }
.cm-summary-txt { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.cm-summary-txt b { font-size: 15px; }
.cm-summary-txt small { font-size: 12px; color: #8b93ab; }
.cm-steps { display: flex; gap: 6px; }

/* botones cuadrados */
.cm-sq { width: 40px; height: 40px; border-radius: 11px; border: 1px solid var(--line);
  background: rgba(255,255,255,.07); color: #fff; display: inline-flex; align-items: center; justify-content: center; flex: none; }
.cm-sq.on { background: rgba(139,92,246,.35); border-color: var(--purple); color: #ddd6fe; }
.cm-sq.danger { background: rgba(239,68,68,.15); border-color: rgba(239,68,68,.65); color: #fca5a5; }
button:disabled { opacity: .4; }
button { font-family: inherit; cursor: pointer; -webkit-tap-highlight-color: transparent; }

/* onda */
.cm-wave-card { padding: 10px 12px 12px; }
.cm-ruler { display: flex; justify-content: space-between; font-size: 11px; color: #aab1c9; margin-bottom: 6px;
  font-variant-numeric: tabular-nums; }
.cm-wave { height: 96px; display: flex; align-items: center; gap: 2px; }
.cm-wave i { flex: 1; min-width: 2px; border-radius: 3px; background: linear-gradient(180deg, #67e8f9, #06b6d4);
  transition: background .3s; }
.cm-wave i.done { background: linear-gradient(180deg, #c4b5fd, #8b5cf6); }

/* transporte */
.cm-transport { display: flex; align-items: center; gap: 12px; }
.cm-play { width: 54px; height: 54px; border-radius: 15px; flex: none; color: #fff;
  background: rgba(139,92,246,.22); border: 2px solid var(--purple);
  display: inline-flex; align-items: center; justify-content: center; }
.cm-play .cm-ico { width: 28px; height: 28px; }
.cm-play.stop { background: rgba(239,68,68,.2); border-color: var(--red); }
.cm-now { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.cm-time { font-family: ui-monospace, Consolas, monospace; font-size: 18px; }
.cm-time small { color: #7f87a3; font-size: 13px; }
.cm-now-app { font-size: 12px; color: #9aa2bd; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* opciones */
.cm-opts { padding: 12px; }
.cm-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.cm-row > label { width: 78px; flex: none; font-size: 11.5px; font-weight: 700; color: #aab1c9; letter-spacing: .03em; }
.cm-chips { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; }
.cm-chip { padding: 7px 13px; border-radius: 999px; font-size: 13px; color: #e6e8f2;
  background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.14); }
.cm-chip.on { background: rgba(139,92,246,.35); border-color: var(--purple); color: #ddd6fe; font-weight: 600; }
.cm-input { flex: 1; min-width: 0; background: rgba(0,0,0,.35); color: #fff; border: 1px solid rgba(255,255,255,.14);
  border-radius: 10px; padding: 9px 11px; font-size: 14px; }

.cm-cta-row { display: flex; gap: 8px; margin-top: 4px; }
.cm-cta { flex: 1; height: 50px; border-radius: 13px; color: #fff; font-size: 16px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  background: linear-gradient(135deg, #5b21b6, #7c3aed); border: 1px solid var(--purple); }
.cm-cta.stop { background: linear-gradient(135deg, #991b1b, #dc2626); border-color: var(--red); }
.cm-cta-row .cm-sq { width: 50px; height: 50px; border-radius: 13px; }

/* tiles de atajos */
.cm-tiles { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 12px; }
.cm-tile { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 14px 6px;
  border-radius: 18px; color: #fff; background: rgba(18, 20, 32, 0.62); border: 1px solid var(--line);
  backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px); }
.cm-tile b { font-size: 13px; font-weight: 600; }

/* cola */
.cm-q-item { display: flex; align-items: center; gap: 8px; padding: 6px 4px; border-radius: 12px; }
.cm-q-item.now { background: rgba(139,92,246,.2); }
.cm-num { width: 20px; text-align: center; font-size: 12px; color: #7f87a3; }
.cm-q-ic { width: 32px; height: 32px; flex: none; }
.cm-q-ic img, .cm-q-ic b { width: 32px; height: 32px; border-radius: 9px; display: flex; align-items: center;
  justify-content: center; font-size: 14px; color: #fff; }
.cm-q-name { flex: 1; min-width: 0; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cm-mini { width: 32px; height: 32px; border-radius: 9px; border: 1px solid var(--line);
  background: rgba(255,255,255,.07); color: #fff; display: inline-flex; align-items: center; justify-content: center; }
.cm-mini .cm-ico { width: 18px; height: 18px; }

/* apps */
.cm-apps-head { display: flex; align-items: center; justify-content: space-between; }
.cm-apps-head h2 { margin-bottom: 10px; }
.cm-count { font-size: 12px; padding: 3px 9px; border-radius: 999px; background: rgba(139,92,246,.3);
  border: 1px solid var(--purple); color: #ddd6fe; margin-bottom: 10px; }
.cm-search { display: flex; align-items: center; gap: 8px; padding: 0 12px; margin-bottom: 12px; color: #8b93ab;
  background: rgba(0,0,0,.35); border: 1px solid rgba(255,255,255,.12); border-radius: 999px; }
.cm-search input { flex: 1; min-width: 0; background: transparent; border: 0; outline: 0; color: #fff;
  font-size: 14px; padding: 10px 0; }
.cm-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px 6px; }
.cm-app { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 8px 2px;
  border-radius: 14px; color: #e6e8f2; background: transparent; border: 1px solid transparent; }
.cm-app.on { background: rgba(139,92,246,.2); border-color: var(--purple); box-shadow: 0 0 14px rgba(139,92,246,.35); }
.cm-app-ic { position: relative; width: 48px; height: 48px; }
.cm-app-ic img, .cm-app-ic b { width: 48px; height: 48px; border-radius: 13px; display: flex; align-items: center;
  justify-content: center; font-size: 20px; color: #fff; }
.cm-app-ic em { position: absolute; top: -6px; right: -8px; min-width: 20px; height: 20px; padding: 0 4px;
  border-radius: 10px; background: var(--purple); color: #fff; font-size: 11px; font-style: normal; font-weight: 700;
  display: flex; align-items: center; justify-content: center; border: 2px solid #12141f; box-sizing: border-box; }
.cm-app-name { font-size: 11.5px; line-height: 1.2; width: 100%; text-align: center; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }

/* registro */
.cm-log { font-family: ui-monospace, Consolas, monospace; font-size: 11.5px; max-height: 170px; overflow: auto; color: #aab2c8; }

@media (max-width: 360px) {
  .cm-grid { grid-template-columns: repeat(3, 1fr); }
  .cm-row > label { width: 64px; }
}
</style>