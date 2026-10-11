const RUTA_GLOBAL = import.meta.env.VITE_API_URL;
const HttpService = {
  async registrar_carrito_producto(ruta, datos) {
    let respuesta = await fetch(`${RUTA_GLOBAL}/${ruta}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(datos),
    });
    return await respuesta.json();
  },

  async inventario_inicial(ruta, datos) {
    let respuesta = await fetch(`${RUTA_GLOBAL}/${ruta}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(datos),
    });
    return await respuesta.json();
  },

  async registrar(ruta, datos) {
    try {
      let respuesta = await fetch(`${RUTA_GLOBAL}/${ruta}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(datos),
      });
      if (respuesta.ok && respuesta.status !== 204) {
        return await respuesta.json();
      } else {
        throw new Error(`Error en el servidor: ${respuesta.statusText}`);
      }
    } catch (error) {
      throw error;
    }
  },

  async obtener(ruta) {
    let respuesta = await fetch(`${RUTA_GLOBAL}/${ruta}`);
    return await respuesta.json();
  },

  async editar(ruta, datos) {
    let respuesta = await fetch(`${RUTA_GLOBAL}/${ruta}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(datos),
    });
    return await respuesta.json();
  },

  async eliminar(ruta, datos) {
    let respuesta = await fetch(`${RUTA_GLOBAL}/${ruta}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(datos),
    });
    return await respuesta.json();
  },

  async obtenerConConsultas(ruta, payload) {
  let respuesta = await fetch(`${RUTA_GLOBAL}/${ruta}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!respuesta.ok) throw new Error(`Error HTTP ${respuesta.status}`);
  return await respuesta.json();
}
};

export default HttpService;