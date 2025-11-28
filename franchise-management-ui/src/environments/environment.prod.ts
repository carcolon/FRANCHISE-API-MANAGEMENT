export const environment = {
  production: true,
  /**
   * Usa una ruta relativa para que el frontend consuma el mismo host donde se hospeda.
   */
  apiBaseUrl: '/api/v1',
  /**
   * En produccion no se muestran usuarios/credenciales predefinidos.
   */
  showDefaultCredentials: false
};
