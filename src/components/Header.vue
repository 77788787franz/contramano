<template>
  <div
    id="header"
    class="app-header"
    :class="{
      'in-home': isHomeRoute,
      'has-scroll': hasScroll
    }"
  >
    <!-- BEGIN navbar-header -->
    <div class="navbar-header">
      <a href="index.html" class="navbar-brand">
        <span class="navbar-logo"></span>
        <b class="me-1">Contra</b>mano
      </a>
      <div class="desktop-toggler"></div>
    </div>
    <!-- END navbar-header -->

    <!--
    <div class="navbar-nav">
      <div class="navbar-item navbar-form">
        <form action="" method="POST" name="search" @submit.prevent="buscar">
          <div class="form-group">
            <input
              type="text"
              class="form-control"
              placeholder="Buscar..."
              v-model="searchText"
            />
            <button type="submit" class="btn btn-search">
              <i class="fa fa-search"></i>
            </button>
          </div>
        </form>
      </div>

      <div class="navbar-item dropdown">
        <a
          href="#"
          data-bs-toggle="dropdown"
          class="navbar-link dropdown-toggle icon"
        >
          <i class="fa fa-bell"></i>
          <span class="badge" v-if="notificacionesCount > 0">{{
            notificacionesCount
          }}</span>
        </a>
        <div class="dropdown-menu media-list dropdown-menu-end">
          <div class="dropdown-header">
            NOTIFICACIONES ({{ notificacionesCount }})
          </div>
          <div
            v-if="notificaciones.length === 0"
            class="dropdown-item text-center text-muted"
          >
            No hay notificaciones
          </div>
          <a
            v-for="notificacion in notificaciones"
            :key="notificacion.id"
            href="javascript:;"
            class="dropdown-item media"
          >
            <div class="media-body">
              <h6 class="media-heading">{{ notificacion.titulo }}</h6>
              <p class="text-muted mb-0">{{ notificacion.mensaje }}</p>
              <small class="text-muted">{{ notificacion.fecha }}</small>
            </div>
          </a>
        </div>
      </div>

      <div class="navbar-item dropdown">
        <div class="dropdown-menu dropdown-menu-end">
          <div class="dropdown-divider"></div>
          <a class="dropdown-item" href="#" @click="cambiarTema">
            <i class="fa fa-moon me-2"></i> Tema Oscuro
          </a>
        </div>
      </div>

      <div class="navbar-item navbar-user dropdown">
        <a
          href="#"
          class="navbar-link dropdown-toggle d-flex align-items-center"
          data-bs-toggle="dropdown"
          @click="toggleMenuUsuario"
        >
          <div class="user-avatar me-2">
            <img
              v-if="fotoUrl"
              :src="fotoUrl"
              :alt="nombre"
              loading="lazy"
              decoding="async"
              class="avatar-img"
              @error="handleImageError"
            />
            <img
              v-else
              src="/src/assets/bolivia.svg"
              :alt="nombre"
              loading="lazy"
              decoding="async"
            />
          </div>
          <span>
            <span class="d-none d-md-inline user-name"> Bolivia</span>
          </span>
        </a>
        <div
          class="dropdown-menu dropdown-menu-end me-1"
          :class="{ show: menuUsuarioVisible }"
        >
          <div class="dropdown-user-info">
           <div class="dropdown-avatar">
              <img v-if="fotoUrl" 
                   :src="fotoUrl" 
                   :alt="nombre" 
                   @error="fotoUrl = ''; mostrarImagenEstatica = true">
              
              <img v-else-if="mostrarImagenEstatica" 
                   src="/src/assets/img/a2.png" 
                   :alt="nombre" 
                   @error="mostrarImagenEstatica = false">
              
              <div v-else 
                   class="dropdown-avatar-fallback">
                {{ iniciales }}
              </div>
            </div>
            <div class="dropdown-user-details">
              <div class="dropdown-user-name">Solo personal autorizado</div>
              <div class="dropdown-user-email">Archivos encriptados</div>
              <div class="dropdown-user-role">
                <span class="badge" :class="roleClass">{{ roleText }}</span>
                <span v-if="esUsuarioGoogle" class="badge bg-google">
                  <i class="fab fa-google me-1"></i>Google
                </span>
              </div>
            </div>
          </div>
          <div class="dropdown-divider"></div>
          <a class="dropdown-item" href="#" @click="toggleModoOscuro">
            <i class="fa" :class="modoOscuro ? 'fa-sun' : 'fa-moon'"></i>
            {{ modoOscuro ? 'Modo Claro' : 'Modo Oscuro' }}
          </a>
          <div class="dropdown-divider"></div>
          
            <button class="dropdown-item text-danger"  @click="salir"><Icon icon="solar:login-2-bold-duotone" class="fs-18px"></Icon> <label class="ms-2"> Cerrar sesión</label></button>
        </div>
      </div>
    </div>
    -->
    <!-- END header-nav -->
  </div>
</template>

<script>
import AyudanteSesion from '@/services/AyudanteSesion.js'

export default {
  name: 'Header',
  data() {
    return {
      searchText: '',
      id: '',
      nombre: '',
      email: '',
      gmail: '',
      tipo: 0,
      fotoUrl: '',
      esUsuarioGoogle: false,
      menuUsuarioVisible: false,
      notificacionesCount: 0,
      notificaciones: [],
      modoOscuro: localStorage.getItem('modo-oscuro') === 'true',
      bootstrapDropdowns: [],
      mostrarImagenEstatica: false,
      isHomeRoute: false,
      hasScroll: false,
      scrollFrame: null,
      isMobile: false,
    }
  },
  computed: {
    iniciales() {
      if (!this.nombre) return 'U'
      const palabras = this.nombre.split(' ')
      return palabras.length === 1
        ? palabras[0].charAt(0).toUpperCase()
        : (palabras[0].charAt(0) + palabras[palabras.length - 1].charAt(0)).toUpperCase()
    },
    roleText() {
      const tipos = {
        1: 'Postulante',
        2: 'Empresa',
        3: 'Administrativo',
        4: 'Estudiante',
        5: 'Docente',
        6: 'Administrador',
      }
      return tipos[this.tipo] || 'Usuario'
    },
    roleClass() {
      const clases = {
        1: 'bg-primary',
        2: 'bg-success',
        3: 'bg-green',
        4: 'bg-warning',
        5: 'bg-secondary',
        6: 'bg-danger',
      }
      return clases[this.tipo] || 'bg-light text-dark'
    },
  },
  mounted() {
    // Configurar los toggles del menú (versión ligera)
    this.setupMenuTogglers()

    // Inicialización diferida para no bloquear el renderizado
    this.$nextTick(() => {
      this.inicializarDatos()
      this.cargarNotificaciones()
      this.aplicarModoOscuro()
      this.checkIfHome()
      this.checkMobile()
    })

    // Inicializar dropdowns de Bootstrap después del primer idle
    this.scheduleDropdownInit()

    // Listener de scroll optimizado con requestAnimationFrame (passive)
    this.setupScrollListener()

    // Eventos globales
    window.addEventListener('session-changed', this.inicializarDatos)
    document.addEventListener('click', this.cerrarMenuExterno)
    window.addEventListener('resize', this.checkMobile, { passive: true })

    // Observar cambios de ruta
    this.$watch(
      () => this.$route,
      () => {
        this.$nextTick(() => this.checkIfHome())
      }
    )
  },
  beforeDestroy() {
    // Cancelar frame de scroll pendiente
    if (this.scrollFrame) {
      cancelAnimationFrame(this.scrollFrame)
      this.scrollFrame = null
    }

    // Remover listeners
    window.removeEventListener('scroll', this.handleScroll)
    window.removeEventListener('resize', this.checkMobile)
    window.removeEventListener('session-changed', this.inicializarDatos)
    document.removeEventListener('click', this.cerrarMenuExterno)

    // Destruir dropdowns de Bootstrap
    this.cleanupDropdowns()
  },
  methods: {
    /**
     * Configura los botones toggle del menú sin jQuery
     */
    setupMenuTogglers() {
      const togglers = this.$el.querySelectorAll('.menu-toggler')
      togglers.forEach((btn) => {
        btn.addEventListener('click', () => {
          const targetSelector = btn.dataset.toggleTarget
          const toggleClass = btn.dataset.toggleClass
          const dismissClass = btn.dataset.dismissClass

          if (!targetSelector || !toggleClass) return

          const target = document.querySelector(targetSelector)
          if (!target) return

          if (dismissClass && target.classList.contains(dismissClass)) {
            target.classList.remove(dismissClass)
          }
          target.classList.toggle(toggleClass)
        })
      })
    },

    /**
     * Inicializa los dropdowns de Bootstrap de manera eficiente y diferida
     */
    scheduleDropdownInit() {
      // Usar requestIdleCallback si está disponible, sino setTimeout con baja prioridad
      const initFn = () => this.inicializarDropdowns()

      if ('requestIdleCallback' in window) {
        requestIdleCallback(initFn, { timeout: 2000 })
      } else {
        setTimeout(initFn, 200)
      }
    },

    async inicializarDropdowns() {
      // Solo los dropdowns dentro de este componente
      const dropdownToggles = this.$el.querySelectorAll(
        '.dropdown-toggle:not([data-bs-toggle-manual])'
      )
      if (!dropdownToggles.length) return

      try {
        // Import dinámico de Bootstrap
        const { Dropdown } = await import('bootstrap')

        dropdownToggles.forEach((el) => {
          // Verificar si ya tiene una instancia
          const existingInstance = Dropdown.getInstance(el)
          if (existingInstance) {
            this.bootstrapDropdowns.push(existingInstance)
            return
          }
          const dropdown = new Dropdown(el)
          this.bootstrapDropdowns.push(dropdown)
        })
      } catch (error) {
        // Fallback si Bootstrap no está en módulo
        if (window.bootstrap?.Dropdown) {
          dropdownToggles.forEach((el) => {
            const existing = window.bootstrap.Dropdown.getInstance(el)
            if (existing) {
              this.bootstrapDropdowns.push(existing)
              return
            }
            const dropdown = new window.bootstrap.Dropdown(el)
            this.bootstrapDropdowns.push(dropdown)
          })
        }
      }
    },

    cleanupDropdowns() {
      this.bootstrapDropdowns.forEach((dropdown) => {
        if (dropdown?.dispose) {
          dropdown.dispose()
        }
      })
      this.bootstrapDropdowns = []
    },

    /**
     * Scroll listener optimizado con requestAnimationFrame y passive
     */
    setupScrollListener() {
      this.handleScroll = () => {
        if (this.scrollFrame) return // Ya hay un frame pendiente

        this.scrollFrame = requestAnimationFrame(() => {
          const scrollTop =
            window.pageYOffset ||
            document.documentElement.scrollTop ||
            document.body.scrollTop

          const newHasScroll = scrollTop > 10
          if (this.hasScroll !== newHasScroll) {
            this.hasScroll = newHasScroll
          }
          this.scrollFrame = null
        })
      }

      window.addEventListener('scroll', this.handleScroll, { passive: true })
    },

    /**
     * Detecta si la ruta actual es el home (dashboard)
     */
    checkIfHome() {
      const route = this.$route
      this.isHomeRoute = !!(
        route.name === 'home' ||
        route.path === '/' ||
        route.path === '/home' ||
        route.path.includes('inicio') ||
        route.path.includes('dashboard')
      )
    },

    /**
     * Detecta si es dispositivo móvil
     */
    checkMobile() {
      this.isMobile = window.innerWidth <= 768
    },

    /**
     * Carga los datos del usuario desde el helper de sesión
     */
    inicializarDatos() {
      const datosSesion = AyudanteSesion.obtenerDatosSesion()
      this.id = datosSesion.id
      this.nombre = datosSesion.nombre || 'Usuario'
      this.email = datosSesion.email || datosSesion.gmail || ''
      this.gmail = datosSesion.gmail || ''
      this.tipo = datosSesion.tipo || 0

      this.fotoUrl = AyudanteSesion.obtenerFotoUsuario()
      this.esUsuarioGoogle = AyudanteSesion.esUsuarioGoogle()
      this.mostrarImagenEstatica = false
    },

    /**
     * Cierra sesión y redirige al login
     */
    salir() {
      AyudanteSesion.cerrarSesion()
      this.$router.replace('/login997')
    },

    /**
     * Controla el menú de usuario manual (evita conflictos con Bootstrap)
     */
    toggleMenuUsuario(event) {
      event.preventDefault()
      event.stopPropagation()
      this.menuUsuarioVisible = !this.menuUsuarioVisible
    },

    cerrarMenuExterno(event) {
      const dropdown = this.$el.querySelector('.navbar-user.dropdown')
      if (dropdown && !dropdown.contains(event.target)) {
        this.menuUsuarioVisible = false
      }
    },

    handleImageError(event) {
      // En producción podrías loguear solo en desarrollo
      if (process.env.NODE_ENV === 'development') {
        console.warn('Error cargando imagen de perfil')
      }
      this.fotoUrl = ''
      this.mostrarImagenEstatica = true
    },

    buscar() {
      if (this.searchText.trim()) {
        // Implementa tu lógica de búsqueda aquí
        // Por ejemplo: this.$emit('search', this.searchText)
        this.searchText = ''
      }
    },

    /**
     * Carga las notificaciones de forma asíncrona (simulación)
     */
    cargarNotificaciones() {
      // Usar microtask en lugar de setTimeout
      Promise.resolve().then(() => {
        this.notificaciones = [
          {
            id: 1,
            titulo: 'Bienvenido al sistema',
            mensaje: 'Tu cuenta ha sido activada correctamente',
            fecha: 'Hace 5 minutos',
          },
        ]
        this.notificacionesCount = this.notificaciones.length
      })
    },

    toggleModoOscuro(event) {
      if (event) event.preventDefault()
      this.modoOscuro = !this.modoOscuro
      localStorage.setItem('modo-oscuro', this.modoOscuro)
      this.aplicarModoOscuro()
      this.menuUsuarioVisible = false
    },

    aplicarModoOscuro() {
      requestAnimationFrame(() => {
        if (this.modoOscuro) {
          document.body.classList.add('dark-mode')
          document.documentElement.setAttribute('data-bs-theme', 'dark')
        } else {
          document.body.classList.remove('dark-mode')
          document.documentElement.setAttribute('data-bs-theme', 'light')
        }
      })
    },

    cambiarTema(event) {
      event.preventDefault()
      this.toggleModoOscuro(event)
    },
  },
}
</script>

<style scoped>

/* ============================================
   ESTILOS DEL AVATAR DEL USUARIO
   ============================================ */
.user-avatar {
  position: relative;
  width: 30px !important;
  height: 30px !important;
  min-width: 30px !important;
  min-height: 30px !important;
  border-radius: 50%;
  margin-top: 7px;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-img {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  border: 3px solid #fff !important;
  box-shadow: 0 3px 6px rgba(0,0,0,0.2) !important;
}

.avatar-fallback {
  width: 100% !important;
  height: 100% !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-weight: bold !important;
  font-size: 20px !important;
  border: 3px solid #fff !important;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
}

/* ============================================
   DROPDOWN DEL USUARIO
   ============================================ */
.dropdown-user-info {
  display: flex;
  align-items: center;
  padding: 20px !important;
  background: linear-gradient(135deg, #1011123b, #00000063) !important;
  border-radius: 8px 8px 0 0 !important;
}

.dropdown-avatar {
  width: 80px !important;
  height: 80px !important;
  border-radius: 50% !important;
  overflow: hidden !important;
  margin-right: 15px !important;
  border: 4px solid white !important;
  flex-shrink: 0 !important;
}

.dropdown-avatar img {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

.dropdown-avatar-fallback {
  width: 100% !important;
  height: 100% !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-weight: bold !important;
  font-size: 30px !important;
}

.dropdown-user-details {
  flex: 1;
  min-width: 0;
}

.dropdown-user-name {
  font-weight: 600 !important;
  font-size: 14px !important;
  color: #ffffffb3 !important;
  margin-bottom: 4px !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-user-email {
  font-size: 13px !important;
  color: #666 !important;
  margin-bottom: 8px !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-user-role {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dropdown-user-role .badge {
  font-size: 13px !important;
  padding: 6px 12px !important;
}

.bg-google {
  background: #4285f4 !important;
  color: white !important;
}

/* ============================================
   MENÚS DROPDOWN
   ============================================ */
.dropdown-menu {
  min-width: 320px !important;
  box-shadow: 0 6px 20px rgba(0,0,0,0.2) !important;
  border: none !important;
  border-radius: 12px !important;
  padding: 0 !important;
}

.dropdown-menu .dropdown-item {
  padding: 12px 15px !important;
  color: #ffffff !important;
  transition: background-color 0.3s !important;
  font-size: 14px !important;
}

.dropdown-menu .dropdown-item:hover {
  background-color: #00000058 !important;
}

.dropdown-menu .dropdown-item i {
  width: 20px !important;
  margin-right: 8px !important;
  color: #666 !important;
}

.dropdown-menu .dropdown-divider {
  margin: 5px 0 !important;
}

/* ============================================
   MODO OSCURO
   ============================================ */

.dark-mode .navbar-brand {
  color: #fff !important;
}

.dark-mode .navbar-link {
  color: #ccc !important;
}

.dark-mode .dropdown-menu {
  background-color: #2d2d2d !important;
  color: #fff !important;
}

.dark-mode .dropdown-item {
  color: #ccc !important;
}

.dark-mode .dropdown-item:hover {
  background-color: #3d3d3d !important;
}

.dark-mode .dropdown-divider {
  border-color: #444 !important;
}

.dark-mode .dropdown-user-info {
  background: linear-gradient(135deg, #3d3d3d 0%, #2d2d2d 100%) !important;
}

.dark-mode .dropdown-user-name {
  color: #fff !important;
}

.dark-mode .dropdown-user-email {
  color: #aaa !important;
}

.navbar-brand {
  font-size: 1.4rem !important;
}

/* ============================================
   RESPONSIVE - MÓVILES
   ============================================ */
@media (max-width: 768px) {
  #header.app-header {
    height: 56px;
  }
  
  /* Fondo más fuerte en móviles */
  #header.app-header.has-scroll {
    backdrop-filter: blur(8px) !important;
    -webkit-backdrop-filter: blur(8px) !important;
    height: 56px; /* Mantener altura en móviles */
  }
  
  /* Ajustes de tamaño */
  .user-avatar {
    width: 25px !important;
    height: 25px !important;
    min-width: 25px !important;
    min-height: 25px !important;
  }
  
  .navbar-brand {
    font-size: 1.2rem !important;
  }
  
  .dropdown-menu {
    min-width: 280px !important;
  }
  
  /* Suavizar animaciones en móviles */
  #header.app-header {
    transition: background-color 0.25s ease, 
                backdrop-filter 0.25s ease,
                box-shadow 0.25s ease !important;
  }
}

/* ============================================
   MEJORAS DE PERFORMANCE Y DISEÑO
   ============================================ */
/* Hardware acceleration para animaciones suaves */
#header.app-header {
  will-change: background-color, backdrop-filter;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

/* Para evitar problemas de render en algunos navegadores */
#header.app-header * {
  backdrop-filter: blur(10px) !important;
  transform: translateZ(0);
}

/* Mejorar visibilidad en dispositivos táctiles */
@media (hover: none) and (pointer: coarse) {
  #header.app-header {
    -webkit-tap-highlight-color: transparent;
  }
  
  .dropdown-item {
    padding: 15px 20px !important; /* Más grande para touch */
  }
}

/* Asegurar que el contenido no se esconda detrás del header fijo */
body {
  padding-top: 60px !important;
}

/* Ajustar padding en móviles */
@media (max-width: 768px) {
  body {
    padding-top: 56px !important;
  }
}
</style>