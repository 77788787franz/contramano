import { createRouter, createWebHashHistory } from 'vue-router';
import { h, defineAsyncComponent } from 'vue';
import CryptoJS from 'crypto-js';

const CLAVE = import.meta.env.VITE_ENCRYPTION_KEY || 'tu-clave-secreta-aqui';

const decrypt = (txt) => {
  if (!txt) return '';
  try {
    const t = txt.replace(/-/g, '+').replace(/_/g, '/').replace(/\./g, '=');
    return CryptoJS.AES.decrypt(t, CLAVE).toString(CryptoJS.enc.Utf8);
  } catch (e) {
    return '';
  }
};

const encriptar = (texto) => {
  const enc = CryptoJS.AES.encrypt(String(texto), CLAVE).toString();
  return enc.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '.');
};

// 👇 Agrega aquí todas tus vistas
const mapa = {
  home: defineAsyncComponent(() => import('@/views/Home.vue')),
 
};

const Home = mapa.home;

const routes = [
  { path: '/', redirect: () => `/app/${encriptar('home')}` },
  {
    path: '/app/:t',
    name: 'enc',
    component: {
      render() {
        const token = this.$route.params.t;
        const clave = decrypt(token) || 'home';
        const comp = mapa[clave] || Home; 
        return h(comp);
      }
    }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
});

export function linkEncriptado(clave) {
  return `/app/${encriptar(clave)}`;
}

export default router;