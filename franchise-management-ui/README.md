# Franchise Management UI

Aplicacion de pagina unica creada con Angular 17 que consume el backend de Franchise API Management. Ofrece una consola empresarial para crear y mantener franquicias, sucursales y productos mientras expone metricas operativas.

## Objetivos clave

- Landing page publica que lista todas las franquicias disponibles con sus datos resumidos.
- Botones "Iniciar sesion como administrador" y "Iniciar sesion como usuario" visibles en la cabecera y en la landing.
- Panel administrativo con CRUD completo sobre franquicias, sucursales, inventario y demas recursos expuestos por la API.
- Vista autenticada de usuario (rol `user`) con permisos de solo lectura sobre franquicias, sucursales e inventario.
- Persistencia automatica en MongoDB mediante las operaciones REST del backend.
- Soporte para multiples usuarios con gestion de roles (`admin`, `user`).

## Arquitectura propuesta

- `LandingPageComponent`: carga todas las franquicias mediante `FranchiseApiService`, muestra cards, buscador y botones de inicio de sesion por rol.
- `AuthLayoutComponent` + `LoginComponent`: formulario reutilizable con modo (`admin` o `user`) que controla el endpoint de login y el manejo de credenciales/tokens.
- `AdminDashboardComponent`: shell del backoffice con sidebar (franquicias, sucursales, inventario, usuarios, reportes) exclusivo para `admin`.
- `UserExplorerComponent` (opcional): layout de lectura para usuarios autenticados que reaprovecha la landing pero habilita filtros avanzados.
- `FranchiseCrudComponent`, `BranchCrudComponent`, `InventoryCrudComponent`: formularios reactivos para crear, editar y eliminar entidades.
- `UsersCrudComponent`: gestion de usuarios y asignacion de roles.
- `AuthService`: maneja login, almacenamiento de tokens, renovacion, cierre de sesion y verificacion de roles.
- `AuthGuard`, `RoleGuard` y `PublicGuard`: protegen rutas segun autenticacion y rol requerido.
- `UsersService`: consulta y administra usuarios y roles expuestos por la API.
- Signals y `toSignal` para estados de carga (`isLoading`), datos (`franchisesSignal`), errores y mensajes de exito.

## Guia de implementacion

1. **Configurar endpoints**  
   Revisar `src/app/core/config/app-config.ts` y asegurar que `apiBaseUrl` apunta al backend (`http://localhost:8080/api/v1`). Todos los servicios deben construir las rutas a partir de esta constante para que las escrituras impacten la base en MongoDB.

2. **Generar componentes base**  
   ```
   ng g component features/landing/landing-page
   ng g component features/auth/login
   ng g module features/admin --route admin --module app
   ng g component features/admin/dashboard
   ng g component features/admin/franchises/franchise-crud
   ng g component features/admin/branches/branch-crud
   ng g component features/admin/inventory/inventory-crud
   ng g component features/admin/users/users-crud
   ng g component features/user/user-explorer
   ```
   Ajustar los paths segun el estilo del proyecto.

3. **Definir rutas**  
   En `src/app/app.routes.ts`:
   - Ruta raiz (`''`) renderiza `LandingPageComponent` (publica, sin guardas).
   - Ruta `/login/:role` renderiza `LoginComponent`; usar data `{ expectedRole: 'admin' | 'user' }`.
   - Ruta `/admin` protegida por `AuthGuard` + `RoleGuard` (requiere `admin`) que carga el `AdminDashboardComponent` con rutas hijas para franquicias, sucursales, inventario y usuarios.
   - Ruta `/explorar` protegida por `AuthGuard` (acepta `user` o `admin`) que muestra `UserExplorerComponent` con vistas de solo lectura.

4. **Implementar AuthService**  
   Crear `src/app/core/services/auth.service.ts` con metodos `login(role, credentials)`, `logout`, `getToken`, `isAuthenticated`, `hasRole`.  
   - `login` debe enviar POST a `/auth/login` o `/auth/login-admin` segun el rol (ajustar a la API real).  
   - Almacenar token JWT y metadata (rol, expiracion) en `localStorage` o `sessionStorage`.  
   - Publicar un signal `currentUserSignal` para consumo en componentes.  
   - Implementar `refreshToken` si la API lo soporta.  
   Agregar un `AuthInterceptor` que coloque `Authorization: Bearer <token>` en cada request autenticada.

5. **Configurar guards**  
   - `AuthGuard`: verifica `AuthService.isAuthenticated()` antes de permitir acceso a rutas privadas.  
   - `RoleGuard`: recibe `expectedRoles` (array) desde la configuracion de la ruta y valida `AuthService.hasRole`.  
   - `PublicGuard` (opcional): bloquea rutas de login cuando ya hay sesion activa.  
   Redirigir segun el contexto: admins a `/admin`, usuarios a `/explorar`.

6. **Construir la landing**  
   - Inyectar `FranchiseApiService` y exponer `franchisesSignal` con `toSignal(this.api.getFranchises())`.  
   - Mostrar cards con informacion basica (nombre, ubicacion, sucursales, top productos).  
   - Agregar filtros simples (por nombre/ciudad) y un componente de busqueda.  
   - Incluir botones prominentes:  
     - `Iniciar sesion como administrador` -> `routerLink="/login/admin"`.  
     - `Iniciar sesion como usuario` -> `routerLink="/login/user"`.  
   - Si hay sesion activa, reemplazar los botones por accesos directos a `/admin` o `/explorar` segun el rol del token.

7. **Pantallas de login (admin y user)**  
   - Usar un solo `LoginComponent` con `@Input() role` o leer `route.data.expectedRole`.  
   - Formularios reactivos con validacion de campos requeridos.  
   - Invocar `AuthService.login(role, formValue)`.  
   - Guardar token y roles devueltos por el backend (`response.roles`).  
   - Redirigir segun el rol: admins a `/admin`, usuarios a `/explorar`.  
   - Mostrar mensajes de error cuando la API devuelva 401/403 y proveer enlace para recuperar contraseña si aplica.

8. **Panel administrativo (rol admin)**  
   - Dashboard principal con KPIs (franquicias totales, sucursales activas, inventario).  
   - Subvistas CRUD:  
     - `franchises`: tabla + dialogos para crear, editar y eliminar.  
     - `branches`: gestion de sucursales por franquicia.  
     - `inventory`: productos por sucursal con campos de stock y precios.  
     - `users`: listado de usuarios, asignacion de roles, activacion y baja.  
   - Cada accion debe llamar al endpoint REST correspondiente (`POST /franchises`, `PUT /franchises/:id`, `DELETE`, etc.). El backend actualiza MongoDB de forma transparente.  
   - Refrescar signals tras cada operacion (`franchisesSignal.mutate` o recarga desde la API).

9. **Experiencia autenticada para usuarios (rol user)**  
   - `UserExplorerComponent` muestra la misma informacion de franquicias e inventario que la landing pero con datos adicionales (inventario detallado, sucursales).  
   - Funcionalidades solo lectura: los botones de crear, editar y eliminar deben estar ocultos o deshabilitados.  
   - Utilizar `*ngIf="authService.hasRole('admin')"` o directivas personalizadas (`hasRole`) para controlar la UI.

10. **Sincronizacion con MongoDB**  
    - Las operaciones CRUD que dispara el frontend pasan por la API; no se requiere manipular MongoDB directamente.  
    - Validar respuestas exitosas (status 200/201/204) antes de refrescar la UI.  
    - Manejar errores (409, 422, 500) con un `NotificationService` y logs en consola solo en modo desarrollo.

11. **Gestion de usuarios y administradores**  
    - `UsersService` debe exponer metodos: `getUsers()`, `createUser(payload)`, `updateUser(id, payload)`, `deleteUser(id)`, `assignRole(id, role)`.  
    - Formularios para altas: campos de nombre, email, password temporal y rol.  
    - Permitir actualizar roles desde la UI del administrador (switch o dropdown).  
    - Implementar confirmaciones antes de eliminar usuarios.  
    - Para usuarios con rol `user`, ofrecer onboarding (email con instrucciones) si la API lo soporta.  
    - Si la API maneja invitaciones, integrar el flujo para enviar invitaciones y validar tokens de registro.

12. **Control de permisos en la UI**  
    - Crear la directiva `hasRole` para mostrar u ocultar nodos segun roles.  
    - Deshabilitar rutas completas mediante `canMatch` en la configuracion de Angular Router.  
    - Proteger llamadas al backend: aun si la UI oculta botones, la API debe validar roles en cada endpoint.

13. **Pruebas y validacion**  
    - Pruebas unitarias para `AuthService`, `UsersService`, `FranchiseApiService`, guards y directivas.  
    - Simular respuestas del backend con `HttpTestingController`.  
    - Pruebas de integracion/E2E (Playwright o Cypress) cubriendo:  
      - Visualizacion publica de franquicias.  
      - Login como usuario y navegacion a `/explorar`.  
      - Login como admin, creacion de franquicia y verificacion en la landing.  
      - Cambio de rol de un usuario y verificacion de permisos.

## Scripts de npm

| Comando         | Descripcion                                         |
|-----------------|-----------------------------------------------------|
| `npm start`     | `ng serve` con recarga en vivo.                      |
| `npm run build` | Compilacion de produccion via Angular CLI.           |
| `npm run test`  | Pruebas unitarias con Karma + Jasmine.               |
| `npm run watch` | Recompila ante cambios sin iniciar el servidor.      |

## Estructura del proyecto

```
src/
+- app/
   +- app.component.*        # Layout principal (header, nav, footer)
   +- app.routes.ts          # Rutas: landing, login(user/admin), panel admin y explorer
   +- core/
      +- config/             # Configuracion global (URL base de la API)
      +- guards/             # AuthGuard, RoleGuard, PublicGuard
      +- models/             # Interfaces TypeScript para DTOs
      +- services/           # FranchiseApiService, AuthService, UsersService
      +- interceptors/       # AuthInterceptor para el token JWT
   +- shared/
      +- directives/         # Directiva hasRole y utilidades comunes
      +- components/         # Header, footer, notifications, dialogs
   +- features/
      +- landing/            # LandingPageComponent y subcomponentes publicos
      +- auth/               # LoginComponent (modo admin/user) y vistas auxiliares
      +- admin/              # Dashboard, CRUDs de franquicias, sucursales, inventario y usuarios
      +- user/               # UserExplorerComponent y vistas de solo lectura
+- styles.css                 # Tokens globales y estilos base
```

La UI se comunica con el backend exclusivamente mediante `src/app/core/services/franchise-api.service.ts` y servicios relacionados. Esto garantiza que todas las operaciones lleguen a la API y se repliquen en MongoDB. Centralizar el manejo de errores y notificaciones en un servicio (p.ej. `NotificationService`) permite ofrecer retroalimentacion consistente al usuario.

## Recomendaciones adicionales

- Externalizar la configuracion por entorno (environment.ts, .env o configuracion runtime) para apuntar a despliegues distintos.  
- Agregar autenticacion multifactor para administradores si la API lo soporta.  
- Integrar autorizacion basada en claims para futuros roles (super-admin, auditor).  
- Documentar en la API los endpoints necesarios para gestionar usuarios y roles, y mantener la UI sincronizada con cualquier cambio.
