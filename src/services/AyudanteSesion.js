const AyudanteSesion = {
  verificarSesion() {
    let sesion = localStorage.getItem('id');
    return sesion ? true : false;
  },

  obtenerDatosSesion() {
    const id = localStorage.getItem('id');
    const usuario = localStorage.getItem('usuario');
    const nombre = localStorage.getItem('nombre');
    const lic_b78PL = localStorage.getItem('lic_b78PL');
    const lic_978XX = localStorage.getItem('lic_978XX');
    const final_hora = localStorage.getItem('final_hora');
    const moneda = localStorage.getItem('moneda');
    const estado = localStorage.getItem('estado');
    const hotel = localStorage.getItem('hotel');
    const direccion = localStorage.getItem('direccion');
    const celular = localStorage.getItem('celular');
    const lib = localStorage.getItem('lib');
    const pais = localStorage.getItem('pais');
    const departamento = localStorage.getItem('departamento');
    const provincia = localStorage.getItem('provincia');
    const periodo = localStorage.getItem('periodo');
    const gmail = localStorage.getItem('gmail');
    // Nuevos campos agregados
    const nro_registro = localStorage.getItem('nro_registro');
    const ci = localStorage.getItem('ci');
    const celular_2 = localStorage.getItem('celular_2');
    const tipo = localStorage.getItem('tipo');
    const sexo = localStorage.getItem('sexo');
    const estado_civil = localStorage.getItem('estado_civil');
    const modalidad_ingreso = localStorage.getItem('modalidad_ingreso');
    const usuario_2 = localStorage.getItem('usuario_2');
    const pass = localStorage.getItem('pass');
    const pass_2 = localStorage.getItem('pass_2');
    const usuario_3 = localStorage.getItem('usuario_3');
    const pass_3 = localStorage.getItem('pass_3');
    const nacido = localStorage.getItem('nacido');
    // NUEVO: Foto de Google almacenada en lic_PP54J
    const foto = localStorage.getItem('foto');
    // NUEVO: Google ID almacenado en lic_A54FH
    const google_id = localStorage.getItem('google_id');
    // NUEVO: Email alternativo
    const email = localStorage.getItem('email');

    return {
      id,
      usuario,
      lic_b78PL,
      lic_978XX,
      final_hora,
      lib,
      nombre,
      moneda,
      estado,
      hotel,
      direccion,
      celular,
      pais,
      departamento,
      provincia,
      periodo,
      gmail,
      pass,
      // Nuevos campos
      nro_registro,
      ci,
      celular_2,
      tipo,
      sexo,
      estado_civil,
      modalidad_ingreso,
      usuario_2,
      pass_2,
      usuario_3,
      pass_3,
      nacido,
      // NUEVO: Campos para Google
      foto,
      google_id,
      email
    };
  },

  establecerSesion(usuario) {
    localStorage.setItem('id', usuario.id);
    localStorage.setItem('usuario', usuario.usuario || '');
    localStorage.setItem('nombre', usuario.nombre || '');
    localStorage.setItem('lic_b78PL', usuario.lic_b78PL || '');
    localStorage.setItem('lic_978XX', usuario.lic_978XX || '');
    localStorage.setItem('final_hora', usuario.final_hora || '');
    localStorage.setItem('moneda', usuario.moneda || '');
    localStorage.setItem('estado', usuario.estado || '');
    localStorage.setItem('hotel', usuario.hotel || '');
    localStorage.setItem('direccion', usuario.direccion || '');
    localStorage.setItem('celular', usuario.celular || '');
    localStorage.setItem('lib', usuario.lib || '');
    localStorage.setItem('gmail', usuario.gmail || '');
    localStorage.setItem('pais', usuario.pais || '');
    localStorage.setItem('departamento', usuario.departamento || '');
    localStorage.setItem('provincia', usuario.provincia || '');
    localStorage.setItem('periodo', usuario.periodo || '');
    localStorage.setItem('pass', usuario.pass || '');
    localStorage.setItem('nro_registro', usuario.nro_registro || '');
    localStorage.setItem('ci', usuario.ci || '');
    localStorage.setItem('celular_2', usuario.celular_2 || '');
    localStorage.setItem('tipo', usuario.tipo || '');
    localStorage.setItem('sexo', usuario.sexo || '');
    localStorage.setItem('estado_civil', usuario.estado_civil || '');
    localStorage.setItem('modalidad_ingreso', usuario.modalidad_ingreso || '');
    localStorage.setItem('usuario_2', usuario.usuario_2 || '');
    localStorage.setItem('pass_2', usuario.pass_2 || '');
    localStorage.setItem('usuario_3', usuario.usuario_3 || '');
    localStorage.setItem('pass_3', usuario.pass_3 || '');
    localStorage.setItem('nacido', usuario.nacido || '');
    
    // NUEVO: Campos para Google
    localStorage.setItem('foto', usuario.foto || usuario.lic_PP54J || '');
    localStorage.setItem('google_id', usuario.google_id || usuario.lic_A54FH || '');
    localStorage.setItem('email', usuario.email || usuario.gmail || '');
    
    // Guardar también el objeto completo para facilitar el acceso
    localStorage.setItem('usuario_completo', JSON.stringify(usuario));
  },

  obtenerDatosNegocio() {
    return {
      hotel: localStorage.getItem('hotel'),
      moneda: localStorage.getItem('moneda'),
      celular_1: localStorage.getItem('celular'),
    };
  },
  
  // ✅ Método para guardar datos de sesión (alias de establecerSesion)
  guardarDatosSesion(usuario) {
    this.establecerSesion(usuario);
  },
  
  // ✅ Obtener la foto del usuario
  obtenerFotoUsuario() {
    // Primero intentar con el campo 'foto'
    let foto = localStorage.getItem('foto');
    
    // Si no hay, intentar con el objeto completo
    if (!foto) {
      const completo = localStorage.getItem('usuario_completo');
      if (completo) {
        const parsed = JSON.parse(completo);
        foto = parsed.foto || parsed.lic_PP54J || '';
      }
    }
    
    return foto;
  },
  
  // ✅ Verificar si el usuario inició con Google
  esUsuarioGoogle() {
    const google_id = localStorage.getItem('google_id');
    const lic_A54FH = localStorage.getItem('lic_A54FH');
    return !!(google_id || lic_A54FH);
  },
  
  // ✅ Obtener iniciales para avatar
  obtenerIniciales() {
    const nombre = localStorage.getItem('nombre') || '';
    if (!nombre) return 'U';
    
    // Tomar la primera letra de cada palabra
    const palabras = nombre.split(' ');
    if (palabras.length === 1) {
      return palabras[0].charAt(0).toUpperCase();
    } else {
      return (palabras[0].charAt(0) + palabras[palabras.length - 1].charAt(0)).toUpperCase();
    }
  },

  cerrarSesion() {
    // Eliminar todos los campos individuales
    localStorage.removeItem('id');
    localStorage.removeItem('usuario');
    localStorage.removeItem('nombre');
    localStorage.removeItem('moneda');
    localStorage.removeItem('estado');
    localStorage.removeItem('hotel');
    localStorage.removeItem('tipo');
    localStorage.removeItem('lib');
    localStorage.removeItem('pais');
    localStorage.removeItem('departamento');
    localStorage.removeItem('provincia');
    localStorage.removeItem('periodo');
    localStorage.removeItem('gmail');
    localStorage.removeItem('pass');
    localStorage.removeItem('nro_registro');
    localStorage.removeItem('ci');
    localStorage.removeItem('celular_2');
    localStorage.removeItem('sexo');
    localStorage.removeItem('estado_civil');
    localStorage.removeItem('modalidad_ingreso');
    localStorage.removeItem('usuario_2');
    localStorage.removeItem('pass_2');
    localStorage.removeItem('usuario_3');
    localStorage.removeItem('pass_3');
    localStorage.removeItem('nacido');
    
    // NUEVO: Eliminar campos de Google
    localStorage.removeItem('foto');
    localStorage.removeItem('google_id');
    localStorage.removeItem('email');
    
    // Eliminar objeto completo
    localStorage.removeItem('usuario_completo');
    
    // 🔔 Notificar al resto de la app que se cerró sesión
    window.dispatchEvent(new CustomEvent('session-logout', { 
      detail: { loggedIn: false } 
    }));
  }
};

export default AyudanteSesion;