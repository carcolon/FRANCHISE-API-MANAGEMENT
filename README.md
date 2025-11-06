# Franchise API Management

[![CI](https://github.com/carcolon/FRANCHISE-API-MANAGEMENT/actions/workflows/ci.yml/badge.svg)](https://github.com/carcolon/FRANCHISE-API-MANAGEMENT/actions/workflows/ci.yml)

API REST para administrar franquicias, sus sucursales y el inventario de productos. Permite crear, actualizar, consultar y eliminar entidades, ademas de obtener el producto con mayor stock por sucursal.

## Funcionalidades clave

- Gestion completa de franquicias, sucursales y productos con controles de activacion/desactivacion y eliminacion cascada.
- Panel de administracion de usuarios (`/users`) accesible solo para administradores:
  - Crear cuentas con nombre completo, correo y contrasena.
  - Asignar roles `ADMIN` y/o `USER`, activar/desactivar usuarios y eliminarlos definitivamente.
  - Forzar el cambio de contrasena en el primer inicio de sesion cuando se entrega una contrasena temporal.
- Autenticacion basada en JWT con roles diferenciados: `ADMIN` (permisos totales) y `USER` (solo lectura). La interfaz es responsive: topbar, formularios y tablas se adaptan automaticamente a tablets y moviles.
- Recuperacion de contrasenas con token temporal (15 min). La UI muestra el token, permite validarlo (`POST /api/v1/auth/validate-reset-token`) y habilita el cambio de contrasena (`POST /api/v1/auth/reset-password`).
- SPA Angular que consume la API, utiliza guards para proteger rutas y oculta acciones segun el rol.

## Integracion continua

- El repositorio incluye un pipeline de GitHub Actions (`.github/workflows/ci.yml`) que ejecuta:
  - `mvn -B test` sobre el backend para validar la capa de servicios.
  - `npm ci && npm run build` dentro de `franchise-management-ui/` para comprobar que la SPA compila sin errores.
- Se ejecuta en cada *push* a las ramas main/master y en todos los *pull requests* dirigidos a ellas.
- Puedes extenderlo agregando pruebas de UI, linting o despliegues editando ese workflow.

## Autenticacion y roles

- Endpoint de login: `POST /api/v1/auth/login` (body JSON con `username` y `password`). Devuelve JWT, roles y fecha de expiracion.
- El JWT debe enviarse en la cabecera `Authorization: Bearer <token>` en cada peticion. El frontend ya lo agrega via interceptor.
- Recuperacion de contrasena: `POST /api/v1/auth/forgot-password` genera un token temporal y lo devuelve en la respuesta; puedes validarlo con `POST /api/v1/auth/validate-reset-token` y aplicar el cambio definitivo con `POST /api/v1/auth/reset-password`.
- Roles disponibles:
  - `ADMIN`: crear/eliminar franquicias y sucursales, activar/desactivar sucursales/franquicias, gestionar inventario, administrar usuarios.
  - `USER`: acceso de solo lectura a franquicias, sucursales, inventario y metricas.
- Rutas protegidas:
  - `GET /api/v1/franchises/**`: `ADMIN` o `USER`.
  - Resto de operaciones `POST/PATCH/DELETE /api/**` y `/api/v1/users/**`: solo `ADMIN`.
- Credenciales iniciales (creadas automaticamente):
  - `admin / Admin123!` (ADMIN + USER)
  - `analyst / Analyst123!` (USER)
- Puedes modificar o crear nuevas cuentas en MongoDB (coleccion `users`), las contrasenas deben almacenarse con BCrypt.

## Arquitectura y tecnologias

- Spring Boot 3.3
- Java 17
- MongoDB como base de datos documental
- Spring Data MongoDB y Spring Validation
- SpringDoc OpenAPI 3 (Swagger UI)
- Maven para construccion y gestion de dependencias
- Docker y Docker Compose (opcional) para ejecucion contenerizada
- Angular 17 (frontend en `franchise-management-ui/`)

## Cliente web (Angular)

El repositorio incluye una interfaz SaaS construida en Angular que consume esta API.

1. Cambia al directorio del frontend:
   ```bash
   cd franchise-management-ui
   ```
2. Instala dependencias y levanta el dev server:
   ```bash
   npm install
   npm start
   ```
3. La UI quedara disponible en http://localhost:4200 y apunta por defecto a http://localhost:8080/api/v1.

Para builds de produccion:
```bash
npm run build
```
Los artefactos se generan en `franchise-management-ui/dist/franchise-management-ui`.

Si la API corre en otra direccion/puerto, actualiza `src/app/core/config/app-config.ts` o exporta la variable correspondiente en tu pipeline.

> **Credenciales iniciales**: `admin / Admin123!` (rol ADMIN) y `analyst / Analyst123!` (solo lectura). Tras iniciar sesion se habilitan las funciones segun rol.

## Requisitos previos

- Java Development Kit (JDK) 17 o superior
- Maven 3.9.11 (o compatible)
- Docker Desktop 4.x (opcional, requerido para la ejecucion contenerizada)
- Cuenta o instancia de MongoDB (local, contenedor o en la nube)

## Configuracion rapida

1. Clonar el repositorio.
2. (Opcional) Crear un archivo `.env` o definir la variable `MONGODB_URI` si se desea apuntar a una instancia distinta (por ejemplo `mongodb://localhost:27017/franchise_db` o una URI con autenticacion).
3. Verificar versiones:
   ```bash
   mvn -v
   java -version
   docker --version   # solo si se utilizara Docker
   ```
4. Definir el secreto JWT (produccion): `setx JWT_SECRET "mi-secreto-ultra"` / `export JWT_SECRET="mi-secreto-ultra"` (por defecto se usa `change-me-in-production`).

## Ejecucion local con Maven

1. Asegurate de que MongoDB este disponible:
   - Con Docker Compose: `docker compose up -d mongo`
   - O bien inicia tu propia instancia de MongoDB (servicio local o remoto administrado con MongoDB Compass).
     - Instala MongoDB Community Server o usa Atlas y conectate desde Compass.
     - Crea la base `franchise_db` (se generara automaticamente la primera vez que la API escriba datos).
     - Opcional: agrega un usuario dedicado y actualiza la URI con credenciales (`mongodb://usuario:password@host:27017/franchise_db?authSource=admin`).
2. (Opcional) Define la URI personalizada:
   - PowerShell: `setx MONGODB_URI "mongodb://usuario:password@host:27017/franchise_db?authSource=admin"`
   - Bash: `export MONGODB_URI="mongodb://usuario:password@host:27017/franchise_db?authSource=admin"`
3. Inicia la aplicacion:
   ```bash
   mvn spring-boot:run
   ```
4. La API quedara disponible en http://localhost:8080.

### Parar la aplicacion

Presiona Ctrl+C en la terminal donde se ejecuto `mvn spring-boot:run`.

## Ejecucion con Docker Compose

1. Construir e iniciar todos los servicios:
   ```bash
   docker compose up --build
   ```
   - Se inicia un contenedor Mongo con persistencia en volumen.
   - Se construye la imagen de la aplicacion usando el Dockerfile y se expone en http://localhost:8080.
   - La API se conecta a Mongo con la URI `mongodb://root:example@mongo:27017/franchise_db?authSource=admin`.
2. Ver logs:
   ```bash
   docker compose logs -f franchise-api
   ```
3. Detener y limpiar recursos:
   ```bash
   docker compose down
   ```
   - Para eliminar volumenes agrega `--volumes`.

> Nota: si PowerShell indica `docker : The term 'docker' is not recognized`, agrega `C:\Program Files\Docker\Docker\resources\bin` al PATH o abre una nueva terminal con soporte Docker.

## Configuracion de la aplicacion

Valor por defecto en `src/main/resources/application.yml`:
```yaml
spring:
  data:
    mongodb:
      uri:
```
Se puede sobrescribir mediante variable de entorno `MONGODB_URI` o propiedades de linea de comando (`--spring.data.mongodb.uri=...`).

## Endpoints principales

| Recurso | Metodo | Ruta | Descripcion |
|---------|--------|------|-------------|
| Franquicias | POST | /api/v1/franchises | Crea una franquicia |
| Franquicias | GET | /api/v1/franchises | Lista todas las franquicias |
| Franquicias | GET | /api/v1/franchises/{franchiseId} | Obtiene una franquicia |
| Franquicias | PATCH | /api/v1/franchises/{franchiseId} | Actualiza el nombre |
| Franquicias | PATCH | /api/v1/franchises/{franchiseId}/status | Activa o desactiva una franquicia |
| Franquicias | DELETE | /api/v1/franchises/{franchiseId} | Elimina una franquicia (incluye sucursales y productos) |
| Sucursales | POST | /api/v1/franchises/{franchiseId}/branches | Agrega sucursal |
| Sucursales | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId} | Actualiza nombre |
| Sucursales | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId}/status | Activa o desactiva la sucursal |
| Sucursales | DELETE | /api/v1/franchises/{franchiseId}/branches/{branchId} | Elimina sucursal |
| Productos | POST | /api/v1/franchises/{franchiseId}/branches/{branchId}/products | Agrega producto |
| Productos | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId} | Actualiza nombre |
| Productos | PATCH | /api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock | Actualiza stock |
| Productos | DELETE | /api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId} | Elimina producto |
| Consultas | GET | /api/v1/franchises/{franchiseId}/branches/top-products | Producto con mayor stock por sucursal |
| Usuarios | GET | /api/v1/users | Lista usuarios registrados (solo ADMIN) |
| Usuarios | POST | /api/v1/users | Crea un nuevo usuario con roles asignados (solo ADMIN) |
| Usuarios | PATCH | /api/v1/users/{userId}/status | Activa o desactiva un usuario (solo ADMIN) |
| Usuarios | DELETE | /api/v1/users/{userId} | Elimina un usuario (solo ADMIN) |
| Auth | POST | /api/v1/auth/forgot-password | Genera token temporal de recuperacion |
| Auth | POST | /api/v1/auth/validate-reset-token | Valida vigencia del token de recuperacion |
| Auth | POST | /api/v1/auth/reset-password | Actualiza contrasena usando token valido |

## Documentacion OpenAPI

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs?format=yaml

## Pruebas

Ejecutar pruebas unitarias:
```bash
mvn test
```
Generar paquete sin pruebas (build CI/CD):
```bash
mvn -DskipTests clean package
```

## Estructura del proyecto

- Dockerfile
- docker-compose.yml
- pom.xml
- README.md
- src/
  - main/
    - java/com/franchise/api/
      - controller/
      - domain/
      - dto/
      - exception/
      - mapper/
      - security/
      - service/
    - resources/application.yml
  - test/java/com/franchise/api/
- franchise-management-ui/
  - README.md
  - src/app/... (Angular SPA)

## Solucion de problemas

- **`docker` : command not found**: agrega el directorio de Docker CLI al PATH (`C:\Program Files\Docker\Docker\resources\bin`) o reinicia Docker Desktop.
- **Fallo al conectar con MongoDB**: valida `MONGODB_URI`, la base de datos debe aceptar conexiones. Con Docker Compose, confirma con `docker compose ps` que el contenedor esta en estado `running`.
- **No puedo agregar productos a una sucursal inactiva**: vuelve a activar la sucursal con `PATCH /api/v1/franchises/{franchiseId}/branches/{branchId}/status` antes de enviar nuevas altas.
- **Puerto 8080 en uso**: cambia el puerto con `--server.port=9090` o ajusta `application.yml`.
- **Advertencia de API deprecada**: compila con `mvn compile -Xlint:deprecation` para identificar el metodo y sustituirlo en el servicio correspondiente.
