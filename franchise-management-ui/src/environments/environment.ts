export const environment = {
  production: false,
  /**
   * API publica cuando se usa `ng serve`. Para desarrollo apuntamos directo al backend local.
   */
  apiBaseUrl: 'http://localhost:8080/api/v1',
  /**
   * Solo en desarrollo se muestran las credenciales de prueba en la pantalla de login.
   */
  showDefaultCredentials: true
};
