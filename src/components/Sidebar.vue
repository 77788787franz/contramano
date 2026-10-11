<template>
   <div class="sidebar-wrapper">
      <div id="sidebar" ref="sidebarRef" class="app-sidebar">
         <div class="app-sidebar-content" data-scrollbar="true" data-height="100%">
            <div class="container">
               <!-- SIDEBAR IZQUIERDO: Ítems por institución CON SCROLL -->
               <div class="sidebar left" style="display: flex; flex-direction: column; height: 100%;">
                  <div style="flex: 1; overflow-y: auto; overflow-x: hidden; padding: 2px 0;">
                     <ul>
                        <!-- 1. ALCALDÍA DE BERMEJO -->
                        <li 
                           :class="{ active: activeContent === 'item1' }" 
                           @click="updateContent('item1')"
                           @mouseenter="showTooltip($event, 'Contramano /', '(Ctrl + 1)')"
                           @mouseleave="hideTooltip">
                           <Icon icon="solar:buildings-2-bold-duotone" class="fs-25px me-2 ms-n1"></Icon>
                        </li>
                     
                        <!-- Puedes agregar más ítems aquí, el scroll aparecerá automáticamente -->
                     </ul>
                  </div>
               </div>
               
               <!-- PANEL PRINCIPAL (CONTENIDO) -->
               <div id="mainContent" style="display: flex; flex-direction: column; height: 100%; overflow: hidden;">
                  <!-- Buscador (fijo) -->
                  <div class="menu" style="flex-shrink: 0;">
                     <div class="menu-search mb-n4">
                        <input type="text" class="form-control" placeholder="Filtrar menú..." data-sidebar-search="true" />
                     </div>
                  </div>
                  
                  <!-- Contenedor scrolleable -->
                  <div style="flex: 1; overflow-y: auto; padding-right: 5px;">
                     
                     <!-- ============================================== -->
                     <!-- 1. CONTENIDO PARA ALCALDÍA DE BERMEJO (item1) -->
                     <!-- ============================================== -->
                     <div id="item1" class="content pt-3" :class="{ active: activeContent === 'item1' }">
                        <div class="menu">

                           <div class="menu-header pt-3">🏛️ Componentes  </div>
                           
                           <!-- Panel de Control -->
                           <div class="menu-item">
                              <router-link :to="linkEncriptado('home')" class="menu-link">
                                 <div class="menu-icon"><Icon icon="solar:chart-2-bold-duotone" class="fs-20px"></Icon></div>
                                 <div class="menu-text">Dashboard General</div>
                              </router-link>
                           </div>
                         



                              <div class="menu-item">
                              <router-link :to="linkEncriptado('pista_voces')" class="menu-link">
                                 <div class="menu-icon"><Icon icon="solar:chart-2-bold-duotone" class="fs-20px"></Icon></div>
                                 <div class="menu-text">Pista y voces</div>
                              </router-link>
                           </div>




                        </div>
                     </div>
                  
                  </div> <!-- Fin contenedor scrolleable -->
               </div> <!-- Fin #mainContent -->
            </div> <!-- Fin .container -->
         </div> <!-- Fin .app-sidebar-content -->
      </div> <!-- Fin #sidebar -->
      
      <!-- Tooltip -->
      <div 
         v-if="tooltipVisible" 
         class="custom-tooltip" 
         :style="{ top: tooltipTop + 'px', left: tooltipLeft + 'px' }">
         <div class="tooltip-title">{{ tooltipTitle }}</div>
         <div class="tooltip-shortcut">{{ tooltipShortcut }}</div>
      </div>
   </div> <!-- Fin .sidebar-wrapper -->
</template>

<script>
import AyudanteSesion from "@/services/AyudanteSesion.js";
import CryptoJS from 'crypto-js';

const CLAVE = import.meta.env.VITE_ENCRYPTION_KEY || 'tu-clave-secreta-aqui';

export default {
  name: "Sidebar",
  inheritAttrs: false,
  props: {
    isSidebarOpen: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close-sidebar'],
  data() {
    return {
      tipoSesion: 0,
      activeLink: "",
      activeContent: "item1",
      tooltipVisible: false,
      tooltipTitle: "",
      tooltipShortcut: "",
      tooltipTop: 0,
      tooltipLeft: 0,
      datosSesionDebug: null
    };
  },
  mounted() {
    const datosSesion = AyudanteSesion.obtenerDatosSesion();
    this.datosSesionDebug = datosSesion;
    
    if (!datosSesion) {
      console.warn('⚠️ No hay datos de sesión');
      return;
    }
    
    console.log('📦 Datos de sesión:', datosSesion);
    console.log('📦 Campo tipo:', datosSesion.tipo, '| Type:', typeof datosSesion.tipo);
    
    try {
      this.tipoSesion = parseInt(datosSesion.tipo, 10) || 0;
      console.log('✅ tipoSesion:', this.tipoSesion);
    } catch (error) {
      console.error('❌ Error:', error);
      this.tipoSesion = 0;
    }
    
    const sidebarWrapper = this.$refs.sidebarRef;
    if (sidebarWrapper) {
      sidebarWrapper.scrollTop = 1;
    }
    
    document.addEventListener('keydown', this.handleKeyboardShortcut);
  },

  beforeUnmount() {
    document.removeEventListener('keydown', this.handleKeyboardShortcut);
  },

  computed: {
    isHomeRoute() {
      return this.$route.path === "/home";
    }
  },

  methods: {
    encriptar(texto) {
      const enc = CryptoJS.AES.encrypt(String(texto), CLAVE).toString();
      return enc.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '.');
    },

    linkEncriptado(nombreRuta) {
      return {
        name: 'enc',
        params: { t: this.encriptar(nombreRuta) }
      };
    },

    updateContent(itemId) {
      this.activeContent = itemId;
      this.hideTooltip();
    },

    isActive(route) {
      return this.$route.path === route;
    },
    
    toggleDivs(link) {
      this.activeLink = link;
    },
    
    goBack() {
      this.showRepDiv = false;
      this.activeLink = null;
    },
    
    salir() {
      AyudanteSesion.cerrarSesion();
    },
    
    navegarYCerrarSidebar(ruta) {
      this.activeLink = ruta;
      const appContainer = document.querySelector('.app');
      if (appContainer) {
        appContainer.classList.remove('app-sidebar-mobile-toggled');
      }
      const backdrop = document.querySelector('.app-sidebar-mobile-backdrop');
      if (backdrop) {
        backdrop.remove();
      }
      this.$emit('close-sidebar');
    },
    
    showTooltip(event, title, shortcut) {
      this.tooltipTitle = title;
      this.tooltipShortcut = shortcut;
      this.tooltipVisible = true;
      
      const rect = event.currentTarget.getBoundingClientRect();
      this.tooltipTop = rect.top + (rect.height / 2) - 20;
      this.tooltipLeft = rect.right + 10;
      
      const tooltipWidth = 170;
      if (this.tooltipLeft + tooltipWidth > window.innerWidth) {
        this.tooltipLeft = window.innerWidth - tooltipWidth - 10;
      }
    },
    
    hideTooltip() {
      this.tooltipVisible = false;
    },

    handleKeyboardShortcut(event) {
      if (this.tipoSesion !== 1 && this.tipoSesion !== 2) return;
      const ctrlKey = event.ctrlKey || event.metaKey;
      const key = event.key;
      if (ctrlKey && key >= '1' && key <= '5') {
        event.preventDefault();
        const itemNumber = parseInt(key);
        this.updateContent(`item${itemNumber}`);
        return;
      }
    }
  },
};
</script>

<style>
/* Todos los estilos se mantienen exactamente como estaban */
.sidebar-wrapper {
   display: contents;
}
.app-sidebar {
   z-index: 9999 !important;
   position: fixed;
   left: 0;
   top: 0;
   bottom: 0;
   width: 260px;
   transition: transform 0.3s ease;
}
.app-sidebar-bg {
   z-index: 9998 !important;
}
.app-sidebar-mobile-backdrop {
   z-index: 9997 !important;
}
.sidebar-open {
   transform: translateX(0);
}
@media (min-width: 992px) {
   .app-sidebar {
      transform: translateX(0) !important;
      z-index: 1000;
   }
   #content {
      margin-left: 250px;
      transition: margin 0.3s ease;
   }
   .navbar-mobile-toggler {
      display: none;
   }
}
#content {
   position: relative;
   z-index: 1;
}
.punto {
   width: 8px;
   height: 3px;
   background-color: rgb(255, 255, 255);
   border-radius: 2%;
   animation: parpadeo 0.5s infinite;
}
@keyframes parpadeo {
   0% { opacity: 1; }
   50% { opacity: 0; }
   100% { opacity: 1; }
}
.menu-item-active .menu-link {
   border-left: 2px solid rgba(255, 255, 255, 0.486);
   color: #ffffff !important;
   border-radius: 1px !important;
   background: #00000000;
   font-weight: 700;
}
.blinking-dot {
   height: 8px;
   width: 8px;
   background-color: #1558b3;
   border-radius: 50%;
   display: inline-block;
   animation: blinking 1s infinite;
}
@keyframes blinking {
   0% { opacity: 1; }
   50% { opacity: 0; }
   100% { opacity: 1; }
}
body {
   margin: 0;
   font-family: Arial, sans-serif;
}
.container {
   display: flex;
   height: 100%;
}
.sidebar {
   padding: 2px;
   box-sizing: border-box;
   margin-left: -3.5em;
   margin-top: 9px;
}
.left {
   border-right: 3px solid rgba(156, 163, 175, 0.2);
   background: rgb(0 0 0 / 20%);
   box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
}
.right {
   border-left: 2px solid #dddddd7c;
}
#mainContent {
   flex-grow: 1;
   background-color: #00000000;
   height: 100%;
}
.content {
   display: none;
   position: relative;
   flex-direction: column;
   height: 100%;
}
.content.active {
   display: flex;
   width: 17em;
}
h1 {
   margin-top: 0;
}
ul {
   list-style: none;
   padding: 0;
}
.menu-list li {
   margin: 18px 0;
   cursor: pointer;
   padding-left: 10px;
   display: flex;
   align-items: center;
   font-size: 13px;
}
.menu-list li.active {
   border-left: 2px solid #1558b3;
   padding-left: 8px;
   background: #ffffff1f;
}
ul li i {
   margin-right: 8px;
}
ul li {
   align-items: center;
   cursor: pointer;
   display: flex;
   font-size: 14px;
   margin: 3px 1px;
   padding: 16px 0 5px 10px;
   position: relative;
}
ul li.active {
   border-left: 2px solid rgba(255, 255, 255, 0.486);
   padding-left: 8px;
   font-weight: 700;
}
#sidebar.app-sidebar.sidebar-home {
   height: 500px;
}
#sidebar.app-sidebar.sidebar-home::after {
   background-color: hsla(0, 0%, 100%, .14);
   border-left-width: medium;
   border-right: hidden;
   bottom: -32.79em;
   content: "";
   height: 65%;
   position: absolute;
   right: 2.3px;
   width: 5.5px;
}
.app-sidebar-content {
   padding-bottom: 0;
   height: 100%;
}
.content-wrapper {
   flex: 1;
   overflow-y: auto;
   padding-bottom: 10px;
}
.barra-progreso-item {
   margin-top: auto;
   padding: 10px 15px;
   background: rgba(0, 0, 0, 0.3);
   border-top: 1px solid rgba(255, 255, 255, 0.2);
   position: sticky;
   bottom: 0;
   width: 100%;
   box-sizing: border-box;
}
.progreso-contenedor {
   width: 100%;
   height: 6px;
   background: transparent;
   border-radius: 3px;
   overflow: hidden;
   display: flex;
}
.progreso-barra {
   height: 100%;
   background: #1e3a8a;
   transition: width 0.3s ease;
}
.progreso-restante {
   height: 100%;
   background: rgba(255, 255, 255, 0.3);
}
.progreso-texto {
   margin-top: 5px;
   font-size: 11px;
   color: white;
   text-align: center;
}
.custom-tooltip {
   position: fixed;
   background-color: #000;
   color: white;
   padding: 10px 12px;
   border-radius: 6px;
   font-size: 12px;
   white-space: nowrap;
   z-index: 10000;
   pointer-events: none;
   box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
   animation: fadeIn 0.2s ease-in;
   display: flex;
   align-items: center;
   gap: 10px;
   min-width: 170px;
}
.custom-tooltip::before {
   content: '';
   position: absolute;
   left: -5px;
   top: 50%;
   transform: translateY(-50%);
   border-width: 5px;
   border-style: solid;
   border-color: transparent #000 transparent transparent;
}
.tooltip-title {
   font-weight: 700;
   flex: 1;
}
.tooltip-shortcut {
   background: rgba(255, 255, 255, 0.2);
   padding: 3px 6px;
   border-radius: 3px;
   font-size: 10px;
   font-weight: 700;
   border: 1px solid rgba(255, 255, 255, 0.3);
   font-family: 'Courier New', monospace;
}
@keyframes fadeIn {
   from { opacity: 0; transform: translateX(-10px); }
   to { opacity: 1; transform: translateX(0); }
}
</style>